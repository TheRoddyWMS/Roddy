/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.roddy.knowledge.files.BaseFile

/**
 * A filename pattern that matches by the name of the script parameter.
 * This allows definition of job dependencies from the XML without reference to the JVM code.
 */
class OnScriptParameterFilenamePattern extends FilenamePattern {

    private final String toolID
    private final String parameterID

    OnScriptParameterFilenamePattern(Class<BaseFile> cls, String toolID, String parameter, String pattern, String selectionTag) {
        super(cls, pattern, selectionTag)
        this.toolID = toolID
        this.parameterID = parameter
    }

    @Override
    FilenamePatternDependency getFilenamePatternDependency() {
        FilenamePatternDependency.onScriptParameter
    }

    //@Override
    protected BaseFile getSourceFile(BaseFile[] baseFiles) {
        BaseFile baseFile = baseFiles[0]
        if (baseFile.getParentFiles() != null && baseFile.parentFiles.size() > 0)
            return (BaseFile) baseFile.parentFiles[0]  // In this case sourcefile is the first of the base files, if at least one basefile is available.
        return null
    }

    @Override
    String getID() {
        String.format('SCRIPTPARM::onParm_%s:%s[%s]', toolID ?: '[ANY]', parameterID, selectionTag)
    }

    String getParameterID() {
        parameterID
    }

    String getToolID() {
        toolID
    }

    String toString() {
        super.toString() + " toolID=$toolID parameterID=$parameterID"
    }
}
