/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

import de.dkfz.roddy.client.RoddyStartupOptions
import de.dkfz.roddy.client.cliclient.CommandLineCall
import de.dkfz.roddy.core.InfoObject

/**
 * This script creates a folder with
 */

//@groovy.transform.CompileStatic
public class ConfigurationPreparator {

    enum Modes {
        create,
        update
    }

    public static File getScriptFolder() {
        return new File(getClass().protectionDomain.codeSource.location.path).parentFile
    }

    public void doIt(CommandLineCall clc) {

        Modes mode = clc.getArguments()[0] as Modes;
        File basePath = clc.getArguments().size() > 2 ? new File(clc.getArguments().get(1)) : null;


        if (!mode) {
            println("Please supply the mode as either create or upate."); System.exit(1)
        }

        if (!basePath) {
            println("For create and update mode, you need to set the base project path."); System.exit(2)
        }

        File versionsFolder = new File(basePath, "versions");
        File targetConfigFolder = new File(versionsFolder, "version_" + InfoObject.formatTimestamp(new Date()));

        if (mode == Modes.create) {
            doCreate(basePath, targetConfigFolder, clc);
        } else if (mode == Modes.update) {
            doUpdate(basePath, targetConfigFolder, clc);
        }
    }

    void doCreate(File basePath, File targetConfigFolder, CommandLineCall clc) {
        if (basePath.exists()) {
            println("The folder ${basePath} is already existing, create mode will not work."); System.exit(3)
        }
        targetConfigFolder.mkdirs();

        boolean useRepo = clc.isOptionSet(RoddyStartupOptions.userepository);

        if(useRepo) {
            // Copy all files from a specific repository and adapt them.
        } else {
            new File("")
        }

//        cp ${SCRIPTS_DIR}/skeletonProject_minimal.xml $targetConfigFolder/project_minimal.xml
//        cp ${SCRIPTS_DIR}/skeletonProject_extended.xml $targetConfigFolder/project_extended.xml
//        #	cp ${SCRIPTS_DIR}/skeletonAppProperties.ini $targetConfigFolder/applicationProperties.ini
//
//        groovy -e "String cfgDir = new File(\"${SCRIPTS_DIR}/skeletonAppProperties.ini\").text.replace(\"configurationDirectories=\", \"configurationDirectories=${targetConfigFolder}\"); new File(\"${targetConfigFolder}/applicationProperties.ini\").write(cfgDir);"

    }

    void doUpdate(File basePath, File targetConfigFolder, CommandLineCall clc) {
//        # Find latest version and copy that.
//
//        [[ ! -d $versionsFolder ]] && echo "The folder ${versionsFolder} is not existing, cannot perform update" && exit 6
//        [[ `ls -d $versionsFolder/*/ | wc -l` -eq 0 ]] && echo "There are no version folders available" && exit 7
//
//	latestVersion=`ls -d $versionsFolder/* | tail -n 1`
//	cp -r $latestVersion $targetConfigFolder
//	groovy -e "String cfgDir = new File(\"${targetConfigFolder}/applicationProperties.ini\").text.replace(\"${latestVersion}\", \"${targetConfigFolder}\"); new File(\"${targetConfigFolder}/applicationProperties.ini\").write(cfgDir);"
    }
}

new ConfigurationPreparator().doIt(new CommandLineCall(args))