package mqtt

import (
	"reflect"
	"testing"
)

func Test_PacketTypeHeader(t *testing.T) {
	t.Parallel()
	a := PacketTypeHeader(PT_PUBLISH<<4 | 0x0D)

	assert(t, a.Qos() == 2, 1)
	assert(t, a.Dup(), 2)
	assert(t, a.Retain(), 3)
	assert(t, a.PacketType() == PT_PUBLISH, 4)
}

func Test_Ping(t *testing.T) {
	t.Parallel()
	assert(t, PT_PINGREQ == 12, 1)
	assert(t, IsPingreqPacket(12<<4), 2)

	x := NewPingreqPacket()
	en := GetEncoder(false)
	x.Encode(en)
	assert(t, reflect.DeepEqual(en.buf[en.start:en.end], []byte{12 << 4, 0}), 3)

	en = GetEncoder(true) // test ws
	x.Encode(en)
	wsbin := 2
	wsfin := 0x80
	assert(t, reflect.DeepEqual(en.buf[en.start:en.end], []byte{byte(wsfin | wsbin), 2, 12 << 4, 0}), 4)

}

func Test_Disconnect(t *testing.T) {
	t.Parallel()
	assert(t, PT_DISCONNECT == 14, 1)
	assert(t, IsDisconnectPacket(14<<4), 2)

	x := NewDisconnectPacket()
	en := GetEncoder(false)
	x.Encode(en)
	assert(t, reflect.DeepEqual(en.buf[en.start:en.end], []byte{14 << 4, 0}), 3)

	en = GetEncoder(true) // test ws
	x.Encode(en)
	wsbin := 2
	wsfin := 0x80
	assert(t, reflect.DeepEqual(en.buf[en.start:en.end], []byte{byte(wsfin | wsbin), 2, 14 << 4, 0}), 4)
}

func Test_Pong(t *testing.T) {
	t.Parallel()
	assert(t, PT_PINGRESP == 13, 1)
	assert(t, IsPingrespPacket(13<<4), 2)

	x := NewPingrespPacket()
	en := GetEncoder(false)
	x.Encode(en)
	assert(t, reflect.DeepEqual(en.buf[en.start:en.end], []byte{13 << 4, 0}), 3)

	en = GetEncoder(true) // test ws
	x.Encode(en)
	wsbin := 2
	wsfin := 0x80
	assert(t, reflect.DeepEqual(en.buf[en.start:en.end], []byte{byte(wsfin | wsbin), 2, 13 << 4, 0}), 4)
}

func Test_Connack(t *testing.T) {
	t.Parallel()
	assert(t, PT_CONNACK == 2, 1)
	assert(t, IsConnackPacket(2<<4), 2)

	p := NewConnackPacket(true, RC_ERR_REFUSED_ID_REJECTED)

	assert(t, p.SessionPresent, 3)
	assert(t, p.ReturnCode == RC_ERR_REFUSED_ID_REJECTED, 4)

	src := []byte{1, RC_ERR_REFUSED_ID_REJECTED}
	p2, er := DecodeConnackPacket(src)
	assert(t, p2.SessionPresent, 5)
	assert(t, p2.ReturnCode == RC_ERR_REFUSED_ID_REJECTED, 6)
	assert(t, er == nil, 7)

	src = []byte{2, 1}
	p2, er = DecodeConnackPacket(src)
	assert(t, er != nil, 8)
	assert(t, p2 == nil, 9)

	src = []byte{1, 2, 3}

	p2, er = DecodeConnackPacket(src)
	assert(t, er != nil, 10)
	assert(t, p2 == nil, 11)
}

func Test_Puback(t *testing.T) {
	t.Parallel()
	assert(t, PT_PUBACK == 4, 1)
	assert(t, IsPubackPacket(4<<4), 2)

	p := NewPubackPacket(10)

	assert(t, p.PacketId == 10, 3)

	en := GetEncoder(false)
	p.Encode(en)

	y := en.buf[en.start:en.end]

	assert(t, reflect.DeepEqual(y, []byte{pubackH, 2, 0, 10}), 4)

	z, er := DecodePubackPacket(y[2:])
	assert(t, z.PacketId == 10, 5)
	assert(t, er == nil, 6)
}

func Test_Pubrec(t *testing.T) {
	t.Parallel()
	assert(t, PT_PUBREC == 5, 1)
	assert(t, IsPubrecPacket(5<<4), 2)

	p := NewPubrecPacket(10)

	assert(t, p.PacketId == 10, 3)

	en := GetEncoder(false)
	p.Encode(en)

	y := en.buf[en.start:en.end]

	assert(t, reflect.DeepEqual(y, []byte{pubrecH, 2, 0, 10}), 4)

	z, er := DecodePubrecPacket(y[2:])

	assert(t, z.PacketId == 10, 5)
	assert(t, er == nil, 6)

}

func Test_Pubrel(t *testing.T) {
	t.Parallel()
	assert(t, PT_PUBREL == 6, 1)
	assert(t, !IsPubrelPacket(6<<4), 2)
	assert(t, IsPubrelPacket(6<<4|2), 3)

	p := NewPubrelPacket(10)

	assert(t, p.PacketId == 10, 4)

	en := GetEncoder(false)
	p.Encode(en)

	y := en.buf[en.start:en.end]

	assert(t, reflect.DeepEqual(y, []byte{pubrelH, 2, 0, 10}), 5)

	z, er := DecodePubrelPacket(y[2:])

	assert(t, z.PacketId == 10, 6)

	assert(t, er == nil, 7)
}

func Test_Pubcomp(t *testing.T) {
	t.Parallel()
	assert(t, PT_PUBCOMP == 7, 1)
	assert(t, IsPubcompPacket(7<<4), 2)

	p := NewPubcompPacket(10)

	assert(t, p.PacketId == 10, 3)

	en := GetEncoder(false)
	p.Encode(en)

	y := en.buf[en.start:en.end]

	assert(t, reflect.DeepEqual(y, []byte{pubcompH, 2, 0, 10}), 4)

	z, er := DecodePubcompPacket(y[2:])

	assert(t, z.PacketId == 10, 5)
	assert(t, er == nil, 6)

}

func Test_Unsuback(t *testing.T) {
	t.Parallel()
	assert(t, PT_UNSUBACK == 11, 1)
	assert(t, IsUnsubackPacket(11<<4), 2)

	p := NewUnsubackPacket(10)

	assert(t, p.PacketId == 10, 3)

	en := GetEncoder(false)
	p.Encode(en)

	y := en.buf[en.start:en.end]

	assert(t, reflect.DeepEqual(y, []byte{unsubackH, 2, 0, 10}), 4)

	z, er := DecodeUnsubackPacket(y[2:])

	assert(t, z.PacketId == 10, 5)
	assert(t, er == nil, 6)
}
