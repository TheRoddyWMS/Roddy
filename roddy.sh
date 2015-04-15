#!/bin/bash

cd `dirname $0`

RODDY_DIRECTORY=`readlink -f .`
parm1=${1-}

#Resolve the configuration file
# TODO This script is one of the more important ones but could also underly changes.
# Let's use the most current one by default but think hard if this is really good.
source dist/bin/current/helperScripts/resolveAppConfig.sh

overrideRoddyVersionParameter=""

#Is the roddy binary or anything set via command line?
for i in $*
do
    if [[ $i == --useRoddyVersion* ]]; then
        overrideRoddyVersionParameter=${i:18:40}
        RODDY_BINARY_DIR=dist/bin/${overrideRoddyVersionParameter}
        RODDY_BINARY=$RODDY_BINARY_DIR/Roddy.jar
        RODDY_BSCRIPT=$RODDY_BINARY_DIR/roddy.sh
        if [[ ! -f $RODDY_BINARY  ]]; then
            echo "${RODDY_BINARY} not found, the following versions might be available:"
            for bin in `ls -d dist/bin`; do
                echo "  ${bin}"
            done
            exit 1
        fi
    fi
done

SCRIPTS_DIR=$RODDY_BINARY_DIR/helperScripts

source $RODDY_BSCRIPT
