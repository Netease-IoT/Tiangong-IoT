package mqtt

import (
	"reflect"
	"testing"
)

func Test_TypeHeader(t *testing.T) {
	x := PacketTypeHeader(PT_PUBLISH<<4 | 0xd)
	y := PacketTypeHeader(PT_CONNECT << 4)

	assert(t, IsPublishPacket(x), 1)
	assert(t, !IsPublishPacket(y), 2)
	assert(t, x.Dup(), 3)
	assert(t, x.Qos() == 2, 4)
	assert(t, x.Retain(), 5)
}

func Test_Pub_Decode_Encode(t *testing.T) {
	h := PacketTypeHeader(PT_PUBLISH<<4 | 0xd)
	x := []byte{
		0, 4, 't', 'o', 'p', 'i',
		0, 10,
		't', 'e', 's', 't',
	}

	y, er := DecodePublishPacket(h, x)
	assert(t, y != nil, 1)
	assert(t, er == nil, 2)
	assert(t, y.Topic == "topi", 3)
	assert(t, y.PacketId == 10, 4)
	assert(t, reflect.DeepEqual(y.Payload, []byte("test")), 5)

	en := GetEncoder(false)
	y.Encode(en)
	assert(t, reflect.DeepEqual(en.buf[en.start+2:en.end], x), 6)
}
