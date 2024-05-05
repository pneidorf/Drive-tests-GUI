package database

import (
	"fmt"
	"github.com/jinzhu/gorm"
	_ "github.com/jinzhu/gorm/dialects/postgres"
	"github.com/sirupsen/logrus"
)

var logs = logrus.New()
var DB *gorm.DB

func Connect(host string, user string, db string, pass string) {
	dbUri := fmt.Sprintf("host=%s user=%s dbname=%s sslmode=disable password=%s", host, user, db, pass)
	logs.Info("Connected db: " + dbUri)

	conn, err := gorm.Open("postgres", dbUri)
	if err != nil {
		logs.Warning("DB connection fault")
	}

	DB = conn
}
