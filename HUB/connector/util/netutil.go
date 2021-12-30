package util

import (
	"crypto/tls"
	"errors"
	"net"
	"os"
	"reflect"
	"syscall"
	"unsafe"
)

const SO_REUSEPORT = 15
const backlog = 512

// addr should be in format '0.0.0.0:1883'
func NewTCPListener(address string, reusePort bool) (net.Listener, error) {
	if !reusePort {
		return net.Listen("tcp", address)
	} else {
		addr, e := net.ResolveTCPAddr("tcp", address)
		if e != nil {
			return nil, e
		}

		var fd int

		fd, e = syscall.Socket(syscall.AF_INET, syscall.SOCK_STREAM, syscall.IPPROTO_TCP)
		if e != nil {
			return nil, e
		}

		e = syscall.SetsockoptInt(fd, syscall.SOL_SOCKET, SO_REUSEPORT, 1)
		if e != nil {
			return nil, e
		}

		addr4 := syscall.SockaddrInet4{
			Port: addr.Port,
			Addr: [4]byte{addr.IP[0], addr.IP[1], addr.IP[2], addr.IP[3]},
		}

		e = syscall.Bind(fd, &addr4)
		if e != nil {
			return nil, e
		}

		e = syscall.Listen(fd, backlog)

		if e != nil {
			return nil, e
		}

		f := os.NewFile(uintptr(fd), "")
		if f == nil {
			return nil, errors.New("fd is invalid")
		}

		defer f.Close()

		return net.FileListener(f)
	}
}

func GetConnFd(c net.Conn) int64 {
	if t, ok := c.(*net.TCPConn); ok {
		return getTCPConnFd(t)
	}

	tlsv, _ := c.(*tls.Conn)
	cv := reflect.Indirect(reflect.ValueOf(tlsv)).FieldByName("conn").InterfaceData()
	cc := (*net.TCPConn)(unsafe.Pointer(cv[1]))
	return getTCPConnFd(cc)
}

var fdCheck int32 = 1

func getTCPConnFd(c *net.TCPConn) int64 {
	fd := reflect.Indirect(reflect.ValueOf(c)).FieldByName("fd")
	return reflect.Indirect(fd).FieldByName("pfd").FieldByName("Sysfd").Int()
}

func DetectHost() string {
	if h, e := os.Hostname(); e == nil {
		addrs, e := net.LookupHost(h)
		if e == nil && len(addrs) > 0 {
			return addrs[0]
		}
	}

	if eth0, e := net.InterfaceByName("eth0"); e == nil {
		if addrs, e := eth0.Addrs(); e == nil && len(addrs) > 0 {
			return addrs[0].String()
		}
	}

	if eth1, e := net.InterfaceByName("eth1"); e == nil {
		if addrs, e := eth1.Addrs(); e == nil && len(addrs) > 0 {
			return addrs[0].String()
		}
	}

	if eth2, e := net.InterfaceByName("eth2"); e == nil {
		if addrs, e := eth2.Addrs(); e == nil && len(addrs) > 0 {
			return addrs[0].String()
		}
	}
	return ""
}
