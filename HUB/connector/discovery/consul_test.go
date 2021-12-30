package discovery

import "strings"
import "encoding/json"
import "testing"

const jsonStr = `[
{
	"Service": {
		"Address": "1.1.1.1",
		"Port": 3344,
		"Tag": [],
		"CreateIndex": 1111,
		"Service": "bbb"
	},
	"Node": {
		"Address": "1.1.1.1"
	}
},
{
	"Service": {
		"Address": "2.2.2.2",
		"Port": 2222,
		"Tag": [],
		"CreateIndex": 1111,
		"Service": "aaa"
	},
	"Node": {
		"Address": "1.1.1.1"
	}
}]`

func Test_decode(t *testing.T) {
	var cr consulResponse

	dec := json.NewDecoder(strings.NewReader(jsonStr))
	err := dec.Decode(&cr)

	if err != nil {
		t.Fatalf("decode error: %v", err)
		return
	}

	t.Logf("%v", cr)
}
