#!/bin/bash
# As of version 2.1.1, Roddy supports mutliple binary and plugin versions. So the useconfig option is already resolved here (and also in the roddy binary).

# IMPORTANT: The file needs to be called by roddy.sh to work as designed

customconfigfile=applicationProperties.ini

for option in $@
do
    [[ $option == --useconfig* ]] && customconfigfile=${option:12:800}
done
#set -xv

if [[ ${customconfigfile-false} != false ]]
then
    if [[ -f ${customconfigfile} ]]    # Check the full path.
    then
        customconfigfile=$customconfigfile
    elif [[ -f ~/.roddy/${customconfigfile} ]]    # Look in the settings directory.
    then
        customconfigfile=~/.roddy/${customconfigfile}
    elif [[ -f `dirname $0`/${customconfigfile} ]]    # Finally look in the application directory.
    then
        customconfigfile=`dirname $0`/${customconfigfile}
    fi

    _temp=`cat ${customconfigfile} | grep useRoddyVersion || echo 0` 
    if [[ $_temp != 0 ]] && [[ $_temp != "useRoddyVersion=" ]]
    then
        useRoddyVersion=`cat ${customconfigfile} | grep useRoddyVersion | cut -d "=" -f 2 | cut -d "#" -f 1 | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'`
        RODDY_BINARY_DIR=${RODDY_DIRECTORY}/dist/bin/$useRoddyVersion
        RODDY_BINARY=$RODDY_BINARY_DIR/Roddy.jar
        RODDY_BSCRIPT=$RODDY_BINARY_DIR/roddy.sh
    fi
fi

overrideRoddyVersionParameter=""

#Is the roddy binary or anything set via command line?
for i in $*
do
    if [[ $i == --useRoddyVersion* ]]; then
        overrideRoddyVersionParameter=${i:18:40}
        RODDY_BINARY_DIR=${RODDY_DIRECTORY}/dist/bin/${overrideRoddyVersionParameter}
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

if [[ -z ${RODDY_BINARY_DIR-} ]]
then
    RODDY_BINARY_DIR=${RODDY_DIRECTORY}/dist/bin/current
    RODDY_BINARY=$RODDY_BINARY_DIR/Roddy.jar
    RODDY_BSCRIPT=$RODDY_BINARY_DIR/roddy.sh
fi

# Resolve used groovy and java version
# Reads out the 7th byte and translates JDK 1.8 is 52.
# note: 16# outputs the hexadecimal number as decimal!
JDK_VERSION=$(( 16#`unzip -p $RODDY_BINARY de/dkfz/roddy/Constants.class | hexdump  -n 8  | cut -d " " -f 5  | head -n 1 | cut -b 1-2` ))
GROOVY_VERSION=$( basename `ls $RODDY_BINARY_DIR/lib/groovy-*.jar` | cut -d "-" -f 3  | cut -d "." -f 1-2 )
#set +xv
