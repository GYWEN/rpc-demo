#!/bin/bash

. ./env.conf

echo "##########start boar_web_start.sh##########"

cd ${TOMCAT_BIN}
is_suc "cd ${TOMCAT_BIN}"

sh startup.sh
is_suc "exec startup.sh"


echo "##########boar_web_start.sh SUC##########"
exit 0

