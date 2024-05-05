package api

import (
	"backend/internal/net"
	"backend/internal/types"
	"net/http"
	"os"
)

type linkResponse struct {
	TestLink string `json:"test_link"`
}

type linkResponseErr struct {
	Err string `json:"error"`
}

// UserVerify верифицирует почту.
//
// @Summary Верификация почты путём перехода по ссылке
// @Description Проверка валидности переданного ключа верификации пользователя
// @Description Для выдачи такой ссылки в поле key ничего не пишите, поставьте поле "тестовый режим" на значение 1 и сделайте отправку
// @Description Для конечной проверки перейдите по тестовой ссылке (НЕ ЯВЛЯЕТСЯ ЧАСТЬЮ API ради забавы разместил)
// @ID verifyUserKey
// @Accept json
// @Produce json
// @Param key query string false "Ключ верификации пользователя"
// @Param test query integer false "Тестовый режим" Enums(0, 1) default(0)
// @Success 200 {object} net.Msg "Успешная проверка ключа"
// @Success 426 {object} linkResponse "Ключ"
// @Failure 400 {object} linkResponseErr "Ошибка при проверке ключа"
// @Router /api/user/verify [get]
func UserVerify(w http.ResponseWriter, r *http.Request) {
	queryParams := r.URL.Query()
	keyParam := queryParams.Get("key")
	testParam := queryParams.Get("test")

	// for tests
	if testParam == "1" && keyParam == "" {
		testUser := types.Account{Email: "email@email.com"}
		link := testUser.VerificationLink()

		net.Respond(w, http.StatusUpgradeRequired, net.Msg{
			"test_link": "http://" + os.Getenv("API_HOST") + ":" + os.Getenv("SERVER_PORT") + "/api/user/verify?key=" + link,
		})
		return
	}

	if keyParam == "" {
		net.Respond(w, http.StatusBadRequest, net.Msg{
			"error": "unknown link",
		})
		return
	}

	result, err := types.SignVerificationLink(keyParam)

	if !result && err != nil {
		net.Respond(w, http.StatusBadRequest, net.Msg{
			"error": err.Error(),
		})
		return
	}

	net.Respond(w, http.StatusOK, net.Msg{
		"message": "validated",
	})
}
