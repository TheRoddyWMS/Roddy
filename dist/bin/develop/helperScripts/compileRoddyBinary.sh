#!/bin/bash

set -e

echo "Storing buildinfo for the new Roddy jar file"
echo JDKVersion=$JDK_VERSION > dist/bin/develop/buildinfo.txt
echo GroovyVersion=$GROOVY_VERSION >> dist/bin/develop/buildinfo.txt
echo RoddyAPIVersion=`head RoddyCore/buildversion.txt -n 1` >> dist/bin/develop/buildinfo.txt

declare -a gradleParameters=$*
./gradlew build "${fullParameterList[@]:1:99}"
