package app

import (
	"github.com/go-redis/redis"

	"connector/discovery"
	crpc "connector/rpc"
	"connector/session"
)

var Global = &struct {
	Server *Server
	Stats  Stats

	RedisClient    *redis.ClusterClient
	RpcClient      crpc.HandlerClient
	Discovery      discovery.ServiceDiscovery
	SessionStorage session.SessionStorage
	SessionPrefix  string
}{
	Stats: newStats(),
}
