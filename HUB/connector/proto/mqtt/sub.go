package mqtt

import (
	"errors"
	"fmt"
)

const subscribeH = PT_SUBSCRIBE<<4 | 0x2 // Qos for subscribe is 1

func IsSubscribePacket(p PacketTypeHeader) bool {
	return subscribeH == p
}

type SubscribePacket struct {
	PacketId uint16

	Topics []string
	Qoss   []int8
}

func (p *SubscribePacket) String() string {
	return fmt.Sprintf("SUBSCRIBE:[PacketId: %d Topics: %s, Qoss: %v]", p.PacketId, p.Topics, p.Qoss)
}

func NewSubscribePacket(packetId uint16, topics []string, qoss []int8) *SubscribePacket {
	if len(qoss) != len(topics) {
		return nil
	}
	return &SubscribePacket{
		PacketId: packetId,
		Topics:   topics,
		Qoss:     qoss,
	}

}

const maxSubscribePerTime = 16

const maxTopicFilterLen = 128

// valid: + +/a a/+ a/+/b +/# a/+/#
// invalid: #/ #/a +a
// empty topic level is considered invalid
// topic filter max size 128
func isValidTopicFilter(s string) bool {
	if len(s) > maxTopicFilterLen {
		return false
	}

	if s[len(s)-1] == '/' {
		return false
	}

	for i, c := range s {
		if c == '+' {
			// invalid: not first and prior is not / OR not last and next is not /
			if (i != 0 && s[i-1] != '/') || (i != len(s)-1 && s[i+1] != '/') {
				return false
			}
		} else if c == '#' {
			// invalid: not last OR not first and prior is not /
			if (i != len(s)-1) || (i != 0 && s[i-1] != '/') {
				return false
			}
		}
	}
	return true
}

// Qos 2 is not supported
func isValidQos(qos int8) bool {
	if qos == 0 || qos == 1 {
		return true
	}
	return false
}

func DecodeSubscribePacket(src []byte) (*SubscribePacket, error) {
	topics := make([]string, 0, maxSubscribePerTime)
	qoss := make([]int8, 0, maxSubscribePerTime)

	var packetId uint16
	var e error
	var pos int = 0
	var topic string
	var qos int8

	if packetId, pos, e = decodeUint16(src, pos); e != nil {
		return nil, errors.New("Decode packetId error")
	}

	for pos != len(src) {
		if len(topics) == maxSubscribePerTime {
			return nil, errors.New("Max sub per time is 16")
		}

		if topic, pos, e = decodeString(src, pos); e != nil {
			return nil, errors.New("Decode topic error")
		}

		if !isValidTopicFilter(topic) {
			return nil, errors.New("Invalid topic filter error")
		}

		if qos, pos, e = decodeInt8(src, pos); e != nil {
			return nil, errors.New("Decode qos error")
		}

		if !isValidQos(qos) {
			return nil, errors.New("Invalid topic qos error")
		}

		topics = append(topics, topic)
		qoss = append(qoss, qos)
	}
	return NewSubscribePacket(packetId, topics, qoss), nil
}

func (p *SubscribePacket) Encode(en *Encoder) error {
	en.Write([]byte{subscribeH, 0})
	en.WriteUint16(p.PacketId)

	var e error
	for i, topic := range p.Topics {
		if e = en.WriteString(topic); e != nil {
			return e
		}
		if e = en.WriteByte(byte(p.Qoss[i])); e != nil {
			return e
		}
	}

	en.Finish()
	return nil
}

//-----------------------------------
const subackH = PT_SUBACK << 4

func IsSubackPacket(p PacketTypeHeader) bool {
	return subackH == p
}

type SubackPacket struct {
	PacketId uint16

	ReturnCodes []int8
}

func NewSubackPacket(packetId uint16, retcodes []int8) *SubackPacket {
	if len(retcodes) > maxSubscribePerTime {
		panic("Max subscribe per time should less than 16")
	}

	return &SubackPacket{
		PacketId:    packetId,
		ReturnCodes: retcodes,
	}
}

func DecodeSubackPacket(src []byte) (*SubackPacket, error) {
	var packetId uint16
	var e error
	retcodes := make([]int8, 0, maxSubscribePerTime)
	var ret int8
	var pos int = 0

	if packetId, pos, e = decodeUint16(src, pos); e != nil {
		return nil, e
	}

	for pos != len(src) {
		if len(retcodes) == maxSubscribePerTime {
			return nil, errors.New("Max subscribe per time is 16")
		}

		if ret, pos, e = decodeInt8(src, pos); e != nil {
			retcodes = append(retcodes, ret)
		}
	}
	return NewSubackPacket(packetId, retcodes), nil
}

func (p *SubackPacket) String() string {
	return fmt.Sprintf("SUBACK:[PacketId: %d ReturnCodes: %v]", p.PacketId, p.ReturnCodes)
}

func (p *SubackPacket) Encode(en *Encoder) error {
	en.Write([]byte{subackH, 0})
	en.WriteUint16(p.PacketId)

	for _, code := range p.ReturnCodes {
		en.WriteByte(byte(code))
	}
	en.Finish()

	return nil
}
