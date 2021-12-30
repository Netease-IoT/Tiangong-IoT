package fsm

import (
	"handler/orm"
	"handler/pb"
	"reflect"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
)

func TestShouldCnctTransation(t *testing.T) {
	mdb, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mdb.Close()
	orm.Init(mdb)

	sqlRows := sqlmock.NewRows([]string{"device_secret"}).AddRow("someSecret")
	mock.ExpectQuery("^SELECT (.+) FROM `device_info` (.+)$").WillReturnRows(sqlRows)

	mock.ExpectBegin()
	mock.ExpectExec("^UPDATE (.+)$").WillReturnResult(sqlmock.NewResult(1, 1))
	mock.ExpectCommit()

	mockReq := &pb.ConnReq{
		Cred: &pb.DeviceCred{
			ProductKey: "testProd",
			DeviceName: "testDevice",
		},
		Token:        "v1:62a8aa003a152cf84aea",
		CleanSession: true,
	}
	state := &CnctReqState{nil, mockReq, 1583377947}

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

func TestShouldNotCnctTransation(t *testing.T) {
	mdb, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mdb.Close()
	orm.Init(mdb)

	sqlRows := sqlmock.NewRows([]string{"device_secret"}).AddRow("someSecret")
	mock.ExpectQuery("^SELECT (.+) FROM `device_info` (.+)$").WillReturnRows(sqlRows)

	mockReq := &pb.ConnReq{
		Cred: &pb.DeviceCred{
			ProductKey: "testProd",
			DeviceName: "testDevice",
		},
		Token:        "badToken",
		CleanSession: true,
	}
	state := &CnctReqState{nil, mockReq, 1583377947}

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
