package fsm

import (
	"handler/orm"
	"handler/pb"
	"reflect"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
)

func TestShouldUnsubTransation(t *testing.T) {
	mdb, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mdb.Close()
	orm.Init(mdb)

	mock.ExpectBegin()
	mock.ExpectExec("^UPDATE (.+)$").WillReturnResult(sqlmock.NewResult(1, 1))
	mock.ExpectCommit()

	mockReq := &pb.UnsubReq{
		Cred: &pb.DeviceCred{
			ProductKey: "testProd",
			DeviceName: "testDevice",
		},
		TopicFilters: []string{"testProd/testDevice/topic"},
	}
	state := &UnsubReqState{nil, mockReq}

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
