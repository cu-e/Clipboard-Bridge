@echo off
chcp 65001 > nul
echo Запуск клиента Clipboard Bridge с поддержкой UTF-8

if "%1"=="" (
  echo Использование буфера обмена
  java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Djava.awt.headless=false -jar build\libs\client-0.0.1-SNAPSHOT.jar
) else (
  echo Использование аргумента: %*
  java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Djava.awt.headless=false -jar build\libs\client-0.0.1-SNAPSHOT.jar -m "%*"
)