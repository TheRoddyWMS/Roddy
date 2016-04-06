#!/bin/bash

# Convert read out JDK version entry to a readable version entry.
jdkIsSet=false
[[ ${JDK_VERSION-52} == 52 ]] && JDK_VERSION=1.8
[[ ${JDK_VERSION} == 1.8 ]] && jdkIsSet=true;

[[ $jdkIsSet == false ]] && echo "The requested Java runtime version ${JDK_VERSION} is not supported." && exit 1
echo Required JRE/JDK: $JDK_VERSION
echo Required Groovy:  $GROOVY_VERSION

# First, check dist/runtime folder
# Second, check ~/.roddy/runtime
# Third, check system JDK/JRE
tmp_JAVA_HOME=$JAVA_HOME && unset JAVA_HOME
tmp_JDK_HOME=$JDK_HOME && unset JDK_HOME
tmp_GROOVY_HOME=$GROOVY_HOME && unset GROOVY_HOME


# Example for a date call (for timestamps)
#date +"%M %S %N"
function checkAndSetJDKInFolder() {
  baseFolder=$1
  export GROOVY_HOME=`ls -d $baseFolder/runtime*/groovy-$GROOVY_VERSION* 2> /dev/null | tail -n 1`
  export JDK_HOME=`ls -d $baseFolder/runtime*/jdk${JDK_VERSION}* 2> /dev/null | tail -n 1`
  [[ -n $JDK_HOME ]] && export JAVA_HOME=`ls -d $JDK_HOME/jre`
  [[ -z $JAVA_HOME ]] && export JAVA_HOME=`ls -d $baseFolder/runtime*/jre${JDK_VERSION}* 2> /dev/null | tail -n 1`
}

checkAndSetJDKInFolder ${PWD}/dist

[[ -z $JAVA_HOME ]] && checkAndSetJDKInFolder ~/.roddy

[[ -z $JAVA_HOME ]] && echo "There was no java runtime environment or jdk setup. Roddy cannot be compiled." && exit 1
[[ -z $GROOVY_HOME ]] && echo "Groovy SDK / Runtime not found, Roddy cannot be compiled or started." && exit 1

PATH=$JDK_HOME/bin:$JAVA_HOME/bin:$GROOVY_HOME/bin:$PATH
JFX_LIBINFO_FILE=~/.roddy/jfxlibInfo
if [[ ! -f ${JFX_LIBINFO_FILE} ]] || [[ ! -f `cat ${JFX_LIBINFO_FILE}` ]]; then
	echo `find ${JAVA_HOME}/ -name "jfxrt.jar"` > ${JFX_LIBINFO_FILE}
fi
