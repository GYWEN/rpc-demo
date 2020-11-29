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

cd ${work_path}/..
is_suc "cd ${work_path}/.."

LOG_DIR=${LOG_PATH}/${APP_NAME}/${start_port}
ret=`ls |grep ".jar" |grep "${APP_NAME}" |grep -v "grep" |tr '\n' ':'`
MAIN_JAR=`echo ${ret%:*}`
echo "[DEBUG]: CURLINE[$LINENO], MAIN_JAR[${MAIN_JAR}]"
RUN_COMMAND="java -classpath ${CLASSPATH}:${LIBPATH}:${MAIN_JAR} -Dlog.dir=${LOG_DIR} -Dport=${start_port} com.boarsoft.rpc.center.Main"
echo "RUN_COMMAND[${RUN_COMMAND}]"

echo "[INFO]: 应用[$APP_NAME]正在初始化..";
if [ ! -d "$LOG_DIR" ]; then
        mkdir -p "$LOG_DIR"
        echo "[INFO]: 创建目录[$LOG_DIR]成功."
fi
v_count=`ps -ef | grep "$RUN_COMMAND" | grep -v 'grep'|wc -l`
if [ $v_count -eq 0 ]; then
    echo "[INFO]: 应用[$APP_NAME]检查完毕.";
else
		echo "[ERROR]: 已经有应用[$APP_NAME]在运行，无法再次启动!!";
		echo `ps -ef | grep "$RUN_COMMAND" | grep -v 'grep'`
		exit 1
fi

echo "[INFO]: 应用[$APP_NAME]正在启动，时间: "`date +%Y%m%d_%H:%M:%S`
nohup $RUN_COMMAND 1>${LOG_DIR}/out.log 2>${LOG_DIR}/err.log &

echo "[INFO]: 命令已提交." 
sleep 1


count=`ps -ef | grep "$RUN_COMMAND" | grep -v 'grep'|wc -l`
if [ $count -eq 0 ]; then
    echo "[ERROR]: 应用[$APP_NAME]启动失败, 请检查!!";
    exit 1
elif [ $count -gt 1 ]; then
	echo "[ERROR]: 应用[$APP_NAME]启动进程数多于1个, 请检查!!"
	exit 1
fi


info="[INFO]: Application[$APP_NAME] is starting"
COUNTER=0
TOTAL=60
_R=${#info}
_C=`tput cols`
_PROCEC=`tput cols`
tput cup $_C 0
printf "$info."
while [ true ]
do
    if [ `grep "Startup successfully." ${LOG_DIR}/out.log | wc -l` == 1 ]; then
        printf "\n"
        echo "[INFO]: 应用[$APP_NAME]已启动, 时间: "`date +%Y%m%d_%H:%M:%S`
        break;
    fi
    sleep 0.5
    printf "."
    _R=`expr $_R + 1`
    _C=`expr $_C + 1`
    tput cup $_C $_R
    COUNTER=`expr $COUNTER + 1`
    TEMP_COUNT=`expr $COUNTER / 2`
    if [ $TEMP_COUNT -eq $TOTAL ]; then
    	printf "\n"
    	echo "[ERROR]: ip[${LOCAL_IP}]应用[$APP_NAME]在${TOTAL}s内未能启动成功, 请手工检查!!"
    	exit 1
        #break;
    fi
done
echo "[INFO]: 查看输出日志目录[$LOG_DIR/]"


exit 0

