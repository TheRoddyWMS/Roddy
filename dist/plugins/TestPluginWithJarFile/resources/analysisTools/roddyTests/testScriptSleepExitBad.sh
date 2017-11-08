#!/bin/bash

set -xuv

sleepCnt=${SLEEP_COUNT-5}
sleep ${sleepCnt}

echo "Got an infile ${FILENAME_IN}"
echo "Slept ${sleepCnt} seconds"
echo "Put 0 to an outfile ${FILENAME_OUT}"
echo "0" > ${FILENAME_OUT}

exit -5
