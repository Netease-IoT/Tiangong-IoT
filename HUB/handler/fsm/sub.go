package fsm

import (
	"context"
	"errors"
	"handler/orm"
	"handler/pb"
	"sync"
)

type SubReqState struct {
	Ctx context.Context
	Req *pb.SubReq
}

func (s *SubReqState) Transation() (*pb.SubRes, error) {
	if len(s.Req.TopicFilters) != len(s.Req.Qoss) {
		return nil, errors.New("invalid parameters")
	}

	r := &pb.SubRes{
		Qoss: make([]int32, len(s.Req.Qoss)),
	}

	wg := &sync.WaitGroup{}
	for i := 0; i < len(s.Req.TopicFilters); i++ {
		wg.Add(1)
		go handleSub(i, s.Req, r, wg)
	}
	wg.Wait()

	return r, nil
}

func handleSub(index int, req *pb.SubReq, res *pb.SubRes, wg *sync.WaitGroup) {
	defer wg.Done()

	topicClass, err := GetTopicClass(req.Cred.ProductKey, req.Cred.DeviceName, req.TopicFilters[index], 1)
	if err != nil {
		res.Qoss[index] = -1
		return
	}

	if req.Qoss[index] < int32(topicClass.Qos) {
		res.Qoss[index] = req.Qoss[index]
	} else {
		res.Qoss[index] = int32(topicClass.Qos)
	}

	ts := &orm.TopicSubscription{
		ProductKey:  req.Cred.ProductKey,
		DeviceName:  req.Cred.DeviceName,
		TopicId:     topicClass.TopicId,
		TopicFilter: req.TopicFilters[index],
		Qos:         int8(res.Qoss[index]),
		TopicType:   topicClass.TopicType,
		DeleteFlag:  0,
	}

	orm.AddSubscription(ts)
	return
}
