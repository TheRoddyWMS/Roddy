/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileGroup
import groovy.transform.CompileStatic

/**
 * Parameters for generic tools (tools which are not programmatically set!).
 * Parameters can be for in and for output.
 * Parameters can have constraints.
 *
 * File groups are supposed to contain multiple files of the same type. A good example would be
 * a group of .bed files for the different chromosomes.
 */
@CompileStatic
class ToolFileGroupParameter extends ToolEntry.ToolParameterOfFiles {
    public final Class<FileGroup> groupClass
    public final Class<BaseFile> genericFileClass
    public final List<ToolFileParameter> files
    public final PassOptions passOptions
    public final IndexOptions indexOptions

    enum PassOptions {
        parameters,
        array
    }

    enum IndexOptions {
        numeric,
        strings
    }

    @Deprecated
    ToolFileGroupParameter(Class<FileGroup> groupClass, List<ToolFileParameter> children, String scriptParameterName, PassOptions passas = PassOptions.parameters, IndexOptions indexOptions = IndexOptions.numeric) {
        super(scriptParameterName)
        this.groupClass = groupClass
        this.passOptions = passas
        this.genericFileClass = null
        this.indexOptions = indexOptions
        this.files = children
    }

    ToolFileGroupParameter(Class<FileGroup> groupClass, Class<BaseFile> genericFileClass, String scriptParameterName, PassOptions passas = PassOptions.parameters, IndexOptions indexOptions = IndexOptions.numeric) {
        super(scriptParameterName)
        this.groupClass = groupClass
        this.passOptions = passas
        this.genericFileClass = genericFileClass
        this.indexOptions = indexOptions
    }

    /**
     * Copy constructor
     */
    @Deprecated
    ToolFileGroupParameter(String scriptParameterName, Class<FileGroup> groupClass, Class<BaseFile> genericFileClass, PassOptions passOptions, IndexOptions indexOptions, List<ToolFileParameter> files) {
        super(scriptParameterName)
        this.groupClass = groupClass
        this.genericFileClass = genericFileClass
        this.passOptions = passOptions
        this.indexOptions = indexOptions
        this.files = files
    }

    @Override
    public ToolFileGroupParameter clone() {
        List<ToolFileParameter> _files = new LinkedList<ToolFileParameter>()
        for (ToolFileParameter tf : files) {
            _files.add(tf.clone())
        }
        return new ToolFileGroupParameter(scriptParameterName, groupClass, genericFileClass, passOptions, indexOptions, _files)
    }

    @Override
    boolean equals(o) {
        //Backed by test
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        ToolFileGroupParameter that = o as ToolFileGroupParameter

        if (files != that.files) return false
        if (genericFileClass != that.genericFileClass) return false
        if (groupClass != that.groupClass) return false
        if (indexOptions != that.indexOptions) return false
        if (passOptions != that.passOptions) return false

        return true
    }

    int hashCode() {
        int result
        result = (groupClass != null ? groupClass.hashCode() : 0)
        result = 31 * result + (genericFileClass != null ? genericFileClass.hashCode() : 0)
        result = 31 * result + (passOptions != null ? passOptions.hashCode() : 0)
        result = 31 * result + (indexOptions != null ? indexOptions.hashCode() : 0)
        result = 31 * result + (files != null ? files.hashCode() : 0)
        return result
    }

    boolean isGeneric() {
        return genericFileClass != null
    }

    String getGenericClassString() {
        if (isGeneric()) {
            return groupClass.getName() + "<" + genericFileClass.getName() + ">"
        } else {
            return groupClass.getName()
        }
    }

        @Override
    public boolean hasSelectionTag() {
        return false
    }

    @Override
    public List<ToolFileParameter> getAllFiles() {
        return files.collect { it.getAllFiles() }.flatten() as List<ToolFileParameter>
    }

    @Override
    public List<ToolFileParameter> getFiles() {
        return files
    }


}
