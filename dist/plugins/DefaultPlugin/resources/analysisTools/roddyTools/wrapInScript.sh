#!/bin/bash

[[ ${debugWrapInScript-false} == true ]] && set -xv
[[ ${debugWrapInScript-false} == false ]] && set +xv

waitForFile() {
    local file="${1:?No file to wait for}"
    local waitCount=0
    while [[ ! -r ${file} && ${waitCount-0} -lt 3 ]]; do sleep 5; waitCount=$((waitCount + 1)); done
    [[ ! -f ${file} || ! -r ${file} ]] && echo "The file '${file}' does not exist or is not readable." && exit 200
}

# This script wraps in another script.
# The configuration file is sourced and has to be sourced again in the wrapped script.
# A job error entry is created in the results list along with a timestamp
#   i.e. 1237474.tbi-pbs1,START,928130918393
# This status is ignored if the script is currently planned or running
# When the job finished an entry with the job scripts exit code is created with a timestamp
#
# Cluster options (like i.e. PBS ) have to be parsed and set before job submission!
# They will be ignored after the script is wrapped.

dumpEnvironment() {
    local message="${1:?No log message given}"
    echo "$message"
    while IFS='=' read -r -d '' n v; do     [[ -r $v ]] && echo "$v -> "$(readlink -f "$v"); done < <(env -0)
    echo ""
}

createToolVariableName() {
    local varName="${1:?No variable name given}"
    echo "$varName" | perl -ne 's/([A-Z])/_$1/g; print uc($_)'
}

# Basic modules / environment support
# Load the environment script (source), if it is defined. If the file is defined but the file not accessible exit with
# code 200. Additionally, expose the used environment script path as ENVIRONMENT_SCRIPT variable to the wrapped script.
runEnvironmentSetupScript() {
    local envScriptVar="${TOOL_ID}EnvironmentScript"
    if [[ -n "${!envScriptVar}" ]]; then
        declare -gx ENVIRONMENT_SCRIPT="${!envScriptVar}"
    elif [[ -n "$workflowEnvironmentScript" ]]; then
        if [[ -n "$ENVIRONMENT_SCRIPT" ]]; then
            echo "ENVIRONMENT_SCRIPT variable is set externally (e.g. in the XML) to '$TOOL_ENVIRONMENT'. It will be reset." > /dev/stderr
        fi
        declare -gx ENVIRONMENT_SCRIPT="$workflowEnvironmentScript"
    fi

    if [[ -n "$ENVIRONMENT_SCRIPT" ]]; then
        if [[ ! -f "$ENVIRONMENT_SCRIPT" ]]; then
            echo "ERROR: You defined an environment loader script for the workflow but the script is not available: '$ENVIRONMENT_SCRIPT'" > /dev/stderr
            exit 200
        fi
        echo "Sourcing environment setup script from '$ENVIRONMENT_SCRIPT'" > /dev/stderr
        source "$ENVIRONMENT_SCRIPT"
    fi
}

###### Main ############################################################################################################

[[ ${CONFIG_FILE-false} == false ]] && echo "The parameter CONFIG_FILE is not set but is mandatory!" && exit 200

# Perform some initial checks
# Store the environment, store file locations in the env
extendedLogsDir=$(dirname "$CONFIG_FILE")/extendedLogs

extendedLogFile=${extendedLogsDir}/$(basename "$PARAMETER_FILE" .parameters)
mkdir -p ${extendedLogsDir}

dumpEnvironment "Files in environment before source configs" >> ${extendedLogFile}

waitForFile "$PARAMETER_FILE"
source ${PARAMETER_FILE}

waitForFile "$CONFIG_FILE"
source ${CONFIG_FILE}
dumpEnvironment "Files in environment after source configs" >> ${extendedLogFile}

runEnvironmentSetupScript
dumpEnvironment "Files in environment after sourcing the environment script" >> ${extendedLogFile}

isOutputFileGroup=${outputFileGroup-false}

if [[ $isOutputFileGroup != false && ${newGrpIsCalled-false} == false ]]; then
  export newGrpIsCalled=true
  export LD_LIB_PATH=$LD_LIBRARY_PATH
  # OK so something to note for you. newgrp has an undocumented feature (at least in the manpages)
  # and resets the LD_LIBRARY_PATH to "" if you do -c. -l would work, but is not feasible, as you
  # cannot call a script with it. Also I do not know whether it is possible to use it in a non-
  # interactive session (like qsub). So we just export the variable and import it later on, if it
  # was set earlier.
  # Funny things can happen... instead of newgrp we now use sg.
  # newgrp is part of several packages and behaves differently
  sg $outputFileGroup -c "/bin/bash $0"
  exit $?

