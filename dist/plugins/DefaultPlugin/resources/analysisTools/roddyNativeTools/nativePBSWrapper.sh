#!/usr/bin/env bash
# Script used to wrap in a native PBS based workflow

# This script wraps in a complete native workflow script and executes it.
source ${CONFIG_FILE}

cd $(dirname $WRAPPED_SCRIPT)

set -xv
# get the current path and put the directory with replacement commands before it.
QSUB_FOLDER=$(dirname ${TOOL_QSUB_REPLACEMENT})
export PATH="${QSUB_FOLDER}":${PATH}
set +xv

[[ ${debugWrapInScript-false} == true ]]  && set -xv
[[ ${debugWrapInScript-false} == false ]] && set +xv

export WRAPPED_SCRIPT=${WRAPPED_SCRIPT} # Export script so it can identify itself
set -xv
[[ ! -d $DIR_TEMP ]] && mkdir $DIR_TEMP

export tmpFileCnt=$DIR_TEMP/counter
export tmpFileCall=$DIR_TEMP/calls
export cur=0

echo 0 > $tmpFileCnt

#qsub "ABC"

echo "Calling script ${WRAPPED_SCRIPT}"
source ${WRAPPED_SCRIPT}
exitCode=$?
echo "Exited script ${WRAPPED_SCRIPT} with value ${exitCode}"

[[ ${debugWrapInScript-false} == true ]]  && set -xuv
[[ ${debugWrapInScript-false} == false ]] && set +xuv

sleep 2