package app

import (
	"encoding/base64"
	"errors"
	"fmt"
	"math/rand"
	"net"
	"net/url"
	"strconv"
	"strings"
	"time"

	"connector/args"
	"connector/proto"
	"connector/proto/mqtt"
	"connector/util"
)

func init() {
	rand.Seed(time.Now().Unix())
}

func handleConnection(s *Server, c net.Conn, ws bool) {
	fd := util.GetConnFd(c)

	ctx := GetCtxForFd(fd)
	if ctx == nil {
		c.Close()
		return
	}

	util.Infof("Client connect, address: %s, isWs: %t, fd: %d", c.RemoteAddr(), ws, fd)

	ctx.Reset(ws, c)
	Global.Stats.IncConn()

	ctx.handleClient()

	if ctx.exitCode != ecClientClose {
		ctx.notifyDisconnect()
	}

	ctx.Mux.Lock()

	util.Infof("Client disconnect, address: %s, isWs: %t, fd: %d, reason: %s, err: %s",
		c.RemoteAddr(), ws, fd, reasons[ctx.exitCode], ctx.err)

	if ctx.productKey != "" {
		Global.Stats.DecProductConn(ctx.productKey)
	}

	ctx.destroySession()
	ctx.status = inactiveState

	ctx.Conn.Close()
	ctx.Conn = nil
	ctx.StopCh = nil
	ctx.productKey = ""
	ctx.deviceName = ""

	ctx.resetPendingRpcReqs()
	ctx.resetPendingPacket()

	ctx.Mux.Unlock()

	Global.Stats.DecConn()
}

func (ctx *ClientCtx) destroySession() {
	sessionStorage := Global.SessionStorage

	// if not connected, sessionKey & sessionValue will be nil
	if ctx.sessionKey != "" && ctx.sessionValue != "" {
		if err := sessionStorage.Clear(ctx.sessionKey, ctx.sessionValue); err != nil {
			util.Errorf("Session clear error: %v, %s, %s", err, ctx.sessionKey, ctx.sessionValue)
		}
	}
}

const leastReadTimeout = 120 * time.Second

func (ctx *ClientCtx) handleClient() {
	var err error
	de := &ctx.decoder

	for {
		select {
		case <-ctx.StopCh:
			ctx.exitCode = ecServerClose
			return
		default:
		}

		var timeout time.Duration
		if ctx.keepalive == 0 { // if not connected
			timeout = *args.ClientConnTimeout
		} else {
			timeout = time.Duration(ctx.keepalive*2) * time.Second

			if timeout < leastReadTimeout {
				timeout = leastReadTimeout
			}
		}

		ctx.Conn.SetReadDeadline(time.Now().Add(timeout))
		if err = de.Read(ctx.Conn); err != nil {
			ctx.exitCode = ecReadError
			ctx.err = err
			util.V(2).Infof("Read error: %s", err)
			return
		}

		select {
		case <-ctx.StopCh:
			ctx.exitCode = ecServerClose
			return
		default:
		}

		now := time.Now().Unix()
		if ctx.keepalive != 0 && now-ctx.lastReceived >= int64(ctx.keepalive*2) {
			ctx.exitCode = ecSessionTimeout
			ctx.err = errors.New("Session timeout")
			return
		}

		if now >= ctx.nextRefresh {
			ctx.refreshSession()
		}

		if ctx.ws {
			ret, err, payload := de.DecodeWs()
			if err != nil {
				util.Errorf("Invalid websocket protocol: %s", err)
				ctx.exitCode = ecInvalidProtocol
				ctx.err = err
				return
			}

			switch ret {
			case proto.WsFrameNotComplete:
				continue
			case proto.WsFrameError:
				ctx.exitCode = ecInvalidProtocol
				ctx.err = errors.New("Websocket frame error")
				util.Errorf("Invalid websocket protocol")
				return

			case proto.WsPing:
				util.V(3).Infof("Receive ws ping %s", string(payload))

				ctx.Mux.Lock()
				ctx.encoder.Write(payload)
				ctx.encoder.FinishWsPong()
				e := ctx.encoder.WriteTo(ctx.Conn, *args.MqttWriteTimeout)
				ctx.Mux.Unlock()

				if e != nil {
					util.V(2).Infof("Write error: %s", e)
					ctx.exitCode = ecWriteError
					ctx.err = e
					return
				}

				continue
			case proto.WsPong:
				// ignore pong packet
				util.V(3).Infof("Ignore ws pong %v", payload)
				continue
			case proto.WsClose:
				util.V(2).Infof("Client close websocket: %v", payload)
				ctx.exitCode = ecClientClose
				ctx.err = errors.New("Websocket close recieved")
				return
			case proto.WsBinayPayload:
			}
		}

		for {
			ret, err := de.DecodeMQTT()
			if err != nil {
				util.Errorf("Invalid mqtt protocol: %s", err)
				ctx.exitCode = ecInvalidProtocol
				ctx.err = err
				return
			}
			ctx.lastReceived = time.Now().Unix()

			if ret != nil {
				err := ctx.handlePacket(ret)
				if err != nil {
					ctx.err = err
					return
				}
			} else {
				break
			}
		}
	}
}

