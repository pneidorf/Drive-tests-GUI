package main

import (
	"backend/internal/database"
	"backend/internal/types"
	"os"

	"github.com/joho/godotenv"
	log "github.com/sirupsen/logrus"
)

var Log = log.New()

func init() {
	err := godotenv.Load()

	if err != nil {
		Log.Error("Error loading .env file")
	}

	log.Info("Loaded .env")

	database.Connect(os.Getenv("PG_HOST"), os.Getenv("PG_USER"), os.Getenv("PG_DATA"), os.Getenv("PG_PASS"))

	log.Info("Start migrating data")
	database.DB.AutoMigrate(&types.Account{}, &types.VerifyLink{})
}
