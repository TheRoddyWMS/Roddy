/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.examples;

import de.dkfz.roddy.knowledge.files.BaseFile;

/**
 * A very simple text file containing nothing or some text.
 */
public class FileWithChildren extends BaseFile {

    private SimpleTestTextFile childFile0;

    private SimpleTestTextFile childFile1;

    public FileWithChildren(SimpleTestTextFile parentFile) {
        super(parentFile);
    }

    public SimpleTestTextFile getChildFile0() {
        return childFile0;
    }

    public void setChildFile0(SimpleTestTextFile childFile0) {
        this.childFile0 = childFile0;
    }

    public SimpleTestTextFile getChildFile1() {
        return childFile1;
    }

    public void setChildFile1(SimpleTestTextFile childFile1) {
        this.childFile1 = childFile1;
    }
}
