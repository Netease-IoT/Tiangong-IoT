package mqtt

import (
	"encoding/binary"
	"errors"
	"fmt"
)

// Remaining length should be 0:
//     Pingreq, Pingresp, Disconnect

// Remaining length should be 2
//     ConnackPacket, PubackPacket, PubrecPacket, PubrelPacket,
//     PubcompPacket, UnsubackPacket

// PacketTypeHeader is first byte of mqtt packet
type PacketTypeHeader byte

func (h PacketTypeHeader) Qos() int8 {
	return int8((h & 0x06) >> 1)
}

func (h PacketTypeHeader) Dup() bool {
	return h&0x08 != 0
}

func (h PacketTypeHeader) Retain() bool {
	return h&0x01 != 0
}

func (h PacketTypeHeader) PacketType() uint8 {
	return uint8((h & 0xf0) >> 4)
}

func (h PacketTypeHeader) String() string {
	return fmt.Sprintf("PacketTypeHeader:[0x%x: %s]", byte(h), PacketNames[h.PacketType()])
}

const (
	pingreqH    = PT_PINGREQ << 4
	pingrespH   = PT_PINGRESP << 4
	disconnectH = PT_DISCONNECT << 4

	connackH  = PT_CONNACK << 4
	pubackH   = PT_PUBACK << 4
	pubrecH   = PT_PUBREC << 4
	pubrelH   = PT_PUBREL<<4 | 0x02 // pub rel QoS is 1
	pubcompH  = PT_PUBCOMP << 4
	unsubackH = PT_UNSUBACK << 4
)

type PingreqPacket struct{}
type PingrespPacket struct{}
type DisconnectPacket struct{}

var (
	pingreqBuf    = []byte{pingreqH, 0}
	pingrespBuf   = []byte{pingrespH, 0}
	disconnectBuf = []byte{disconnectH, 0}

	// ping, pong, disconnect are singletons
	pingreqIns    = PingreqPacket{}
	pingrespIns   = PingrespPacket{}
	disconnectIns = DisconnectPacket{}
)

func IsPingreqPacket(p PacketTypeHeader) bool {
	return p == pingreqH
}

func NewPingreqPacket() *PingreqPacket {
	return &pingreqIns
}

func DecodePingreqPacket(src []byte) (*PingreqPacket, error) {
	if len(src) != 0 {
		return nil, errors.New("Ping req packet length should be 0")
	}
	return &pingreqIns, nil
}

func (_ *PingreqPacket) String() string {
	return "PINGREQ"
}

func (_ *PingreqPacket) Encode(e *Encoder) error {
	e.Write(pingreqBuf)
	e.Finish()
	return nil
}

//------------------------------------------------

func IsPingrespPacket(p PacketTypeHeader) bool {
	return p == pingrespH
}

func NewPingrespPacket() *PingrespPacket {
	return &pingrespIns
}

func DecodePingrespPacket(src []byte) (*PingrespPacket, error) {
	if len(src) != 0 {
		return nil, errors.New("Ping resp packet length should be 0")
	}
	return &pingrespIns, nil
}

func (_ *PingrespPacket) String() string {
	return "PINGRESP"
}

func (_ *PingrespPacket) Encode(e *Encoder) error {
	e.Write(pingrespBuf)
	e.Finish()
	return nil
}

//---------------------------------------------

func IsDisconnectPacket(p PacketTypeHeader) bool {
	return p == disconnectH
}

func NewDisconnectPacket() *DisconnectPacket {
	return &disconnectIns
}

func DecodeDisconnectPacket(src []byte) (*DisconnectPacket, error) {
	if len(src) != 0 {
		return nil, errors.New("Disconnect packet length should be 0")
	}
	return &disconnectIns, nil
}

func (_ *DisconnectPacket) String() string {
	return "DISCONNECT"
}

func (_ *DisconnectPacket) Encode(e *Encoder) error {
	e.Write(disconnectBuf)
	e.Finish()
	return nil
}

//------------------------------------------

func IsConnackPacket(p PacketTypeHeader) bool {
	return p == connackH
}

type ConnackPacket struct {
	SessionPresent bool
	ReturnCode     byte
}

func NewConnackPacket(sessionPresent bool, returnCode byte) (p *ConnackPacket) {
	return &ConnackPacket{
		SessionPresent: sessionPresent,
		ReturnCode:     returnCode,
	}
}

func DecodeConnackPacket(src []byte) (*ConnackPacket, error) {
	if len(src) != 2 {
		return nil, errors.New("Connect ack packet length should be 2")
	}

	var sessionPresent bool
	if src[0] == 0 {
		sessionPresent = false
	} else if src[0] == 1 {
		sessionPresent = true
	} else {
		return nil, fmt.Errorf("Invalid conn ack flags: %v", src[0])
	}

	return &ConnackPacket{
		SessionPresent: sessionPresent,
		ReturnCode:     src[1],
	}, nil
}

func (p *ConnackPacket) String() string {
	return fmt.Sprintf("CONNACK:[sessionPresent: %t, returnCode: %d]",
		p.SessionPresent, p.ReturnCode)
}

func (p *ConnackPacket) Encode(e *Encoder) error {
	var sp byte
	if p.SessionPresent {
		sp = 1
	}
	e.Write([]byte{connackH, 2, sp, p.ReturnCode})
	e.Finish()
	return nil
}