func (ctx *ClientCtx) handlePacket(p interface{}) error {
	util.V(3).Infof("Receive pkt: %s", p)
	switch pkt := p.(type) {
	case *mqtt.ConnectPacket:
		return ctx.handleConnectPacket(pkt)
	case *mqtt.PublishPacket:
		return ctx.handlePublishPacket(pkt)
	case *mqtt.PubackPacket:
		return ctx.handlePubackPacket(pkt)
	case *mqtt.SubscribePacket:
		return ctx.handleSubscribePacket(pkt)
	case *mqtt.UnsubscribePacket:
		return ctx.handleUnsubPacket(pkt)
	case *mqtt.PingreqPacket:
		return ctx.handlePingreqPacket(pkt)
	case *mqtt.DisconnectPacket:
		return ctx.handleDisconnectPacket(pkt)
	default:
		// Pubrec, Pubrel, Pubcomp, Suback, Unsuback, Pingresp
		ctx.exitCode = ecInvalidProtocol
		return errors.New("Protocol volatilation")
	}

}

var sessionPresent = true

func (ctx *ClientCtx) handleConnectPacket(p *mqtt.ConnectPacket) error {
	if ctx.hasConnected {
		util.Errorf("Receive 2nd connect packet")
		ctx.exitCode = ecInvalidProtocol
		return errors.New("Client has connected")
	}

	productKey, err := getProductKey(p.Username)

	if err != nil {
		util.Errorf("Username is invalid: %s", p.Username)
		ctx.exitCode = ecBadClientRequest
		return err
	}

	deviceName := string(p.ClientId)
	token := string(p.Password)
	cleanSession := bool(p.Flags.CleanSession())
	willFlag := bool(p.Flags.WillFlag())

	rc := Global.RpcClient
	code := rc.Connect(deviceName, productKey, token, cleanSession)
	connack := mqtt.NewConnackPacket(sessionPresent, byte(code))

	// Connack is fixed size, so encode will always succeed
	// encode error check is not necessary
	connack.Encode(&ctx.encoder)

	// Now there is only one goroutine to access ctx, no mutex required.
	e := ctx.encoder.WriteTo(ctx.Conn, *args.MqttWriteTimeout)

	if e != nil {
		util.V(2).Infof("Write error: %s", e)
		ctx.exitCode = ecWriteError
		return e
	}

	if code != mqtt.RC_ACCEPTED {
		util.Errorf("Mqtt connect error: %d", code)
		ctx.exitCode = ecBadClientRequest
		return errors.New("Connect error")
	}
	ctx.hasConnected = true
	ctx.keepalive = int(p.Keepalive)
	ctx.productKey = productKey
	ctx.deviceName = deviceName
	Global.Stats.IncProductConn(productKey)
	ctx.sessionValue = generateValue(Global.SessionPrefix, ctx.Fd)
	ctx.sessionKey = generateKey(ctx.productKey, ctx.deviceName)
	ctx.cleanSession = cleanSession
	ctx.willFlag = willFlag

	if willFlag {
		ctx.willTopic = string(p.WillTopic)
		ctx.willQos = int8(p.Flags.WillQos())
		ctx.willMessageB64 = base64.StdEncoding.EncodeToString(p.WillMessage)
	}

	// After activate, ctx may be accessed by multi goroutines
	ctx.Mux.Lock()
	ctx.status = activeState
	ctx.Mux.Unlock()

	ctx.refreshSession()

	return nil
}

const clientSessionKeyNamespace = "C"

func generateKey(productKey, deviceName string) string {
	return fmt.Sprintf("%s:%s:%s", clientSessionKeyNamespace, productKey, deviceName)
}

func generateValue(prefix string, fd int) string {
	return fmt.Sprintf("%s:%d", prefix, fd)
}

