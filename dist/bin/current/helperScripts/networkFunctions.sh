#!/usr/bin/env bash

function checkAndDownloadGroovyServ() {
  # Check for groovyserv. If it does not exist, try to download it. If it still fails, start Roddy without it.
  local callerBinary=java
  local roddyApplicationDirectory=${1-""}
  local runtimeFolder=${roddyApplicationDirectory}/dist/runtime
  local forbiddenFile=${runtimeFolder}/gservforbidden

  if [[ ! -d "$runtimeFolder" ]]; then
    mkdir -p "$runtimeFolder" 2> /dev/null
  fi

  if [[ ! -d $(find ${runtimeFolder} -maxdepth 1 -mindepth 1 -type d -name "groovys*") && ! -f ${forbiddenFile} ]]; then
    (cd ${runtimeFolder} &&
        wget -S $(curl -s https://kobo.github.io/groovyserv/download.html \
        | grep groovyserv \
        | grep bin.zip \
        | sed 's/href=/\n/g' \
        | sed 's/>/\n/g' \
        | sed 's/</\n/g' \
        | sed 's/"//g'  \
        | grep .zip)) &> /dev/null

    if [[ $? -ne 0 ]]; then
        echo "Error downloading groovyserv. Skipping groovyserv setup. Remove ${forbiddenFile} before trying again." >> /dev/stderr
        touch ${forbiddenFile}
    else
      (cd ${runtimeFolder} && unzip groovyser*.zip && rm groovyser*.zip*) &> /dev/null
      if [[ $? -ne 0 ]]; then
        echo "Error unzipping groovyserv. Skipping groovyserv setup. Remove ${forbiddenFile} before trying again." >> /dev/stderr
        touch ${forbiddenFile}
      fi
      ${runtimeFolder}/groovys*/bin/groovyclient &> /dev/null
      if [[ $? -ne 0 ]]; then
        echo "Error starting groovyserv. Skipping groovyserv setup. Remove ${forbiddenFile} before trying again." >> /dev/stderr
        touch ${forbiddenFile}
      fi
    fi
  fi

  [[ ! -f ${forbiddenFile} ]] && callerBinary=$(readlink -f $(ls ${runtimeFolder}/groovyserv*/bin/groovyclient) 2> /dev/null) || touch ${forbiddenFile}
  echo $callerBinary
}

function getExistingOrNewGroovyServPort() {
  # Check first, if GroovyServ is running for the local user!
  local runningGServInstance=$(ps -O command -U $(whoami) | grep GroovyServ | grep -v grep)

  if [[ -n ${runningGServInstance-} ]]; then
    # Use the existing port, it is passed as an argument to GroovyServ, so we can just get it from that.

    # Chaining things together above would be nice, but it fails. If we assign the output above
    # to a variable first, the whitespaces will be reduced to a single character thuse making it
    # easy to use cut and rev
    echo ${runningGServInstance} | perl -ne '/.+GroovyServer\.main\(args\)\s--\s(\d+)\s.+/; print "$1\n"'

  else
    # Find a port and return that.
    # Hopefully java is already available

    echo $(java FindPort)
  fi
}