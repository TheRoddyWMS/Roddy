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

    enum PassOptions {
        parameters,
        array
    }

    enum IndexOptions {
        numeric,
        strings
    }

    ToolFileGroupParameter(Class<FileGroup> groupClass, Class<BaseFile> genericFileClass, String scriptParameterName, PassOptions passas = PassOptions.parameters, IndexOptions indexOptions = IndexOptions.numeric) {
        super(scriptParameterName)
        this.groupClass = groupClass
        this.passOptions = passas
        this.genericFileClass = genericFileClass
        this.indexOptions = indexOptions
    }



    @Override
    ToolFileGroupParameter clone() {
        return new ToolFileGroupParameter(groupClass, genericFileClass, scriptParameterName, passOptions)
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
