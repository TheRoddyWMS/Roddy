#!/bin/sh
# As of version 2.1.1, Roddy supports mutliple binary and plugin versions. So the useconfig option is already resolved here (and also in the roddy binary).

customconfigfile=applicationProperties.ini

for option in $@
do
    [[ $option == --useconfig* ]] && customconfigfile=${option:12:800}
done

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

if [[ -z ${RODDY_BINARY_DIR-} ]]
then
    RODDY_BINARY_DIR=${RODDY_DIRECTORY}/dist/bin/current
    RODDY_BINARY=$RODDY_BINARY_DIR/Roddy.jar
    RODDY_BSCRIPT=$RODDY_BINARY_DIR/roddy.sh
fi
