package discovery

type ServiceDiscovery interface {
	GetServerByService(service string) string
}
