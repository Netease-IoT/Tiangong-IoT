package mqtt

// Code is fetched from eclipse
import "errors"

// Below are the constants assigned to each of the MQTT packet types
const (
	PT_CONNECT     = 1
	PT_CONNACK     = 2
	PT_PUBLISH     = 3
	PT_PUBACK      = 4
	PT_PUBREC      = 5
	PT_PUBREL      = 6
	PT_PUBCOMP     = 7
	PT_SUBSCRIBE   = 8
	PT_SUBACK      = 9
	PT_UNSUBSCRIBE = 10
	PT_UNSUBACK    = 11
	PT_PINGREQ     = 12
	PT_PINGRESP    = 13
	PT_DISCONNECT  = 14
)

// PacketNames maps the constants for each of the MQTT packet types
// to a string representation of their name.
var PacketNames = map[byte]string{
	PT_CONNECT:     "CONNECT",
	PT_CONNACK:     "CONNACK",
	PT_PUBLISH:     "PUBLISH",
	PT_PUBACK:      "PUBACK",
	PT_PUBREC:      "PUBREC",
	PT_PUBREL:      "PUBREL",
	PT_PUBCOMP:     "PUBCOMP",
	PT_SUBSCRIBE:   "SUBSCRIBE",
	PT_SUBACK:      "SUBACK",
	PT_UNSUBSCRIBE: "UNSUBSCRIBE",
	PT_UNSUBACK:    "UNSUBACK",
	PT_PINGREQ:     "PINGREQ",
	PT_PINGRESP:    "PINGRESP",
	PT_DISCONNECT:  "DISCONNECT",
}

// Below are the const definitions for error codes returned by
// Connect()
const (
	RC_ACCEPTED                             = 0x00
	RC_ERR_REFUSED_BAD_PROTOCOL_VERSION     = 0x01
	RC_ERR_REFUSED_ID_REJECTED              = 0x02
	RC_ERR_REFUSED_SERVER_UNAVAILABLE       = 0x03
	RC_ERR_REFUSED_BAD_USERNAME_OR_PASSWORD = 0x04
	RC_ERR_REFUSED_NOT_AUTHORIZED           = 0x05
	RC_ERR_NETWORK_ERROR                    = 0xFE
	RC_ERR_PROTOCOL_VIOLATION               = 0xFF
)

// ConnackReturnCodes is a map of the error codes constants for Connect()
// to a string representation of the error
var ConnackReturnCodes = map[byte]string{
	RC_ACCEPTED:                             "Accepted",
	RC_ERR_REFUSED_BAD_PROTOCOL_VERSION:     "Refused: Bad Protocol Version",
	RC_ERR_REFUSED_ID_REJECTED:              "Refused: Client Identifier Rejected",
	RC_ERR_REFUSED_SERVER_UNAVAILABLE:       "Refused: Server Unavailable",
	RC_ERR_REFUSED_BAD_USERNAME_OR_PASSWORD: "Refused: Bad Username or Password",
	RC_ERR_REFUSED_NOT_AUTHORIZED:           "Refused: Not Authorized",
	RC_ERR_NETWORK_ERROR:                    "Network Error",
	RC_ERR_PROTOCOL_VIOLATION:               "Refused: Protocol Violation",
}

// ConnErrors is a map of the errors codes constants for Connect()
// to a Go error
var ConnErrors = map[byte]error{
	RC_ACCEPTED:                             nil,
	RC_ERR_REFUSED_BAD_PROTOCOL_VERSION:     errors.New("Bad protocol version"),
	RC_ERR_REFUSED_ID_REJECTED:              errors.New("Client identifier rejected"),
	RC_ERR_REFUSED_SERVER_UNAVAILABLE:       errors.New("Server Unavailable"),
	RC_ERR_REFUSED_BAD_USERNAME_OR_PASSWORD: errors.New("Bad user name or password"),
	RC_ERR_REFUSED_NOT_AUTHORIZED:           errors.New("Not authorized"),
	RC_ERR_NETWORK_ERROR:                    errors.New("Network error"),
	RC_ERR_PROTOCOL_VIOLATION:               errors.New("Protocol violation"),
}
