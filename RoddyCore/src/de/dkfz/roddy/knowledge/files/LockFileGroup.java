package de.dkfz.roddy.knowledge.files;

import java.util.List;

/**
 * A group of locked files
 */
public class LockFileGroup extends FileGroup<LockFile> {
    public LockFileGroup(List<LockFile> files) {
        super(files);
    }

    public static LockFileGroup createFromFileList(List<BaseFile> lockfileParents) {
        return lockfileParents.get(0).getExecutionContext().createLockFiles((List)lockfileParents);
    }
}
