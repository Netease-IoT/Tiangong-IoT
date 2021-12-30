# 简介

handler 模块负责物联网设备接入的鉴权及消息数据处理。

## 依赖项

- Mysql
需提供 Mysql 库连接地址，并导入以下表：
```sql
DROP TABLE IF EXISTS `device_info`;
CREATE TABLE `device_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `product_key` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '产品KEY',
  `device_name` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '设备名',
  `device_secret` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '设备密钥',
  `create_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_active_at` timestamp NULL DEFAULT NULL COMMENT '上次活跃时间',
  `apply_id` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '申请ID',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-offline,1-online',
  `delete_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-valid,1-deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `device_name` (`device_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

DROP TABLE IF EXISTS `topic_subscription`;
CREATE TABLE `topic_subscription` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `product_key` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '产品KEY',
  `device_name` varchar(64) NOT NULL,
  `topic_id` varchar(32) NOT NULL,
  `topic_filter` varchar(128) NOT NULL,
  `qos` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0-qos0, 1-qos1',
  `topic_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0-custom, 1-system, 2-ota',
  `subscribe_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-valid, 1-deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `product_device_topic_id` (`product_key`,`device_name`,`topic_id`),
  KEY `product_device_topic_filter` (`product_key`,`device_name`,`topic_filter`),
  KEY `topic_product` (`topic_id`,`product_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

DROP TABLE IF EXISTS `sys_topic_info`;
CREATE TABLE `sys_topic_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `topic_id` varchar(32) NOT NULL,
  `topic_filter` varchar(128) NOT NULL,
  `operation` tinyint(4) NOT NULL COMMENT '0-all, 1-sub, 2-pub',
  `qos` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0-qos0, 1-qos1',
  `topic_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '0-custom, 1-system, 2-ota',
  `description` varchar(256) NOT NULL DEFAULT '',
  `create_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-valid, 1-deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `topic_id` (`topic_id`),
  UNIQUE KEY `topic_filter` (`topic_filter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

DROP TABLE IF EXISTS `custom_topic_info`;
CREATE TABLE `custom_topic_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `product_key` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '产品KEY',
  `topic_id` varchar(32) NOT NULL,
  `topic_filter` varchar(128) NOT NULL,
  `operation` tinyint(4) NOT NULL COMMENT '0-all, 1-sub, 2-pub',
  `qos` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0-qos0, 1-qos1',
  `topic_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0-custom, 1-system, 2-ota',
  `description` varchar(256) NOT NULL DEFAULT '',
  `create_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-valid, 1-deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `topic_id` (`topic_id`),
  UNIQUE KEY `product_topic_filter` (`product_key`,`topic_filter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
```
- Kafka
需提供 Kafka 连接地址，并本地安装 librdkafka

## 编译及测试

handler 目录下

```bash
make # 编译生成 handler 二进制可执行文件
make cov # 测试覆盖率
```

### 运行

命令行参数启动

```bash
./handler -rpcPort=":13000" -kafkaBroker="localhost:9092" -dbAddr="localhost:3306" -dbName="iot" -dbUser="root" -dbPswd="1234" -maxProcs=4
```

或者环境变量启动

```bash
export HANDLER_RPC_PORT=":13000"
export HANDLER_KAFKA_BROKER="localhost:9092"
export HANDLER_DB_ADDR="localhost:3306"
export HANDLER_DB_NAME="iot"
export HANDLER_DB_USER="root"
export HANDLER_DB_PSWD="1234"
export MQTTHUB_MAX_GOPROCS=4
./handler
```
