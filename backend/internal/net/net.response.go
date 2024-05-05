package net

import (
	"encoding/json"
	"net/http"
	"strings"
)

type Msg map[string]interface{}

func Respond(w http.ResponseWriter, status int, data Msg) {
	w.Header().Add("Content-Type", "application/json")
	w.WriteHeader(status)
	err := json.NewEncoder(w).Encode(data)
	if err != nil {
		return
	}
}

func RequestToken(r *http.Request) string {
	tokenHeader := r.Header.Get("Authorization")
	token := strings.Split(tokenHeader, " ")

	if len(token) > 1 {
		return token[1]
	} else {
		return ""
	}
}