func getProductKey(username string) (string, error) {
	ss := strings.Split(username, ":")
	if len(ss) == 1 {
		return username, nil
	} else if len(ss) == 2 {
		return ss[0], nil
	}

	return "", errors.New("Invalid username")
}

const randRange = 4 * 60 // 4 minutes

func refreshDelay() int64 {
	return int64(*args.RedisSessionRefresh/time.Second) + int64(rand.Int()%randRange)
}

const kickPath = "/v3/kick"

func kickDupConnection(sessionValue string, productKey string, deviceName string, oldSessionValue string) {
	if strings.Count(oldSessionValue, ":") != 2 {
		util.Warningf("Kick value error: %s %s %s", productKey, deviceName, oldSessionValue)
		return
	}

	d := strings.LastIndex(oldSessionValue, ":")

	// Same server
	if strings.HasPrefix(sessionValue, oldSessionValue[:d+1]) && sessionValue != oldSessionValue {
		fd, e := strconv.ParseInt(oldSessionValue[d+1:], 10, 32)
		if e == nil {
			ctx := GetCtxForFd(fd)
			if ctx != nil {
				ctx.Kick(deviceName, productKey)
			}
		}
		return
	}

	reqUrl := &url.URL{
		Host:   oldSessionValue[:d],
		Scheme: "http",
		Path:   kickPath,
	}
	urlStr := reqUrl.String()

	params := url.Values{}

	params.Add("deviceName", deviceName)
	params.Add("productKey", productKey)
	params.Add("fd", oldSessionValue[d+1:])

	c := util.GetHttpClient()
	resp, err := c.PostForm(urlStr, params)
	if err != nil {
		util.Warningf("Kick resp error: %v %s %s %s", err, productKey, deviceName, oldSessionValue)
		return
	}
	resp.Body.Close()
	// return code is ignored
}

func (ctx *ClientCtx) refreshSession() {
	ctx.Mux.RLock()
	if ctx.status != activeState {
		ctx.Mux.RUnlock()
		return
	}

	sessions := make(map[string]string, 1)
	sessVal := ctx.sessionValue
	if ctx.productKey != "" {
		sessions[ctx.productKey] = ctx.deviceName
	}
	ctx.Mux.RUnlock()

	sessionStorage := Global.SessionStorage

	for productKey, deviceName := range sessions {
		util.V(3).Infof("Refresh session: %s, %s", productKey, deviceName)

		k := generateKey(productKey, deviceName)
		oldSessionValue, err := sessionStorage.Refresh(k, sessVal, *args.RedisSessionTimeout)
		if err != nil {
			util.Errorf("Session refresh error: %v, %s, %s", err, k, sessVal)
		} else if oldSessionValue != "" {
			util.Errorf("Session duplication, kick former, %s, %s, former: %s", k, sessVal, oldSessionValue)
			kickDupConnection(sessVal, productKey, deviceName, oldSessionValue)
		}
	}
	ctx.nextRefresh = time.Now().Unix() + refreshDelay()
}

func (ctx *ClientCtx) publishForward(p *mqtt.PublishPacket) error {
	if p.Header.Qos() == 2 {
		util.Errorf("Qos 2 is not supported")
		return errors.New("Qos 2 is not supported")
	} else if p.Header.Qos() == 1 {
		puback := mqtt.NewPubackPacket(p.PacketId)

		ctx.Mux.Lock()

		// puback is fixed size
		// encode error check is not necessary
		puback.Encode(&ctx.encoder)
		e := ctx.encoder.WriteTo(ctx.Conn, *args.MqttWriteTimeout)

		ctx.Mux.Unlock()

		if e != nil {
			util.V(2).Infof("Write error: %s", e)
			ctx.exitCode = ecWriteError
			return e
		}
	}

	rc := Global.RpcClient
	rc.Publish(ctx.productKey, ctx.deviceName, p.Topic, p.Header.Qos(), p.Payload)
	return nil
}

const rpcRespTopicPrefix = "rrpc/response/"

func (ctx *ClientCtx) handlePublishPacket(p *mqtt.PublishPacket) error {
	if !ctx.hasConnected {
		util.Error("Publish before connect")
		ctx.exitCode = ecInvalidProtocol
		return errors.New("Client has not been connected before publish")
	}

	if strings.HasPrefix(p.Topic, rpcRespTopicPrefix) {
		if p.Header.Qos() != 0 {
			util.Errorf("Qos of rpc resp should be 0")
			ctx.exitCode = ecBadClientRequest
			return errors.New("Qos of rpc resp is not 0")
		}

		reqIdStr := p.Topic[len(rpcRespTopicPrefix):]
		if reqId, e := strconv.Atoi(reqIdStr); e == nil {
			resp := base64.StdEncoding.EncodeToString(p.Payload)
			ctx.RespToPendingRpcReq(uint16(reqId), rcRequestOK, resp)
			return nil
		} else {
			util.Errorf("Topic %s is invalid, invalid rpc request id", p.Topic)
			ctx.exitCode = ecBadClientRequest
			return errors.New("Client publish invalid topic")
		}

	}

	return ctx.publishForward(p)
}

