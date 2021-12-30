package fsm

import (
	"context"
	"handler/orm"
	"handler/pb"
	"sync"
)

type UnsubReqState struct {
	Ctx context.Context
	Req *pb.UnsubReq
}

func (u *UnsubReqState) Transation() (*pb.Response, error) {
	wg := &sync.WaitGroup{}

	for i := 0; i < len(u.Req.TopicFilters); i++ {
		wg.Add(1)
		go func(i int) {
			orm.CleanSubscription(u.Req.Cred.ProductKey, u.Req.Cred.DeviceName, u.Req.TopicFilters[i])
			wg.Done()
		}(i)
	}
	wg.Wait()

	return &pb.Response{Succeed: true}, nil
}
