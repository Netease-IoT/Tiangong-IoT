package mqtt

import (
	"reflect"
	"testing"
)

func Test_Conn_Const(t *testing.T) {
	t.Parallel()
	assert(t, ProtocolLevel31 == 3, 1)
	assert(t, ProtocolLevel311 == 4, 2)

	assert(t, ProtocolName31 == "MQIsdp", 3)
	assert(t, ProtocolName311 == "MQTT", 4)

	assert(t, connectH == 1<<4, 5)

	assert(t, IsConnectPacket(connectH), 6)
	assert(t, !IsConnectPacket(connectH|0x2), 7)
	assert(t, !IsConnectPacket(connectH|0x80), 8)
}

func Test_ConnectFlag(t *testing.T) {
	t.Parallel()
	x := ConnectFlag(1<<7 | 1<<6 | 1<<5 | 2<<3 | 1<<2 | 1<<1)

	assert(t, x.CleanSession(), 1)
	assert(t, x.WillFlag(), 2)
	assert(t, x.WillQos() == 2, 3)
	assert(t, x.WillRetain(), 4)
	assert(t, x.PasswordFlag(), 5)
	assert(t, x.UsernameFlag(), 6)

	x ^= 1 << 7
	assert(t, !x.UsernameFlag(), 7)
	x ^= 1 << 6
	assert(t, !x.PasswordFlag(), 8)
	x ^= 1 << 5
	assert(t, !x.WillRetain(), 9)
	x ^= 1 << 2
	assert(t, !x.WillFlag(), 10)
	x ^= 1 << 1
	assert(t, !x.CleanSession(), 11)
}

func Test_Con_Decode_Encode(t *testing.T) {
	t.Parallel()
	x := []byte{
		connectH,
		0,
		0, 4,
		'M', 'Q', 'T', 'T',
		4,
		1<<7 | 1<<6 | 1<<1,
		0, 10,
		0, 6, 'c', 'l', 'e', 't', 'i', 'd',
		0, 2, 'u', 's',
		0, 2, 'p', 'w',
	}

	x[1] = byte(len(x) - 2)

	xc, _ := DecodeConnectPacket(x[2:])
	assert(t, xc.ProtocolLevel == ProtocolLevel311, 1)
	assert(t, xc.Keepalive == 10, 2)
	assert(t, xc.ClientId == "cletid", 3)
	assert(t, xc.Username == "us", 4)
	assert(t, reflect.DeepEqual(xc.Password, []byte("pw")), 5)
	assert(t, xc.WillTopic == "", 6)
	assert(t, xc.WillMessage == nil, 7)

	assert(t, xc.Flags.UsernameFlag(), 8)
	assert(t, xc.Flags.PasswordFlag(), 9)
	assert(t, !xc.Flags.WillFlag(), 10)

	en := GetEncoder(false)
	xc.Encode(en)
	assert(t, reflect.DeepEqual(x, en.buf[en.start:en.end]), 11)
}

func Test_Conn_Decode_Encode_Will(t *testing.T) {
	t.Parallel()
	x := []byte{
		connectH,
		0,
		0, 4,
		'M', 'Q', 'T', 'T',
		4,
		1<<7 | 1<<6 | 1<<1 | 1<<2,
		0, 10,
		0, 6, 'c', 'l', 'e', 't', 'i', 'd',
		0, 5, 'w', 'i', 'l', 'l', 't',
		0, 1, 'p',
		0, 2, 'u', 's',
		0, 2, 'p', 'w',
	}

	x[1] = byte(len(x) - 2)

	xc, _ := DecodeConnectPacket(x[2:])
	assert(t, xc.ProtocolLevel == ProtocolLevel311, 1)
	assert(t, xc.Keepalive == 10, 2)
	assert(t, xc.ClientId == "cletid", 3)
	assert(t, xc.Username == "us", 4)
	assert(t, reflect.DeepEqual(xc.Password, []byte("pw")), 5)
	assert(t, xc.WillTopic == "willt", 6)
	assert(t, reflect.DeepEqual(xc.WillMessage, []byte("p")), 7)

	assert(t, xc.Flags.UsernameFlag(), 8)
	assert(t, xc.Flags.PasswordFlag(), 9)
	assert(t, xc.Flags.WillFlag(), 10)

	en := GetEncoder(false)
	xc.Encode(en)
	assert(t, reflect.DeepEqual(x, en.buf[en.start:en.end]), 11)
}
