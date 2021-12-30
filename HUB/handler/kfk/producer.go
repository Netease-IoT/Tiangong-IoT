package kfk

import (
	"encoding/json"
	"fmt"
	"handler/args"

	"gopkg.in/confluentinc/confluent-kafka-go.v1/kafka"
)

var p *kafka.Producer

type Message struct {
	ProductKey string `json:"ProductKey"`
	DeviceName string `json:"DeviceName"`
	Topic      string `json:"Topic"`
	Qos        int32  `json:"Qos"`
	MessageId  string `json:"MessageId"`
	Content    string `json:"Content"`
	Timestamp  int64  `json:"Timestamp"`
}

func Init() {
	var err error
	if p, err = kafka.NewProducer(&kafka.ConfigMap{"bootstrap.servers": *args.KafkaBroker}); err != nil {
		panic(fmt.Sprintf("Failed to create producer: %v", err))
	}
}

func Produce(msg *Message) error {
	deliveryChan := make(chan kafka.Event)
	defer close(deliveryChan)

	topic := "IOT-" + msg.ProductKey
	value, _ := json.Marshal(msg)

	err := p.Produce(&kafka.Message{
		TopicPartition: kafka.TopicPartition{Topic: &topic, Partition: kafka.PartitionAny},
		Value:          value,
	}, deliveryChan)

	if err != nil {
		return err
	}

	e := <-deliveryChan
	m := e.(*kafka.Message)

	if m.TopicPartition.Error != nil {
		fmt.Printf("Delivery failed: %v\n", m.TopicPartition.Error)
	} else {
		fmt.Printf("Delivered message to topic %s [%d] at offset %v\n",
			*m.TopicPartition.Topic, m.TopicPartition.Partition, m.TopicPartition.Offset)
	}

	return m.TopicPartition.Error
}
