package main

import (
	"backend/api"
	appAuth "backend/app/auth"
	appLogs "backend/app/logs"
	appUser "backend/app/user"
	_ "backend/docs"
	"fmt"
	"net/http"
	"os"

	"github.com/gorilla/mux"
	httpSwagger "github.com/swaggo/http-swagger"
)

func main() {
	Log.Info("Server has been started")

	routerRun()
}

func routerRun() {

	router := mux.NewRouter()

	router.Handle("/", http.FileServer(http.Dir("./client/build"))) // Путь до Frontend части. |СОБРАННОЙ!|

	// Маршрут для документации Swagger
	router.PathPrefix("/swagger").Handler(httpSwagger.WrapHandler)

	// Маршрут для отображения документации на странице /openapi
	router.HandleFunc("/openapi", api.OpenAPI).Methods("GET")

	// login page and register page (for testing api)
	router.HandleFunc("/user", appUser.Handler).Methods("GET") // -> in Dev

	// admin page
	router.HandleFunc("/admin/users", appUser.Handler).Methods("GET")        // -> in Dev
	router.HandleFunc("/admin/user/accept", appUser.Handler).Methods("POST") // -> in Dev

	// user
	router.HandleFunc("/api/user/register", api.UserRegister).Methods("POST") // <- in Release
	router.HandleFunc("/api/user/auth", api.UserAuth).Methods("POST")         // <- in Release
	router.HandleFunc("/api/user/verify", api.UserVerify).Methods("GET")      // <- in Release

	// for testing jwt
	router.HandleFunc("/api/jwt/test", api.JwtTest).Methods("POST")     // <- in Release
	router.HandleFunc("/api/jwt/verify", api.JwtVerify).Methods("POST") // <- in Release

	// Sockets
	router.HandleFunc("/api/sockets/termalmap", api.SocketThermal).Methods("GET") // -> in Dev

	router.Use(appLogs.Handler)
	router.Use(appAuth.Handler)

	port := os.Getenv("SERVER_PORT")
	if port == "" {
		port = "8000"
	}

	err := http.ListenAndServe(":"+port, router)
	if err != nil {
		fmt.Print(err)
	}

}
