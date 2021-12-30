package args

import (
	"os"
	"strconv"
	"strings"
	"time"
)

func getFromEnvInt(name string, defaultNum int) int {
	if s, exist := os.LookupEnv(name); exist {
		s = strings.TrimSpace(s)
		if i, e := strconv.Atoi(s); e == nil {
			return i
		}
	}
	return defaultNum
}

func getFromEnvBool(name string, defaultBool bool) bool {
	if s, exist := os.LookupEnv(name); exist {
		s = strings.ToLower(strings.TrimSpace(s))
		if s == "true" {
			return true
		} else if s == "false" {
			return false
		}
	}
	return defaultBool
}

func getFromEnvString(name string, defaultStr string) string {
	if s, exist := os.LookupEnv(name); exist {
		s = strings.TrimSpace(s)
		return s
	}
	return defaultStr
}

func getFromEnvDuration(name string, defaultDur time.Duration) time.Duration {
	if s, exist := os.LookupEnv(name); exist {
		s = strings.TrimSpace(s)
		if d, e := time.ParseDuration(s); e == nil {
			return d
		}
	}
	return defaultDur
}
