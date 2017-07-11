#!/bin/bash

# Convert read out JDK version entry to a readable version entry.
jdkIsSet=false
[[ ${JDK_VERSION-52} == 52 ]] && JDK_VERSION=1.8
[[ ${JDK_VERSION} == 1.8 ]] && jdkIsSet=true;

[[ $jdkIsSet == false ]] && echo "The requested Java runtime version ${JDK_VERSION} is not supported." && exit 1
echo Required JRE/JDK: $JDK_VERSION > /dev/stderr
echo Required Groovy:  $GROOVY_VERSION > /dev/stderr

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
  export GROOVY_HOME=`ls -d $baseFolder/runtime*/groovy-$GROOVY_VERSION* 2> /dev/null | sort -V | tail -n 1`
  export JDK_HOME=`ls -d $baseFolder/runtime*/jdk${JDK_VERSION}* 2> /dev/null | sort -V | tail -n 1`
  [[ -n $JDK_HOME ]] && export JAVA_HOME=`ls -d $JDK_HOME/jre`
  [[ -z $JAVA_HOME ]] && export JAVA_HOME=`ls -d $baseFolder/runtime*/jre${JDK_VERSION}* 2> /dev/null | tail -n 1`
}

checkAndSetJDKInFolder ${PWD}/dist

[[ -z $JAVA_HOME ]] && checkAndSetJDKInFolder ~/.roddy

if [[ ! -z $JAVA_HOME && ! -z $GROOVY_HOME ]]; then
  PATH=$JDK_HOME/bin:$JAVA_HOME/bin:$GROOVY_HOME/bin:$PATH
#  JFX_LIBINFO_FILE=~/.roddy/jfxlibInfo
#  if [[ ! -f ${JFX_LIBINFO_FILE} ]] || [[ ! -f `cat ${JFX_LIBINFO_FILE}` ]]; then
#    echo `find ${JAVA_HOME}/ -name "jfxrt.jar"` > ${JFX_LIBINFO_FILE}
#  fi
  [[ -z $JAVA_HOME ]] && echo "There was no java runtime environment or jdk setup. Roddy cannot be compiled." && exit 1
  [[ -z $GROOVY_HOME ]] && echo "Groovy SDK / Runtime not found, Roddy cannot be compiled or started." && exit 1
else

  # Check, if Java can be called and extract the version
  # OpenJDK and Sun JDK should both work from version 8 on.
  javaBinary=`which java 2> /dev/null`
  javaSearchError=$?
  groovyBinary=`which groovy 2> /dev/null`
  groovySearchError=$?

  [[ $javaSearchError == 1 ]]   && echo "There was no java runtime environment or jdk setup. Roddy cannot be compiled." && exit 1
  [[ $groovySearchError == 1 ]] && echo "Groovy SDK / Runtime not found, Roddy cannot be compiled or started." && exit 1

fi

>&2 echo `which java`
>&2 echo `which groovy`

function compareVersion {
  local major=$(echo $2 | cut -d "." -f 1)
  local minor=$(echo $2 | cut -d "." -f 2)

  local majorRequired=$(echo $3 | cut -d "." -f 1)
  local minorRequired=$(echo $3 | cut -d "." -f 2)

  [[ $major -lt $majorRequired || $minor -lt $minorRequired ]] && echo "$1 version $2 is too low" && exit 1
}
set -x
versions=`groovy -v`

groovyVersion=$(echo $versions | cut -d ":" -f 2-3 | cut -d " " -f 2)
javaVersion=$(echo $versions | cut -d ":" -f 2-3 | cut -d " " -f 4)
compareVersion "Java" $javaVersion $JDK_VERSION
compareVersion "Groovy" $groovyVersion $GROOVY_VERSION
