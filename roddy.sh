#!/bin/bash

cd `dirname $0`
RODDY_DIRECTORY=`readlink -f .`
parm1=${1-}

#Resolve the configuration file
# TODO This script is one of the more important ones but could also underly changes.
# Let's use the most current one by default but think hard if this is really good.
source ${RODDY_DIRECTORY}/dist/bin/current/helperScripts/resolveAppConfig.sh
source ${RODDY_DIRECTORY}/dist/bin/current/helperScripts/setupRuntimeEnvironment.sh


SCRIPTS_DIR=$RODDY_BINARY_DIR/helperScripts
source $RODDY_BSCRIPT
