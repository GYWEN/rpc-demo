#!/bin/bash

work_path=`dirname $0`
if [[ "$?" != "0" ]]; then
	echo "[ERROR]: CURLINE[$LINENO], dirname $0"
	exit 1
fi

. ${work_path}/env.conf
if [[ "$?" != "0" ]]; then
	echo "[ERROR]: CURLINE[$LINENO], . ${work_path}/env.conf"
	exit 1
fi

sh stop.sh
is_suc "stop app"

sh start.sh
is_suc "start app"

exit 0
