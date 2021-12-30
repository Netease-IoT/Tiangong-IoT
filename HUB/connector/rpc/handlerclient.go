package rpc

import (
	"context"
	"encoding/base64"
	"fmt"
	"time"

	"connector/discovery"
	"connector/pb"
	"connector/proto/mqtt"
	"connector/util"

	"google.golang.org/grpc"
)

type HandlerClient interface {
	Connect(deviceName string, productKey string, token string, cleanSession bool) (code int)
	Subscribe(deviceName string, productKey string, topicFilter []string, qoss []int8) (code []int8)
	Unsubscribe(deviceName string, productKey string, topicFilter []string) (e error)
	Publish(productKey string, deviceName string, topic string, qos int8, payload []byte) error
	Disconnect(productKey string, deviceName string, cleanSession bool, willFlag bool, willTopic string, willQos int8, willMessage string) error
}

type HandlerClientOption struct {
	ServiceDiscovery discovery.ServiceDiscovery
	ServiceName      string
}

type hc struct {
	options *HandlerClientOption
}

var _ HandlerClient = (*hc)(nil)

func NewHandlerClient(hco *HandlerClientOption) HandlerClient {
	return &hc{
		options: hco,
	}
}

func (m *hc) Connect(deviceName string, productKey string, token string, cleanSession bool) (code int) {
	s := m.getAvailServer()
	if s == "" {
		util.Errorf("Can't get avail rpc server")
		return mqtt.RC_ERR_REFUSED_SERVER_UNAVAILABLE
	}

	conn, err := grpc.Dial(s, grpc.WithInsecure())
	defer conn.Close()
	if err != nil {
		util.Errorf("Can't connect rpc server: %v", err)
		return mqtt.RC_ERR_REFUSED_SERVER_UNAVAILABLE
	}

	c := pb.NewMQTTGRPCClient(conn)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	r, err := c.Connect(ctx, &pb.ConnReq{
		Cred:         &pb.DeviceCred{ProductKey: productKey, DeviceName: deviceName},
		Token:        token,
		CleanSession: cleanSession,
	})

	if err != nil || !r.GetSucceed() {
		util.Errorf("Rpc Connect return error: %v", err)
		return mqtt.RC_ERR_REFUSED_ID_REJECTED
	}

	return mqtt.RC_ACCEPTED
}

func (m *hc) Subscribe(deviceName string, productKey string, topicFilter []string, qoss []int8) (code []int8) {
	s := m.getAvailServer()
	if s == "" {
		util.Errorf("Can't get avail rpc server")
		return nil
	}

	conn, err := grpc.Dial(s, grpc.WithInsecure())
	defer conn.Close()
	if err != nil {
		util.Errorf("Can't connect rpc server: %v", err)
		return nil
	}

	c := pb.NewMQTTGRPCClient(conn)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	reqQoss := make([]int32, len(qoss))
	for i := range qoss {
		reqQoss[i] = int32(qoss[i])
	}

	r, err := c.Subscribe(ctx, &pb.SubReq{
		Cred:         &pb.DeviceCred{ProductKey: productKey, DeviceName: deviceName},
		TopicFilters: topicFilter,
		Qoss:         reqQoss,
	})

	if err != nil {
		util.Errorf("Rpc Subscribe return error: %v", err)
		return nil
	}

	resQoss := r.GetQoss()
	if len(resQoss) != len(qoss) {
		return nil
	}

	grantedQos := make([]int8, len(qoss))
	for i := range resQoss {
		grantedQos[i] = int8(resQoss[i])
	}

	return grantedQos
}

func (m *hc) Unsubscribe(deviceName string, productKey string, topicFilter []string) error {
	s := m.getAvailServer()
	if s == "" {
		util.Errorf("Can't get avail rpc server")
		return fmt.Errorf("No available rpc server")
	}

	conn, err := grpc.Dial(s, grpc.WithInsecure())
	defer conn.Close()
	if err != nil {
		util.Errorf("Can't connect rpc server: %v", err)
		return fmt.Errorf("Connect rpc server failed")
	}

	c := pb.NewMQTTGRPCClient(conn)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	r, err := c.Unsubscribe(ctx, &pb.UnsubReq{
		Cred:         &pb.DeviceCred{ProductKey: productKey, DeviceName: deviceName},
		TopicFilters: topicFilter,
	})

	if err != nil || !r.GetSucceed() {
		util.Errorf("Rpc Unsubscribe return error: %v", err)
		return fmt.Errorf("Rpc Unsubsribe failed")
	}

	return nil
}

func (m *hc) Publish(productKey string, deviceName string, topic string, qos int8, payload []byte) error {
	s := m.getAvailServer()
	if s == "" {
		util.Errorf("Can't get avail rpc server")
		return fmt.Errorf("No available rpc server")
	}

	conn, err := grpc.Dial(s, grpc.WithInsecure())
	defer conn.Close()
	if err != nil {
		util.Errorf("Can't connect rpc server: %v", err)
		return fmt.Errorf("Connect rpc server failed")
	}

	c := pb.NewMQTTGRPCClient(conn)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	r, err := c.Publish(ctx, &pb.PubReq{
		Cred:      &pb.DeviceCred{ProductKey: productKey, DeviceName: deviceName},
		MsgId:     util.GenerateUUID(),
		Topic:     topic,
		Qos:       int32(qos),
		Content:   base64.StdEncoding.EncodeToString(payload),
		Timestamp: time.Now().UnixNano() / 1000000,
	})

	if err != nil || !r.GetSucceed() {
		util.Errorf("Rpc Publish return error: %v", err)
		return fmt.Errorf("Rpc Publish failed")
	}

	return nil
}

func (m *hc) Disconnect(productKey string, deviceName string, cleanSession bool, willFlag bool, willTopic string, willQos int8, willMessage string) error {
	s := m.getAvailServer()
	if s == "" {
		util.Errorf("Can't get avail rpc server")
		return fmt.Errorf("No available rpc server")
	}

	conn, err := grpc.Dial(s, grpc.WithInsecure())
	defer conn.Close()
	if err != nil {
		util.Errorf("Can't connect rpc server: %v", err)
		return fmt.Errorf("Connect rpc server failed")
	}

	c := pb.NewMQTTGRPCClient(conn)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	msgId := ""
	if willFlag {
		msgId = util.GenerateUUID()
	}

	r, err := c.Disconnect(ctx, &pb.DiscReq{
		Cred:         &pb.DeviceCred{ProductKey: productKey, DeviceName: deviceName},
		CleanSession: cleanSession,
		HasWill:      willFlag,
		MsgId:        msgId,
		WillTopic:    willTopic,
		WillQos:      int32(willQos),
		WillMessage:  willMessage,
	})

	if err != nil || !r.GetSucceed() {
		util.Errorf("Rpc Disconnect return error: %v", err)
		return fmt.Errorf("Rpc Disconnect failed")
	}

	return nil
}

func (m *hc) getAvailServer() string {
	if m.options.ServiceDiscovery != nil {
		return m.options.ServiceDiscovery.GetServerByService(m.options.ServiceName)
	}
	util.V(5).Infof("Service discovery is nil, use ServiceName")
	return m.options.ServiceName
}
