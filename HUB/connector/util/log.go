package util

import "github.com/golang/glog"

// golang/glog wrapper
var (
	V = glog.V

	Info      = glog.Info
	InfoDepth = glog.InfoDepth
	Infoln    = glog.Infoln
	Infof     = glog.Infof

	Warning      = glog.Warning
	WarningDepth = glog.WarningDepth
	Warningln    = glog.Warningln
	Warningf     = glog.Warningf

	Error      = glog.Error
	ErrorDepth = glog.ErrorDepth
	Errorln    = glog.Errorln
	Errorf     = glog.Errorf

	Fatal      = glog.Fatal
	FatalDepth = glog.FatalDepth
	Fatalln    = glog.Fatalln
	Fatalf     = glog.Fatalf

	Exit      = glog.Exit
	ExitDepth = glog.ExitDepth
	Exitln    = glog.Exitln
	Exitf     = glog.Exitf
)
