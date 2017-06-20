#!/bin/bash
# As of version 2.1.1, Roddy supports mutliple binary and plugin versions. So the useconfig option is already resolved here (and also in the roddy binary).

# IMPORTANT: The file needs to be called by roddy.sh to work as designed

BASE_DIR=`dirname $0`
SCRIPTS_DIR=$BASE_DIR/dist/bin/current/helperScripts
customconfigfile=applicationProperties.ini
autoSelectRoddy=false
foundPluginID=none

function grepFromConfigFile() {
  local stringToGrep=$1
                                  # Strip comments                    get the second field
                                  #              Grep all the strings                   strip comments from end and trim.
  echo `cat ${customconfigfile} | grep -v "^#" | grep -v "^;" | grep $stringToGrep | cut -d "=" -f 2 | cut -d "#" -f 1 | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' | tail -n 1`
}

function tryExtractRoddyVersionFromPlugin() {
  [[ $parm1 == compile* ]] && echo "current" # Output current in case of compilation
  echo $( getValueFromConfigOrCommandLine useRoddyVersion useRoddyVersion rv )
}

function tryExtractPluginIDFromConfig() {
  local var=$( getValueFromConfigOrCommandLine usePlugin usePlugin )
  echo ${var-none}
}

function setRoddyBinaryVariables() {
  local useRoddyVersion=$1
  [[ $1 == auto ]] && useRoddyVersion=current && autoSelectRoddy=true
  local numberOfDots=$(grep -o "[.]" <<< "$useRoddyVersion" | wc -l)    # Try find out, if we got an api level input like 2.2 or 2.3
  [[ $numberOfDots == 1 ]] && useRoddyVersion=$(cd $RODDY_DIRECTORY/dist/bin; ls -d $useRoddyVersion* | sort -V | tail -n 1)
  export activeRoddyVersion=$useRoddyVersion
  RODDY_BINARY_DIR=${RODDY_DIRECTORY}/dist/bin/$useRoddyVersion
  RODDY_BINARY=$RODDY_BINARY_DIR/Roddy.jar
  RODDY_BSCRIPT=$RODDY_BINARY_DIR/roddy.sh
}

