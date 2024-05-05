package api

import (
	"github.com/gorilla/websocket"
	log "github.com/sirupsen/logrus"
	"net/http"
	"os"
	"sync"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

var connPool = sync.Pool{
	New: func() interface{} {
		conn, _, err := websocket.DefaultDialer.Dial("ws://"+os.Getenv("SERVER_HOST")+":"+os.Getenv("SERVER_PORT")+"/api/sockets/termalmap", nil)
		if err != nil {
			log.Fatalf("failed to dial websocket server: %v", err)
		}
		return conn
	},
}

func SocketThermal(w http.ResponseWriter, r *http.Request) {

}
