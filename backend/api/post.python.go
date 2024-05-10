package api

import (
	"encoding/json"
	"net/http"
)

////////////////////////////////////

type Communication struct {
	Bandwidth  float64 `json:"bandwidth"`
	Latency    float64 `json:"latency"`
	PacketLoss float64 `json:"packet_loss"`
}

var communicationData = Communication{
	Bandwidth:  100, // Mbps
	Latency:    20,  // ms
	PacketLoss: 0.5, // %
}

////////////////////////////////////

func pythonHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-type", "application/json")
	json.NewEncoder(w).Encode(communicationData)

}
