package fsm

import (
	"context"
	"handler/kfk"
	"handler/orm"
	"handler/pb"
	"time"
)

type DiscReqState struct {
	Ctx     context.Context
	Req     *pb.DiscReq
	Produce ProduceFun
}

func (d *DiscReqState) Transation() (*pb.Response, error) {
	orm.SetStatus(d.Req.Cred.ProductKey, d.Req.Cred.DeviceName, 0)

	if d.Req.CleanSession {
		orm.CleanAllSubscription(d.Req.Cred.ProductKey, d.Req.Cred.DeviceName)
	}

	if d.Req.HasWill {
		msg := &kfk.Message{
			ProductKey: d.Req.Cred.ProductKey,
			DeviceName: d.Req.Cred.DeviceName,
			Topic:      d.Req.WillTopic,
			Qos:        d.Req.WillQos,
			MessageId:  d.Req.MsgId,
			Content:    d.Req.WillMessage,
			Timestamp:  time.Now().UnixNano() / 1000000,
		}
		doPublish(msg, d.Produce)
	}

	return &pb.Response{Succeed: true}, nil
}
