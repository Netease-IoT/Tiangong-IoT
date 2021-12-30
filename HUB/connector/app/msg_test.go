package app

import (
	"testing"
)

func Test_handleMessageCompat(t *testing.T) {
	t.Parallel()

	srcs := [2][]byte{
		[]byte("aGVsbG8="),
		[]byte("{\"msgId\": \"msgId1\", \"content\": \"aGVsbG8=\"}"),
	}
	res := "hello"

	for _, s := range srcs {
		dat, _ := handleMessageCompat(s)
		if string(dat) != res {
			t.Fatalf("result is: %v", dat)
		}
	}
}
