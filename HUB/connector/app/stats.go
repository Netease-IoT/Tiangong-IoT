package app

import (
	"sync"
	"sync/atomic"
)

type Stats interface {
	IncConn()
	DecConn()
	IncProductConn(productKey string)
	DecProductConn(productKey string)
	DecProductsConn(productKeys []string)
	GetConn() int64
	GetConnsMap() map[string]int64
}

type stats struct {
	mu sync.RWMutex

	conn    int64
	connMap map[string]int64
}

func newStats() Stats {
	return &stats{
		connMap: make(map[string]int64, 128),
	}
}

func (s *stats) IncConn() {
	atomic.AddInt64(&s.conn, 1)
}

func (s *stats) DecConn() {
	atomic.AddInt64(&s.conn, -1)
}

func (s *stats) IncProductConn(productKey string) {
	s.mu.Lock()

	x := s.connMap[productKey]
	s.connMap[productKey] = x + 1

	s.mu.Unlock()
}

func (s *stats) DecProductConn(productKey string) {
	s.mu.Lock()
	x := s.connMap[productKey]
	x -= 1

	if x < 0 {
		x = 0
	}

	if x == 0 {
		delete(s.connMap, productKey)
	} else {
		s.connMap[productKey] = x
	}

	s.mu.Unlock()
}

func (s *stats) DecProductsConn(productKeys []string) {
	s.mu.Lock()
	for _, d := range productKeys {
		x := s.connMap[d]
		x -= 1
		if x < 0 {
			x = 0
		}
		if x == 0 {
			delete(s.connMap, d)
		} else {
			s.connMap[d] = x
		}
	}
	s.mu.Unlock()
}

func (s *stats) GetConn() int64 {
	return atomic.LoadInt64(&s.conn)
}

func (s *stats) GetConnsMap() map[string]int64 {
	ret := make(map[string]int64, 128)

	s.mu.RLock()
	for k, v := range s.connMap {
		ret[k] = v
	}
	s.mu.RUnlock()

	return ret
}
