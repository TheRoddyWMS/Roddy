/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.core.ExecutionContext;

/**
 * An abstract base class for tuple objects
 */
public abstract class AbstractFileObjectTuple extends FileObject {

    public AbstractFileObjectTuple(ExecutionContext executionContext) {
        super(executionContext);
    }

    @Override
    public void runDefaultOperations() {

    }
}
