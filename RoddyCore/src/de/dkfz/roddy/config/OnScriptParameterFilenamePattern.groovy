/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.knowledge.files.BaseFile;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Created by kaercher on 24.02.16.
 */
public class OnScriptParameterFilenamePattern extends FilenamePattern {

    private final String toolName;
    private final String calledParameterId;

    public OnScriptParameterFilenamePattern(Class<BaseFile> cls, String toolName, String parameter, String pattern) {
        super(cls, pattern, "default");
        this.toolName = toolName;
        this.calledParameterId = parameter;
    }

    @Override
    public FilenamePatternDependency getFilenamePatternDependency() {
        return FilenamePatternDependency.onScriptParameter;
    }

    //@Override
    /** By default, the source file for this filename pattern is the first base file. However, if the filename pattern has the sourcefilename attribute
     *  set, the base file is the one with the matching scriptparametername attribute. If no matching basefile is found, an exception is thrown.
     *
     * @param baseFiles
     * @return
     */
    protected BaseFile getSourceFile(BaseFile[] baseFiles) {
        BaseFile baseFile = baseFiles[0]
        if (baseFile.getParentFiles() != null && baseFile.getParentFiles().size() > 0)
            return (BaseFile) baseFile.getParentFiles().get(0); //In this case sourcefile is the first of the base files, if at least one basefile is available.
        return null;
    }

    @Override
    public String getID() {
        return String.format("SCRIPTPARM::onParm_%s:%s[%s]", toolName?:"[ANY]", calledParameterId, selectionTag);
    }

    public String getCalledParameterId() {
        return calledParameterId;
    }

    public String getToolName(){
        return toolName;
    }
}
