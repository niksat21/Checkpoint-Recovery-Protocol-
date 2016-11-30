#!/bin/bash


# Change this to your netid
netid=axr140930


# Root directory of your project
PROJDIR=/home/010/a/ax/axr140930/AOS_Project3/com.aos.lab3

CONFIG=$PROJDIR/conf/config.txt

#
# Directory your java classes are in
#
BINDIR=$PROJDIR/bin

#
# Your main project class
#
PROG=com.aos.lab3.Process


cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" | grep "\s*[0-9]\+\s*\w\+.*" |
(
	read temp
	noOfNodes=$(echo $temp | awk '{ print $1}')
	count=0
    while read line 
    do
	n=$( echo $line | awk '{ print $1 }' )
	host=$( echo $line | awk '{ print $2 }' )
	nodeId=$( echo $line | awk '{ print $1 }' )
    ssh -o StrictHostKeyChecking=no $netid@$host java -cp $BINDIR:$PROJDIR/lib/* -Dconfig=$CONFIG -Dlog4j.configurationFile=$PROJDIR/conf/log4j.xml -DnodeId=$nodeId $PROG $n &
	((count++))
	if [ "$noOfNodes" -eq "$count" ]
	then
		break
	fi
    done
)
