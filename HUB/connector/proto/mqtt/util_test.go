package mqtt

import (
	"testing"
)

func assert(t *testing.T, c bool, label int) {
	if !c {
		t.Error(label)
	}
}