else

  # Set LD_LIBRARY_PATH to LD_LIB_PATH, if the script was called recursively.
  [[ ${LD_LIB_PATH-false} != false ]] && export LD_LIBRARY_PATH=$LD_LIB_PATH

  export RODDY_JOBID=${RODDY_JOBID-$$}
  declare -ax RODDY_PARENT_JOBS=${RODDY_PARENT_JOBS-()}
  echo "RODDY_JOBID is set to ${RODDY_JOBID}"

  # Replace #{RODDY_JOBID} in passed variables.
  while read line; do
    echo $line
    _temp=$RODDY_JOBID
    export RODDY_JOBID=`echo $RODDY_JOBID | cut -d "." -f 1`
    line=${line//-x/};
    eval ${line//#/\$};
    export RODDY_JOBID=$_temp
  done <<< `export | grep "#{"`

  # Default to the data folder on the node
  defaultScratchDir=${defaultScratchDir-/data/roddyScratch}
  [[ ${RODDY_SCRATCH-x} == "x" ]] && export RODDY_SCRATCH=${defaultScratchDir}/${RODDY_JOBID}
  [[ ! -d ${RODDY_SCRATCH} ]] && mkdir -p ${RODDY_SCRATCH}
  echo "RODDY_SCRATCH is set to ${RODDY_SCRATCH}"

  # Check
  _lock="$jobStateLogFile~"

  # Select the proper lock command. lockfile-create is not tested though.
  lockCommand="lockfile -s 1 -r 50"
  unlockCommand="rm -f"

  useLockfile=true
  [[ -z `which lockfile` ]] && useLockfile=false
  [[ ${useLockfile} == false ]] && lockCommand=lockfile-create && unlockCommand=lockfile-remove && echo "Set lockfile commands to lockfile-create and lockfile-remove"

  startCode=STARTED

  # Check if the jobs parent jobs are stored and passed as a parameter. If so Roddy checks the job jobState logfile
  # if at least one of the parent jobs exited with a value different to 0.
  if [[ ${#RODDY_PARENT_JOBS} -gt 0 ]]
  then
    # Now check all lines in the file
    for parentJob in ${RODDY_PARENT_JOBS[@]}; do
      [[ ${exitCode-} == 250 ]] && continue;
      result=`cat ${jobStateLogFile} | grep -a "^${parentJob}:" | tail -n 1 | cut -d ":" -f 2`
      [[ ! $result -eq 0 ]] && echo "At least one of this parents jobs exited with an error code. This job will not run." && startCode="ABORTED"
    done
  fi

  # Check the wrapped script for existence
  [[ ${WRAPPED_SCRIPT-false} == false || ! -f ${WRAPPED_SCRIPT} ]] && startCode=ABORTED && echo "The wrapped script is not defined or not existing."

  ${lockCommand} $_lock;
  echo "${RODDY_JOBID}:${startCode}:"`date +"%s"`":${TOOL_ID}" >> ${jobStateLogFile};
  ${unlockCommand} $_lock
  [[ ${startCode} == "ABORTED" ]] && echo "Exiting because a former job died." && exit 250
  # Sleep a second before and after executing the wrapped script. Allow the system to get different timestamps.
  sleep 2

  export WRAPPED_SCRIPT=${WRAPPED_SCRIPT} # Export script so it can identify itself

  # Create directories
  mkdir -p ${DIR_TEMP} 2 > /dev/null

  echo "Calling script ${WRAPPED_SCRIPT}"
  jobProfilerBinary=${JOB_PROFILER_BINARY-}
  [[ ${enableJobProfiling-false} == false ]] && jobProfilerBinary=""

  myGroup=`groups  | cut -d " " -f 1`
  outputFileGroup=${outputFileGroup-$myGroup}

  $jobProfilerBinary bash ${WRAPPED_SCRIPT}
  exitCode=$?
  echo "Exited script ${WRAPPED_SCRIPT} with value ${exitCode}"

  # If the tool supports auto checkpoints and the exit code is 0, then go on and create it.
  [[ ${AUTOCHECKPOINT-""} && exitCode == 0 ]] && touch ${AUTOCHECKPOINT}

  sleep 2

  ${lockCommand} $_lock;
  echo "${RODDY_JOBID}:${exitCode}:"`date +"%s"`":${TOOL_ID}" >> ${jobStateLogFile};
  ${unlockCommand} $_lock

  # Set this in your command factory class, when roddy should clean up the dir for you.
  [[ ${RODDY_AUTOCLEANUP_SCRATCH-false} == "true" ]] && rm -rf ${RODDY_SCRATCH} && echo "Auto cleaned up RODDY_SCRATCH"

  [[ ${exitCode} -eq 0 ]] && exit 0

  [[ ${exitCode} -eq 100 ]] && echo "Finished script with 99 for compatibility reasons with Sun Grid Engine. 100 is reserved for SGE usage." && exit 99
  exit $exitCode

fi
