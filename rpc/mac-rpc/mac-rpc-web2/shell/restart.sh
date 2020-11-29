#!/bin/bash

. ./env.conf

echo "##########start boar_web_restart.sh##########"

sh stop.sh
is_suc "exec stop.sh"

sh start.sh
is_suc "exec start.sh"

echo "##########boar_web_restart.sh SUC##########"



exit 0

