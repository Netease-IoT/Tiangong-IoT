package mqtt

import (
	"errors"
	"fmt"
)

const (
	ProtocolLevel31  = 0x3
	ProtocolLevel311 = 0x4

	ProtocolName31  = "MQIsdp"
	ProtocolName311 = "MQTT"
)

type ConnectFlag byte

// |    7   |   6  |    5     | 4 | 3 |   2    |     1      |   0    |
// |    X   |   X  |    X     | X | X |   X    |     X      |   X    |
// |username|passwd|willRetain|willQos|willFlag|cleanSession|reserved|
func (f ConnectFlag) CleanSession() bool {
	return (f & (1 << 1)) != 0
}

func (f ConnectFlag) WillFlag() bool {
	return (f & (1 << 2)) != 0
}

func (f ConnectFlag) WillQos() int8 {
	return int8((f & (1<<3 | 1<<4)) >> 3)
}

func (f ConnectFlag) WillRetain() bool {
	return (f & (1 << 5)) != 0
}

func (f ConnectFlag) PasswordFlag() bool {
	return (f & (1 << 6)) != 0
}

func (f ConnectFlag) UsernameFlag() bool {
	return (f & (1 << 7)) != 0
}

type ConnectPacket struct {
	// PacketTypeHeader for connect packet is fixed, so ignore it
	ProtocolLevel uint8
	Flags         ConnectFlag
	Keepalive     uint16

	ClientId    string
	WillTopic   string
	WillMessage []byte
	Username    string
	Password    []byte
}

const connectH = PT_CONNECT << 4

func IsConnectPacket(p PacketTypeHeader) bool {
	return connectH == p
}

func NewConnectPacket(pl uint8, flag ConnectFlag, keepalive uint16, clientId string, willtopic string, willmessage []byte, username string, password []byte) *ConnectPacket {
	return &ConnectPacket{
		ProtocolLevel: pl,
		Flags:         flag,
		Keepalive:     keepalive,

		ClientId:    clientId,
		WillTopic:   willtopic,
		WillMessage: willmessage,
		Username:    username,
		Password:    password,
	}
}

func (p *ConnectPacket) String() string {
	return fmt.Sprintf("CONNECT:[ProtocolVersion: %d  CleanSession: %t WillFlag: %t WillQos: %d WillRetain: %t Usernameflag: %t PasswordFlag: %t Keepalive: %d ClientId: %s Willtopic: %s Willmessage: %s Username: %s Password: %s]", p.ProtocolLevel, p.Flags.CleanSession(), p.Flags.WillFlag(), p.Flags.WillQos(), p.Flags.WillRetain(), p.Flags.UsernameFlag(), p.Flags.PasswordFlag(), p.Keepalive, p.ClientId, p.WillTopic, p.WillMessage, p.Username, p.Password)
}

func DecodeConnectPacket(src []byte) (*ConnectPacket, error) {
	var pos int = 0
	var e error
	var protocolName string
	var protocolLevel uint8
	var flags ConnectFlag
	var keepalive uint16
	var clientId string
	var willTopic string
	var willMessage []byte

	var username string
	var password []byte

	if protocolName, pos, e = decodeString(src, pos); e != nil {
		return nil, errors.New("Decode protocol name error")
	}

	if protocolLevel, pos, e = decodeByte(src, pos); e != nil {
		return nil, errors.New("Decode protocol level error")
	}

	if protocolLevel != ProtocolLevel31 && protocolLevel != ProtocolLevel311 {
		return nil, errors.New("Unknown protocol level")
	}

	if protocolLevel == ProtocolLevel31 && protocolName != ProtocolName31 {
		return nil, errors.New("Protocol name is not match protocol level, should be MQIsdp")
	}

	if protocolLevel == ProtocolLevel311 && protocolName != ProtocolName311 {
		return nil, errors.New("Protocol name is not match protocol level, should be MQTT")
	}

	var fbyte byte
	if fbyte, pos, e = decodeByte(src, pos); e != nil {
		return nil, errors.New("Decode connect flag error")
	}
	flags = ConnectFlag(fbyte)

	if flags&0x1 != 0 {
		return nil, errors.New("Reserved bit for connect flag should be 0")
	}

	if (flags.WillFlag() && flags.WillQos() == 3) || (!flags.WillFlag() && flags.WillQos() != 0) {
		return nil, errors.New("Will flag is error")
	}

	if keepalive, pos, e = decodeUint16(src, pos); e != nil {
		return nil, errors.New("Decode keepalive error")
	}

	if clientId, pos, e = decodeString(src, pos); e != nil {
		return nil, errors.New("Decode clientId error")
	}

	if flags.WillFlag() {
		if willTopic, pos, e = decodeString(src, pos); e != nil {
			return nil, errors.New("Decode will topic error")
		}
		if willMessage, pos, e = decodeBytes(src, pos); e != nil {
			return nil, errors.New("Decode will message error")
		}
	}
	if flags.UsernameFlag() {
		if username, pos, e = decodeString(src, pos); e != nil {
			return nil, errors.New("Decode username error")
		}
	}
	if flags.PasswordFlag() {
		if password, pos, e = decodeBytes(src, pos); e != nil {
			return nil, errors.New("Decode password error")
		}
	}
	if pos != len(src) {
		return nil, errors.New("Buffer maybe invalid, bytes remaining")
	}
	return NewConnectPacket(protocolLevel, flags, keepalive, clientId, willTopic, willMessage, username, password), nil
}

func (p *ConnectPacket) Encode(en *Encoder) error {
	en.Write([]byte{connectH, 0})

	if p.ProtocolLevel == ProtocolLevel31 {
		en.WriteString(ProtocolName31)
	} else {
		en.WriteString(ProtocolName311)
	}

	en.WriteByte(byte(p.ProtocolLevel))

	en.WriteByte(byte(p.Flags))
	en.WriteUint16(p.Keepalive)

	var e error
	if e = en.WriteString(p.ClientId); e != nil {
		return e
	}
	if p.Flags.WillFlag() {
		if e = en.WriteString(p.WillTopic); e != nil {
			return e
		}
		if e = en.WriteBytes(p.WillMessage); e != nil {
			return e
		}
	}

	if p.Flags.UsernameFlag() {
		if e = en.WriteString(p.Username); e != nil {
			return e
		}
	}

	if p.Flags.PasswordFlag() {
		if e = en.WriteBytes(p.Password); e != nil {
			return e
		}
	}

	en.Finish()
	return nil
}
