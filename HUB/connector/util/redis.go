package util

import (
	"strings"
	"sync"
	"time"

	"github.com/go-redis/redis"

	"connector/args"
)

var initOnce sync.Once

var client *redis.ClusterClient

const poolTimeout = 512 * time.Millisecond
const idleTimeout = 1 * time.Minute
const idleCheckFrequency = 5 * time.Minute
const maxRetry = 2
const maxRedirects = 8

func GetClusterClient(clusterAddr string) *redis.ClusterClient {
	initOnce.Do(func() {
		addrs := strings.Split(clusterAddr, ",")
		opt := &redis.ClusterOptions{
			Addrs: addrs,
			// XXX: enable readonly again when master overrun?
			ReadOnly:       false,
			RouteByLatency: false,
			MaxRedirects:   maxRedirects,

			MaxRetries:   maxRetry,
			DialTimeout:  *args.RedisReqTimeout,
			ReadTimeout:  *args.RedisReqTimeout,
			WriteTimeout: *args.RedisReqTimeout,

			PoolSize:           *args.RedisPoolSize,
			PoolTimeout:        poolTimeout,
			IdleTimeout:        idleTimeout,
			IdleCheckFrequency: idleCheckFrequency,
		}

		client = redis.NewClusterClient(opt)
		if client == nil {
			panic("Redis cluster initialize fail")
		}
	})

	return client
}
