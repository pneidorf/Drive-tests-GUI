package api

import (
	"backend/internal/net"
	"backend/internal/types"
	"encoding/json"
	"net/http"
	"os"
)

type registerRequest struct {
	Password string `json:"password"`
	Email    string `json:"email"`
}

type registerResponseError struct {
	Err string `json:"error"`
}

type registerResponseSuccess struct {
	Email   string `json:"email"`
	Jwt     string `json:"jwt"`
	Message string `json:"message"`
}

// UserRegister регистрирует пользователя и выдает токен доступа.
//
// @Summary Регистрирует пользователя и выдача токена доступа
// @Description Производит аутентификацию пользователя на основе предоставленных данных
// @Description При авторизации без пароля можно произвести авторизацию через Token
// @Description Для этого по аналогии с /api/jwt/verify в поле Authorization нужно разместить ваш значение Bearer: <your_token>
// @Description Email всё равно нужно указать для избежания "призрачных" аккаунтов, сопоставляется с текущими email
// @ID registrationUser
// @Accept json
// @Produce json
// @Param Authorization header string false "Bearer your_token" default:"Bearer your_token" in:header
// @Param body body registerRequest false "Данные пользователя для аутентификации"
// @Success 200 {object} registerResponseSuccess "Успешная аутентификация"
// @Failure 400 {object} registerResponseError "Ошибка аутентификации"
// @Router /api/user/register [post]
func UserRegister(w http.ResponseWriter, r *http.Request) {
	user := &types.Account{}

	if err := json.NewDecoder(r.Body).Decode(user); err != nil {
		net.Respond(w, http.StatusBadRequest, net.Msg{
			"error": "fault decoding body",
		})
		return
	}

	if isValid, err := user.Validate(); !isValid {
		net.Respond(w, http.StatusBadRequest, net.Msg{
			"error": err,
		})
		return
	}

	link := user.VerificationLink()

	user.Email.SendMessage("Подтверждение email", `
		Ваш email был кандидатом на регистрацию в нашем сервисе.<br>
		Перейдите по ссылке для подтверждения:<br>
		http://`+os.Getenv("API_HOST")+`:`+os.Getenv("SERVER_PORT")+`/api/user/verify?key=`+link+
		`<br>`)

	if err := user.New(); err != nil {
		net.Respond(w, http.StatusBadRequest, net.Msg{
			"error":   err.Error(),
			"message": "check your email",
		})
		return
	}

	net.Respond(w, http.StatusOK, net.Msg{
		"jwt":     user.Token.JWT,
		"email":   user.Email,
		"message": "Check your email",
	})
}
