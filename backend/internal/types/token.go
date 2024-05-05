package types

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"time"
)

type Token struct {
	JWT string `json:"jwt"`
}

var jwtHeader = `{
	"typ": "JWT",
	"alg": "HS256",
}`

func JWT(username string, duration time.Duration) Token {
	token := Token{}
	encodedHeader := base64.URLEncoding.EncodeToString([]byte(jwtHeader))

	expTime := int64(0)

	if duration != 0 {
		expTime = time.Now().Add(duration).Unix()
	}

	claims := fmt.Sprintf(`{"sub":"%s","exp":%d}`, username, expTime)
	encodedClaims := base64.URLEncoding.EncodeToString([]byte(claims))

	unsignedToken := encodedHeader + "." + encodedClaims

	mac := hmac.New(sha256.New, []byte(os.Getenv("JWT_SECRET_KEY")))
	mac.Write([]byte(unsignedToken))
	signature := mac.Sum(nil)
	encodedSignature := base64.URLEncoding.EncodeToString(signature)

	signedTokenWithClaims := unsignedToken + "." + encodedSignature

	token.JWT = signedTokenWithClaims

	return token
}

func (token Token) Verify() (bool, error) {
	parts := strings.Split(token.JWT, ".")

	if len(parts) != 3 {
		return false, fmt.Errorf("invalid token format")
	}

	_, err := base64.URLEncoding.DecodeString(parts[0])
	if err != nil {
		return false, fmt.Errorf("error decoding header: %v", err)
	}

	decodedClaims, err := base64.URLEncoding.DecodeString(parts[1])
	if err != nil {
		return false, fmt.Errorf("error decoding claims: %v", err)
	}

	mac := hmac.New(sha256.New, []byte(os.Getenv("JWT_SECRET_KEY")))
	mac.Write([]byte(parts[0] + "." + parts[1]))
	expectedSignature := mac.Sum(nil)
	decodedSignature, err := base64.URLEncoding.DecodeString(parts[2])
	if err != nil {
		return false, fmt.Errorf("error decoding signature: %v", err)
	}

	if !hmac.Equal(decodedSignature, expectedSignature) {
		return false, fmt.Errorf("invalid signature")
	}

	// Распаковываем полезную нагрузку в map
	var claims map[string]interface{}
	err = json.Unmarshal(decodedClaims, &claims)
	if err != nil {
		return false, fmt.Errorf("error unmarshalling claims: %v", err)
	}

	// Проверяем срок действия токена
	expiration, ok := claims["exp"].(float64)

	fmt.Println(expiration)

	if !ok {
		return false, fmt.Errorf("invalid expiration time")
	}
	if int64(expiration) != 0 && int64(expiration) < time.Now().Unix() {
		return false, fmt.Errorf("token has expired")
	}

	return true, nil
}
