#!/bin/bash
# This script creates a new workflow.
# If an existing plugin is specified, this is used otherwise the script will create a new plugin and will also ask for the plugin folder.
# Call this like: $configFile PluginID[:dependencyPlugin] [native:]WorkflowID

#customconfigfile=${1-false}

workflowPlugin=${2-false}
workflowName=${3-false}
if  [[ ${workflowPlugin} == false ]] || \
[[ ${workflowName} == false ]]
then
    echo "You need to provide a roddy app config file, a plugin name and a workflow name you need to provide a workflow name"
    echo "Please call Roddy like: bash roddy.sh PluginID[:dependencyPlugin] [native:]WorkflowID [--useconfig=yourconfig.ini>]"
    echo "  dependencyPlugin - Specify this, if you want to add dependencies to other workflows"
    echo "  native|brawl     - Set this, if you do want to create either a native (shell script) or a brawl workflow"
    echo "                     If nothing is set, Roddy will create a Java based workflow."
    exit 1
fi

pluginDirectories=`grep pluginDirectories ${customconfigfile}`
pluginDirectory=`groovy ${RODDY_DIRECTORY}/helperScripts/findPluginFolders.groovy ${pluginDirectories} ${PWD} ${pluginID}`
pluginIsAvailable=false
[[ ! $pluginDirectory == null ]] && pluginIsAvailable=true

workflowIsNative=false
workflowIsBrawl=false
[[ $workflowName == native:* ]] && workflowIsNative=true && workflowName=${workflowName:7:200}
[[ $workflowName == brawl:* ]] && workflowIsBrawl=true && workflowName=${workflowName:6:200}

# Is the plugin a new workflow? Ask for this!
if [[ $pluginIsAvailable == false ]]; then
    read -p "The plugin is unknown, do you want to create it? " -n 1 -r
    echo    # (optional) move to a new line
    [[ ! $REPLY =~ ^[Yy]$ ]] && echo "Plugin not created, exitting." && exit 0

    # Ask for the folder
    oFS=$IFS
    IFS=","
    pluginDirectories=${pluginDirectories:18:300}
    pluginDirectories=${pluginDirectories},${pluginDirectories}_1,${pluginDirectories}_2
    echo "Please select one of the following directories for the new plugin:"
    cnt=0
    for pDir in ${pluginDirectories[@]}; do
        echo " [${cnt}] $pDir";
        let cnt++
    done
    set -xuv
    read -p "Select number: " -n 1 -r
    no=$REPLY
    echo $no
    echo ${pluginDirectories[$no]}
    IFS=$oFS
fi
set -xuv

# Collect all template files which will be copied to the target folder.
templateReadmeFile=${RODDY_DIRECTORY}/dist/plugins/Template/README.template.txt
templateBuildFile=${RODDY_DIRECTORY}/dist/plugins/Template/buildversion.txt

templateWorkflowClass=${RODDY_DIRECTORY}/dist/plugins/Template/src/TemplateWorkflowClass.java
templateAnalysisConfig=${RODDY_DIRECTORY}/dist/plugins/Template/resources/configurationFiles/analysisTemplate.xml
templateAnalysisConfigNative=${RODDY_DIRECTORY}/dist/plugins/Template/resources/configurationFiles/analysisTemplateNative.xml
templateNativeWorkflowScript=${RODDY_DIRECTORY}/dist/plugins/Template/resources/analysisTools/NativeWorkflowTemplate.sh
templateAnalysisConfigBrawl=${RODDY_DIRECTORY}/dist/plugins/Template/resources/configurationFiles/analysisTemplateNative.xml
templateBrawlScript=${RODDY_DIRECTORY}/dist/plugins/Template/resources/analysisTools/NativeWorkflowTemplate.sh

templateCleanupScript=${RODDY_DIRECTORY}/dist/plugins/Template/resources/analysisTools/cleanupScript.sh
templatePluginClass=${RODDY_DIRECTORY}/dist/plugins/Template/src/CopyThisClass.java
templateIdeaProjectFile=${RODDY_DIRECTORY}/dist/plugins/Template/Template.iml

targetAnalysisToolsFolder=$pluginDirectory/resources/analysisTools/${workflowName}Native
targetBrawlScriptsFolder=$pluginDirectory/resources/brawlscripts
targetConfigurationFileFolder=$pluginDirectory/resources/configurationFiles

tgtDependenciesFile=$pluginDirectory/buildinfo.txt
tgtReadmeFile=$pluginDirectory/README.${workflowName}.txt
tgtBuildfile=$pluginDirectory/buildversion.txt
tgtAnalysisConfig=${targetConfigurationFileFolder}/analysis${workflowName}.xml
tgtAnalysisConfigNative=${targetConfigurationFileFolder}/analysis${workflowName}Native.xml
tgtNativeShellScript=${targetAnalysisToolsFolder}/NativeShellWorkflow.sh
tgtAnalysisConfigBrawl=${targetConfigurationFileFolder}/analysis${workflowName}Native.xml
tgtBrawlScript=${targetAnalysisToolsFolder}/NativeShellWorkflow.sh
tgtCleanupScript=${targetAnalysisToolsFolder}/cleanupScript.sh

#[[ -f ${tgtReadmeFile} ]] && echo "The workflow already seems to exist! No files will be created!" && exit 1
set -xuv
[[ ! -f ${tgtBuildfile} ]] && cp $templateBuildFile $pluginDirectory && svn add ${tgtBuildfile}

cp ${templateReadmeFile} ${tgtReadmeFile}

mkdir -p ${targetAnalysisToolsFolder}
mkdir -p ${targetConfigurationFileFolder}

cp ${templateCleanupScript} ${tgtCleanupScript}

#svn add ${targetAnalysisToolsFolder} ${targetConfigurationFileFolder} 2> /dev/null
#svn add ${tgtReadmeFile} ${templateCleanupScript}

if [[ $workflowIsNative == true ]]; then
    mkdir -p ${targetAnalysisToolsFolder}
    # Copy the native workflow shell script to the target folder.
    cp ${templateNativeWorkflowScript} ${tgtNativeShellScript}
    cp ${templateAnalysisConfigNative} ${tgtAnalysisConfigNative}
#    svn add ${tgtNativeShellScript} ${tgtAnalysisConfigNative}
elif [[ $workflowIsBrawl == true ]]; then
    mkdir -p ${targetAnalysisToolsFolder}
    cp ${templateBrawlScript} ${tgtBrawlScript}
    cp ${templateAnalysisConfigBrawl} ${tgtAnalysisConfigBrawl}
#    svn add ${tgtNativeShellScript} ${tgtAnalysisConfigNative}
else
    # Ask for the workflow class name and package
    read -p "Please enter the target package like: de.dkfz.b080.co : " -r
    targetPackage=${REPLY,,}.${workflowName,,}workflow
    targetPackageDir=${targetPackage//./"/"}
    $targetPackage $targetPackageDir
    targetWorkflowClass=$pluginDirectory/src/${targetPackageDir}/${workflowName^}Workflow.java
    $targetWorkflowClass

    mkdir -p $pluginDirectory/src/${targetPackageDir}
    cp ${templateWorkflowClass} ${targetWorkflowClass}
    cp ${templateAnalysisConfig} ${tgtAnalysisConfig}

#    svn add ${targetWorkflowClass} ${tgtAnalysisConfig}
fi
