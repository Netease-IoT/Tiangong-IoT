package proto

import (
	"bufio"
	"crypto/sha1"
	"encoding/base64"
	"net"
	"net/http"
	"strings"
	"time"
	"unsafe"
)

const (
	WsContinue = 0

	WsFrameError       = -1
	WsFrameNotComplete = -2
	WsPing             = 0x9
	WsPong             = 0xa
	WsClose            = 0x8
	WsBinayPayload     = 0x2
)

const wordSize = int(unsafe.Sizeof(uintptr(0)))

func UmaskWs(b []byte) {
	// b[0:4] is mask key
	// payload start from b[4:]

	bl := len(b)
	if bl < 5 {
		return
	}

	payload := b[4:]

	if bl < 2*wordSize {
		for i := range payload {
			payload[i] ^= b[i&3]
		}
		return
	}

	pos := 0
	// Mask one byte at a time to word boundary.
	if n := int(uintptr(unsafe.Pointer(&payload))) % wordSize; n != 0 {
		n = wordSize - n
		for i := range payload[:n] {
			payload[i] ^= b[pos&3]
			pos++
		}
		payload = payload[n:]
	}

	// Create aligned word size key.
	var k [wordSize]byte
	for i := range k {
		k[i] = b[(pos+i)&3]
	}
	kw := *(*uintptr)(unsafe.Pointer(&k))

	// Mask one word at a time.
	n := (len(payload) / wordSize) * wordSize
	for i := 0; i < n; i += wordSize {
		*(*uintptr)(unsafe.Pointer(uintptr(unsafe.Pointer(&payload[0])) + uintptr(i))) ^= kw
	}

	// Mask one byte at a time for remaining bytes.
	payload = payload[n:]
	for i := range payload {
		payload[i] ^= b[pos&3]
		pos++
	}
}

var keyGUID = []byte("258EAFA5-E914-47DA-95CA-C5AB0DC85B11")

func computeAcceptKey(challengeKey string) string {
	h := sha1.New()
	h.Write([]byte(challengeKey))
	h.Write(keyGUID)
	return base64.StdEncoding.EncodeToString(h.Sum(nil))
}

func returnError(w http.ResponseWriter, status int) {
	w.Header().Set("Sec-Websocket-Version", "13")
	http.Error(w, http.StatusText(status), status)
}

const wsHandshakeTimeout = 5 * time.Second

func HandleWsHandshake(w http.ResponseWriter, r *http.Request) net.Conn {
	if r.Method != "GET" {
		// websocket handshake method should be GET
		returnError(w, http.StatusMethodNotAllowed)
		return nil
	}

	if !tokenListContainsValue(r.Header, "Connection", "upgrade") {
		// Connection header should contain upgrade
		returnError(w, http.StatusBadRequest)
		return nil
	}

	if !tokenListContainsValue(r.Header, "Upgrade", "websocket") {
		// Upgrade header should contain websocket
		returnError(w, http.StatusBadRequest)
		return nil
	}

	if !tokenListContainsValue(r.Header, "Sec-Websocket-Version", "13") {
		// we support RFC 6455 Websocket Version is 13
		returnError(w, http.StatusBadRequest)
		return nil
	}

	challengeKey := r.Header.Get("Sec-Websocket-Key")
	if challengeKey == "" {
		// Sec-Websocket-Key header should not be empty
		returnError(w, http.StatusBadRequest)
		return nil
	}
	// We do not check origin here
	// We do not support compress, ignore 'Sec-Websocket-Extensions'

	// we choose first subprotocol
	var subprotocol string
	subprotocols := strings.TrimSpace(r.Header.Get("Sec-Websocket-Protocol"))
	if subprotocols == "" {
		subprotocol = ""
	} else {
		subprotocol = strings.TrimSpace(strings.Split(subprotocols, ",")[0])
	}

	h, ok := w.(http.Hijacker)
	if !ok {
		// response does not implement http.Hijacker
		returnError(w, http.StatusInternalServerError)
		return nil
	}

	var brw *bufio.ReadWriter
	var netConn net.Conn
	var err error

	netConn, brw, err = h.Hijack()
	if err != nil {
		// Hijack fail
		returnError(w, http.StatusInternalServerError)
		return nil
	}

	if brw.Reader.Buffered() > 0 {
		// client send data before handshake is complete
		netConn.Close()
		return nil
	}

	p := make([]byte, 0, 1024)
	p = append(p, "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "...)
	p = append(p, computeAcceptKey(challengeKey)...)
	p = append(p, "\r\n"...)
	if subprotocol != "" {
		p = append(p, "Sec-Websocket-Protocol: "...)
		p = append(p, subprotocol...)
		p = append(p, "\r\n"...)
	}
	p = append(p, "Access-Control-Allow-Origin: *\r\n"...)
	p = append(p, "Server: Netease MQTT Hub\r\n"...)

	p = append(p, "\r\n"...)

	// Clear deadlines set by HTTP server.
	netConn.SetDeadline(time.Time{})
	netConn.SetWriteDeadline(time.Now().Add(wsHandshakeTimeout))

	if _, err = netConn.Write(p); err != nil {
		netConn.Close()
		return nil
	}
	netConn.SetDeadline(time.Time{})

	return netConn
}

