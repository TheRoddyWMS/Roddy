/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy

import groovy.transform.CompileStatic

@CompileStatic
enum ExitReasons {

    wrongStartupLocation(255, "You must call roddy from the right location! (Base roddy folder where roddy.sh resides."),
    groovyServError(254, "SystemExitException throw by GroovyServ, will exit now."),
    malformedCommandLine(253, "Command line was malformed."),
    unfulfilledRequirements(252, "Execution requirements unfulfilled."),
    unparseableStartupOptions(251, "Startup options could not be parsed."),
    unknownFeatureToggleFile(250, "Cannot find requested feature toggle file."),
    unknownFeatureToggle(249, "Feature toggle is not known."),
    unknownProxyProblem(248, "Unknown problem with proxy setup."),
    scratchDirNotConfigured(247, Constants.APP_PROPERTY_SCRATCH_BASE_DIRECTORY + " is not defined."),
    wrongJobManagerClass(246, "Wrong job manager class."),
    appPropertiesFileNotFound(245, "Application properties file not found or loadable."),
    analysisNotLoadable(244, "Could not load analysis"),
    severeConfigurationErrors(243, "Severe configuration errors occurred."),
    unhandledException(242, "Unhandled exception."),
    unknownSSHHost(241, "Unknown SSH host."),
    invalidSSHConfig(240, "SSH setup is not valid."),
    fatalSSHError(239, "Fatal error during SSH setup."),

    wrongExitCodeUsed(100, "Someone uses a wrong exit code somewhere in Roddy. Exit codes should be in class ExitReasons (if possible) and must be in the range [1;255].")

    final String message

    final int code

    ExitReasons(int code, String message) {
        this.message = message
        this.code = code
    }
}
