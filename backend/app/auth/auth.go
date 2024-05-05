package appAuth

import (
	"backend/internal/net"
	"backend/internal/types"
	"net/http"
	"strings"
)

var noAuthPages = map[string]bool{
	"/api/user/auth":     true,
	"/api/user/register": true,
	"/api/user/verify":   true,
	"/api/jwt/test":      true,
	"/api/jwt/verify":    true,
	"/openapi":           true,
}

func Handler(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		value, exists := noAuthPages[r.URL.Path]
		if exists && value || strings.Contains(r.URL.Path, "swagger") {
			next.ServeHTTP(w, r)
			return
		} else {
			token := types.Token{JWT: net.RequestToken(r)}

			if result, _ := token.Verify(); !result {
				net.Respond(w, http.StatusForbidden, net.Msg{
					"error": "Unauthorized access blocked",
				})
				return
			}
		}

		next.ServeHTTP(w, r)
	})
}
