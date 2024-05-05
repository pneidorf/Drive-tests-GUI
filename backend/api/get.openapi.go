package api

import "net/http"

func OpenAPI(w http.ResponseWriter, r *http.Request) {
	http.Redirect(w, r, "/swagger/index.html", http.StatusFound)
}
