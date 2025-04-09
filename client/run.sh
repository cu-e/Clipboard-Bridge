#!/bin/bash
echo "Запуск клиента Clipboard Bridge с поддержкой UTF-8"

if [ -z "$1" ]; then
  echo "Использование буфера обмена"
  java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -jar build/libs/client-0.0.1-SNAPSHOT.jar
else
  echo "Использование аргумента: $*"
  java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -jar build/libs/client-0.0.1-SNAPSHOT.jar -m "$*"
fi 