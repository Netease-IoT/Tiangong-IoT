# 简介
connector 连接服务器，负责接受、维护设备的 MQTT 连接，并将消息转发给 handler 模块处理。

## 依赖项

- redis cluster
- HUB handler 模块
- consul（可选）

## 运行命令

```
./connector
    -clientHost 0.0.0.0
    -host 192.168.0.29
    -redisClusterAddr 192.168.0.21:6380,192.168.0.25:6380
    -serverCloseWait=1s
    -logtostderr
    -v 5
    -useConsul=false
    -rpcService 192.168.0.28:13000
    -tcpPort=1883
    -maxProcs 4
```

### 重要参数

* TcpPort

```
TcpPort = flag.Int("tcpPort",
		getFromEnvInt("MQTTHUB_TCP_PORT", 2883),
		"MQTTHUB_TCP_PORT Mqtt over tcp listens to")
```
mqtt tcp监听端口

* TLSPort

```
TLSPort = flag.Int("tlsPort",
        getFromEnvInt("MQTTHUB_TLS_PORT", 1883),
        "MQTTHUB_TLS_PORT Mqtt over tls listens to")
```
mqtt security port 监听端口

* Wsport
```
WsPort = flag.Int("wsPort",
    getFromEnvInt("MQTTHUB_WS_PORT", 2443),
    "MQTTHUB_WS_PORT Mqtt over ws listens to")
```
web socket 端口

* WssPort
```
WssPort = flag.Int("wssPort",
    getFromEnvInt("MQTTHUB_WSS_PORT", 1443),
    "MQTTHUB_WSS_PORT Mqtt over wss listens to")
```
web security socket 端口

* logtostderr

```
日志打印参数，此参数为使日志打印到标准输入输出
其他参数请参考：https://github.com/golang/glog/blob/master/glog.go
```
