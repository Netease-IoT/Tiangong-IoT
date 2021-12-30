package mqtt

import (
	"encoding/binary"
	"errors"
	"fmt"
	"io"

	"connector/proto"
	"connector/util"
)

const (
	parseMQTTTypeState = iota
	parseMQTTLengthState
	parseMQTTPayloadState

	parseWsFlagsState
	parseWsFrameLengthState
	parseWsFramePayloadState
)

type Decoder struct {
	dat   [blockSize]byte
	buf   []byte
	end   int
	start int

	mqttStart int
	mqttEnd   int

	mqttState    int
	mqttLen      int
	mqttLenBytes int
	packetType   PacketTypeHeader

	isWs bool

	wsState    int
	wsFrameLen int
	wsLenBytes int
	mask       bool
	opcode     byte
}

func (de *Decoder) Reset(isWs bool) {
	if len(de.buf) == largeBlockSize {
		putLargeBuf(de.buf)
	}

	de.buf = de.dat[:]
	de.end = 0
	de.start = 0

	de.mqttStart = 0
	de.mqttEnd = 0
	de.mqttState = parseMQTTTypeState

	de.isWs = isWs

	if de.isWs {
		de.wsState = parseWsFlagsState
	}
}

var (
	rdPacketIsTooLargeError    = errors.New("Read: Packet is too large, 16k buffer is full")
	wsFrameTooLargeError       = errors.New("WS: frame is too large")
	wsReservedBitsError        = errors.New("WS: reserved bits are not set to 0")
	wsUnsupportedOpError       = errors.New("WS: unsupported op code")
	wsMalformedLengthError     = errors.New("WS: length less than 126 use two bytes")
	wsControlMessageInOneFrame = errors.New("WS: control message should in one frame")

	mqttPacketTooLargeError  = errors.New("MQTT: packet too large")
	mqttMalformedLengthError = errors.New("MQTT: malformed packet length")
)

func (de *Decoder) Read(r io.Reader) error {
	if de.end == cap(de.buf) { // buffer is full, use large buffer
		if cap(de.buf) == blockSize {
			b := getLargeBuf()
			copy(b, de.buf)
			de.buf = b
		} else {
			return rdPacketIsTooLargeError
		}
	}

	// if at least 1k space is remaining when using small buffer
	// we can release large buffer and use small buffer
	if cap(de.buf) == largeBlockSize && de.end < blockSize-1024 {
		n := de.dat[:]
		copy(n, de.buf)
		putLargeBuf(de.buf)
		de.buf = n
	}

	n, err := r.Read(de.buf[de.end:])
	if n <= 0 {
		return err
	}

	de.end += n

	if !de.isWs {
		de.mqttEnd = de.end
	}
	return nil
}

func isValidOpcode(opcode byte) bool {
	if opcode == 0x0 || // continuation
		opcode == 0x2 || // binary frame
		opcode == 0x8 || // close
		opcode == 0x9 || // ping
		opcode == 0xa { // pong
		return true
	}
	return false
}

func isControlPacket(opcode byte) bool {
	if opcode == 0x2 || opcode == 0x0 { // binary frame, continuation frame
		return false
	}
	return true
}

func (de *Decoder) wsFlagsState() error {
	f := de.buf[de.start]
	de.start += 1

	if f&0x70 != 0 {
		return wsReservedBitsError
	}

	de.opcode = f & 0xf
	if !isValidOpcode(de.opcode) {
		return wsUnsupportedOpError
	}

	// NOTE: Here, we violate RFC 6455
	// We limit control message must be in `ONE` frame
	// This limit almost does no matter
	fin := f&0x8 != 0
	if isControlPacket(de.opcode) && !fin {
		return wsControlMessageInOneFrame
	}

	// we ignore FIN bit here, and treat websocket as a stream-like protocol
	de.wsLenBytes = 0

	de.wsState = parseWsFrameLengthState
	return nil
}

