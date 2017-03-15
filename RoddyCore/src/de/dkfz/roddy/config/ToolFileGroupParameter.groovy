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
 */
@CompileStatic
class ToolFileGroupParameter extends ToolEntry.ToolParameter<ToolFileGroupParameter> {
    public final Class<FileGroup> groupClass
    public final Class<BaseFile> genericFileClass
    public final PassOptions passOptions
    public final IndexOptions indexOptions

    @Deprecated
    public final List<ToolFileParameter> files

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
    ToolFileGroupParameter clone() {
        return new ToolFileGroupParameter(scriptParameterName, groupClass, genericFileClass,  passOptions, indexOptions, files?.collect { ToolFileParameter tfp -> tfp.clone() })
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
}
