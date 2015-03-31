#!/bin/bash

cd `dirname $0`

RODDY_DIRECTORY=`readlink -f .`
parm1=${1-}

# Call some scripts before other steps start.
if [[ "$parm1" == "prepareprojectconfig" ]]; then
    source helperScripts/prepareProjectConfiguration.sh
    exit 0
elif [[ "$parm1" == "setup" ]]; then
    source helperScripts/setupRoddy.sh
    exit 0
fi

# Example for a date call (for timestamps)
#date +"%M %S %N"
GROOVY_HOME=`ls -d ${PWD}/dist/runtime*/groovy 2> /dev/null`
JAVA_HOME=`ls -d ${PWD}/dist/runtime*/jre 2> /dev/null`
RODDY_BINARY=dist/Roddy.jar

if [[ -z $JAVA_HOME ]]
then
	GROOVY_HOME=`ls -d ~/.roddy/runtime*/groovy 2> /dev/null`
    JAVA_HOME=`ls -d ~/.roddy/runtime*/jre 2> /dev/null`
fi

[[ ! -d $JAVA_HOME ]] && echo "There was no java runtime environment or jdk setup. Roddy cannot be compiled." && exit 1

PATH=$JAVA_HOME/bin:$GROOVY_HOME/bin:$PATH
JFX_LIBINFO_FILE=~/.roddy/jfxlibInfo
if [[ ! -f ${JFX_LIBINFO_FILE} ]] || [[ ! -f `cat ${JFX_LIBINFO_FILE}` ]]; then
	echo `find ${JAVA_HOME}/ -name "jfxrt.jar"` > ${JFX_LIBINFO_FILE}
fi

#TODO Resolve the PluginBase.jar This might be set in the ini file.
pluginbaseLib=${RODDY_DIRECTORY}/dist/plugins/PluginBase/PluginBase.jar
jfxlibInfo=`cat ${JFX_LIBINFO_FILE}`
libraries=`ls -d1 dist/lib/** | tr "\\n" ":"`; libraries=${libraries:0:`expr ${#libraries} - 1`}
libraries=$libraries:$jfxlibInfo:$pluginbaseLib

#Resolve the configuration file
source helperScripts/resolveAppConfig.sh

overrideRoddyVersionParameter=""
overridePluginParameters=""

#Is the roddy binary or anything set via command line?
for i in $*
do
    if [[ $i == --useRoddyVersion* ]]; then
        overrideRoddyVersionParameter=${i:18:40}
        RODDY_BINARY=dist/Roddy_${overrideRoddyVersionParameter}.jar
        if [[ ! -f $RODDY_BINARY ]]; then
            echo "${RODDY_BINARY} not found, the following files are available:"
            for bin in `ls dist/Roddy*.jar`; do
                echo "  ${bin}"
            done
            exit 1
        fi
    elif [[ $i == --usePluginVersion* ]]; then
        overridePluginParameters=${i:19:140}
    fi
done

if [[ "$parm1" == "compile" ]]; then
    bash helperScripts/compile.sh
    exit 0
elif [[ "$parm1" == "pack" ]]; then
    groovy helperScripts/addChangelistVersionTag.groovy README.md RoddyCore/rbuildversions.txt
    major=`head RoddyCore/rbuildversions.txt -n 1`
    minor=`tail RoddyCore/rbuildversions.txt -n 1`
    filename=dist/Roddy_${major}.${minor}.jar
    cp dist/Roddy.jar $filename
    svn info > ${filename}.nfo
    svn status >> ${filename}.nfo
    find ${RODDY_DIRECTORY}/dist >> ${filename}.nfo
    ls -l ${RODDY_DIRECTORY}/dist >> ${filename}.nfo
    svn add ${filename} ${filename}.nfo
    exit 0
elif [[ "$parm1" == "compileplugin" ]]; then
    echo "Using Roddy binary "`basename ${RODDY_BINARY}`
    source helperScripts/compileToJarFile.sh
    exit 0
elif [[ "$parm1" == "packplugin" || "$parm1" == "testpackplugin" ]]; then
    [[ "$parm1" == "testpackplugin" ]] && set -xuv
    set -xuv
    source helperScripts/compileToJarFile.sh $2 increasebuildonly

    # Test pack does not put things to svn so it is safe to use. Test will not change the zip file but will increase the buildnumber.
    source ${RODDY_DIRECTORY}/helperScripts/resolveAppConfig.sh
    pluginID=$2
    pluginDirectories=`grep pluginDirectories ${customconfigfile}`
    pluginDirectory=`groovy ${RODDY_DIRECTORY}/helperScripts/findPluginFolders.groovy ${pluginDirectories} ${PWD} ${pluginID}`
    for i in `ls ${pluginDirectory}/README*.txt`; do
        groovy helperScripts/addChangelistVersionTag.groovy $i ${pluginDirectory}/buildversion.txt
    done

    major=`head ${pluginDirectory}/buildversion.txt -n 1`
    minor=`tail ${pluginDirectory}/buildversion.txt -n 1`
    filename=${pluginID}_${major}.${minor}
    cd ${pluginDirectory}/..
    echo "Copying current to ${filename} ..."
    [[ ! -d ${filename} ]] && mkdir ${filename}
    cp -r $pluginID/* ${filename}
    cd $filename

    echo "Removing obsolete files"
    # Find .svn folders?
    find -type d -name "*.svn" | xargs rm -rf
    rm -rf build
    rm -rf out
    rm README*.txt~

    echo "Creating list of included files ..."
    (find -maxdepth 1 -type f | sort | xargs md5sum) > FileList.nfo
    (for i in `find -mindepth 1 -maxdepth 1 -type d | sort | grep -v "build"`; do find $i -type f | sort | xargs md5sum ; done) >> FileList.nfo

    # Step back to main dir
    cd ..

    echo "Compressing to ${filename}.zip ..."
    [[ -f {filename}.zip ]] && rm ${filename}.zip
    infoFile=${filename}/${pluginID}.nfo
    svn info > ${infoFile}
    svn status >> ${infoFile}
    find ${RODDY_DIRECTORY}/dist >> ${infoFile}
    ls -l ${RODDY_DIRECTORY}/dist >> ${infoFile}

    if [[ "$parm1" == "packplugin" ]]; then
        zip -x "*/build/*" -r9 ${filename}.zip ${filename} > /dev/null
        echo "Adding to SVN"
        svn add ${filename}.zip ${filename}.nfo 2> /dev/null
    fi

    cd ..; echo "Done"; exit 0

    # Only unzip if necessary!
elif [[ "$parm1" == "createworkflow" ]]; then
    source ${RODDY_DIRECTORY}/helperScripts/resolveAppConfig.sh
    pluginID=$2
    workflowID=$3
    source ${RODDY_DIRECTORY}/helperScripts/createNewWorkflow.sh ${customconfigfile} ${pluginID} ${3-}
    exit 0
fi

java -cp .:$libraries:./${RODDY_BINARY} de.dkfz.roddy.Roddy $*