func (ctx *ClientCtx) handlePubackPacket(p *mqtt.PubackPacket) error {
	if !ctx.hasConnected {
		util.Error("Receive puback before connect")
		ctx.exitCode = ecInvalidProtocol
		return errors.New("Client has not been connected before")
	}
	util.V(3).Infof("Push ack packet ok, packetId: %d", p.PacketId)
	ctx.AckPacket(p.PacketId, rcRequestOK)

	return nil
}

func (ctx *ClientCtx) handleSubscribePacket(p *mqtt.SubscribePacket) error {
	if !ctx.hasConnected {
		util.Error("Receive subscribe before connect")
		ctx.exitCode = ecInvalidProtocol
		return errors.New("Client has not been connected before")
	}

	rc := Global.RpcClient
	codes := rc.Subscribe(ctx.deviceName, ctx.productKey, p.Topics, p.Qoss)

	// if internal call error, set codes to -2
	if codes == nil {
		codes = make([]int8, len(p.Qoss))
		for i := range codes {
			codes[i] = -2
		}
	}

	ack := mqtt.NewSubackPacket(p.PacketId, codes)

	ctx.Mux.Lock()

	// we limit max topic num per sub is 16
	// so suback payload is small, encode will always succeed
	ack.Encode(&ctx.encoder)
	e := ctx.encoder.WriteTo(ctx.Conn, *args.MqttWriteTimeout)

	ctx.Mux.Unlock()

	if e != nil {
		util.V(2).Infof("Write error: %s", e)
		ctx.exitCode = ecWriteError
		return e
	}
	return nil
}

func (ctx *ClientCtx) handleUnsubPacket(p *mqtt.UnsubscribePacket) error {
	if !ctx.hasConnected {
		util.Error("Receive unsub before connect")
		ctx.exitCode = ecInvalidProtocol
		return errors.New("Client has not been connected before")
	}

	rc := Global.RpcClient
	e := rc.Unsubscribe(ctx.deviceName, ctx.productKey, p.Topics)
	if e == nil {
		ack := mqtt.NewUnsubackPacket(p.PacketId)

		ctx.Mux.Lock()
		// unsuback is fixed size and small,
		// encode will always succeed
		ack.Encode(&ctx.encoder)
		e = ctx.encoder.WriteTo(ctx.Conn, *args.MqttWriteTimeout)
		ctx.Mux.Unlock()

		if e != nil {
			util.V(2).Infof("Write error: %s", e)
			ctx.exitCode = ecWriteError
			return e
		}
	}

	return nil
}

func (ctx *ClientCtx) handlePingreqPacket(p *mqtt.PingreqPacket) error {
	pingresp := mqtt.NewPingrespPacket()

	ctx.Mux.Lock()

	pingresp.Encode(&ctx.encoder)
	e := ctx.encoder.WriteTo(ctx.Conn, *args.MqttWriteTimeout)

	ctx.Mux.Unlock()

	if e != nil {
		util.V(2).Infof("Write error: %s", e)
		ctx.exitCode = ecWriteError
		return e
	}
	return nil
}

func (ctx *ClientCtx) handleDisconnectPacket(p *mqtt.DisconnectPacket) error {
	ctx.exitCode = ecClientClose
	return errors.New("Client close the connection")
}

func (ctx *ClientCtx) Kick(deviceName string, productKey string) {
	util.V(3).Infof("Handle kick request: %s, %s", deviceName, productKey)

	ctx.Mux.Lock()
	defer ctx.Mux.Unlock()

	if ctx.status != activeState {
		return
	}

	if deviceName == ctx.deviceName && productKey == ctx.productKey {
		select {
		case <-ctx.StopCh:
			return
		default:
			close(ctx.StopCh)
			ctx.Conn.SetDeadline(time.Now())
		}

	}
}

const defaultDup = false
const defaultRetain = false

