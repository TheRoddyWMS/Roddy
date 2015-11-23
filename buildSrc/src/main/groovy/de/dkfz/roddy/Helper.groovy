package de.dkfz.roddy

import org.gradle.api.GradleScriptException

class Helper {

    public String getReleaseLevel() {
        String releaseLevel = "increasePatch"
        if (project.hasProperty("release")) {
            if (project.release == "major") {
                releaseLevel = "increaseMajor"
            } else if (project.release == "minor") {
                releaseLevel = "increaseMinor"
            } else if (project.release == "patch") {
                releaseLevel = "increasePatch"
            } else if (project.release == "revision") {
                releaseLevel = "increaseRevision"
            } else {
                throw new GradleScriptException("Unknown release type '${project.release}'!")
            }
        }
        return releaseLevel
    }


}
