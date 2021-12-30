package svr

import (
	"context"
	"fmt"
	"handler/args"
	"handler/fsm"
	"handler/kfk"
	"handler/pb"
	"net"
	"time"

	"google.golang.org/grpc"
)

var svr *grpc.Server

type MqttGrpcServer struct {
	pb.UnimplementedMQTTGRPCServer
}

func (m *MqttGrpcServer) Connect(ctx context.Context, req *pb.ConnReq) (*pb.Response, error) {
	state := &fsm.CnctReqState{ctx, req, time.Now().Unix()}
	return state.Transation()
}

func (m *MqttGrpcServer) Disconnect(ctx context.Context, req *pb.DiscReq) (*pb.Response, error) {
	state := &fsm.DiscReqState{ctx, req, kfk.Produce}
	return state.Transation()
}

func (m *MqttGrpcServer) Subscribe(ctx context.Context, req *pb.SubReq) (*pb.SubRes, error) {
	state := &fsm.SubReqState{ctx, req}
	return state.Transation()
}

func (m *MqttGrpcServer) Unsubscribe(ctx context.Context, req *pb.UnsubReq) (*pb.Response, error) {
	state := &fsm.UnsubReqState{ctx, req}
	return state.Transation()
}

func (m *MqttGrpcServer) Publish(ctx context.Context, req *pb.PubReq) (*pb.Response, error) {
	state := &fsm.PubReqState{ctx, req, kfk.Produce}
	return state.Transation()
}

func Run() {
	lis, err := net.Listen("tcp", *args.RpcPort)
	if err != nil {
		panic(fmt.Sprintf("failed to listen: %+v", err))
	}

	svr = grpc.NewServer()
	pb.RegisterMQTTGRPCServer(svr, &MqttGrpcServer{})

	if err := svr.Serve(lis); err != nil {
		panic(fmt.Sprintf("failed to serve: %+v", err))
	}

	go fsm.CacheSysTopicInfo()
	go fsm.CacheCustomTopicInfo()

}

func Stop() {
	svr.GracefulStop()
}
