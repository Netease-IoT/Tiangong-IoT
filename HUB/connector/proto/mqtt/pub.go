package mqtt

import (
	"errors"
	"fmt"
)

type PublishPacket struct {
	Header PacketTypeHeader

	Topic    string
	PacketId uint16

	Payload []byte
}

const maxPayloadOutput = 125
const dot = 0x2E

const publishMask = PT_PUBLISH << 4

func IsPublishPacket(p PacketTypeHeader) bool {
	return publishMask^(p&0xf0) == 0
}

func NewPublishPacketTypeHeader(qos int8, dup bool, retain bool) PacketTypeHeader {
	x := publishMask | uint8(qos)<<1
	if dup {
		x |= 0x8
	}
	if retain {
		x |= 1
	}
	return PacketTypeHeader(x)
}

func NewPublishPacket(h PacketTypeHeader, topic string, packetId uint16, payload []byte) *PublishPacket {
	return &PublishPacket{
		Header:   h,
		Topic:    topic,
		PacketId: packetId,
		Payload:  payload,
	}

}

func (p *PublishPacket) String() string {
	var s []byte
	if len(p.Payload) > maxPayloadOutput {
		s = []byte(p.Payload[:maxPayloadOutput])
		s = append(s, dot, dot, dot)
	} else {
		s = []byte(p.Payload)
	}

	return fmt.Sprintf("PUBLISH:[Qos: %d Dup: %t Retain: %t PacketId: %d Topic: %s PayloadLen: %d Payload: %s]", p.Header.Qos(), p.Header.Dup(), p.Header.Retain(), p.PacketId, p.Topic, len(p.Payload), string(s))
}

const maxTopicLen = 256

func isValidTopic(s string) bool {
	if len(s) > maxTopicLen {
		return false
	}
	// empty topic level is not allowed
	if s[len(s)-1] == '/' {
		return false
	}
	for _, c := range s {
		if c == '+' || c == '#' {
			return false
		}
	}
	return true
}

func DecodePublishPacket(t PacketTypeHeader, src []byte) (*PublishPacket, error) {
	var pos int = 0
	var e error
	var topic string
	var packetId uint16
	var payload []byte

	if !isValidQos(t.Qos()) {
		return nil, errors.New("Invalid qos")
	}

	if t.Retain() {
		return nil, errors.New("Retain publish is not supported")
	}

	if topic, pos, e = decodeString(src, pos); e != nil {
		return nil, errors.New("Decode topic name error")
	}

	if !isValidTopic(topic) {
		return nil, errors.New("Invalid topic")
	}

	if t.Qos() != 0 {
		if packetId, pos, e = decodeUint16(src, pos); e != nil {
			return nil, errors.New("Decode packetId error")
		}
	}

	payload = src[pos:]
	return NewPublishPacket(t, topic, packetId, payload), nil
}

func (p *PublishPacket) Encode(en *Encoder) error {
	en.Write([]byte{byte(p.Header), 0})

	var e error
	if e = en.WriteString(p.Topic); e != nil {
		return e
	}

	if p.Header.Qos() > 0 {
		if e = en.WriteUint16(p.PacketId); e != nil {
			return e
		}
	}

	if e = en.Write(p.Payload); e != nil {
		return e
	}

	en.Finish()
	return nil
}
