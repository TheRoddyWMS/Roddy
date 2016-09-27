#!/bin/bash

env
source ${CONFIG_FILE}

echo $INPUT_FILES
echo ${INPUT_FILES[@]}
for v in ${INPUT_FILES[@]}
do
  echo $v
done

set -xuv

sleepCnt=${SLEEP_COUNT-5}
sleep ${sleepCnt}

echo "Got an infile ${FILENAME_IN}"
echo "Slept ${sleepCnt} seconds"
echo "Put 0 to an outfile ${FILENAME_OUT}"
echo "0" > ${FILENAME_OUT}

exit 0
