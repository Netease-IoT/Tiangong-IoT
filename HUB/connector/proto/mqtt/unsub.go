package mqtt

import (
	"errors"
	"fmt"
)

type UnsubscribePacket struct {
	PacketId uint16

	Topics []string
}

const unsubscribeH = PT_UNSUBSCRIBE<<4 | 0x2 // qos for unsubscribe is 1

func IsUnsubscribePacket(p PacketTypeHeader) bool {
	return p == unsubscribeH
}

func NewUnsubscribePacket(packetId uint16, topics []string) *UnsubscribePacket {
	return &UnsubscribePacket{
		PacketId: packetId,
		Topics:   topics,
	}
}

func DecodeUnsubscribePacket(src []byte) (*UnsubscribePacket, error) {
	var e error
	var packetId uint16
	topics := make([]string, 0, maxSubscribePerTime)
	var topic string
	var pos int = 0
	if packetId, pos, e = decodeUint16(src, pos); e != nil {
		return nil, errors.New("Decode packetId error")
	}

	for pos != len(src) {
		if len(topics) == maxSubscribePerTime {
			return nil, errors.New("Max unsub per time is 16")
		}

		if topic, pos, e = decodeString(src, pos); e != nil {
			return nil, errors.New("Decode topic error")
		}

		if !isValidTopicFilter(topic) {
			return nil, errors.New("Invalid topic filter")
		}

		topics = append(topics, topic)
	}
	return NewUnsubscribePacket(packetId, topics), nil
}

func (p *UnsubscribePacket) String() string {
	return fmt.Sprintf("UNSUBSCRIBE:[PacketId: %d Topics: %s]", p.PacketId, p.Topics)
}

func (p *UnsubscribePacket) Encode(en *Encoder) error {
	en.Write([]byte{unsubscribeH, 0})
	en.WriteUint16(p.PacketId)

	var e error
	for _, topic := range p.Topics {
		if e = en.WriteString(topic); e != nil {
			return e
		}
	}
	en.Finish()
	return nil
}
