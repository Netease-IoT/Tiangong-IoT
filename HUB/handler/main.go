package main

import (
	"flag"
	"fmt"
	"handler/args"
	"handler/kfk"
	"handler/orm"
	"handler/svr"
	"os"
	"os/signal"
	"runtime"
	"syscall"
)

func main() {
	flag.Parse()

	runtime.GOMAXPROCS(*args.MaxProcs)

	dbConf := fmt.Sprintf("%s:%s@(%s)/%s?charset=utf8&parseTime=True&loc=Local", *args.DbUser, *args.DbPswd, *args.DbAddr, *args.DbName)
	orm.Init(dbConf)

	kfk.Init()

	svr.Run()

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGTERM, syscall.SIGINT)
	<-sigChan

	svr.Stop()
}
