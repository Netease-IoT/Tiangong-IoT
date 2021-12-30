package acceptor

import (
	"context"
	"fmt"
	"net"
	"net/http"
	"strconv"
	"sync"

	"connector/app"
	"connector/args"
	"connector/util"
)

type Acceptor interface {
	AddrString() string
	Stop()
	Run(*sync.WaitGroup)
}

var _ Acceptor = (*httpAcceptor)(nil)

type httpAcceptor struct {
	host       string
	addrString string
	lis        net.Listener
	server     *http.Server

	w *sync.WaitGroup
}

func (h *httpAcceptor) AddrString() string {
	return h.addrString
}

func (h *httpAcceptor) Stop() {
	util.Info("Acceptor is going to stop")

	c, cancel := context.WithTimeout(context.Background(), *args.AcceptorCloseWait)
	defer cancel()

	h.server.Shutdown(c)

	util.Info("Acceptor stopped")
	h.w.Done()
}

func NewHTTPAcceptor(host string) Acceptor {
	if host == "" {
		host = util.DetectHost()
	}

	haddr := net.JoinHostPort(host, "0")
	lis, err := util.NewTCPListener(haddr, false)

	if err != nil {
		util.Errorf("Acceptor new listener error: %e", err)
		return nil
	}

	addrString := lis.Addr().String()

	util.Info("Acceptor listen on: ", addrString)

	return &httpAcceptor{
		host:       host,
		lis:        lis,
		addrString: addrString,
	}
}

func (h *httpAcceptor) Run(w *sync.WaitGroup) {
	h.w = w

	h.server = &http.Server{
		Handler:      &acceptorHandler{h: h},
		ReadTimeout:  *args.AcceptorReadTimeout,
		WriteTimeout: *args.AcceptorWriteTimeout,
		IdleTimeout:  *args.AcceptorIdleTimeout,
	}

	util.Info("Acceptor starts to run...")
	if e := h.server.Serve(h.lis); e != nil {
		util.Errorf("Acceptor starts to run error: %v", e)
	}
}

//------------------------------------------------

type acceptorHandler struct {
	h *httpAcceptor
}

const kickPath = "/v3/kick"
const healthz = "/v3/healthz"
const rpcPath = "/v3/rpc"
const publishPath = "/v3/publish"

func (ah *acceptorHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	u := r.URL.EscapedPath()

	if u == healthz {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("ok"))
		return
	}

	// we will check parameter later, so ignore error here
	r.ParseForm()

	if u == publishPath {
		publishToClient(ah.h, r, w)
	} else if u == kickPath {
		kickClient(ah.h, r, w)
	} else if u == rpcPath {
		rpcToClient(ah.h, r, w)
	} else {
		util.Warningf("Unkown request: %s", u)
		w.WriteHeader(http.StatusBadRequest)
	}
}

func queryContext(r *http.Request) (*app.ClientCtx, string) {
	fd := r.FormValue("fd")
	if fd == "" {
		return nil, "Fd is empty"
	}

	fdi, e := strconv.Atoi(fd)
	if e == nil {
		ctx := app.GetCtxForFd(int64(fdi))
		if ctx != nil {
			return ctx, ""
		} else {
			return nil, "No fd found"
		}
	} else {
		return nil, "Fd is not number"
	}
}

func kickClient(h *httpAcceptor, r *http.Request, w http.ResponseWriter) {
	ctx, errMsg := queryContext(r)
	if ctx != nil {
		deviceName := r.FormValue("deviceName")
		productKey := r.FormValue("productKey")

		if deviceName == "" || productKey == "" {
			util.Warningf("Bad kick client request, %v", r)
			w.WriteHeader(http.StatusBadRequest)
			return
		}

		ctx.Kick(deviceName, productKey)
		w.WriteHeader(http.StatusOK)
		return
	}

	util.Warningf("Bad kick client request, %v, %s", r, errMsg)
	w.WriteHeader(http.StatusBadRequest)
}

const defaultTimeoutMs = 10000 // 10 seconds
const maxTimeoutMs = 25000     // 25 seconds, must less than args.AcceptorWriteTimeout

func rpcToClient(h *httpAcceptor, r *http.Request, w http.ResponseWriter) {
	ctx, errMsg := queryContext(r)

	if ctx != nil {
		deviceName := r.FormValue("deviceName")
		productKey := r.FormValue("productKey")
		content := r.FormValue("content")
		timeout := r.FormValue("timeout") // millisecond

		if deviceName == "" || productKey == "" || content == "" {
			util.Warningf("Bad rpc request, %v", r)
			w.WriteHeader(http.StatusBadRequest)
			return
		}

		timeoutMs := defaultTimeoutMs // millisecond
		if timeout != "" {
			var err error
			timeoutMs, err = strconv.Atoi(timeout)
			if err != nil {
				util.Warningf("Bad rpc request, invalid timeout, %v", r)
				w.WriteHeader(http.StatusBadRequest)
				return
			}
		}

		if timeoutMs > maxTimeoutMs {
			timeoutMs = maxTimeoutMs
		}

		code, resp := ctx.RpcToClient(deviceName, productKey, content, timeoutMs)

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		body := fmt.Sprintf("{\"code\": %d, \"response\": \"%s\"}", code, resp)
		w.Write([]byte(body))
		return
	}

	// empty ctx, should return offline?
	util.Warningf("Bad rpc request, %v, %s", r, errMsg)
	w.WriteHeader(http.StatusInternalServerError)
}

func publishToClient(h *httpAcceptor, r *http.Request, w http.ResponseWriter) {
	ctx, errMsg := queryContext(r)
	if ctx != nil {
		deviceName := r.FormValue("deviceName")
		productKey := r.FormValue("productKey")
		msgId := r.FormValue("messageId")
		topic := r.FormValue("topic")
		qosStr := r.FormValue("qos")

		if deviceName == "" || productKey == "" || msgId == "" ||
			topic == "" || qosStr == "" {
			util.Warningf("Bad publish message request, %v", r)
			w.WriteHeader(http.StatusBadRequest)
			return
		}

		qos, e := strconv.Atoi(qosStr)
		if e != nil {
			util.Warningf("Bad publish message request, qos is not number")
			w.WriteHeader(http.StatusBadRequest)
			return
		}

		if qos != 0 && qos != 1 {
			util.Warningf("Bad publish message request, qos should be 0 or 1")
			w.WriteHeader(http.StatusBadRequest)
			return
		}

		code := ctx.PublishMessage(deviceName, productKey, msgId, topic, int8(qos))

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		body := fmt.Sprintf("{\"code\": %d}", code)
		w.Write([]byte(body))
		return
	}

	util.Warningf("Bad publish message request, %v, error: %s", r, errMsg)
	w.WriteHeader(http.StatusBadRequest)
}