func (de *Decoder) wsFrameLengthState() (int, error) {
	if de.wsLenBytes == 0 {
		f := de.buf[de.start]
		de.mask = (f&0x80 != 0)
		plen := f & 0x7f
		if plen < 126 {
			de.wsFrameLen = int(plen)
			if de.mask {
				de.wsFrameLen += 4
			}
			de.start += 1
			de.wsState = parseWsFramePayloadState
		} else if plen == 126 {
			de.wsLenBytes = 2
			de.start += 1 // skip 126
		} else {
			// the largest packet is 16K, so four bytes is too large
			util.Error("Websocket length should not be 4 bytes encoding")
			return proto.WsFrameError, wsFrameTooLargeError
		}

	}
	if de.wsLenBytes == 2 {
		if de.end-de.start < 2 {
			return proto.WsFrameNotComplete, nil
		} else {
			plen0 := de.buf[de.start]
			plen1 := de.buf[de.start+1]
			de.start += 2

			de.wsFrameLen = int(plen0)<<8 + int(plen1)
			if de.wsFrameLen < 126 {
				util.Error("Websocket length invalid, less than 126 but 2 bytes")
				return proto.WsFrameError, wsMalformedLengthError
			}
			if de.mask {
				de.wsFrameLen += 4
			}

			if de.wsFrameLen > maxPacketLimit {
				util.Error("Websocket length too large")
				return proto.WsFrameError, wsFrameTooLargeError
			}

			de.wsState = parseWsFramePayloadState
		}
	}
	return proto.WsContinue, nil
}

func (de *Decoder) wsFramePayloadState() (int, error, []byte) {
	if de.end-de.start >= de.wsFrameLen {
		var realFrameLen int = de.wsFrameLen
		if de.mask {
			proto.UmaskWs(de.buf[de.start : de.start+de.wsFrameLen])

			// skip mask key
			de.start += 4
			realFrameLen -= 4
		}

		de.wsState = parseWsFlagsState

		if isControlPacket(de.opcode) {
			ctlPayload := de.buf[de.start : de.start+realFrameLen]
			de.start += realFrameLen

			// WsPing, WsPong, WsClose
			return int(de.opcode), nil, ctlPayload
		} else {
			// fill mqtt frame
			n := copy(de.buf[de.mqttEnd:], de.buf[de.start:de.start+realFrameLen])
			de.mqttEnd += n
			de.start += realFrameLen
			return proto.WsBinayPayload, nil, nil
		}
	} else {
		return proto.WsFrameNotComplete, nil, nil
	}

}

func (de *Decoder) DecodeWs() (int, error, []byte) {
	for de.start < de.end {
		switch de.wsState {
		case parseWsFlagsState:
			e := de.wsFlagsState()
			if e != nil {
				return proto.WsFrameError, e, nil
			}
		case parseWsFrameLengthState:
			r, e := de.wsFrameLengthState()
			if r != proto.WsContinue {
				return r, e, nil
			}
		case parseWsFramePayloadState:
			return de.wsFramePayloadState()
		}

	}

	if de.wsState == parseWsFramePayloadState && de.wsFrameLen == 0 {
		return de.wsFramePayloadState()
	}

	return proto.WsFrameNotComplete, nil, nil
}

func (de *Decoder) typeState() error {
	if de.mqttStart != 0 { // do a compact
		copy(de.buf, de.buf[de.mqttStart:])
		de.mqttEnd -= de.mqttStart
		de.start -= de.mqttStart
		de.end -= de.mqttStart
		de.mqttStart = 0
	}

	de.packetType = PacketTypeHeader(de.buf[de.mqttStart])

	// simple check, fail fast
	// both 0 and 15 are reserved, MQTT v3.1.1 2.2.1
	if de.packetType>>4 == 0 || de.packetType>>4 == 15 {
		return errors.New("Unknown Packet Type")
	}

	de.mqttStart += 1
	de.mqttLen = 0
	de.mqttLenBytes = 0
	de.mqttState = parseMQTTLengthState
	return nil
}

func (de *Decoder) lengthState() error {
	for de.mqttStart < de.mqttEnd {
		h := de.buf[de.mqttStart]
		de.mqttStart += 1
		de.mqttLen = de.mqttLen + int(h&0x7f)*(1<<uint(de.mqttLenBytes*7))
		if de.mqttLen > maxPacketLimit {
			util.Error("Mqtt length is too large")
			return mqttPacketTooLargeError
		}

		de.mqttLenBytes += 1

		if h&0x80 == 0 {
			if de.mqttLenBytes > 1 && h == 0 { // something like 0x80,0x80,0x00, malformed packet length
				util.Error("Mqtt length is malformed, like 0x80,0x80,0x00")
				return mqttMalformedLengthError
			}

			de.mqttState = parseMQTTPayloadState
			return nil
		} else {
			if de.mqttLenBytes == 4 {
				util.Error("Mqtt length bytes should be less than 4")
				return mqttMalformedLengthError
			}
		}

	}
	return nil
}

