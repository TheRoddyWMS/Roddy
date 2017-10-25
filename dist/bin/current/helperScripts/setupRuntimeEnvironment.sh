#!/bin/bash

# Convert read out JDK version entry to a readable version entry.
jdkIsSet=false
[[ ${JDK_VERSION-52} == 52 ]] && JDK_VERSION=1.8
[[ ${JDK_VERSION} == 1.8 ]] && jdkIsSet=true;

[[ $jdkIsSet == false ]] && echo "The requested Java runtime version ${JDK_VERSION} is not supported." && exit 1
echo Required JRE/JDK: $JDK_VERSION >> /dev/stderr
echo Required Groovy:  $GROOVY_VERSION >> /dev/stderr

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
else

  # Check, if Java can be called and extract the version
  # OpenJDK and Sun JDK should both work from version 8 on.
  javaBinary=`which java 2> /dev/null`
  javaSearchError=$?
  groovyBinary=`which groovy 2> /dev/null`
  groovySearchError=$?

  # Check, if java and groovy could be found
  [[ $javaSearchError == 1 ]]   && echo "There was no java runtime environment or jdk setup. Roddy cannot be compiled." && exit 1
  [[ $groovySearchError == 1 ]] && echo "Groovy SDK / Runtime not found, Roddy cannot be compiled or started." && exit 1

  # Check, if JAVA_HOME and GROOVY_HOME are set. If not, set them.
  if [[ -z $JAVA_HOME ]]; then
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $javaBinary)))
    export JDK_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
    export PATH=$JDK_HOME/bin:$JAVA_HOME/bin:$PATH
  fi
  if [[ -z $GROOVY_HOME ]]; then
    export GROOVY_HOME=$(dirname $(dirname $(readlink -f $groovyBinary)))
    export PATH=$GROOVY_HOME/bin$PATH
  fi
fi

>&2 echo "Runtime Environment:"
>&2 echo "  "$(readlink -f $(which java))
>&2 echo "  "$(readlink -f $(which javac))
>&2 echo "  "$(readlink -f $(which groovy))

# The method compares two version numbers in the format:
# Major.Minor.*
# Only the first two numbers are compared.
# Parameters:
#   $1 - String which identifies the software
#   $2 - Actual version
#   $3 - Requested version
function compareVersion {
  local major=$(echo $2 | cut -d "." -f 1)
  local minor=$(echo $2 | cut -d "." -f 2)

  local majorRequired=$(echo $3 | cut -d "." -f 1)
  local minorRequired=$(echo $3 | cut -d "." -f 2)

  [[ $major -lt $majorRequired || $minor -lt $minorRequired ]] && echo "$1 version $2 is too low" && exit 1
}

# Groovy knows about both versions. This way we can just query Groovy.
versions=`groovy -v`

# Get the groovy and java versions ...
javaVersion=$(echo $versions | cut -d ":" -f 2-3 | cut -d " " -f 4)
groovyVersion=$(echo $versions | cut -d ":" -f 2-3 | cut -d " " -f 2)

# ... and compare them.
compareVersion "Java" $javaVersion $JDK_VERSION
compareVersion "Groovy" $groovyVersion $GROOVY_VERSION
