#!/bin/bash


# Change this to your netid
netid=axr140930

#
# Root directory of your project
PROJDIR=$HOME/AOS_Project2/com.aos.lab2

#
# This assumes your config file is named "config.txt"
# and is located in your project directory
#
CONFIG=$PROJDIR/config.txt

#
# Directory your java classes are in
#
BINDIR=$PROJDIR/bin

#
# Your main project class
#
#PROG=Project1

n=1

cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    echo $i
    while read line 
    do
        host=$( echo $line | awk '{ print $1 }' )

        echo $host
        ssh -o "StrictHostKeyChecking no" $netid@$host  pkill -f java -u axr140930 &
        ssh -o "StrictHostKeyChecking no" $netid@$host  pkill -f tcp-server -u axr140930 &
        sleep 1

        n=$(( n + 1 ))
    done
   
)


echo "Cleanup complete"
