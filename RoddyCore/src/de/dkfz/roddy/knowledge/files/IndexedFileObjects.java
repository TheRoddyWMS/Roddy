/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.core.ExecutionContext;

import java.util.Map;
import java.util.List;

/**
 * Stores a range of file objects which are returned by an array job.
 */
public class IndexedFileObjects<F extends FileObject> extends FileObject {
    protected final List<String> indices;

    private final Map<String, F> indexedObjects;

    public IndexedFileObjects(List<String> indices, Map<String, F> indexedObjects,
                              ExecutionContext executionContext) {
        super(executionContext);
        assert(null != indices && indices.size() > 0);
        this.indexedObjects = indexedObjects;
        this.indices = indices;
    }

    @Override
    public void runDefaultOperations() {

    }

    public List<String> getIndices() {
        return indices;
    }

    public Map<String, F> getIndexedFileObjects() {
        return indexedObjects;
    }

}
