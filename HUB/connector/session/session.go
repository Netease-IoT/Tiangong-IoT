package session

import (
	"crypto/sha1"
	"encoding/hex"
	"io"
	"strings"
	"time"

	"github.com/go-redis/redis"
)

type SessionStorage interface {
	Refresh(key string, value string, expire time.Duration) (string, error)
	Clear(key string, value string) error
}

type redisStorage struct {
	client *redis.ClusterClient
}

var _ SessionStorage = (*redisStorage)(nil)

// ARGV[1]: value
// ARGV[2]: expire time
var refreshScriptSha1 string
var refreshScript = `
local v = redis.call('get', KEYS[1])
redis.call('setex', KEYS[1], ARGV[2], ARGV[1])
if not v or v == ARGV[1] then
  return 0
else
  return v
end`

// ARGV[1]: value
var clearScriptSha1 string
var clearScript = `
if redis.call('get', KEYS[1]) == ARGV[1] then
  redis.call('del', KEYS[1])
end
return 0`

func init() {
	hr := sha1.New()
	io.WriteString(hr, refreshScript)
	refreshScriptSha1 = hex.EncodeToString(hr.Sum(nil))

	hc := sha1.New()
	io.WriteString(hc, clearScript)
	clearScriptSha1 = hex.EncodeToString(hc.Sum(nil))
}

func NewRedisSessionStorage(client *redis.ClusterClient) SessionStorage {
	return &redisStorage{client: client}
}

func (rs *redisStorage) Refresh(key, value string, expire time.Duration) (string, error) {
	var err error
	e := int(expire / time.Second)

	r := rs.client.EvalSha(refreshScriptSha1, []string{key}, value, e)
	if err = r.Err(); err != nil && strings.HasPrefix(err.Error(), "NOSCRIPT ") {
		r = rs.client.Eval(refreshScript, []string{key}, value, e)
	}

	if err = r.Err(); err != nil {
		return "", err
	}

	val := r.Val()
	switch v := val.(type) {
	case int:
		return "", nil
	case int64:
		return "", nil
	case string:
		return v, nil
	}

	return "", nil
}

func (rs *redisStorage) Clear(key, value string) error {
	var err error
	r := rs.client.EvalSha(clearScriptSha1, []string{key}, value)
	if err = r.Err(); err != nil && strings.HasPrefix(err.Error(), "NOSCRIPT ") {
		r = rs.client.Eval(clearScript, []string{key}, value)
	}

	return r.Err()
}
