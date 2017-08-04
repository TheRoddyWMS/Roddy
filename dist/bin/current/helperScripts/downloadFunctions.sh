#!/usr/bin/env bash

function checkAndDownloadGroovyServ() {
  # Check for groovyserv. If it does not exist, try to download it. If it still fails, start Roddy without it.
  local callerBinary=java
  local roddyApplicationDirectory=${1-""}
  local runtimeFolder=${roddyApplicationDirectory}/dist/runtime
  local forbiddenFile=${runtimeFolder}/gservforbidden

  if [[ ! -d $(find ${runtimeFolder} -maxdepth 1 -mindepth 1 -type d -name "groovys*") && ! -f ${forbiddenFile} ]]; then

    mkdir -p dist/runtime/ 2>/dev/null
    (cd ${runtimeFolder} && wget -S $(curl -s https://kobo.github.io/groovyserv/download.html | grep groovyserv | grep bin.zip | sed 's/href=/\n/g' | sed 's/>/\n/g' | sed 's/</\n/g' | sed 's/"//g'  | grep .zip)) &> /dev/null

    if [[ $? -ne 0 ]]; then
      touch ${forbiddenFile}
    else
      (cd ${runtimeFolder} && unzip groovyser*.zip && rm groovyser*.zip*)
      [[ $? -ne 0 ]] && touch ${forbiddenFile}
      (${runtimeFolder}/groovys*/bin/groovyclient 2&> /dev/null)
      [[ $? -ne 0 ]] && touch ${forbiddenFile}
    fi
  fi

  [[ ! -f ${forbiddenFile} ]] && callerBinary=$(readlink -f $( ls ${runtimeFolder}/groovyserv*/bin/groovyclient 2> /dev/null ) 2> /dev/null) || touch ${forbiddenFile}
  echo $callerBinary
}
