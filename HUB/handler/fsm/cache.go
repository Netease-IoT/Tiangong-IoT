package fsm

import (
	"errors"
	"handler/orm"
	"strings"
	"sync"
	"time"
)

type TopicClass struct {
	TopicFilter string
	Qos         int8
	TopicId     string
	TopicType   int8
}

var sysTopicCache []orm.SysTopicInfo
var sysLock sync.RWMutex

var cusTopicCache map[string][]orm.CustomTopicInfo
var cusLock sync.RWMutex

func cacheSys() {
	var tmpSysCache []orm.SysTopicInfo
	var err error

	if tmpSysCache, err = orm.GetSysTopicInfo(); err != nil {
		return
	}

	sysLock.Lock()
	defer sysLock.Unlock()

	sysTopicCache = tmpSysCache
}

func CacheSysTopicInfo() {

	cacheSys()

	t := time.NewTicker(1 * time.Hour)
	defer t.Stop()

	for {
		<-t.C
		cacheSys()
	}
}

func cacheCus() {
	var rawCusCache []orm.CustomTopicInfo
	var err error

	if rawCusCache, err = orm.GetCustomTopicInfo(); err != nil {
		return
	}

	tmpCusCache := make(map[string][]orm.CustomTopicInfo)

	for i := 0; i < len(rawCusCache); i++ {
		tmpCusCache[rawCusCache[i].ProductKey] = append(tmpCusCache[rawCusCache[i].ProductKey], rawCusCache[i])
	}

	cusLock.Lock()
	defer cusLock.Unlock()

	cusTopicCache = tmpCusCache
}

func CacheCustomTopicInfo() {

	cacheCus()

	t := time.NewTicker(1 * time.Minute)
	defer t.Stop()

	for {
		<-t.C
		cacheCus()
	}
}

func GetTopicClass(productKey, deviceName, topicFilter string, operation int8) (t *TopicClass, err error) {

	cusStr := strings.Replace(topicFilter, deviceName, "${deviceName}", 1)
	cusLock.RLock()
	if v, ok := cusTopicCache[productKey]; ok {
		for i := 0; i < len(v); i++ {
			if cusStr == v[i].TopicFilter {
				if operation == v[i].Operation || v[i].Operation == 0 {
					t = &TopicClass{
						TopicFilter: v[i].TopicFilter,
						Qos:         v[i].Qos,
						TopicId:     v[i].TopicId,
						TopicType:   v[i].TopicType,
					}
				} else {
					err = errors.New("invalid qos")
				}
				break
			}
		}
	}
	cusLock.RUnlock()

	if t != nil || err != nil {
		return
	}

	sysStr := strings.Replace(cusStr, productKey, "${productKey}", 1)
	sysLock.RLock()
	for i := 0; i < len(sysTopicCache); i++ {
		if sysStr == sysTopicCache[i].TopicFilter {
			if operation == sysTopicCache[i].Operation || sysTopicCache[i].Operation == 0 {
				t = &TopicClass{
					TopicFilter: sysTopicCache[i].TopicFilter,
					Qos:         sysTopicCache[i].Qos,
					TopicId:     sysTopicCache[i].TopicId,
					TopicType:   sysTopicCache[i].TopicType,
				}
			} else {
				err = errors.New("invalid qos")
			}
			break
		}
	}
	sysLock.RUnlock()

	if t == nil && err == nil {
		err = errors.New("invalid topic")
	}
	return
}
