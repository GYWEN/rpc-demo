#!/bin/bash

. ./env.conf
echo "##########start boar_web_replace.sh##########"

if [ ! -d ${APP_PATCH} ]; then
	mkdir -p ${APP_PATCH}
	is_suc "mkdir -p ${APP_PATCH}"
fi

cd ${APP_PATCH}
is_suc "exec cd ${APP_PATCH}"

echo "about to delete old files:${APP_PATCH}/*"
rm -rf ${APP_PATCH}/*
is_suc "rm -rf ${APP_PATCH}/*"

echo "about to copy:from ${SEND_TEMP_PATH}/soagov*.war to ${APP_PATCH}"
cp -rf ${SEND_TEMP_PATH}/soagov*.war ${APP_PATCH}
is_suc "exec cp -rf ${SEND_TEMP_PATH}/boar*.war ${APP_PATCH}"

jar -xvf ${APP_WAR_NAME}
is_suc "jar -xvf ${APP_WAR_NAME}"

rm ${APP_WAR_NAME}
is_suc "rm ${APP_WAR_NAME}"

rm -rf ${SEND_TEMP_PATH}/*
is_suc "rm -rf ${SEND_TEMP_PATH}/*"

echo "##########boar_web_replace.sh SUC##########"



exit 0

