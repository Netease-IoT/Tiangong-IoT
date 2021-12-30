package mqtt

import (
	"sync"
)

const blockSize = 4 * 1024
const largeBlockSize = 20 * 1024

// max mqtt header 1 type + max 3 length len bytes = 4 bytes
// max ws header 1 flags + max 3 length + 4 mask data = 8 bytes
// so max header data is 12 bytes for ws, 4 bytes for non-ws
const maxPacketLimit = largeBlockSize - 12

var bpool = sync.Pool{
	New: func() interface{} {
		return make([]byte, largeBlockSize)
	},
}

func getLargeBuf() []byte {
	return bpool.Get().([]byte)
}

func putLargeBuf(b []byte) {
	bpool.Put(b)
}
