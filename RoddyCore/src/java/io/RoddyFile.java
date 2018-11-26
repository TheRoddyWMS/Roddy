/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package java.io;

import java.net.URI;

/**
 * Roddy files are a subclass of File. Roddy files can exist both locally and remotely.
 * Different file system instances are used for local and remote path normalization.
 */
public class RoddyFile extends File {
    private static FileSystem remoteFileSystem;

    private static FileSystem getRemoteFileSystem() {
        return new UnixFileSystem();
//        Roddy.getInstance().getFileSystem();
//        FileSystem
    }


    public RoddyFile(String pathname) {
        super(pathname);

    }

    public RoddyFile(String parent, String child) {
        super(parent, child);
    }

    public RoddyFile(File parent, String child) {
        super(parent, child);
    }

    public RoddyFile(URI uri) {
        super(uri);
    }


}
