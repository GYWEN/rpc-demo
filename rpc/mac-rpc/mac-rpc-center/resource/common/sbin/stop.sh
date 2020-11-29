#!/bin/bash
#$1: port

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

if [ $# -lt 1 ]; then
	start_port=${DEFAULT_PORT}
else
	start_port=$1
fi


is_stop()
{
	if [ "$1" == "" ]; then
		echo "pid is null, please input process id"
		return 1
	fi

	local cur_num
	local pid=$1
	for ((i=0; i<${APP_SLEEP_NUM}; i++))
	do
		cur_num=1
		#cur_num=`ps -ef |grep "$RUN_COMMAND" |grep "$1" |grep -v "grep" |wc -l`
		cur_num=`ps -ef |awk '$2=='"$pid"' {print}' |wc -l`
		is_suc "ps -ef |awk 'pid==$pid {print}' |wc -l"
		if [ ${cur_num} -eq 0 ]; then
			echo "pid[${pid}] is stopped"
			return 0
		fi

		echo "stop [${APP_SLEEP_TIME}], please wait..."
		sleep ${APP_SLEEP_TIME}
	done

	echo "sleep ${APP_SLEEP_NUM}*${APP_SLEEP_TIME} second, pid[$1] unable to stop, please check !!!"
	return 1
}

cd ${work_path}/..
is_suc "cd ${work_path}/.."

LOG_DIR=${LOG_PATH}/${APP_NAME}/${start_port}
ret=`ls |grep ".jar" |grep "${APP_NAME}" |grep -v "grep" |tr '\n' ':'`
MAIN_JAR=`echo ${ret%:*}`
echo "[DEBUG]: CURLINE[$LINENO], MAIN_JAR[${MAIN_JAR}]"
RUN_COMMAND="java -classpath ${CLASSPATH}:${LIBPATH}:${MAIN_JAR} -Dlog.dir=${LOG_DIR} -Dport=${start_port} com.boarsoft.rpc.center.Main"
echo "RUN_COMMAND[${RUN_COMMAND}]"
 
pids=`ps -ef|grep "$RUN_COMMAND" | grep -v "grep"|awk '{print $2}'`
if [ "$pids" = "" ]; then
	echo "[INFO]: Application[$APP_NAME] does not started !!"
else
	for pid in ${pids}; do
		kill ${pid} 1>/dev/null 2>&1
		is_stop ${pid}
 		#is_suc "stop Application[$APP_NAME](pid=${pid})"
 		if [ $? -ne 0 ]; then
			echo "start kill -9 [${pid}]"
			kill -9 ${pid} 1>/dev/null 2>&1
			is_suc "kill -9 ${pid} 1>/dev/null 2>&1"
		fi
	done
fi


exit 0

