package api

import (
	"backend/internal/net"
	"backend/internal/types"
	"encoding/json"
	"net/http"
)

type authRequest struct {
	Password string `json:"password"`
	Email    string `json:"email"`
}

type authResponseError struct {
	Err string `json:"error"`
}

type authResponseSuccess struct {
	Email string `json:"email"`
	Jwt   string `json:"jwt"`
}

// UserAuth аутентифицирует пользователя и выдает токен доступа.
//
// @Summary Аутентификация пользователя и выдача токена доступа
// @Description Производит аутентификацию пользователя на основе предоставленных данных
// @Description При авторизации без пароля можно произвести авторизацию через Token
// @Description Для этого по аналогии с /api/jwt/verify в поле Authorization нужно разместить ваш значение Bearer: <your_token>
// @Description Email всё равно нужно указать для избежания "призрачных" аккаунтов, сопоставляется с текущими email
// @ID authenticateUser
// @Accept json
// @Produce json
// @Param Authorization header string false "Bearer your_token" default:"Bearer your_token" in:header
// @Param body body authRequest false "Данные пользователя для аутентификации"
// @Success 200 {object} authResponseSuccess "Успешная аутентификация"
// @Failure 400 {object} authResponseError "Ошибка аутентификации"
// @Router /api/user/auth [post]
func UserAuth(w http.ResponseWriter, r *http.Request) {
	user := &types.Account{}

	if err := json.NewDecoder(r.Body).Decode(user); err != nil {
		net.Respond(w, http.StatusBadRequest, net.Msg{
			"error": "fault decoding body",
		})
		return
	}

	token := types.Token{JWT: net.RequestToken(r)}

	if result, _ := token.Verify(); result {
		if !user.Email.IsValid() {
			net.Respond(w, http.StatusBadRequest, net.Msg{
				"error": "Invalid email format",
			})
			return
		}

		if errLogin := user.Login(true); errLogin != nil {
			net.Respond(w, http.StatusBadRequest, net.Msg{
				"error": errLogin.Error(),
			})
			return
		}

		net.Respond(w, http.StatusOK, net.Msg{
			"jwt":   token.JWT,
			"email": user.Email,
		})
		return
	}

	if isValid, err := user.Validate(); !isValid {
		net.Respond(w, http.StatusBadRequest, net.Msg{
			"error": err,
		})
		return
	}

	if err := user.Login(false); err != nil {
		net.Respond(w, http.StatusBadRequest, net.Msg{
			"error": err.Error(),
		})
		return
	}

	net.Respond(w, http.StatusBadRequest, net.Msg{
		"jwt":   user.Token.JWT,
		"email": user.Email,
	})
}
