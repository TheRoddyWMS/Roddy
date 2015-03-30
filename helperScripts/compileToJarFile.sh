#!/bin/bash
#TODO Build in additional configured plugin folders!

increasebuildonly=${increasebuildonly-false}

#[[ "$1" == "" ]] && exit 5
#[[ "$2" == "increasebuildonly" ]] && increasebuildonly=true

pluginDirectories=`grep pluginDirectories ${customconfigfile}`

#TODO Find other plugin directories as well
set -xuv
pluginID=$2
srcDirectory=`groovy ${RODDY_DIRECTORY}/helperScripts/findPluginFolders.groovy ${pluginDirectories} ${RODDY_DIRECTORY} ${pluginID}`
cd $srcDirectory

echo "Increasing build number and date"
pluginClass=`find $srcDirectory -name "*Plugin.java" | head -n 1`
groovy helperScripts/IncreaseAndSetBuildVersion.groovy $srcDirectory/buildversion.txt $pluginClass
echo "  Increased to" `head -n 1 $srcDirectory/buildversion.txt`.`tail -n 1 $srcDirectory/buildversion.txt`

[[ $increasebuildonly == true ]] && echo "Compilation will be skipped!" && exit 0

echo "Switching to plugin directory"
set -xuv
libName=$2

echo "Creating necessary paths"
rm -rf build
mkdir -p build/classes

echo "Searching source files"

test=`find src/ -type f \( -name "*.groovy" -or -name "*.java"  \)`

echo "Searching libraries"

roddyLibrary=${RODDY_DIRECTORY}/${RODDY_BINARY}           #Always use the most current jar file.
jfxLibrary=`find $JDK_HOME/ -name "jfxrt.jar"`
libraries=`ls -d1 ${RODDY_DIRECTORY}/dist/lib/** | tr "\\n" ":"`; libraries=${libraries:0:`expr ${#libraries} - 1`}
libraries=$roddyLibrary:$pluginbaseLib:$libraries:$jfxLibrary

# Check if there is a buildinfo.txt and resolve additional library dependencies.
if [[ -f buildinfo.txt ]]
then
    [[ $? == 0 ]] && pluginLibs=`groovy ${RODDY_DIRECTORY}/helperScripts/findPluginLibraries.groovy ${pluginDirectories} ${PWD}`
    [[ $? == 0 && $pluginLibs != "null" ]] && libraries=$libraries:$pluginLibs
fi

echo "Working in:" $PWD
echo "Using jfx lib:" $jfxLibrary
librariesForManifest=`ls -d1 ${RODDY_DIRECTORY}/dist/lib/*groo*`
librariesForManifest="$librariesForManifest $pluginbaseLib $jfxLibrary ../Roddy.jar"

echo "Compiling library / plugin in $1"
javac -version
groovyc -version
uncheckedStr=""
groovyc $uncheckedStr -d build/classes -cp $libraries -j $test
[ "$?" -ne 0 ] && "Error during compilation" && exit $?

echo "Creating manifest file"
echo Manifest-Version: 1.0 > manifest.tmp
echo Created-By: 1.8 >> manifest.tmp
echo "" >> manifest.tmp

echo "Searching generated files"
cd build/classes
classFiles=`find . -type f \( -name "*.class" \)`

outputDirectory="../.."

echo "Compressing files to "`readlink -f $outputDirectory/$libName.jar`
jar cf $outputDirectory/$libName.jar $classFiles
svn add $outputDirectory/$libName.jar 2> /dev/null
cd ../..
