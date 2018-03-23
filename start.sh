DIR="$( cd "$( dirname "$0" )" && pwd )"
cd "$DIR"

./stop.sh

ulimit -n 20000

instance_id=$(cat config.properties | awk -F "=" '/instanceId/ {print $2}')
if [ -z "$instance_id" ]
then
	echo No instance id defined
	exit;
fi

filter="instance.id=$instance_id"

nohup java -Dfile.encoding=UTF-8 -Dinstance.id=$instance_id -classpath agronomu_server.jar agronomu.server.AgronomuServer >/dev/null 2>&1  & echo ok

printf "\n"
jps -v | grep $filter
printf "\n"
