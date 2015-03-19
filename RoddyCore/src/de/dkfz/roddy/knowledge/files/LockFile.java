package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.core.ExecutionContext;

import java.io.File;

/**
 */
public class LockFile extends BaseFile {
    public LockFile(ExecutionContext run, File path) {
        super(path, run, null, null, null);
    }
}
