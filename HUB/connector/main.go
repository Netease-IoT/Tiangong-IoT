package main

import (
	"flag"
	"os"
	"os/signal"
	"runtime"
	"sync"
	"syscall"

	"connector/acceptor"
	"connector/app"
	"connector/args"
	"connector/discovery"
	crpc "connector/rpc"
	"connector/session"
	"connector/util"
)

const extraFd = 1000

func init() {
	flag.Parse()
	app.ClientCtxs = make([]app.ClientCtx, *args.MaxConns+extraFd)
	for i := 0; i < len(app.ClientCtxs); i += 1 {
		app.ClientCtxs[i].Fd = i
	}
}

func main() {
	if *args.Help {
		flag.PrintDefaults()
		os.Exit(1)
	}

	flag.VisitAll(func(f *flag.Flag) {
		util.V(3).Infof("%s:  %v", f.Name, f.Value)
	})

	runtime.GOMAXPROCS(*args.MaxProcs)

	if *args.RedisClusterAddr == "" {
		util.Errorln("Redis cluster address is not specified")
		flag.PrintDefaults()
		os.Exit(2)
	}
	util.Info("Redis cluster address: ", *args.RedisClusterAddr)

	redisClient := util.GetClusterClient(*args.RedisClusterAddr)
	app.Global.RedisClient = redisClient

	ss := session.NewRedisSessionStorage(redisClient)
	app.Global.SessionStorage = ss

	if *args.UseConsul && *args.ConsulAddr == "" {
		util.Errorln("Consul address is not specified")
		flag.PrintDefaults()
		os.Exit(2)
	}
	util.Info("Consul address: ", *args.ConsulAddr)

	if *args.RpcService == "" {
		util.Errorln("Rpc service address is not specified")
		flag.PrintDefaults()
		os.Exit(2)
	}
	util.Info("Rpc service address: ", *args.RpcService)

	var sdDevice discovery.ServiceDiscovery

	consulAddr := *args.ConsulAddr
	if !*args.UseConsul {
		sdDevice = nil
	} else {
		sdDevice = discovery.NewConsulService(consulAddr, *args.ServiceRefresh,
			[]string{*args.RpcService})
	}
	app.Global.Discovery = sdDevice

	hc := crpc.NewHandlerClient(&crpc.HandlerClientOption{
		ServiceDiscovery: sdDevice,
		ServiceName:      *args.RpcService,
	})
	app.Global.RpcClient = hc

	acceptor := acceptor.NewHTTPAcceptor(*args.Host)
	app.Global.SessionPrefix = acceptor.AddrString()

	if *args.EnableTLS && (*args.CertFile == "" || *args.KeyFile == "") {
		util.Errorln("TLS is enabled, by no certFile or keyFile specified")
		flag.PrintDefaults()
		os.Exit(2)
	}

	sopt := &app.ServerOptions{
		TCPPort:    *args.TcpPort,
		WSPort:     *args.WsPort,
		EnableTLS:  *args.EnableTLS,
		TLSPort:    *args.TLSPort,
		WSSPort:    *args.WssPort,
		CertFile:   *args.CertFile,
		KeyFile:    *args.KeyFile,
		ClientHost: *args.ClientHost,
		ReusePort:  *args.EnableReusePort,
	}

	util.Info("Start to launch server...")

	s := app.NewServer(sopt)
	app.Global.Server = s

	wg := &sync.WaitGroup{}
	wg.Add(2)

	go s.Run(wg)
	go acceptor.Run(wg)

	signalChan := make(chan os.Signal, 1)
	signal.Notify(signalChan, syscall.SIGTERM, syscall.SIGINT)
	<-signalChan

	go acceptor.Stop()
	go s.Stop()

	wg.Wait()
}
