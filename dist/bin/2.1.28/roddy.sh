#!/bin/bash

cd `dirname $0`

RODDY_DIRECTORY=`readlink -f .`
parm1=${1-}

# Call some scripts before other steps start.
if [[ "$parm1" == "prepareprojectconfig" ]]; then
    source ${SCRIPTS_DIR}/prepareProjectConfiguration.sh
    exit 0
elif [[ "$parm1" == "setup" ]]; then
    source ${SCRIPTS_DIR}/setupRoddy.sh
    exit 0
fi

# Example for a date call (for timestamps)
#date +"%M %S %N"
GROOVY_HOME=`ls -d ${PWD}/dist/runtime*/groovy 2> /dev/null`
JAVA_HOME=`ls -d ${PWD}/dist/runtime*/jre 2> /dev/null`

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
pluginbaseLib=${RODDY_DIRECTORY}/dist/plugins/PluginBase_1.0.24/PluginBase.jar
jfxlibInfo=`cat ${JFX_LIBINFO_FILE}`
libraries=`ls -d1 ${RODDY_BINARY_DIR}/lib/** | tr "\\n" ":"`; libraries=${libraries:0:`expr ${#libraries} - 1`}
libraries=$libraries:$jfxlibInfo

#Is the roddy binary or anything set via command line?
for i in $*
do
    if [[ $i == --usePluginVersion* ]]; then
        overridePluginParameters=${i:19:140}
    fi
done

if [[ "$parm1" == "compile" ]]; then
    bash ${SCRIPTS_DIR}/compile.sh
    exit 0
elif [[ "$parm1" == "pack" ]]; then
    groovy ${SCRIPTS_DIR}/addChangelistVersionTag.groovy README.md RoddyCore/rbuildversions.txt
    major=`head RoddyCore/rbuildversions.txt -n 1`
    minor=`tail RoddyCore/rbuildversions.txt -n 1`
    filename=${RODDY_BINARY_DIR}/Roddy.jar
    cp dist/bin/current/Roddy.jar $filename
    svn info > ${filename}.nfo
    svn status >> ${filename}.nfo
    find ${RODDY_DIRECTORY}/dist/bin/current >> ${filename}.nfo
    ls -l ${RODDY_DIRECTORY}/dist/bin/current >> ${filename}.nfo
    svn add ${filename} ${filename}.nfo
    exit 0
elif [[ "$parm1" == "compileplugin" ]]; then
    echo "Using Roddy binary "`basename ${RODDY_BINARY}`
    set -e
    source ${SCRIPTS_DIR}/compileToJarFile.sh
    exit 0
elif [[ "$parm1" == "packplugin" || "$parm1" == "testpackplugin" ]]; then
    [[ "$parm1" == "testpackplugin" ]] && set -xuv
    set -xuv
    source ${SCRIPTS_DIR}/compileToJarFile.sh $2 increasebuildonly

    # Test pack does not put things to svn so it is safe to use. Test will not change the zip file but will increase the buildnumber.
    source ${SCRIPTS_DIR}/resolveAppConfig.sh
    pluginID=$2
    pluginDirectories=`grep pluginDirectories ${customconfigfile}`
    pluginDirectory=`groovy ${SCRIPTS_DIR}/findPluginFolders.groovy ${pluginDirectories} ${PWD} ${pluginID}`
    for i in `ls ${pluginDirectory}/README*.txt`; do
        groovy ${SCRIPTS_DIR}/addChangelistVersionTag.groovy $i ${pluginDirectory}/buildversion.txt
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
    source ${SCRIPTS_DIR}/resolveAppConfig.sh
    pluginID=$2
    workflowID=$3
    source ${SCRIPTS_DIR}/createNewWorkflow.sh ${customconfigfile} ${pluginID} ${3-}
    exit 0
fi

set -xuv

includedPluginLib=":$pluginbaseLib"

java -cp .:$libraries$includedPluginLib:${RODDY_BINARY} de.dkfz.roddy.Roddy $*
