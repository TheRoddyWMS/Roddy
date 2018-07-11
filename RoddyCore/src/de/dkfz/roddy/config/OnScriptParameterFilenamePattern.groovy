/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.knowledge.files.BaseFile

/**
 * Created by kaercher on 24.02.16.
 */
public class OnScriptParameterFilenamePattern extends FilenamePattern {

    private final String toolID;
    private final String calledParameterID;

    public OnScriptParameterFilenamePattern(Class<BaseFile> cls, String toolID, String parameter, String pattern) {
        super(cls, pattern, "default");
        this.toolID = toolID;
        this.calledParameterID = parameter;
    }

    @Override
    public FilenamePatternDependency getFilenamePatternDependency() {
        return FilenamePatternDependency.onScriptParameter;
    }

    //@Override
    protected BaseFile getSourceFile(BaseFile[] baseFiles) {
        BaseFile baseFile = baseFiles[0]
        if (baseFile.getParentFiles() != null && baseFile.getParentFiles().size() > 0)
            return (BaseFile) baseFile.getParentFiles().get(0); //In this case sourcefile is the first of the base files, if at least one basefile is available.
        return null;
    }

    @Override
    public String getID() {
        return String.format("SCRIPTPARM::onParm_%s:%s[%s]", toolID?:"[ANY]", calledParameterID, selectionTag);
    }

    public String getCalledParameterID() {
        return calledParameterID;
    }

    public String getToolID(){
        return toolID;
    }
}
