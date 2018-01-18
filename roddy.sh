#!/bin/bash

export CURRENT_PWD="$PWD"

cd `dirname $0`
RODDY_DIRECTORY=`readlink -f .`
parm1=${1-}
projectAnalysisParameter=$2

declare -a fullParameterList=( "$@" )
export fullParameterList


export GROOVY_BINARY=groovy

# Default Roddy Java options
JAVA_OPTS=${JAVA_OPTS:--Xms64m -Xmx500m}

# OFS is the original field separator
export OFS=$IFS
IFS=""

# This is a hack! Get rid of the field separator to allow the proper translation of BASH arrays
IFS=$OFS

wait $serverpid

#Resolve the configuration file
# TODO This script is one of the more important ones but could also underly changes.
# Let's use the most current one by default but think hard if this is really good.
source ${RODDY_DIRECTORY}/dist/bin/current/helperScripts/resolveAppConfig.sh
source ${RODDY_DIRECTORY}/dist/bin/current/helperScripts/setupRuntimeEnvironment.sh

SCRIPTS_DIR=$RODDY_BINARY_DIR/helperScripts

source $RODDY_BSCRIPT "$@"
