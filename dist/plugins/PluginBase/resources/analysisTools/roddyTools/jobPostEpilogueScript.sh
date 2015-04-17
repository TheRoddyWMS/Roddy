#!/bin/bash
set -x
set -v

# wait for some seconds to make sure that the job is finished, TODO: Maybe check against qstat to see if the job still runs
#sleep 5 

jobStateLogFile=testLog.txt
qstatContent=`qstat -f -1 ${1}`

# extract the config file from the parameters of the job
EXIT_STATUS_TMP=`echo ${qstatContent} | grep "exit_status" | sed -n 1'p' | tr ',' '\n' | grep "exit_status"`
# Set error
EXIT_STATUS=E
# Set good if no error was found
[[ "$EXIT_STATUS_TMP" == `echo $EXIT_STATUS_TMP | grep "exit_status = 0"` ]] && EXIT_STATUS=C

CONFIG_FILE=`echo ${qstatContent} | grep "CONFIG_FILE" | sed -n 1'p' | tr ',' '\n' | while read word; do [[ "$word" == CONFIG_FILE* ]] && echo "/${word#*/}"; done`
# extract the qstat string
echo $CONFIG_FILE
#stateString=`qstat ${1} | tail -n 1 | awk '{print $5}'`

if [ -n "$CONFIG_FILE" ]
then
    source ${CONFIG_FILE} # include the config file to extract the output log file.
    umask $outputUMask
    _lock="$jobStateLogFile~"
    lockfile $_lock

    echo "${1}:${EXIT_STATUS}" >> $jobStateLogFile

    rm -f $_lock
fi

# TODO: Throw some error code or log to tell someone this failed!
