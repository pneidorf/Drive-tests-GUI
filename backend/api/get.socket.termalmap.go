package api

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	log "github.com/sirupsen/logrus"
)

type Message struct {
	Time      time.Time `json:"time"`
	Latitude  string    `json:"latitude"`
	Longitude string    `json:"longitude"`
	Rsrp      string    `json:"rsrp"`
	Rssi      string    `json:"rssi"`
	Rsrq      string    `json:"rsrq"`
	Rssnr     string    `json:"rssnr"`
	Cqi       string    `json:"cqi"`
	Bandwidth string    `json:"bandwidth"`
	CellID    string    `json:"cellID"`
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

// Задел под оптимизация сокета
// Каждый вебсокет будет в отдельном пуле
// Что (в теории) позволит нам не создавать
// вебсокет каждый раз, а будет один объект с сокетом
// Меньше выделяется памяти
var connPool = sync.Pool{
	New: func() interface{} {
		conn, _, err := websocket.DefaultDialer.Dial("ws://"+os.Getenv("SERVER_HOST")+":"+os.Getenv("SERVER_PORT")+"/api/sockets/termalmap", nil)
		if err != nil {
			log.Fatalf("failed to dial websocket server: %v", err)
		}
		return conn
	},
}

////////////////////////////////////

func SocketThermal(w http.ResponseWriter, r *http.Request) {

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("Error upgrading to WebSocket:", err)
		return
	}
	defer conn.Close()
	// Устанавливаем таймаут в 10 минут
	conn.SetReadDeadline(time.Now().Add(10 * time.Minute))

	for {

		_, message, err := conn.ReadMessage()
		if err != nil {
			if websocket.IsCloseError(err, websocket.CloseNormalClosure) {
				log.Println("Connection closed by client")
			} else {
				log.Println("Error reading message:", err)
			}
			break
		}

		// Распечатываем JSON
		var msg Message
		if err := json.Unmarshal(message, &msg); err != nil {
			log.Println("Error decoding JSON:", err)
			break
		}
		fmt.Printf("Received JSON: %v", msg)

	}
}

/*

func SocketThermal(w http.ResponseWriter, r *http.Request) {

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("Error upgrading to WebSocket:", err)
		return
	}
	defer conn.Close()
	// Устанавливаем таймаут в 10 минут
	conn.SetReadDeadline(time.Now().Add(10 * time.Minute))

	for {

		_, message, err := conn.ReadMessage()
		if err != nil {
			if websocket.IsCloseError(err, websocket.CloseNormalClosure) {
				log.Println("Connection closed by client")
			} else {
				log.Println("Error reading message:", err)
			}
			break
		}

		// Распечатываем JSON
		var jsonData map[string]interface{}
		if err := json.Unmarshal(message, &jsonData); err != nil {
			log.Println("Error decoding JSON:", err)
			break
		}
		fmt.Println("Received JSON:", jsonData)

	}
}

*/
