/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.knowledge.files.BaseFile;

import java.util.LinkedList;
import java.util.List;

/**
 * Parameters for generic tools (tools which are not programmatically set!).
 * Parameters can be for in and for output.
 * Parameters can have constraints.
 */
public class ToolFileParameter extends ToolEntry.ToolParameter<ToolFileParameter> {
    public final Class<BaseFile> fileClass;
    public final List<ToolEntry.ToolConstraint> constraints;
    public final String scriptParameterName;
    public final String filenamePatternSelectionTag;
    public final ToolFileParameterCheckCondition checkFile;
    public final String parentVariable;
    private List<ToolFileParameter> childFiles;

    public ToolFileParameter(Class<BaseFile> fileClass, List<ToolEntry.ToolConstraint> constraints, String scriptParameterName, ToolFileParameterCheckCondition checkFile) {
        this(fileClass, constraints, scriptParameterName, checkFile, FilenamePattern.DEFAULT_SELECTION_TAG, null, null);
    }

    public ToolFileParameter(Class<BaseFile> fileClass, List<ToolEntry.ToolConstraint> constraints, String scriptParameterName, ToolFileParameterCheckCondition checkFile, String filenamePatternSelectionTag, List<ToolFileParameter> childFiles, String parentVariable) {
        super(scriptParameterName);
        this.fileClass = fileClass;
        this.constraints = constraints != null ? constraints : new LinkedList<>();
        this.scriptParameterName = scriptParameterName;
        this.checkFile = checkFile;
        this.filenamePatternSelectionTag = filenamePatternSelectionTag;
        this.childFiles = childFiles;
        if (this.childFiles == null) this.childFiles = new LinkedList<>();
        this.parentVariable = parentVariable;
    }

    @Override
    public ToolFileParameter clone() {
        List<ToolEntry.ToolConstraint> _con = new LinkedList<ToolEntry.ToolConstraint>();
        for (ToolEntry.ToolConstraint tc : constraints) {
            _con.add(tc.clone());
        }
        return new ToolFileParameter(fileClass, _con, scriptParameterName, checkFile, filenamePatternSelectionTag, childFiles, parentVariable);
    }

    public List<ToolFileParameter> getChildFiles() {
        return childFiles;
    }

    public boolean hasSelectionTag() {
        return !filenamePatternSelectionTag.equals(FilenamePattern.DEFAULT_SELECTION_TAG);
    }
}
