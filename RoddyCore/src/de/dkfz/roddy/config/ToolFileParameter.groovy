/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.roddy.Constants;
import de.dkfz.roddy.knowledge.files.BaseFile

/**
 * Parameters for generic tools (tools which are not programmatically set!).
 * Parameters can be for in and for output.
 * Parameters can have constraints.
 * ToolFileParameter may contain one or multiple files. If there are "child"-files
 * these are dependent on the main file, like .bai files depend on the .bam file.
 */
class ToolFileParameter extends ToolEntry.ToolParameterOfFiles {
    final Class<BaseFile> fileClass
    final List<ToolEntry.ToolConstraint> constraints
    final String scriptParameterName
    final String filenamePatternSelectionTag
    final ToolFileParameterCheckCondition checkFile
    final String parentVariable
    private List<ToolFileParameter> childFiles

    ToolFileParameter(Class<BaseFile> fileClass, List<ToolEntry.ToolConstraint> constraints, String scriptParameterName, ToolFileParameterCheckCondition checkFile) {
        this(fileClass, constraints, scriptParameterName, checkFile, Constants.DEFAULT, null, null)
    }

    ToolFileParameter(Class<BaseFile> fileClass, List<ToolEntry.ToolConstraint> constraints, String scriptParameterName, ToolFileParameterCheckCondition checkFile, String filenamePatternSelectionTag, List<ToolFileParameter> childFiles, String parentVariable) {
        super(scriptParameterName)
        this.fileClass = fileClass
        this.constraints = constraints != null ? constraints : new LinkedList<ToolEntry.ToolConstraint>()
        this.scriptParameterName = scriptParameterName
        this.checkFile = checkFile
        this.filenamePatternSelectionTag = filenamePatternSelectionTag
        this.childFiles = childFiles
        if (this.childFiles == null) this.childFiles = new LinkedList<>()
        this.parentVariable = parentVariable
    }

    ToolFileParameter clone() {
        List<ToolEntry.ToolConstraint> _con = new LinkedList<ToolEntry.ToolConstraint>();
        for (ToolEntry.ToolConstraint tc : constraints) {
            _con.add(tc.clone())
        }
        return new ToolFileParameter(fileClass, _con, scriptParameterName, checkFile, filenamePatternSelectionTag, childFiles, parentVariable);
    }

    @Override
    boolean equals(Object o) {
        //Backed by test
        if (this.is(o)) return true
        if (o == null || getClass() != o.getClass()) return false
        if (!super.equals(o)) return false


        ToolFileParameter that = (ToolFileParameter) o

        // This is mostly Idea generated code. I added the loops to compare files and constraints.

        if (fileClass != null ? !fileClass.equals(that.fileClass) : that.fileClass != null) return false
        if (constraints != null ? !constraints.equals(that.constraints) : that.constraints != null) return false
        if (constraints.size() != that.constraints.size()) return false
        for (int i = 0; i < constraints.size(); i++) {
            if (!constraints.get(i).equals(that.constraints.get(i))) return false
        }
        if (scriptParameterName != null ? !scriptParameterName.equals(that.scriptParameterName) : that.scriptParameterName != null) return false
        if (filenamePatternSelectionTag != null ? !filenamePatternSelectionTag.equals(that.filenamePatternSelectionTag) : that.filenamePatternSelectionTag != null) return false
        if (checkFile != null ? !checkFile.equals(that.checkFile) : that.checkFile != null) return false
        if (parentVariable != null ? !parentVariable.equals(that.parentVariable) : that.parentVariable != null) return false
        if (childFiles != null ? !childFiles.equals(that.childFiles) : that.childFiles != null) return false

        if (childFiles.size() != that.childFiles.size()) return false
        for (int i = 0; i < childFiles.size(); i++) {
            if (!childFiles.get(i).equals(that.childFiles.get(i))) return false
        }
        return true
    }

    @Override
    List<ToolEntry.ToolParameterOfFiles> getFiles() {
        return childFiles
    }

    @Override
    List<ToolEntry.ToolParameterOfFiles> getAllFiles() {
        return (files.collect { it.getAllFiles() }.flatten() + [this]) as List<ToolFileParameter>
    }

    @Override
    boolean hasSelectionTag() {
        return !filenamePatternSelectionTag.equals(Constants.DEFAULT)
    }

}
