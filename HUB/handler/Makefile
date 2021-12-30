
main: 
	go build -o handler
cov:
	go test -coverprofile=cov ./fsm && go tool cover -func=cov && rm cov
vet:
	go vet -v ./...

.PHONY: main cov vet
