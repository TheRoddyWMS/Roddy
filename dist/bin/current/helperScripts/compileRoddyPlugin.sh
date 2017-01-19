#!/bin/bash

set -e
increasebuildonly=${increasebuildonly-false}

# Check for configured plugin directories. Set empty if no dirs are configured.
pluginDirectories=`grep pluginDirectories ${customconfigfile}`
[[ -z "$pluginDirectories" ]] && pluginDirectories="pluginDirectories="

#TODO Find other plugin directories as well
pluginID=$2
srcDirectory=`groovy ${SCRIPTS_DIR}/findPluginFolders.groovy ${pluginDirectories} ${RODDY_DIRECTORY} ${pluginID}`
cd $srcDirectory
requestedAPIVersion=`grep RoddyAPIVersion buildinfo.txt | cut -d "=" -f 2`
[[ ! $RODDY_API == $requestedAPIVersion ]] && echo "Mismatch between used Roddy version ${RODDY_API} and requested version ${requestedAPIVersion}. Will not compile plugin." && exit 1
#cat buildinfo.txt

echo "Increasing build number and date"
pluginClass=`find $srcDirectory/ -name "*Plugin.java" | head -n 1`
groovy ${SCRIPTS_DIR}/IncreaseAndSetBuildVersion.groovy $srcDirectory/buildversion.txt $pluginClass
echo "  Increased to" `head -n 1 $srcDirectory/buildversion.txt`.`tail -n 1 $srcDirectory/buildversion.txt`

if [[ $increasebuildonly == false ]]
then
    echo "Switching to plugin directory"
    libName=$2

    echo "Creating necessary paths"
    rm -rf build
    mkdir -p build/classes

    echo "Searching source files"

    test=`find src/ -type f \( -name "*.groovy" -or -name "*.java"  \)`

    echo "Searching libraries"

    roddyLibrary=${RODDY_BINARY}
    jfxLibrary=`find $JDK_HOME/ -name "jfxrt.jar"`
    libraries=`ls -d1 ${RODDY_BINARY_DIR}/lib/** | tr "\\n" ":"`; libraries=${libraries:0:`expr ${#libraries} - 1`}
    libraries=$roddyLibrary:$pluginbaseLib:$libraries:$jfxLibrary

    # Check if there is a buildinfo.txt and resolve additional library dependencies.
    if [[ -f buildinfo.txt ]]
    then
        [[ $? == 0 ]] && pluginLibs=`groovy ${SCRIPTS_DIR}/findPluginLibraries.groovy ${pluginDirectories} ${RODDY_DIRECTORY} ${PWD}`
        [[ $? == 0 && $pluginLibs != "null" ]] && libraries=$libraries:$pluginLibs
    fi

    echo "Working in:" $PWD
    echo "Using jfx lib:" $jfxLibrary
    librariesForManifest=`ls -d1 ${RODDY_BINARY_DIR}/lib/*groo*`
    librariesForManifest="$librariesForManifest $pluginbaseLib $jfxLibrary ${RODDY_BINARY}"

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
    cd $outputDirectory
    [[ -d ".svn" ]] && svn add $libName.jar buildversion.txt #2> /dev/null
    [[ -d ".git" ]] && git add $libName.jar buildversion.txt `git st | grep Plugin.java | cut -d ":" -f 2` #2> /dev/null
    rm manifest.tmp
fi
