#!/bin/bash

# Call this script with the following parameters
# 1) Mode
# 		create	Create a new project configuration or override it
# 		update	Update an existing project configuration and
# 2) Base path where the roddyProjects folder will be put to
# 3) SSH / Local
# 4) Roddy version like 2.1.28
# 5) List of plugins and version like COWorkflows:1.0.131

#set -xuv

[[ $# -lt 2 ]] && echo "Please supply the mode as either create or upate" && exit 1

mode=${2-}
[[ ! $mode == "create" && ! $mode == "update" ]] && echo "Wrong mode supplied, needs to be either create or update." && exit 2

[[ $mode == "create" && $# -lt 3 ]] && echo "Wrong parameters supplied, use bash roddy.sh prepareprojectconfig create [base path]" && exit 1

[[ $mode == "update" && $# -lt 3 ]] && echo "Wrong parameters supplied, use bash roddy.sh prepareprojectconfig update [base path]" && exit 1

basePath=${3-}/roddyProject

echo $mode
echo $basePath

versionsFolder=$basePath/versions
targetConfigFolder=$versionsFolder/version_`date +"%Y%m%d_%H%M%S"`

if [[ $mode == "create" ]]; then
	[[ -d $basePath ]] && echo "The folder ${basePath} is already existing, create mode will not work." && exit 4

	mkdir -p $targetConfigFolder
	cp ${SCRIPTS_DIR}/skeletonProject.xml $targetConfigFolder/project.xml
#	cp ${SCRIPTS_DIR}/skeletonAppProperties.ini $targetConfigFolder/applicationProperties.ini

  groovy -e "String cfgDir = new File(\"${SCRIPTS_DIR}/skeletonAppProperties.ini\").text.replace(\"configurationDirectories=\", \"configurationDirectories=${targetConfigFolder}\"); new File(\"${targetConfigFolder}/applicationProperties.ini\").write(cfgDir);"


elif [[ $mode == "update" ]]; then
	# Find latest version and copy that.

	[[ ! -d $versionsFolder ]] && echo "The folder ${versionsFolder} is not existing, cannot perform update" && exit 6
	[[ `ls -d $versionsFolder/*/ | wc -l` -eq 0 ]] && echo "There are no version folders available" && exit 7

	latestVersion=`ls -d $versionsFolder/* | tail -n 1`
	cp -r $latestVersion $targetConfigFolder
	set -xuv
	groovy -e "String cfgDir = new File(\"${targetConfigFolder}/applicationProperties.ini\").text.replace(\"${latestVersion}\", \"${targetConfigFolder}\"); new File(\"${targetConfigFolder}/applicationProperties.ini\").write(cfgDir);"
  set +xuv
fi
