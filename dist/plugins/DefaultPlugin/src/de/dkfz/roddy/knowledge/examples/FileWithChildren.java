package de.dkfz.roddy.knowledge.examples;

import de.dkfz.roddy.knowledge.files.BaseFile;

/**
 * A very simple text file containing nothing or some text.
 */
public class FileWithChildren extends BaseFile {

    private TextFile childFile0;

    private TextFile childFile1;

    public FileWithChildren(TextFile parentFile) {
        super(parentFile);
    }

    public TextFile getChildFile0() {
        return childFile0;
    }

    public void setChildFile0(TextFile childFile0) {
        this.childFile0 = childFile0;
    }

    public TextFile getChildFile1() {
        return childFile1;
    }

    public void setChildFile1(TextFile childFile1) {
        this.childFile1 = childFile1;
    }
}
