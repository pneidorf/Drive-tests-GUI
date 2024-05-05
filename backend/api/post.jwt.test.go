package api

//swag init -g ./api/*.go

import (
	"backend/internal/net"
	"backend/internal/types"
	"net/http"
)

type jwtValidationResult struct {
	Result string `json:"result"`
	Err    string `json:"error"`
}

// JwtTest Выдача тестового JWT токена (не является сохраняемым)
// @Summary Получение JWT токена
// @Description Создание JWT токена для пользователя "test_user"
// @ID createJwtToken
// @Produce json
// @Success 200 {object} types.Token
// @Router /api/jwt/test [post]
func JwtTest(w http.ResponseWriter, r *http.Request) {

	token := types.JWT("test_user", 0)

	net.Respond(w, http.StatusOK, net.Msg{
		"jwt": token.JWT,
	})
}

// JwtVerify Проверка тестового токена на валидность
// @Summary Проверка валидности JWT токена
// @Description Проверка валидности переданного тестового JWT токена
// @ID verifyJwtToken
// @Accept json
// @Produce json
// @Param Authorization header string true "Bearer your_token" default:"Bearer your_token" in:header
// @Success 200 {object} jwtValidationResult
// @Failure 400 {object} jwtValidationResult
// @Router /api/jwt/verify [post]
func JwtVerify(w http.ResponseWriter, r *http.Request) {
	tokenHeader := net.RequestToken(r)

	var token = types.Token{JWT: tokenHeader}

	isValid, err := token.Verify()

	if !isValid && err != nil {
		net.Respond(w, http.StatusBadRequest, net.Msg{
			"result": "not_valid",
			"error":  err.Error(),
		})
	} else {
		net.Respond(w, http.StatusOK, net.Msg{
			"result": "valid",
			"error":  "",
		})
	}
}