//----------------------------

const (
	isTokenOctet = 1 << iota
	isSpaceOctet
)

var octetTypes [256]byte

func init() {
	// From RFC 2616
	//
	// OCTET      = <any 8-bit sequence of data>
	// CHAR       = <any US-ASCII character (octets 0 - 127)>
	// CTL        = <any US-ASCII control character (octets 0 - 31) and DEL (127)>
	// CR         = <US-ASCII CR, carriage return (13)>
	// LF         = <US-ASCII LF, linefeed (10)>
	// SP         = <US-ASCII SP, space (32)>
	// HT         = <US-ASCII HT, horizontal-tab (9)>
	// <">        = <US-ASCII double-quote mark (34)>
	// CRLF       = CR LF
	// LWS        = [CRLF] 1*( SP | HT )
	// TEXT       = <any OCTET except CTLs, but including LWS>
	// separators = "(" | ")" | "<" | ">" | "@" | "," | ";" | ":" | "\" | <">
	//              | "/" | "[" | "]" | "?" | "=" | "{" | "}" | SP | HT
	// token      = 1*<any CHAR except CTLs or separators>
	// qdtext     = <any TEXT except <">>

	for c := 0; c < 256; c++ {
		var t byte
		isCtl := c <= 31 || c == 127
		isChar := 0 <= c && c <= 127
		isSeparator := strings.IndexRune(" \t\"(),/:;<=>?@[]\\{}", rune(c)) >= 0
		if strings.IndexRune(" \t\r\n", rune(c)) >= 0 {
			t |= isSpaceOctet
		}
		if isChar && !isCtl && !isSeparator {
			t |= isTokenOctet
		}
		octetTypes[c] = t
	}
}

func skipSpace(s string) (rest string) {
	i := 0
	for ; i < len(s); i++ {
		if octetTypes[s[i]]&isSpaceOctet == 0 {
			break
		}
	}
	return s[i:]
}

func nextToken(s string) (token, rest string) {
	i := 0
	for ; i < len(s); i++ {
		if octetTypes[s[i]]&isTokenOctet == 0 {
			break
		}
	}
	return s[:i], s[i:]
}

// tokenListContainsValue returns true if the 1#token header with the given
// name contains token.
func tokenListContainsValue(header http.Header, name string, value string) bool {
headers:
	for _, s := range header[name] {
		for {
			var t string
			t, s = nextToken(skipSpace(s))
			if t == "" {
				continue headers
			}
			s = skipSpace(s)
			if s != "" && s[0] != ',' {
				continue headers
			}
			if strings.EqualFold(t, value) {
				return true
			}
			if s == "" {
				continue headers
			}
			s = s[1:]
		}
	}
	return false
}
