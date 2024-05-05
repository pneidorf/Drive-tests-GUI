package appLogs

import (
	"fmt"
	log "github.com/sirupsen/logrus"
	"net/http"
	"time"
)

var logs = log.New()

type CustomFormatter struct{}

func (f *CustomFormatter) Format(entry *log.Entry) ([]byte, error) {
	var logLine string

	colors := map[string]string{
		"POST":    "\033[34m", // Голубой
		"GET":     "\033[33m", // Жёлтый
		"PUT":     "\033[36m", // Синий
		"PATCH":   "\033[35m", // Фиолетовый
		"DELETE":  "\033[31m", // Красный
		"HEAD":    "\033[32m", // Зелёный
		"OPTIONS": "\033[35m", // Розовый
		"default": "\033[0m",  // Сброс цвета
	}

	color := colors[entry.Data["request_type"].(string)]
	resetColor := colors["default"]
	logLine = fmt.Sprintf("%s[router][%s]%s\t->\t%s\t\t%s%s\n", color, time.Now().Format("2006-01-02 15:04:05"), resetColor, entry.Data["url"], entry.Data["request_type"], resetColor)

	return []byte(logLine), nil
}

func Handler(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		log.SetFormatter(&CustomFormatter{})

		log.WithFields(log.Fields{
			"url":          r.URL.Path,
			"request_type": r.Method,
		}).Info("Request handled")

		next.ServeHTTP(w, r)
	})
}
