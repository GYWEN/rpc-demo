#!/bin/bash
#$1: 需要执行shell的目录
#$2: 需要执行shell的名称
#后续: 需要执行shell的参数
#例: sh /home/nlzx/boar-web/shell/pub.sh /home/nlzx/boar-web/shell \
#	package.sh CIT/boar-svn/boar-web CIT


is_suc() 
{
	if [ $? -eq 0 ]; then
		echo "[INFO]: command [$1] is success"
		return 0
	else
		echo ""
		echo "[ERROR]: command [$1] is failed!!!"
		exit 1
	fi
}

echo "##########exec.sh start##########"

sbin_path=$1
if [[ ! -d "${sbin_path}" ]] ; then
	echo "input sbin path[${sbin_path}] not found !!!"
	exit 1
fi

cd ${sbin_path}
is_suc "cd ${sbin_path}"

sbin=$2
cmd="sh ${sbin}"
par_str="$*"
par_num="$#"
echo "par_str[${par_str}], par_num[${par_num}]"

cmd=${cmd}"${par_str#*${sbin}}"
echo "cmd is :[${cmd}]"
${cmd}
#is_suc "exec [${cmd}]"
if [[ $? -ne 0 ]]; then
	echo "[ERROR]: exec [${cmd}] is failed!!!"
	exit 1
fi



exit 0

