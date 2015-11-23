#!/bin/bash

#set -xuv

GROOVY_HOME=`ls -d ${PWD}/dist/runtime*/groovy 2> /dev/null`
JAVA_HOME=`ls -d ${PWD}/dist/runtime*/jdk 2> /dev/null`

if [[ -z $JAVA_HOME ]] 
then
	GROOVY_HOME=`ls -d ~/.roddy/runtime*/groovy 2> /dev/null`
	JAVA_HOME=`ls -d ~/.roddy/runtime*/jdk 2> /dev/null`
fi

[[ ! -d $JAVA_HOME ]] && echo "There was no java runtime environment or jdk setup. Roddy cannot be compiled." && exit 1

JDK_HOME=$JAVA_HOME
PATH=$JDK_HOME/bin:$GROOVY_HOME/bin:$PATH

echo "Increasing build number and date"
groovy ${SCRIPTS_DIR}/IncreaseAndSetBuildVersion.groovy RoddyCore/buildversions.txt RoddyCore/src/de/dkfz/roddy/Constants.java &

echo "Searching source files"
test=`find RoddyCore/src/ -type f \( -name "*.groovy" -or -name "*.java"  \)`

echo "Searching libraries"
if [[ ! -f ~/.roddy/jfxlibInfo ]] || [[ ! -f `cat ~/.roddy/jfxlibInfo` ]]; then
	echo $JAVA_HOME/`find -L ${JAVA_HOME} -name "jfxrt.jar"` > ~/.roddy/jfxlibInfo
fi
jfxLibrary=`cat ~/.roddy/jfxlibInfo`

# TODO Find proper PluginBase jar file Not the most current one!
pluginbaseLib=${RODDY_DIRECTORY}/dist/plugins/PluginBase/PluginBase.jar
libraries=`ls -d1 ${RODDY_DIRECTORY}/dist/bin/current/lib/** | tr "\\n" ":"`; libraries=${libraries:0:`expr ${#libraries} - 1`}
libraries=$libraries:$pluginbaseLib:$jfxLibrary

cd dist
librariesForManifest=`ls -d1 ${RODDY_DIRECTORY}/dist/bin/current/lib/**`
librariesForManifest="$librariesForManifest $pluginbaseLib $jfxLibrary"
cd ..

NEW_RODDY_BINARY=dist/bin/current/Roddy.jar

echo "Creating necessary paths"
[[ -f ${NEW_RODDY_BINARY} ]] && rm ${NEW_RODDY_BINARY} 2> /dev/null
rm -rf build
rm -rf out
mkdir -p build/classes
#mkdir -p dist/lib

echo "Compiling solution"
javac -version
groovyc -version
uncheckedStr=""
[[ "$1" == "unchecked" ]] && uncheckedStr="-F Xlint:unchecked" && echo " .. compiling with unchecked option"
groovyc $uncheckedStr -d build/classes -cp $libraries -j $test
[ "$?" -ne 0 ] && "Error during compilation" && exit $?

echo "Creating manifest file"
echo Manifest-Version: 1.0 > manifest.tmp
echo Created-By: 1.8 >> manifest.tmp
echo Main-Class: de.dkfz.roddy.Roddy >> manifest.tmp

echo "Searching generated files"
cd build/classes
echo $PWD
classFiles=`find . -type f \( -name "*.class" \)`

echo "Compressing files to dist/Roddy.jar"
jar cmf ../../manifest.tmp ../../${NEW_RODDY_BINARY} $classFiles
cd ../../RoddyCore/src
echo $PWD
#set -xuv
# Add JavaFX files and gui images
fxmlFiles=`find . -type f \( -name "*.fxml" \)`
cssFiles=`find . -type f \( -name "*.css" \)`
jar uf ../../${NEW_RODDY_BINARY} imgs/* $cssFiles $fxmlFiles
cd ../..
echo $PWD

javapackager -createjar -srcdir dist/bin/current -srcfiles Roddy.jar -outdir dist/bin/current -outfile RoddyPacked.jar

mv dist/bin/current/RoddyPacked.jar ${NEW_RODDY_BINARY}

[ $? -ne 0 ] && "Error during compression" && exit $?


