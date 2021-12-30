package fsm

import (
	"handler/orm"
	"handler/pb"
	"reflect"
	"testing"
	"time"

	"github.com/DATA-DOG/go-sqlmock"
)

func TestShouldSubTransation(t *testing.T) {
	mdb, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mdb.Close()
	orm.Init(mdb)

	sqlRows := sqlmock.NewRows([]string{"product_key", "topic_id", "topic_filter", "operation", "qos", "topic_type"}).
		AddRow("testProd", "cid", "testProd/${deviceName}/topic", 0, 1, 0)
	mock.ExpectQuery("^SELECT (.+) FROM `custom_topic_info` (.+)$").WillReturnRows(sqlRows)

	mock.ExpectBegin()
	mock.ExpectExec("^INSERT (.+)$").WillReturnResult(sqlmock.NewResult(1, 1))
	mock.ExpectCommit()

	go CacheCustomTopicInfo()
	time.Sleep(3 * time.Second)

	mockReq := &pb.SubReq{
		Cred: &pb.DeviceCred{
			ProductKey: "testProd",
			DeviceName: "testDevice",
		},
		TopicFilters: []string{"testProd/testDevice/topic", "testProd/testDevice/invalid"},
		Qoss:         []int32{1, 1},
	}
	state := &SubReqState{nil, mockReq}

	res, err := state.Transation()

	if err != nil {
		t.Errorf("Expected no error, but got %s instead", err)
	}

	expected := &pb.SubRes{Qoss: []int32{1, -1}}
	if !reflect.DeepEqual(expected, res) {
		t.Errorf("Expected %v, but got %v instead", expected, res)
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
}