//-----------------------------------------------

func IsPubackPacket(h PacketTypeHeader) bool {
	return h == pubackH
}

type PubackPacket struct {
	PacketId uint16
}

func NewPubackPacket(packetId uint16) *PubackPacket {
	return &PubackPacket{
		PacketId: packetId,
	}
}

func DecodePubackPacket(src []byte) (*PubackPacket, error) {
	if len(src) != 2 {
		return nil, errors.New("Puback packet length is not 2")
	}
	id := binary.BigEndian.Uint16(src)
	return &PubackPacket{
		PacketId: id,
	}, nil
}

func (p *PubackPacket) String() string {
	return fmt.Sprintf("PUBACK:[packetId: %d]", p.PacketId)
}

func (p *PubackPacket) Encode(e *Encoder) error {
	ida := [2]byte{}
	binary.BigEndian.PutUint16(ida[:], p.PacketId)
	e.Write([]byte{pubackH, 2, ida[0], ida[1]})
	e.Finish()
	return nil
}

//-------------------------------------------------

type PubrecPacket struct {
	PacketId uint16
}

func IsPubrecPacket(h PacketTypeHeader) bool {
	return h == pubrecH
}

func NewPubrecPacket(packetId uint16) *PubrecPacket {
	return &PubrecPacket{
		PacketId: packetId,
	}
}

func DecodePubrecPacket(src []byte) (*PubrecPacket, error) {
	if len(src) != 2 {
		return nil, errors.New("Pubrec packet length is not 2")
	}
	id := binary.BigEndian.Uint16(src)
	return &PubrecPacket{
		PacketId: id,
	}, nil
}

func (p *PubrecPacket) String() string {
	return fmt.Sprintf("PUBREC:[packetId: %d]", p.PacketId)
}

func (p *PubrecPacket) Encode(e *Encoder) error {
	ida := [2]byte{}
	binary.BigEndian.PutUint16(ida[:], p.PacketId)
	e.Write([]byte{pubrecH, 2, ida[0], ida[1]})
	e.Finish()
	return nil
}

//------------------------------

type PubrelPacket struct {
	PacketId uint16
}

func IsPubrelPacket(h PacketTypeHeader) bool {
	return h == pubrelH
}

func NewPubrelPacket(packetId uint16) *PubrelPacket {
	return &PubrelPacket{
		PacketId: packetId,
	}
}

func DecodePubrelPacket(src []byte) (*PubrelPacket, error) {
	if len(src) != 2 {
		return nil, errors.New("Pubrel packet length is not 2")
	}
	id := binary.BigEndian.Uint16(src)
	return &PubrelPacket{
		PacketId: id,
	}, nil
}

func (p PubrelPacket) String() string {
	return fmt.Sprintf("PUBREL:[packetId: %d]", p.PacketId)
}

func (p *PubrelPacket) Encode(e *Encoder) error {
	ida := [2]byte{}
	binary.BigEndian.PutUint16(ida[:], p.PacketId)
	e.Write([]byte{pubrelH, 2, ida[0], ida[1]})
	e.Finish()
	return nil
}

//---------------------------------

type PubcompPacket struct {
	PacketId uint16
}

func IsPubcompPacket(h PacketTypeHeader) bool {
	return h == pubcompH
}

func NewPubcompPacket(packetId uint16) *PubcompPacket {
	return &PubcompPacket{
		PacketId: packetId,
	}
}

func DecodePubcompPacket(src []byte) (*PubcompPacket, error) {
	if len(src) != 2 {
		return nil, errors.New("Pubcomp packet length is not 2")
	}
	id := binary.BigEndian.Uint16(src)
	return &PubcompPacket{
		PacketId: id,
	}, nil
}

func (p PubcompPacket) String() string {
	return fmt.Sprintf("PUBCOMP:[packetId: %d]", p.PacketId)
}

func (p *PubcompPacket) Encode(e *Encoder) error {
	ida := [2]byte{}
	binary.BigEndian.PutUint16(ida[:], p.PacketId)
	e.Write([]byte{pubcompH, 2, ida[0], ida[1]})
	e.Finish()
	return nil
}

//-------------------------

type UnsubackPacket struct {
	PacketId uint16
}

func IsUnsubackPacket(h PacketTypeHeader) bool {
	return h == unsubackH
}

func NewUnsubackPacket(packetId uint16) *UnsubackPacket {
	return &UnsubackPacket{
		PacketId: packetId,
	}
}

func DecodeUnsubackPacket(src []byte) (*UnsubackPacket, error) {
	if len(src) != 2 {
		return nil, errors.New("Puback packet length is not 2")
	}
	id := binary.BigEndian.Uint16(src)
	return &UnsubackPacket{
		PacketId: id,
	}, nil
}

func (p UnsubackPacket) String() string {
	return fmt.Sprintf("Unsuback:[packetId: %d]", p.PacketId)
}

func (p *UnsubackPacket) Encode(e *Encoder) error {
	ida := [2]byte{}
	binary.BigEndian.PutUint16(ida[:], p.PacketId)
	e.Write([]byte{unsubackH, 2, ida[0], ida[1]})
	e.Finish()
	return nil
}
