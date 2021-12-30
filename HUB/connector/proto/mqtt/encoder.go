package mqtt

import (
	"encoding/binary"
	"errors"
	"io"
	"net"
	"time"
)

const fixedHeaderBytes int = 2

type Encoder struct {
	dat [blockSize]byte
	buf []byte

	start int
	end   int
	ws    bool
}

func (en *Encoder) Reset(ws bool) {
	en.ws = ws
	en.ResetState()
}

func (en *Encoder) ResetState() {
	if len(en.buf) == largeBlockSize {
		putLargeBuf(en.buf)
	}

	en.buf = en.dat[:]

	en.start = 0
	en.end = 0
	en.reserve()
}

// Reserve should be called after reset, before PacketBuffer being used
func (en *Encoder) reserve() {
	var r int = 4 // for mqtt
	if en.ws {
		r += 6 // for ws
	}

	en.start = en.start + r
	en.end = en.start
}

func (en *Encoder) finishMQTTEncode() {
	plen := en.end - en.start
	rLen := plen - fixedHeaderBytes
	encLength := make([]byte, 0, 4)
	for {
		digit := byte(rLen % 128)
		rLen /= 128

		if rLen > 0 {
			digit |= 0x80
		}
		encLength = append(encLength, digit)
		if rLen == 0 {
			break
		}
	}

	if en.start < (len(encLength) - 1) {
		panic("Reserve byte is not enough")
	}

	switch len(encLength) {
	case 1:
		en.buf[en.start+1] = encLength[0]
	case 2:
		en.start -= 1

		en.buf[en.start] = en.buf[en.start+1]
		en.buf[en.start+1] = encLength[0]
		en.buf[en.start+2] = encLength[1]

	case 3:
		en.start -= 2

		en.buf[en.start] = en.buf[en.start+2]
		en.buf[en.start+1] = encLength[0]
		en.buf[en.start+2] = encLength[1]
		en.buf[en.start+3] = encLength[2]
	case 4:
		en.start -= 3

		en.buf[en.start] = en.buf[en.start+3]
		en.buf[en.start+1] = encLength[0]
		en.buf[en.start+2] = encLength[1]
		en.buf[en.start+3] = encLength[2]
		en.buf[en.start+4] = encLength[3]
	default:
		panic("encLength must not be greater than 4")
	}
}

const wsBinaryFrame = 0x2
const wsPongFrame = 0xA

func (en *Encoder) finishWsEncode() {
	en.finishWsHeader(wsBinaryFrame)
}

func (en *Encoder) FinishWsPong() {
	en.finishWsHeader(wsPongFrame)
}

func (en *Encoder) Finish() {
	en.finishMQTTEncode()
	if en.ws {
		en.finishWsEncode()
	}
}

func (en *Encoder) finishWsHeader(opcode byte) {
	wlen := en.end - en.start

	// See RFC 6455 5.2. Base Framing Protocol
	var flag byte = 0x80 | opcode // always FIN bit is set, opcode is pong frame

	if wlen < 126 {
		if en.start-2 < 0 {
			panic("Reserve bytes is not enough")
		}
		en.start -= 2

		en.buf[en.start] = flag
		en.buf[en.start+1] = byte(wlen) // always not masked for server push
	} else {
		if en.start-4 < 0 {
			panic("Reserve bytes is not enough")
		}
		en.start -= 4

		en.buf[en.start] = flag
		en.buf[en.start+1] = byte(126) // always not masked for server push
		binary.BigEndian.PutUint16(en.buf[en.start+2:], uint16(wlen))
	}
	// as our largest packet is 16K, so we need not to handle payload greater than 64K
}

func (en *Encoder) ensure(sz int) error {
	if len(en.buf)-en.end >= sz {
		return nil
	}

	if len(en.buf) == blockSize {
		b := getLargeBuf()
		copy(b, en.buf)
		en.buf = b
	}

	if len(en.buf)-en.end < sz {
		return errors.New("Packet is too large")
	}

	return nil
}

func (en *Encoder) Write(src []byte) error {
	sz := len(src)
	if sz == 0 {
		return nil
	}

	if e := en.ensure(sz); e != nil {
		return e
	}

	c := copy(en.buf[en.end:], src)
	en.end += c

	return nil
}

func (en *Encoder) WriteString(s string) error {
	sb := []byte(s)
	if e := en.WriteUint16(uint16(len(sb))); e != nil {
		return e
	}
	return en.Write(sb)
}

func (en *Encoder) WriteUint16(u uint16) error {
	if e := en.ensure(2); e != nil {
		return e
	}

	binary.BigEndian.PutUint16(en.buf[en.end:], u)
	en.end += 2
	return nil
}

func (en *Encoder) WriteBytes(b []byte) error {

	if e := en.WriteUint16(uint16(len(b))); e != nil {
		return e
	}
	return en.Write(b)
}

func (en *Encoder) WriteByte(b byte) error {
	if e := en.ensure(1); e != nil {
		return e
	}
	en.buf[en.end] = b
	en.end += 1
	return nil
}

func (en *Encoder) WriteTo(w io.Writer, timeout time.Duration) error {
	conn, _ := w.(net.Conn)
	deadline := time.Now().Add(timeout)
	conn.SetWriteDeadline(deadline)
	defer en.ResetState()

	for {
		n, e := conn.Write(en.buf[en.start:en.end])
		if n > 0 {
			en.start += n

			if en.start == en.end {
				conn.SetWriteDeadline(time.Time{})
				return nil
			}
		} else {
			return e
		}
	}
}
