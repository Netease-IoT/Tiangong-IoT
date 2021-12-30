package app

import (
	"crypto/tls"
	"net"
	"net/http"
	"strconv"
	"sync"
	"time"

	"connector/args"
	"connector/proto"
	"connector/util"
)

type ServerOptions struct {
	TCPPort int
	WSPort  int

	EnableTLS bool
	TLSPort   int
	WSSPort   int
	CertFile  string
	KeyFile   string
	ReusePort bool

	ClientHost string
}

func NewServer(opt *ServerOptions) *Server {
	return &Server{
		options: opt,
		StopCh:  make(chan struct{}),
	}
}

type Server struct {
	options *ServerOptions

	w *sync.WaitGroup

	ws  *http.Server
	wss *http.Server

	tls net.Listener
	tcp net.Listener

	StopCh chan struct{}
}

func (s *Server) Run(w *sync.WaitGroup) {
	s.w = w

	if s.options.ClientHost == "" {
		util.Info("Client host is not specified, auto detect")
		s.options.ClientHost = util.DetectHost()
	}
	util.Infof("Client host: %s", s.options.ClientHost)

	s.setUpListener()
}

func (s *Server) Stop() {
	util.Info("Server is going to stop")
	close(s.StopCh)

	if s.ws != nil {
		s.ws.Close()
	}
	if s.wss != nil {
		s.wss.Close()
	}

	if s.tcp != nil {
		s.tcp.Close()
	}
	if s.tls != nil {
		s.tls.Close()
	}

	cnt := len(ClientCtxs)
	for i := 0; i < cnt; i += 1 {
		c := &ClientCtxs[i]
		c.Stop()
	}

	util.Info("Listener is stop, wait for connections stoping...")

	time.Sleep(*args.ServerCloseWait)

	util.Info("Server stopped")
	s.w.Done()
}

func (s *Server) setUpListener() {
	util.Infof("Enable reuse port for linux is %v", s.options.ReusePort)

	ws := &http.Server{
		Handler:      &wsRequestHandler{s: s},
		ReadTimeout:  *args.ClientConnTimeout,
		WriteTimeout: *args.ClientConnTimeout,
	}

	wsaddr := net.JoinHostPort(s.options.ClientHost, strconv.Itoa(s.options.WSPort))
	util.Infof("Websocket address: %s", wsaddr)

	wsl, e := util.NewTCPListener(wsaddr, s.options.ReusePort)
	if e != nil {
		util.Fatalf("New websocket listener error: %v", e)
	}

	go ws.Serve(wsl)
	s.ws = ws
	util.Infof("Start to listen websocket...")

	tcpaddr := net.JoinHostPort(s.options.ClientHost, strconv.Itoa(s.options.TCPPort))
	util.Infof("Tcp addr: %s", tcpaddr)
	tcpl, e := util.NewTCPListener(tcpaddr, s.options.ReusePort)
	if e != nil {
		util.Fatalf("New tcp listener error: %v", e)
	}

	s.tcp = tcpl
	go startListen(s, tcpl)
	util.Info("Start to listen tcp...")

	if s.options.EnableTLS {
		util.Info("TLS is enabled")
		wss := &http.Server{
			Handler:      &wsRequestHandler{s: s},
			ReadTimeout:  *args.ClientConnTimeout,
			WriteTimeout: *args.ClientConnTimeout,
		}

		wssaddr := net.JoinHostPort(s.options.ClientHost, strconv.Itoa(s.options.WSSPort))
		util.Infof("Websocket security address: %v", wssaddr)
		wssl, e := util.NewTCPListener(wssaddr, s.options.ReusePort)
		if e != nil {
			util.Fatalf("New websocket security listener error: %v", e)
		}
		s.wss = wss
		go wss.ServeTLS(wssl, s.options.CertFile, s.options.KeyFile)
		util.Info("Start to listen on wss...")

		tlsaddr := net.JoinHostPort(s.options.ClientHost, strconv.Itoa(s.options.TLSPort))
		util.Infof("TLS address: %v", tlsaddr)
		tlsl, e := util.NewTCPListener(tlsaddr, s.options.ReusePort)
		if e != nil {
			util.Fatalf("New TLS listener error: %v", e)
		}
		s.tls = tlsl
		go startListenTLS(s, s.tls, s.options.CertFile, s.options.KeyFile)
		util.Info("Start to listen on tls...")
	}
}

type wsRequestHandler struct {
	s *Server
}

func (h *wsRequestHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	if c := proto.HandleWsHandshake(w, r); c != nil {
		go handleConnection(h.s, c, true)
	}
}

func startListen(s *Server, l net.Listener) {
	for {
		select {
		case <-s.StopCh:
			return
		default:
		}

		c, err := l.Accept()
		if err == nil {
			go handleConnection(s, c, false)
		} else {
			select {
			case <-s.StopCh:
				return
			default:
			}

			util.Errorf("Accept error, listener: %+v, error: %v", l, err)
		}
	}
}

func startListenTLS(s *Server, l net.Listener, certFile, keyFile string) {
	var err error

	config := &tls.Config{}
	config.Certificates = make([]tls.Certificate, 1)
	config.Certificates[0], err = tls.LoadX509KeyPair(certFile, keyFile)
	if err != nil {
		panic("Start TLS listen error")
	}

	tlsListener := tls.NewListener(l, config)
	startListen(s, tlsListener)
}
