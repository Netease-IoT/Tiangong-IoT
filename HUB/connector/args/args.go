package args

import (
	"flag"
	"time"
)

var (
	// Server listening arguments {{
	MaxConns = flag.Int("maxConns",
		getFromEnvInt("MQTTHUB_MAX_CONNS", 10000),
		"MQTTHUB_MAX_CONNS Max connections the server can holds")

	TcpPort = flag.Int("tcpPort",
		getFromEnvInt("MQTTHUB_TCP_PORT", 2883),
		"MQTTHUB_TCP_PORT Mqtt over tcp listens to")

	WsPort = flag.Int("wsPort",
		getFromEnvInt("MQTTHUB_WS_PORT", 2443),
		"MQTTHUB_WS_PORT Mqtt over ws listens to")

	EnableTLS = flag.Bool("enableTLS",
		getFromEnvBool("MQTTHUB_ENABLE_TLS", false),
		"MQTTHUB_ENABLE_TLS Whether the server support tls")

	CertFile = flag.String("certFile",
		getFromEnvString("MQTTHUB_CERT_FILE", ""),
		"MQTTHUB_CERT_FILE TLS cert file path")

	KeyFile = flag.String("keyFile",
		getFromEnvString("MQTTHUB_KEY_FILE", ""),
		"MQTTHUB_KEY_FILE TLS key file path")

	TLSPort = flag.Int("tlsPort",
		getFromEnvInt("MQTTHUB_TLS_PORT", 1883),
		"MQTTHUB_TLS_PORT Mqtt over tls listens to")

	WssPort = flag.Int("wssPort",
		getFromEnvInt("MQTTHUB_WSS_PORT", 1443),
		"MQTTHUB_WSS_PORT Mqtt over wss listens to")

	ClientHost = flag.String("clientHost",
		getFromEnvString("MQTTHUB_CLIENT_HOST", "0.0.0.0"),
		"MQTTHUB_CLIENT_HOST Host address for client connecting")

	EnableReusePort = flag.Bool("enableReusePort",
		getFromEnvBool("MQTTHUB_ENABLE_REUSEPORT", false),
		"MQTTHUB_ENABLE_REUSEPORT Enable reuse port on linux")
	// }}

	// Server properties {{
	ServerCloseWait = flag.Duration("serverCloseWait",
		getFromEnvDuration("MQTTHUB_SERVER_CLOSEWAIT", 1*time.Minute),
		"MQTTHUB_SERVER_CLOSEWAIT Wait time for server closing gracefully")

	ClientConnTimeout = flag.Duration("clientConnTimeout",
		getFromEnvDuration("MQTTHUB_CONN_TIMEOUT", 10*time.Second),
		"MQTTHUB_CONN_TIMEOUT Client connect timeout")
	// }}

	// Redis arguemts {{
	RedisClusterAddr = flag.String("redisClusterAddr",
		getFromEnvString("MQTTHUB_REDIS_CLUSTER_ADDR", "127.0.0.1:6379"),
		"MQTTHUB_REDIS_CLUSTER_ADDR Redis cluster address")

	RedisReqTimeout = flag.Duration("redisReqTimeout",
		getFromEnvDuration("MQTTHUB_REDIS_REQ_TIMEOUT", 500*time.Millisecond),
		"MQTTHUB_REDIS_REQ_TIMEOUT Redis request timeout")

	RedisPoolSize = flag.Int("redisPoolSize",
		getFromEnvInt("MQTTHUB_REDIS_POOL_SIZE", 20),
		"MQTTHUB_REDIS_POOL_SIZE Redis connection pool size per node")
	// }}

	// Session arguments {{
	RedisSessionTimeout = flag.Duration("redisSessionTimeout",
		getFromEnvDuration("MQTTHUB_SESSION_TIMEOUT", 20*time.Minute),
		"MQTTHUB_SESSION_TIMEOUT Session timeout in redis")

	RedisSessionRefresh = flag.Duration("redisSessionRefresh",
		getFromEnvDuration("MQTTHUB_SESSION_REFRESH", 15*time.Minute),
		"MQTTHUB_SESSION_REFRESH Session refresh interval")
	// }}

	// publish message arguments {{
	GetMsgTimeout = flag.Duration("getMsgTimeout",
		getFromEnvDuration("MQTTHUB_GET_MSG_TIMEOUT", 1*time.Second),
		"MQTTHUB_GET_MSG_TIMEOUT Timeout of getting msg from redis")

	MsgCacheSize = flag.Int("msgCacheSize",
		getFromEnvInt("MQTTHUB_MSG_CACHE_SIZE", 10000),
		"MQTTHUB_MSG_CACHE_SIZE Message cache size")

	MqttWriteTimeout = flag.Duration("writeTimeout",
		getFromEnvDuration("MQTTHUB_WRITE_TIMEOUT", 2*time.Second),
		"MQTTHUB_WRITE_TIMEOUT Timeout of writing to mqtt connection")

	WaitAckTimeout = flag.Duration("waitAckTimeout",
		getFromEnvDuration("MQTTHUB_WAIT_ACK_TIMEOUT", 3*time.Second),
		"Timeout of waiting publish ack")
	// }}

	// service discovery arguments
	UseConsul = flag.Bool("useConsul",
		getFromEnvBool("MQTTHUB_USE_CONSUL", false),
		"MQTTHUB_USE_CONSUL Use consul for service discovery")

	ConsulAddr = flag.String("consulAddr",
		getFromEnvString("MQTTHUB_CONSUL_ADDR", ""),
		"MQTTHUB_CONSUL_ADDR Consul address")

	ServiceRefresh = flag.Duration("consulRefresh",
		getFromEnvDuration("MQTTHUB_CONSUL_REFRESH", 10*time.Second),
		"MQTTHUB_CONSUL_REFRESH Refresh service interval")

	RpcService = flag.String("rpcService",
		getFromEnvString("MQTTHUB_RPC_SERVICE", "handler"),
		"MQTTHUB_RPC_Service name of handler")

	// Acceptor arguments
	Host = flag.String("host",
		getFromEnvString("MQTTHUB_HOST", ""),
		"MQTTHUB_HOST Host address for internal invocation")

	AcceptorReadTimeout = flag.Duration("acceptorReadTimeout",
		getFromEnvDuration("MQTTHUB_ACC_READ_TIMEOUT", 3*time.Second),
		"MQTTHUB_ACC_READ_TIMEOUT Read timeout for acceptor service")

	// for rpc, max timeout is 25 seconds
	AcceptorWriteTimeout = flag.Duration("acceptorWriteTimeout",
		getFromEnvDuration("MQTTHUB_ACC_WRITE_TIMEOUT", 30*time.Second),
		"MQTTHUB_ACC_WRITE_TIMEOUT Max process timeout for acceptor service")

	AcceptorIdleTimeout = flag.Duration("acceptorIdleTimeout",
		getFromEnvDuration("MQTTHUB_ACC_IDLE_TIMEOUT", 120*time.Second),
		"MQTTHUB_ACC_IDLE_TIMEOUT Idle tiemout for keepalive connection")

	AcceptorCloseWait = flag.Duration("acceptorCloseWait",
		getFromEnvDuration("MQTTHUB_ACC_CLOSE_WAIT", 30*time.Second),
		"MQTTHUB_ACC_CLOSE_WAIT Wait time when accceptor closing")

	// Internal invocation timeout
	InternalInvokeTimeout = flag.Duration("internalInvokeTimeout",
		getFromEnvDuration("MQTT_INTERNAL_INVOKE_TIMEOUT", 3*time.Second),
		"MQTT_INTERNAL_INVOKE_TIMEOUT Internal http request timeout")

	// go runtime
	MaxProcs = flag.Int("maxProcs",
		getFromEnvInt("MQTTHUB_MAX_GOPROCS", 0),
		"MQTTHUB_MAX_GOPROCS Go max procs")

	// Others
	Help = flag.Bool("help", false, "Show help")
)
