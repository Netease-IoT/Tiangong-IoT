package discovery

import (
	"encoding/json"
	"fmt"
	"math/rand"
	"net/url"
	"strings"
	"sync"
	"time"

	"connector/util"
)

func init() {
	rand.Seed(time.Now().Unix())
}

type serverList struct {
	healthServers []string
}

type consulService struct {
	mux sync.RWMutex

	stop            chan int
	consulAddr      []string
	refreshInterval time.Duration
	servicesState   map[string]*serverList
}

var _ ServiceDiscovery = (*consulService)(nil)

func (c *consulService) start() {
	for {
		for s, sl := range c.servicesState {
			util.V(5).Infof("Get health server for service: %s, %s", s, c.consulAddr)
			healthServers := getHealthServers(c.consulAddr, s)

			c.mux.Lock()
			sl.healthServers = healthServers
			c.mux.Unlock()

			delay := time.Millisecond * (time.Duration)(rand.Int()%1000)
			time.Sleep(delay)
		}

		select {
		case <-c.stop:
			return
		case <-time.After(c.refreshInterval):
			continue
		}
	}
}

func (c *consulService) GetServerByService(srv string) (s string) {
	c.mux.RLock()
	defer c.mux.RUnlock()

	s = ""
	if si, ok := c.servicesState[srv]; ok {
		r := rand.Int()
		count := len(si.healthServers)
		if count == 0 {
			return
		}

		s = si.healthServers[r%count]
	}
	return
}

func NewConsulService(consulAddr string,
	refreshInterval time.Duration, services []string) ServiceDiscovery {

	consulAddr = strings.Trim(consulAddr, ", ")
	inst := &consulService{
		stop:            make(chan int),
		consulAddr:      strings.Split(consulAddr, ","),
		refreshInterval: refreshInterval,
		servicesState:   make(map[string]*serverList, len(services)),
	}

	for _, s := range services {
		inst.servicesState[s] = &serverList{}
	}

	go inst.start()

	return inst
}

const consulQueryPath = "/v1/health/service/"

type srv struct {
	Address string `json:"Address"`
	Port    int    `json:"Port"`
}

type srvWrapper struct {
	Service srv `json:"Service"`
}

type consulResponse []srvWrapper

func getHealthServers(consulAddr []string, srv string) (servers []string) {
	for _, c := range consulAddr {
		q := url.Values{}
		q.Add("passing", "true")
		qstr := q.Encode()

		reqUrl := &url.URL{
			Host:     c,
			Scheme:   "http",
			Path:     consulQueryPath + srv,
			RawQuery: qstr,
		}

		httpClient := util.GetHttpClient()

		resp, err := httpClient.Get(reqUrl.String())
		if err != nil {
			util.Warningf("Get service from consul error: %v %s %s", err, c, srv)
			continue
		}

		var cr consulResponse

		dec := json.NewDecoder(resp.Body)
		err = dec.Decode(&cr)
		if err != nil {
			util.Warningf("Decode resp from consul error: %v %s %s", err, c, srv)
		}

		util.V(5).Infof("consul response: %+v", cr)

		servers = make([]string, 0, len(cr))

		for _, sw := range cr {
			servers = append(servers, fmt.Sprintf("%s:%d", sw.Service.Address, sw.Service.Port))
		}
		break
	}
	if len(servers) == 0 {
		util.Warningf("No avail servers for service: %s", srv)
	}
	return
}