func (de *Decoder) payloadState() (interface{}, error) {
	if de.mqttEnd-de.mqttStart >= de.mqttLen {
		ret, err := unpack(de.packetType, de.buf[de.mqttStart:de.mqttStart+de.mqttLen])
		if err != nil {
			return nil, err
		}

		de.mqttStart = de.mqttStart + de.mqttLen
		de.mqttLenBytes = 0
		de.mqttLen = 0
		de.packetType = 0
		de.mqttState = parseMQTTTypeState

		// compact should be done at next packet decode beginning
		return ret, nil
	}
	return nil, nil
}

func (de *Decoder) DecodeMQTT() (interface{}, error) {
	for de.mqttStart < de.mqttEnd {
		switch de.mqttState {
		case parseMQTTTypeState:
			if e := de.typeState(); e != nil {
				return nil, e
			}

		case parseMQTTLengthState:
			if e := de.lengthState(); e != nil {
				return nil, e
			}
		case parseMQTTPayloadState:
			return de.payloadState()
		}
	}

	if de.mqttState == parseMQTTPayloadState && de.mqttLen == 0 {
		return de.payloadState()
	}

	return nil, nil
}

func unpack(p PacketTypeHeader, payload []byte) (interface{}, error) {
	switch {
	case IsPingreqPacket(p):
		return DecodePingreqPacket(payload)
	case IsPingrespPacket(p):
		return DecodePingrespPacket(payload)
	case IsDisconnectPacket(p):
		return DecodeDisconnectPacket(payload)
	case IsConnectPacket(p):
		return DecodeConnectPacket(payload)
	case IsConnackPacket(p):
		return DecodeConnackPacket(payload)
	case IsPublishPacket(p):
		return DecodePublishPacket(p, payload) // Qos used by Publish Packet
	case IsPubackPacket(p):
		return DecodePubackPacket(payload)
	case IsPubrecPacket(p):
		return DecodePubrecPacket(payload)
	case IsPubrelPacket(p):
		return DecodePubrelPacket(payload)
	case IsPubcompPacket(p):
		return DecodePubcompPacket(payload)
	case IsSubscribePacket(p):
		return DecodeSubscribePacket(payload)
	case IsSubackPacket(p):
		return DecodeSubackPacket(payload)
	case IsUnsubscribePacket(p):
		return DecodeUnsubscribePacket(payload)
	case IsUnsubackPacket(p):
		return DecodeUnsubackPacket(payload)
	}
	return nil, fmt.Errorf("Unknown packet type: %s", p)
}

var (
	strError   = errors.New("Decode string error")
	u16Error   = errors.New("Decode u16 error")
	byteError  = errors.New("Decode byte error")
	bytesError = errors.New("Decode bytes error")
)

func decodeString(src []byte, pos int) (string, int, error) {
	var err error
	var sz uint16

	if sz, pos, err = decodeUint16(src, pos); err != nil {
		return "", -1, err
	}

	if pos+int(sz) <= len(src) {
		s := string(src[pos : pos+int(sz)])
		pos += int(sz)
		return s, pos, nil
	}
	return "", -1, strError
}

func decodeByte(src []byte, pos int) (byte, int, error) {
	if pos+1 <= len(src) {
		b := src[pos]
		pos += 1
		return b, pos, nil
	}
	return 0, -1, byteError
}

func decodeInt8(src []byte, pos int) (int8, int, error) {
	r, p, e := decodeByte(src, pos)
	return int8(r), p, e
}

func decodeBytes(src []byte, pos int) ([]byte, int, error) {
	var sz uint16
	var err error

	if sz, pos, err = decodeUint16(src, pos); err != nil {
		return nil, -1, err
	}

	if pos+int(sz) <= len(src) {
		ret := src[pos : pos+int(sz)]
		pos += int(sz)
		return ret, pos, nil
	}

	return nil, -1, bytesError
}

func decodeUint16(src []byte, pos int) (uint16, int, error) {
	if pos+2 <= len(src) {
		u := binary.BigEndian.Uint16(src[pos:])
		pos += 2
		return u, pos, nil
	}
	return 0, -1, u16Error
}
