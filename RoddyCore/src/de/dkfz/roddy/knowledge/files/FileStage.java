/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

import java.util.LinkedList;
import java.util.List;

/**
*/
@Deprecated
public class FileStage {

    private static final List<FileStage> listOfAvailableFileStages = new LinkedList<>();

    public static final FileStage GENERIC = new FileStage(32);

    private final FileStage successor;
    private final int value;

    public FileStage getSuccessor() {
        return successor;
    }

    public boolean isMoreDetailedOrEqualTo(FileStage comp) {
        return (this.value > comp.value);
    }

    protected FileStage(FileStage successor, int value) {
        this.successor = successor;
        this.value = value;
    }

    protected FileStage(int value) {
        this(null, value);
    }

    private static void addAvailableFileStage(FileStage fs) {
        synchronized (listOfAvailableFileStages) {
            listOfAvailableFileStages.add(fs);
        }
    }

    public static void parseFileStage(String fsName) {
        LinkedList<FileStage> temp;
        synchronized (listOfAvailableFileStages) {
            temp = new LinkedList<>(listOfAvailableFileStages);
        }
        for(FileStage fs : temp) {
//            fs.getClass().getF
        }
    }

    public String toString() {
        return "fileStage=" + value;
    }
}
