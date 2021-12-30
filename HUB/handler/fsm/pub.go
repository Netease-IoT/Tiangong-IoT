package fsm

import (
	"context"
	"errors"
	"handler/kfk"
	"handler/pb"
)

type ProduceFun func(msg *kfk.Message) error

type PubReqState struct {
	Ctx     context.Context
	Req     *pb.PubReq
	Produce ProduceFun
}

func (p *PubReqState) Transation() (*pb.Response, error) {

	msg := &kfk.Message{
		ProductKey: p.Req.Cred.ProductKey,
		DeviceName: p.Req.Cred.DeviceName,
		Topic:      p.Req.Topic,
		Qos:        p.Req.Qos,
		MessageId:  p.Req.MsgId,
		Content:    p.Req.Content,
		Timestamp:  p.Req.Timestamp,
	}

	if err := doPublish(msg, p.Produce); err != nil {
		return &pb.Response{Succeed: false}, nil
	}

	return &pb.Response{Succeed: true}, nil
}

func doPublish(msg *kfk.Message, produce ProduceFun) error {
	topicClass, err := GetTopicClass(msg.ProductKey, msg.DeviceName, msg.Topic, 2)
	if err != nil {
		return err
	}

	if topicClass.Qos < int8(msg.Qos) {
		return errors.New("invalid qos")
	}

	if err := produce(msg); err != nil {
		return err
	}

	return nil
}
