package args

import (
	"flag"
)

var (
	RpcPort = flag.String("rpcPort",
		getFromEnvString("HANDLER_RPC_PORT", ":13000"),
		"HANDLER_RPC_PORT grpc listen port")

	KafkaBroker = flag.String("kafkaBroker",
		getFromEnvString("HANDLER_KAFKA_BROKER", "localhost:9092"),
		"HANDLER_KAFKA_BROKER kafka broker")

	DbAddr = flag.String("dbAddr",
		getFromEnvString("HANDLER_DB_ADDR", "localhost:3306"),
		"HANDLER_DB_ADDR database address")

	DbName = flag.String("dbName",
		getFromEnvString("HANDLER_DB_NAME", "iot-drill"),
		"HANDLER_DB_NAME database name")

	DbUser = flag.String("dbUser",
		getFromEnvString("HANDLER_DB_USER", "root"),
		"HANDLER_DB_USER database user")

	DbPswd = flag.String("dbPswd",
		getFromEnvString("HANDLER_DB_PSWD", "1234"),
		"HANDLER_DB_PSWD database password")

	MaxProcs = flag.Int("maxProcs",
		getFromEnvInt("MQTTHUB_MAX_GOPROCS", 2),
		"MQTTHUB_MAX_GOPROCS Go max procs")
)
