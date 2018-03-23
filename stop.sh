#!/bin/bash

instance_id=$(cat config.properties | awk -F "=" '/instanceId/ {print $2}')
if [ -z "$instance_id" ]
then
	echo No instance id defined
	exit;
fi

filter="instance.id=$instance_id"

pids=$(ps aux | grep java | grep $filter | awk '{print $2}')

if [ ! -z "$pids" ]
then
	printf "\nPids to kill:\n$pids\n\n"
    echo $pids | xargs kill -15

	for i in `seq 1 10`; do
	  sleep 1s
	  flag=1
	  for pid in $pids; do
		if ps aux | grep $filter | awk '{print $2}' | grep $pid &> /dev/null
		then
		  echo "$pid is alive"
		  flag=0
		else
		  echo "$pid was killed"
		fi
	  done
	  if [ "$flag" -eq 1 ]
	  then
		printf "All killed\n\n"
		break
	  else
		printf "Waiting...\n\n"
	  fi
	done

	if [ "$flag" -eq 0 ]
	then
	  echo "Something is still alive"
	  for pid in $pids; do
		if ps aux | grep $filter | awk '{print $2}' | grep $pid &> /dev/null
		  then
			echo "Sending hard kill to pid $pid"
			kill -9 $pid
		fi
	  done
	fi
fi