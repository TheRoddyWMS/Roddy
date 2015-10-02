#!/bin/bash

# This is a sample native shell script workflow

#source ${CONFIG_FILE}

absolutePath=$(dirname $(readlink -f $WRAPPED_SCRIPT))

abc=`qsub -l mem=1g -l walltime=00:10:00 -m a -N "roddyTest_testScript" $absolutePath/testScriptSleep.sh`
def=`qsub -l mem=1g -l walltime=00:10:00 -m a -W depend=afterok:$abc -N "roddyTest_testScript" $absolutePath/testScriptSleep.sh`
     qsub -l mem=1g -l walltime=00:10:00 -m a -W depend=afterok:$def -N "roddyTest_testScriptExitBad" $absolutePath/testScriptSleepExitBad.sh