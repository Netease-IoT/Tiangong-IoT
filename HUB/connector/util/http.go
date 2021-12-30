package util

import (
	"net/http"

	"connector/args"
)

var c *http.Client

type CommonResp struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

func init() {
	tr := &http.Transport{
		MaxIdleConns:        100,
		MaxIdleConnsPerHost: 100,
	}

	c = &http.Client{
		Timeout:   *args.InternalInvokeTimeout,
		Transport: tr,
	}
}

func GetHttpClient() *http.Client {
	return c
}
