#!/bin/bash

PROG=Node
NETID=$2
PROGRAM_PATH=/home/013/r/rx/rxk152130/testing2

#rm *.class *.out
#javac *.java

cat $1 | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    #echo $i    
    #netId=$( echo $i | awk '{ print $2 }' )
    totalNodes=$( echo $i | awk '{ print $1 }' )
    #echo $netId	
    
    for ((a=1; a <= $totalNodes ; a++))
    do
    	read line 
	#echo $line
	nodeId=$( echo $line | awk '{ print $1 }' )
       	host=$( echo $line | awk '{ print $2 }' )
	echo $nodeId
	echo $host
	echo $NETID
	echo $PROGRAM_PATH
    echo $3
    gnome-terminal -e "ssh -o StrictHostKeyChecking=no -l \"$NETID\" \"$host.utdallas.edu\" \"cd $PROGRAM_PATH;java $PROG $nodeId $1 $3\" "
    #ssh -o StrictHostKeyChecking=no -l "$NETID" "$host.utdallas.edu" "cd $PROGRAM_PATH;java $PROG $nodeId $1 $3" &
	
    done
   
)