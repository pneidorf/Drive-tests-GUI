package types

import (
	"backend/internal/database"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"github.com/jinzhu/gorm"
	"golang.org/x/crypto/bcrypt"
	"gopkg.in/gomail.v2"
	"os"
	"regexp"
	"time"
)

type Email string

type Account struct {
	gorm.Model
	Email    Email  `json:"email" gorm:"primaryKey"`
	Password string `json:"password"`
	Verifed  bool   `json:"verifed"`
	Token    Token  `sql:"-"`
}

type VerifyLink struct {
	gorm.Model
	Email Email  `json:"email"`
	Link  string `json:"link"`
}

func (email Email) IsValid() bool {
	emailRegex := `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`
	match, _ := regexp.MatchString(emailRegex, string(email))

	if match {
		return true
	} else {
		return false
	}
}

func (user Account) Validate() (bool, string) {
	if user.Email == "" {
		return false, "Invalid email len"
	}

	if !user.Email.IsValid() {
		return false, "Invalid email format"
	}

	if len(user.Password) < 2 {
		return false, "Small password"
	}

	return true, ""
}

func (email Email) SendMessage(subject string, text string) {
	fmt.Printf("Send SMTP mail to %s\n", string(email))

	m := gomail.NewMessage()

	m.SetHeader("From", os.Getenv("EMAIL_SMTP_DEFAULT_FROM"))
	m.SetHeader("To", string(email))
	m.SetHeader("Subject", subject)
	m.SetBody("text/html", text)

	send := func(msg *gomail.Message) {
		d := gomail.NewDialer(os.Getenv("EMAIL_SMTP_SERVER"), 465, os.Getenv("EMAIL_SMTP_DEFAULT_FROM"), os.Getenv("EMAIL_SMTP_PASSWORD"))

		if err := d.DialAndSend(m); err != nil {
			panic(err)
		}
	}

	go send(m)
}

func (user *Account) New() error {
	notExists := database.DB.Table("accounts").Where("email = ?", user.Email).First(&Account{}).RecordNotFound()
	if !notExists {
		return fmt.Errorf("user exists")
	}

	tmpUser := USER(user.Email, user.Password)

	user.Password = tmpUser.Password
	user.Token = tmpUser.Token
	user.Verifed = false

	return nil
}

func (user Account) VerificationLink() string {
	verifyLink := VerifyLink{Email: user.Email, Link: ""}

	mac := hmac.New(sha256.New, []byte(os.Getenv("API_KEY")))
	mac.Write([]byte(string(user.Email) + "." + time.Now().String()))
	signature := mac.Sum(nil)
	verifyLinkSign := base64.URLEncoding.EncodeToString(signature)

	verifyLink.Link = verifyLinkSign

	database.DB.Create(&verifyLink)

	return verifyLinkSign
}

func USER(email Email, password string) Account {
	user := Account{
		Email:    email,
		Password: password,
		Token:    JWT(string(email), 0),
	}

	cryptoPass, _ := bcrypt.GenerateFromPassword([]byte(user.Password), bcrypt.DefaultCost)
	user.Password = string(cryptoPass)

	database.DB.Create(&user)

	return user
}

func (user *Account) Login(jwtAuth bool) error {
	tmpUser := Account{}
	if err := database.DB.Table("accounts").Where("email = ?", user.Email).First(&tmpUser).Error; errors.Is(err, gorm.ErrRecordNotFound) {
		return fmt.Errorf("user not exists")
	}

	if !tmpUser.Verifed {
		return fmt.Errorf("email is not verifed! Please check your email")
	}

	if jwtAuth {
		return nil
	}

	if err := bcrypt.CompareHashAndPassword([]byte(tmpUser.Password), []byte(user.Password)); err != nil && errors.Is(err, bcrypt.ErrMismatchedHashAndPassword) {
		return fmt.Errorf("incorrect password")
	}

	user.Token = JWT(string(user.Email), 0)

	return nil
}

func SignVerificationLink(link string) (bool, error) {
	var recordLink VerifyLink
	fmt.Println(link)
	if err := database.DB.Table("verify_links").Where("link = ?", link).First(&recordLink).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return false, fmt.Errorf("unknown verify link: %s", link)
		}

		return false, fmt.Errorf("unknown error: %v", err)
	}

	// Удаляем строку
	database.DB.Table("verify_links").Where("link = ?", link).Unscoped().Delete(&recordLink)

	var recordAccount Account
	if err := database.DB.Table("accounts").Where("email = ?", string(recordLink.Email)).First(&recordAccount).Error; err != nil {
		if recordLink.Email == "email@email.com" {
			return true, nil
		}

		if errors.Is(err, gorm.ErrRecordNotFound) {
			return false, fmt.Errorf("unknown verify account: %v", err)
		}

		return false, fmt.Errorf("unknown error: %v", err)
	}

	database.DB.Table("accounts").Where("email = ?", recordAccount.Email).Update("verifed", true)

	return true, nil
}
