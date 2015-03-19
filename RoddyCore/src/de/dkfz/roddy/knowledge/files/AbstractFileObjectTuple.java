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
