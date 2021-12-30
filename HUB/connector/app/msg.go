package app

import (
	"container/list"
	"encoding/base64"
	"encoding/json"
	"sync"

	"connector/args"
)

const messageNamespace = "m:"

func getMessage(msgId string) ([]byte, error) {
	if ret, ok := stdMessageCache.getFromCache(msgId); ok {
		return ret, nil
	}

	key := messageNamespace + msgId

	client := Global.RedisClient
	r := client.Get(key)

	if r.Err() != nil {
		return nil, r.Err()
	}

	msg, _ := r.Bytes()

	stdMessageCache.addToCache(msgId, msg)

	return msg, nil
}

type msgStruct struct {
	Content string `json:"content"`
}

func handleMessageCompat(msg []byte) ([]byte, error) {
	var m msgStruct

	dlen := base64.StdEncoding.DecodedLen(len(msg))
	dst := make([]byte, dlen)
	if n, err := base64.StdEncoding.Decode(dst, msg); err == nil {
		return dst[:n], err
	}

	if err := json.Unmarshal(msg, &m); err != nil {
		return nil, err
	}

	return base64.StdEncoding.DecodeString(m.Content)
}

func getMessageDecodeBase64(msgId string) ([]byte, error) {
	if ret, ok := b64MessageCache.getFromCache(msgId); ok {
		return ret, nil
	}

	key := messageNamespace + msgId

	client := Global.RedisClient
	r := client.Get(key)

	if r.Err() != nil {
		return nil, r.Err()
	}

	msg, _ := r.Bytes()

	ret, err := handleMessageCompat(msg)
	if err != nil {
		return nil, err
	}

	b64MessageCache.addToCache(msgId, ret)

	return ret, nil
}

type entry struct {
	key   string
	value []byte
}

type messageCache struct {
	cache map[string]*list.Element
	ll    *list.List
	mux   sync.Mutex
}

func (m *messageCache) addToCache(key string, value []byte) {
	m.mux.Lock()
	defer m.mux.Unlock()

	if ee, ok := m.cache[key]; ok {
		m.ll.MoveToFront(ee)
		ee.Value.(*entry).value = value
		return
	}

	ele := m.ll.PushFront(&entry{key, value})
	m.cache[key] = ele

	if m.ll.Len() > *args.MsgCacheSize {
		ele := m.ll.Back()
		if ele != nil {
			m.ll.Remove(ele)
			k := ele.Value.(*entry).key

			delete(m.cache, k)
		}
	}
}

func (m *messageCache) getFromCache(key string) ([]byte, bool) {
	m.mux.Lock()
	defer m.mux.Unlock()

	if ele, hit := m.cache[key]; hit {
		m.ll.MoveToFront(ele)
		return ele.Value.(*entry).value, true
	}
	return nil, false
}

var stdMessageCache = &messageCache{
	cache: make(map[string]*list.Element),
	ll:    list.New(),
}

var b64MessageCache = &messageCache{
	cache: make(map[string]*list.Element),
	ll:    list.New(),
}
