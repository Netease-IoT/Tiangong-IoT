package fsm

import (
	"handler/kfk"
	"handler/orm"
	"handler/pb"
	"reflect"
	"testing"
	"time"

	"github.com/DATA-DOG/go-sqlmock"
)

func TestShouldPubTransation(t *testing.T) {
	mdb, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mdb.Close()
	orm.Init(mdb)

	sqlRows := sqlmock.NewRows([]string{"product_key", "topic_id", "topic_filter", "operation", "qos", "topic_type"}).
		AddRow("testProd", "cid", "testProd/${deviceName}/topic", 0, 1, 0)
	mock.ExpectQuery("^SELECT (.+) FROM `custom_topic_info` (.+)$").WillReturnRows(sqlRows)

	go CacheCustomTopicInfo()
	time.Sleep(3 * time.Second)

	mockReq := &pb.PubReq{
		Cred: &pb.DeviceCred{
			ProductKey: "testProd",
			DeviceName: "testDevice",
		},
		MsgId:     "msgid",
		Topic:     "testProd/testDevice/topic",
		Qos:       1,
		Content:   "test content",
		Timestamp: 1583377947,
	}
	state := &PubReqState{nil, mockReq, func(msg *kfk.Message) error { return nil }}

	res, err := state.Transation()

	if err != nil {
		t.Errorf("Expected no error, but got %s instead", err)
	}

	expected := &pb.Response{Succeed: true}
	if !reflect.DeepEqual(expected, res) {
		t.Errorf("Expected %v, but got %v instead", expected, res)
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
}

func TestShouldNotPubTransation(t *testing.T) {
	mdb, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mdb.Close()
	orm.Init(mdb)

	sqlRows := sqlmock.NewRows([]string{"product_key", "topic_id", "topic_filter", "operation", "qos", "topic_type"}).
		AddRow("testProd", "cid", "testProd/${deviceName}/topic", 0, 0, 0)
	mock.ExpectQuery("^SELECT (.+) FROM `custom_topic_info` (.+)$").WillReturnRows(sqlRows)

	go CacheCustomTopicInfo()
	time.Sleep(3 * time.Second)

	mockReq := &pb.PubReq{
		Cred: &pb.DeviceCred{
			ProductKey: "testProd",
			DeviceName: "testDevice",
		},
		MsgId:     "msgid",
		Topic:     "testProd/testDevice/topic",
		Qos:       1,
		Content:   "test content",
		Timestamp: 1583377947,
	}
	state := &PubReqState{nil, mockReq, func(msg *kfk.Message) error { return nil }}

	res, err := state.Transation()

	if err != nil {
		t.Errorf("Expected no error, but got %s instead", err)
	}

	expected := &pb.Response{Succeed: false}
	if !reflect.DeepEqual(expected, res) {
		t.Errorf("Expected %v, but got %v instead", expected, res)
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
}
