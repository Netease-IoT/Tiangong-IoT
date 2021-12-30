package fsm

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"handler/orm"
	"handler/pb"
	"strconv"
	"strings"
)

type CnctReqState struct {
	Ctx       context.Context
	Req       *pb.ConnReq
	Timestamp int64
}

func (c *CnctReqState) Transation() (*pb.Response, error) {
	secret, err := orm.GetDeviceSecret(c.Req.Cred.ProductKey, c.Req.Cred.DeviceName)
	if err != nil {
		return &pb.Response{Succeed: false}, nil
	}

	if !checkToken(c.Req.Token, secret, c.Timestamp) {
		return &pb.Response{Succeed: false}, nil
	}

	orm.SetStatus(c.Req.Cred.ProductKey, c.Req.Cred.DeviceName, 1)

	if c.Req.CleanSession {
		orm.CleanAllSubscription(c.Req.Cred.ProductKey, c.Req.Cred.DeviceName)
	}

	return &pb.Response{Succeed: true}, nil
}

func checkToken(token, secret string, ts int64) bool {
	v := strings.Split(token, ":")
	if len(v) != 2 {
		return false
	}

	passwd := strings.ToLower(v[1])
	counter := ts / 300

	if passwd == genPasswd(counter, secret) || passwd == genPasswd(counter-1, secret) || passwd == genPasswd(counter+1, secret) {
		return true
	}

	return false
}

func genPasswd(counter int64, secret string) string {
	c := strconv.FormatInt(counter, 10)

	h := hmac.New(sha256.New, []byte(secret))
	h.Write([]byte(c))
	sha := hex.EncodeToString(h.Sum(nil))

	return sha[:20]
}
