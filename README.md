# Drive-tests-GUI
Данный проект предназначен для сбора и обработки информации о базовых станциях с помощью kotlin, go, python, react.
## Kotlin - Vladimir Ponomarenko

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
