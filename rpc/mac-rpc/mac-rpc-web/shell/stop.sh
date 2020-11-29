#!/bin/bash

. ./env.conf

echo "##########start boar_web_stop.sh##########"

cd ${TOMCAT_BIN}
is_suc "cd ${TOMCAT_BIN}"

sh shutdown.sh
is_suc "exec shutdown.sh"


echo "##########boar_web_stop.sh SUC##########"
exit 0

