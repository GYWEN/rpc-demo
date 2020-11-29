java -classpath ${CLASSPATH}:./lib:./mac-rpc-app2-1.0.0.jar -Dlog.dir=./logs -Dport=9102 -Dlog.appender=STDOUT -Dlogback.configurationFile=./conf/logback.xml com.boarsoft.rpc.demo.Main 9102 $1 $2 $3
