#!/bin/bash

cd `dirname $0`
parm1=${1-}

JAVA_OPTS=${JAVA_OPTS:-"-Xms64m -Xmx500m"}

# Call some scripts before other steps start.
if [[ "$parm1" == "prepareprojectconfig" ]]; then
    source ${SCRIPTS_DIR}/prepareProjectConfiguration.sh
    exit 0
fi

PATH=$JDK_HOME/bin:$JAVA_HOME/bin:$GROOVY_HOME/bin:$PATH
#JFX_LIBINFO_FILE=~/.roddy/jfxlibInfo
#if [[ ! -f ${JFX_LIBINFO_FILE} ]] || [[ ! -f `cat ${JFX_LIBINFO_FILE}` ]]; then
#	echo `find ${JAVA_HOME}/ -name "jfxrt.jar"` > ${JFX_LIBINFO_FILE}
#fi

#TODO Resolve the PluginBase.jar This might be set in the ini file.
pluginbaseLib=${RODDY_DIRECTORY}/dist/plugins/PluginBase/PluginBase.jar
#jfxlibInfo=`cat ${JFX_LIBINFO_FILE}`
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
    [[ ! -d $JDK_HOME ]] && echo "There was no JDK home found. Roddy cannot compile workflows." && exit 1
    source ${SCRIPTS_DIR}/compileRoddyBinary.sh
    exit 0
elif [[ "$parm1" == "pack" ]]; then
    $GROOVY_BINARY ${SCRIPTS_DIR}/addChangelistVersionTag.groovy CHANGELIST.md RoddyCore/buildversion.txt
    major=`head RoddyCore/buildversion.txt -n 1`
    minor=`tail RoddyCore/buildversion.txt -n 1`

    packedRoddyDir=${RODDY_DIRECTORY}/dist/bin/${major}.${minor}
    packedZip=${RODDY_DIRECTORY}/dist/bin/Roddy_${major}.${minor}.zip
    currentRoddyDir=${RODDY_DIRECTORY}/dist/bin/current
    mkdir -p $packedRoddyDir

    nfoFile=${packedRoddyDir}/Roddy.jar.nfo
    cp -r $currentRoddyDir/* $packedRoddyDir

    git status > ${filename}.nfo
    svn info > ${filename}.nfo
    svn status >> ${filename}.nfo
    find ${packedRoddyDir} >> ${nfoFile}
    ls -l ${packedRoddyDir} >> ${nfoFile}

    cd ${RODDY_DIRECTORY}/dist/bin
    zip -r9 $packedZip ${major}.${minor}

    exit 0
elif [[ "$parm1" == "compileplugin" ]]; then
    echo "Using Roddy binary "`basename ${RODDY_BINARY}`
    echo "  Roddy version: "$(basename $(dirname ${RODDY_BINARY}))
    [[ ! -d $JDK_HOME ]] && echo "There was no JDK home found. Roddy cannot compile workflows." && exit 1
    source ${SCRIPTS_DIR}/compileRoddyPlugin.sh
    exit 0
elif [[ "$parm1" == "packplugin" || "$parm1" == "testpackplugin" ]]; then
    [[ "$parm1" == "testpackplugin" ]] && set -xuv
    increasebuildonly=true
    source ${SCRIPTS_DIR}/compileRoddyPlugin.sh

    # Test pack does not put things to svn so it is safe to use. Test will not change the zip file but will increase the buildnumber.
    pluginID=$2
    pluginDirectories=`grepFromConfigFile pluginDirectories $customconfigfile`
    pluginDirectory=`$GROOVY_BINARY ${SCRIPTS_DIR}/findPluginFolders.groovy ${pluginDirectories} ${RODDY_DIRECTORY} ${pluginID}`
    for i in `ls ${pluginDirectory}/README*.txt 2> /dev/null`; do
        $GROOVY_BINARY ${SCRIPTS_DIR}/addChangelistVersionTag.groovy $i ${pluginDirectory}/buildversion.txt
    done

    major=`head ${pluginDirectory}/buildversion.txt -n 1`
    minor=`tail ${pluginDirectory}/buildversion.txt -n 1`
    filename=${pluginID}_${major}.${minor}
    cd ${pluginDirectory}/..
    echo "Copying current to ${filename} ..."
    [[ ! -d ${filename} ]] && mkdir ${filename}
    cp -r $pluginID/* ${filename}
    cd $filename
#    set -xuv
    set +e
    echo "Removing obsolete files"
    # Find .svn folders?
    find -type d -name "*.svn" | xargs rm -rf
    rm -rf build
    rm -rf out
    rm README*.txt~ 2> /dev/null

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
#        svn add ${filename}.zip ${filename}.nfo 2> /dev/null
    fi

    cd ..; echo "Done";
    exit 0
    # Only unzip if necessary!
elif [[ "$parm1" == "createworkflow" ]]; then
    pluginID=$2
    workflowID=$3
    source ${SCRIPTS_DIR}/createNewWorkflow.sh ${customconfigfile} ${pluginID} ${3-}
    exit 0
fi

IFS=""
#[[ $RMIPORT != "" ]] && export DBG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" && echo "Opened up rmi debugger port"
java $DBG_OPTS -cp .:$libraries:${RODDY_BINARY} de.dkfz.roddy.Roddy $*
# -verbose:class
IFS=$OFS
