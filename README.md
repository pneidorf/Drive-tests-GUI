# Drive-tests-GUI
Данный проект предназначен для сбора и обработки информации о базовых станциях с помощью kotlin, go, python, react.
## Kotlin - Vladimir Ponomarenko
Android приложение, которое собирает данные о сигнале LTE и местоположении устройства. Оно получает информацию о силе сигнала (RSRP, RSSI, RSRQ, RSSNR), качестве (CQI), пропускной способности и идентификаторе соты. Собранные данные отправляются на сервер через WebSocket соединение в формате JSON. Программа также имеет функции авторизации и регистрации пользователей. Цель приложения - сбор и анализ данных о качестве связи.
## Go - Egor Artemenko

## Python - Denis Babenko
Отрисовка радиуса покрытия сигнала базовой станции и передача на react
## React - Pavel Neidorf
Отображение всех графиков и карты покртия сигнала для пользователей
## Jenkins - Kirill Kulakov
Реализация CI/CD на Jenkins 
На сервере с  ubuntu 20.4, был установлен Jenkins, для него были установлены все необходимые плагины
Сервер был поднят на Yandex Cloud
Адрес сервера: 158.160.172.63:8080 
Реализован pipeline, в котором будет происходить билд проекта, а затем тест каждой его части
Интегрирован webhook для удаленного запуска тестирования после взаимодействия с репозиторием

## Установка
Для того, чтобы загрузить к себе проект, нужно прописать команды
'''
git copy https://github.com/pneidorf/Drive-tests-GUI.git
git submodule init
git submodule update
'''
