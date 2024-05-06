package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

// Структура хранения связи
type Communication struct {
	Bandwidth  float64 `json:"bandwidth"`
	Latency    float64 `json:"latency"`
	PacketLoss float64 `json:"packet_loss"`
}

// Пример данных
var communicationData = Communication{
	Bandwidth:  100, // Mbps
	Latency:    20,  // ms
	PacketLoss: 0.5, // %
}

// Обработчик запроса
func communicationHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	// Преобразование в JSON
	json.NewEncoder(w).Encode(communicationData)
}

func main() {
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	http.HandleFunc("/communication", communicationHandler)
	srv := &http.Server{Addr: ":8080"}
	go func() {
		fmt.Println("Сервер запущен на порту 8080")
		if err := srv.ListenAndServe(); err != nil {
			fmt.Println(err)
		}
	}()
	<-ctx.Done()
	fmt.Println("Остановка сервера")
	if err := srv.Shutdown(context.Background()); err != nil {
		fmt.Println(err)
	}
}
