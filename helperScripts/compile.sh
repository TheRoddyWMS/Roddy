#!/bin/bash

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
groovy helperScripts/IncreaseAndSetBuildVersion.groovy RoddyCore/rbuildversions.txt RoddyCore/src/de/dkfz/roddy/Constants.java &

echo "Searching source files"
test=`find RoddyCore/src/ -type f \( -name "*.groovy" -or -name "*.java"  \)`

echo "Searching libraries"
if [[ ! -f ~/.roddy/jfxlibInfo ]] || [[ ! -f `cat ~/.roddy/jfxlibInfo` ]]; then
	echo $JAVA_HOME/`find -L ${JAVA_HOME} -name "jfxrt.jar"` > ~/.roddy/jfxlibInfo
fi
jfxLibrary=`cat ~/.roddy/jfxlibInfo`

pluginbaseLib=dist/plugins/PluginBase/PluginBase.jar
libraries=`ls -d1 dist/lib/** | tr "\\n" ":"`; libraries=${libraries:0:`expr ${#libraries} - 1`}
libraries=$libraries:$pluginbaseLib:$jfxLibrary

cd dist
librariesForManifest=`ls -d1 lib/**`
librariesForManifest="$librariesForManifest $pluginbaseLib $jfxLibrary"
cd ..

echo "Creating necessary paths"
[[ -f dist/Roddy.jar ]] && rm dist/Roddy.jar 2> /dev/null
rm -rf build
rm -rf out
mkdir -p build/classes
mkdir -p dist/lib

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
jar cmf ../../manifest.tmp ../../dist/Roddy.jar $classFiles
cd ../../RoddyCore/src
echo $PWD
#set -xuv
# Add JavaFX files and gui images
fxmlFiles=`find . -type f \( -name "*.fxml" \)`
cssFiles=`find . -type f \( -name "*.css" \)`
jar uf ../../dist/Roddy.jar imgs/* $cssFiles $fxmlFiles
cd ../..
echo $PWD

javapackager -createjar -srcdir dist/ -srcfiles Roddy.jar -outdir dist/ -outfile RoddyPacked.jar

mv dist/RoddyPacked.jar dist/Roddy.jar

[ $? -ne 0 ] && "Error during compression" && exit $?