func (ctx *ClientCtx) publish(p *mqtt.PublishPacket, deviceName string, productKey string) int {
	if e := p.Encode(&ctx.encoder); e != nil {
		ctx.encoder.ResetState()
		util.Errorf("Encode payload error: %v, %s, %s", e, deviceName, productKey)
		return rcEncodeError
	}

	if e := ctx.encoder.WriteTo(ctx.Conn, *args.MqttWriteTimeout); e != nil {
		util.Errorf("Push message error: %v, %s, %s", e, deviceName, productKey)
		return rcNetworkError
	}

	return rcRequestOK
}

func (ctx *ClientCtx) PublishMessage(deviceName string, productKey string,
	msgId string, topic string, qos int8) int {

	util.V(3).Infof("Handle publish message, deviceName %s, productKey: %s, msgId: %s", deviceName, productKey, msgId)
	msg, err := getMessageDecodeBase64(msgId)
	if err != nil {
		util.Errorf("Get message error: %v", err)
		return rcGetMessageError
	}

	h := mqtt.NewPublishPacketTypeHeader(qos, defaultDup, defaultRetain)
	pktId := ctx.GetPacketId()

	p := mqtt.NewPublishPacket(h, topic, pktId, msg)

	var isOnline bool
	var ret int = rcRequestOK
	var pf chan int = nil

	ctx.Mux.Lock()

	isOnline = ctx.IsClientMatchedMasterLocked(deviceName, productKey)

	if isOnline {
		if qos == 1 {
			pf = ctx.AddPendingPacket(pktId)
		}

		ret = ctx.publish(p, deviceName, productKey)

		if ret != rcRequestOK {
			util.Errorf("publish to client error, %s, %s", deviceName, productKey)
			if qos == 1 {
				ctx.AckPacket(pktId, ret)
			}
		}
	} else {
		util.Errorf("Client is not online: %s, %s", deviceName, productKey)
		ret = rcClientNotOnline
	}

	ctx.Mux.Unlock()

	if ret != rcRequestOK {
		return ret
	}

	if qos == 1 {
		select {
		case ret := <-pf:
			return ret
		case <-time.After(*args.WaitAckTimeout):
			ctx.AckPacket(pktId, rcRequstTimeout)
			return rcRequstTimeout
		}
	}

	return rcRequestOK
}

const rpcTopicPrefix = "rrpc/request/"
const rpcQos = 0

func (ctx *ClientCtx) RpcToClient(deviceName string, productKey string, content string, timeoutMs int) (code int, resp string) {
	util.V(3).Infof("Handle rpc request to client, deviceName: %s, productKey: %s, content: %s",
		deviceName, productKey, content)

	var payload []byte
	var e error

	if content != "" {
		payload, e = base64.StdEncoding.DecodeString(content)
		if e != nil {
			return rcRpcContentError, ""
		}
	}

	h := mqtt.NewPublishPacketTypeHeader(rpcQos, defaultDup, defaultRetain)
	pktId := ctx.GetPacketId()

	topic := fmt.Sprintf("%s%d", rpcTopicPrefix, pktId)
	p := mqtt.NewPublishPacket(h, topic, pktId, payload)

	var isOnline bool
	var ret int = rcRequestOK
	var rc *RpcRequestCtx

	ctx.Mux.Lock()

	isOnline = ctx.IsClientMatchedMasterLocked(deviceName, productKey)

	if isOnline {
		rc = ctx.AddPendingRpcReq(pktId)

		ret = ctx.publish(p, deviceName, productKey)

		if ret != rcRequestOK {
			util.Errorf("publish to client error, %s, %s", deviceName, productKey)
			ctx.RespToPendingRpcReq(pktId, ret, "")
		}
	} else {
		util.Errorf("Client is not online: %s, %s", deviceName, productKey)
		ret = rcClientNotOnline
	}

	ctx.Mux.Unlock()

	if ret != rcRequestOK {
		return ret, ""
	}

	select {
	case ret := <-rc.Done:
		return ret, rc.Response
	case <-time.After(time.Duration(timeoutMs) * time.Millisecond):
		util.V(3).Infof("Rpc response timeout")
		ctx.RespToPendingRpcReq(pktId, rcRequstTimeout, "")
		return rcRequstTimeout, ""
	}
}

func (ctx *ClientCtx) notifyDisconnect() {
	if !ctx.hasConnected {
		return
	}

	rc := Global.RpcClient
	rc.Disconnect(ctx.productKey, ctx.deviceName, ctx.cleanSession, ctx.willFlag, ctx.willTopic, ctx.willQos, ctx.willMessageB64)
}
