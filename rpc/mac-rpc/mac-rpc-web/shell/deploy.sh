#!/bin/bash

. ./env.conf

echo "##########boar_web_deploy.sh start##########"

sh stop.sh
is_suc "exec stop.sh"

sh update.sh
is_suc "exec update.sh"

sh start.sh
is_suc "exec start.sh"


echo "##########boar_web_deploy.sh SUC##########"
exit 0

