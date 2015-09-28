package de.dkfz.roddy.execution.io.fs;

import de.dkfz.roddy.tools.LoggerWrapper;

/**
 * This class exists to ensure backward compatility with older Roddy
 * workflows, after the initial class was renamed to FileSystemAccessProvider.
 *
 * Upon access (which occurs!), an instance is created and overrides the
 * singleton instance in FileSystemAccessProvider.
 *
 * Also a deprecation message is shown.
 */
public class FileSystemInfoProvider extends FileSystemAccessProvider {
    private static LoggerWrapper logger = LoggerWrapper.getLogger(FileSystemAccessProvider.class.getName());

    private FileSystemInfoProvider(FileSystemAccessProvider provider) {
        logger.postAlwaysInfo("FileSystemInfoProvider is deprecated. Please use the FileSystemAccessProvider class in the future.");
        this._userName = provider._userName;
        this._groupID = provider._groupID;
        this._userHome = provider._userHome;
        this._appendLineToFileLock = provider._appendLineToFileLock;
        this._directoryExistsAndIsAccessible.putAll(provider._directoryExistsAndIsAccessible);
        this._groupIDsByGroup.putAll(provider._groupIDsByGroup);
        this.uidToUserCache.putAll(provider.uidToUserCache);
    }

    public static FileSystemInfoProvider getInstance() {
        fileSystemAccessProviderLock.lock();
        try {
            FileSystemAccessProvider instance = FileSystemAccessProvider.getInstance();
            if(!(instance instanceof FileSystemInfoProvider)) {
                instance = new FileSystemInfoProvider(instance);
                FileSystemAccessProvider.resetFileSystemAccessProvider(instance);
            }
            return (FileSystemInfoProvider)instance;
        } finally {
            fileSystemAccessProviderLock.unlock();
        }
    }
}
