package orm

import (
	"fmt"

	"github.com/jinzhu/gorm"
	_ "github.com/jinzhu/gorm/dialects/mysql"
)

var db *gorm.DB

type DbConf interface{}

type DeviceInfo struct {
	ProductKey   string
	DeviceName   string
	DeviceSecret string
	Status       int8
	DeleteFlag   int8
}

type TopicSubscription struct {
	ProductKey  string
	DeviceName  string
	TopicId     string
	TopicFilter string
	Qos         int8
	TopicType   int8
	DeleteFlag  int8
}

type SysTopicInfo struct {
	TopicId     string
	TopicFilter string
	Operation   int8
	Qos         int8
	TopicType   int8
	DeleteFlag  int8
}

type CustomTopicInfo struct {
	ProductKey  string
	TopicId     string
	TopicFilter string
	Operation   int8
	Qos         int8
	TopicType   int8
	DeleteFlag  int8
}

func Init(dbConf DbConf) {
	var err error
	db, err = gorm.Open("mysql", dbConf)

	if err != nil {
		panic("failed to connect database")
	}

	db.SingularTable(true)
	db.DB().SetMaxIdleConns(10)
	db.DB().SetMaxOpenConns(100)
}

func GetDeviceSecret(productKey, deviceName string) (string, error) {
	var device DeviceInfo

	if err := db.Where("product_key = ? AND device_name = ? AND delete_flag = 0", productKey, deviceName).First(&device).Error; err != nil {
		return "", err
	}

	return device.DeviceSecret, nil
}

func SetStatus(productKey, deviceName string, status uint8) (err error) {
	err = db.Model(&DeviceInfo{}).Where("product_key = ? AND device_name = ? AND delete_flag = 0", productKey, deviceName).Update("status", status).Error
	return
}

func AddSubscription(ts *TopicSubscription) (err error) {
	err = db.Set("gorm:insert_option",
		fmt.Sprintf("ON DUPLICATE KEY UPDATE topic_filter = '%s', qos = '%d', topic_type = '%d', delete_flag = '%d'",
			ts.TopicFilter, ts.Qos, ts.TopicType, ts.DeleteFlag)).Create(ts).Error
	return
}

func CleanSubscription(productKey, deviceName, topicFilter string) (err error) {
	err = db.Model(&TopicSubscription{}).Where("product_key = ? AND device_name = ? AND topic_filter = ?", productKey, deviceName, topicFilter).Update("delete_flag", 1).Error
	return
}

func CleanAllSubscription(productKey, deviceName string) (err error) {
	err = db.Model(&TopicSubscription{}).Where("product_key = ? AND device_name = ?", productKey, deviceName).Update("delete_flag", 1).Error
	return
}

func GetSysTopicInfo() (sti []SysTopicInfo, err error) {
	err = db.Where("delete_flag = 0").Find(&sti).Error
	return
}

func GetCustomTopicInfo() (cti []CustomTopicInfo, err error) {
	err = db.Where("delete_flag = 0").Find(&cti).Error
	return
}
