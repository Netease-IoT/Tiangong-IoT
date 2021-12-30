package fsm

import (
	"handler/orm"
	"reflect"
	"testing"
	"time"

	"github.com/DATA-DOG/go-sqlmock"
)

func TestShouldCacheCustomTopicInfo(t *testing.T) {
	mdb, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mdb.Close()
	orm.Init(mdb)

	sqlRows := sqlmock.NewRows([]string{"product_key", "topic_id", "topic_filter", "operation", "qos", "topic_type"}).
		AddRow("testProd", "cid", "testProd/${deviceName}/custom", 0, 1, 0)
	mock.ExpectQuery("^SELECT (.+) FROM `custom_topic_info` (.+)$").WillReturnRows(sqlRows)

	go CacheCustomTopicInfo()

	time.Sleep(3 * time.Second)

	tc, err := GetTopicClass("testProd", "testDevice", "testProd/testDevice/custom", 1)

	if err != nil {
		t.Errorf("Expected no error, but got %s instead", err)
	}

	expected := &TopicClass{
		TopicFilter: "testProd/${deviceName}/custom",
		Qos:         1,
		TopicId:     "cid",
		TopicType:   0,
	}

	if !reflect.DeepEqual(expected, tc) {
		t.Errorf("Expected %v, but got %v instead", expected, tc)
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
}

func TestShouldCacheSysTopicInfo(t *testing.T) {
	mdb, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mdb.Close()
	orm.Init(mdb)

	sqlRows := sqlmock.NewRows([]string{"topic_id", "topic_filter", "operation", "qos", "topic_type"}).
		AddRow("sid", "${productKey}/${deviceName}/sys", 0, 1, 1)
	mock.ExpectQuery("^SELECT (.+) FROM `sys_topic_info` (.+)$").WillReturnRows(sqlRows)

	go CacheSysTopicInfo()

	time.Sleep(3 * time.Second)

	tc, err := GetTopicClass("testProd", "testDevice", "testProd/testDevice/sys", 1)

	if err != nil {
		t.Errorf("Expected no error, but got %s instead", err)
	}

	expected := &TopicClass{
		TopicFilter: "${productKey}/${deviceName}/sys",
		Qos:         1,
		TopicId:     "sid",
		TopicType:   1,
	}

	if !reflect.DeepEqual(expected, tc) {
		t.Errorf("Expected %v, but got %v instead", expected, tc)
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
}
