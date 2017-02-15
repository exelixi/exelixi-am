#!/bin/bash

if [ -z "$4" ]; then
echo "$0 <min> <increment> <max> <file>"
exit
fi

MIN=$1
INCREMENT=$2
MAX=$3
FILE=$4


for ((a=$MIN; a <= $MAX ; ))
do
   echo -e "$a" >> $FILE
   a=$((a+INCREMENT))
done