function getValueFromConfigOrCommandLine() {
  local valueNameInCfg=$1
  local valueNameOnCLI=$2
  local valueNameOnCLIShort=$3
  [[ -z "$3" ]] && valueNameOnCLIShort=$valueNameOnCLI
  local var="none"
  local startIndex

  IFS=""
  for i in ${fullParameterList[@]}; do
    if [[ $i == --${valueNameOnCLI}*  ]]; then
      startIndex=$(expr 2 + ${#valueNameOnCLI} + 1)
      var=${i:$startIndex:800}
    elif [[ -z ${valueNameOnCLIShort-} || $i == --${valueNameOnCLIShort} || $i == --${valueNameOnCLIShort}"="* ]]; then
      startIndex=$(expr 2 + ${#valueNameOnCLIShort} + 1)
      var=${i:$startIndex:800}
    fi
  done
  if [[ ${var-none} == none ]]; then
    var=$(grepFromConfigFile $valueNameInCfg)
  fi
  IFS=$OFS
  echo $var
}

for option in $@
do
    [[ $option == --useconfig* ]] && customconfigfile=${option:12:800}
    [[ $option == --c* ]]         && customconfigfile=${option:4:800}
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
        setRoddyBinaryVariables $(tryExtractRoddyVersionFromPlugin)
        foundPluginID=$(tryExtractPluginIDFromConfig)
    fi
fi


overrideRoddyVersionParameter=""

#Is the roddy binary or anything set via command line?
for i in $*
do
    index=-1
    [[ $i == --useRoddyVersion* ]] && index=18
    [[ $i == --useroddyversion* ]] && index=18
    [[ $i == --rv* ]] && index=5

    if [[ $index -gt 0 ]]; then
        setRoddyBinaryVariables ${i:${index}:40}
        if [[ ! -f $RODDY_BINARY  ]]; then
            echo "${RODDY_BINARY} not found, the following versions might be available:"
            for bin in `ls -d dist/bin`; do
                echo "  ${bin}"
            done
            exit 1
        fi
    fi

    if [[ $i == --usePluginVersion* ]]; then
        foundPluginID=${i:19:140}
    fi
done

if [[ -z ${RODDY_BINARY_DIR-} ]]
then
    # Find the latest version available
    setRoddyBinaryVariables $(cd dist/bin; ls -d *.*.* current | grep -v ".zip" | sort -V | tail -n 1)
fi

# If auto selection is enabled, the script tries to identify the proper version from the buildinfo text file of the called plugin.
# If the plugin is known via ini or via parameter, we can use a fast Bash version to load the roddy version.
# If it is set in xml... we'll have to call a current Roddy version and see what happens.
if [[ $autoSelectRoddy == true && ! $parm1 == autoselect ]]; then
    echo "Roddy auto selection is active! Using current to determine the version from the selected plugin."
    if [[ ${foundPluginID:-none} != "none" ]]; then
        echo "A plugin $foundPluginID was set with usePluginVersion, will try to figure out the Roddy version with Bash."
        pluginDirectories=$(getValueFromConfigOrCommandLine pluginDirectories pluginDirectories)

        # TODO groovy is not yet setup! How can we do this in a convenient way?
        pluginDirectory=`source $SCRIPTS_DIR/setupRuntimeEnvironment.sh &> /dev/null; $GROOVY_BINARY ${SCRIPTS_DIR}/findPluginFolders.groovy pluginDirectories=${pluginDirectories} ${RODDY_DIRECTORY} ${foundPluginID}`
        pluginBuildInfo=$pluginDirectory/buildinfo.txt

        # Extract it from parameter or ini file
        foundAPIVersion=$([[ -f $pluginBuildInfo ]] && cat $pluginBuildInfo | grep "^RoddyAPIVersion" | cut -d "=" -f 2 || echo "")

    else
        echo "Going the long way and figure out the Roddy version with the core application."
        setRoddyBinaryVariables current
        foundAPIVersion=$(source $SCRIPTS_DIR/setupRuntimeEnvironment.sh &>/dev/null; 2>&1  ${BASE_DIR}/roddy.sh autoselect $projectAnalysisParameter --useconfig=$customconfigfile --useRoddyVersion=current | grep "Roddy API level for" | tail -n 1 | cut -d "=" -f 3 )
    fi

    # Fall back to 2.2 (the version before RoddyAPIVersion was introduced), if necessary
    foundAPIVersion=${foundAPIVersion:-2.2}
    echo "Selected Roddy API version ${foundAPIVersion}"
    setRoddyBinaryVariables $foundAPIVersion

    # Replace command line parameter =auto with =x.y.z
    # Store the fullParameterList variable to a new variable to prevent a mess up in Bashs array handling:
    #   a=`echo ${a[@]} sed 's/d/f/g'`  replaced the 4th parameter in fullParameterList so that:
    #   a=( a b c d ) would become a=( a b c f b c d )

    export fullParameterListFinal=""
    if [[ ${fullParameterList[@]} == *useRoddyVersion=auto* ]]; then
      # Replace what is set as a parameter
      fullParameterListFinal=$(echo ${fullParameterList[@]} | sed "s/useRoddyVersion=auto/useRoddyVersion=${activeRoddyVersion}/g")
    else # It is not set or in the ini. So set it now and override everything else.
      IFS="" fullParameterListFinal=(${fullParameterList[@]} --useRoddyVersion=${activeRoddyVersion})
    fi

    # Break here, call Roddy with the new parameter list... Exit afterwards! Get rid of all the s**t which happens when you try to change things... Remember the old lines however!
    ${BASE_DIR}/roddy.sh ${fullParameterListFinal[@]}
    exit $?
fi

# Resolve used groovy and java version
# Reads out the 7th byte and translates JDK 1.8 is 52.
# note: 16# outputs the hexadecimal number as decimal!

JDK_VERSION=$(( 16#`unzip -p $RODDY_BINARY de/dkfz/roddy/Constants.class | hexdump  -n 8  | cut -d " " -f 5  | head -n 1 | cut -b 1-2` ))
if [[  -z $JDK_VERSION || $JDK_VERSION == 0 ]]
then
  echo "Roddy jar not found, getting from dist/bin/[version]/buildinfo.txt or RoddyCore/buildversion.txt"
  [[ -f $RODDY_BINARY_DIR/buildinfo.txt ]] && JDK_VERSION=`grep JDKVersion $RODDY_BINARY_DIR/buildinfo.txt | cut -d "=" -f 2`
  [[ ! -f $RODDY_BINARY_DIR/buildinfo.txt ]] && echo "Not supported yet, should guess latest JDK version now! Unset and use default in setupRuntimeEnvironment.sh." && unset JDK_VERSION
fi

GROOVY_VERSION=$( basename `ls $RODDY_BINARY_DIR/lib/groovy-*.jar` | cut -d "-" -f 3  | cut -d "." -f 1-2 )
RODDY_API=$( basename $RODDY_BINARY_DIR  | cut -d "." -f 1-2 )
if [[ $RODDY_API != *.* || $RODDY_API == "current" ]]; then
  [[ -f $RODDY_BINARY_DIR/buildinfo.txt ]] && RODDY_API=`grep RoddyAPIVersion $RODDY_BINARY_DIR/buildinfo.txt | cut -d "=" -f 2`
  [[ ! -f $RODDY_BINARY_DIR/buildinfo.txt ]] && RODDY_API=`head RoddyCore/buildversion.txt -n 1`
fi
