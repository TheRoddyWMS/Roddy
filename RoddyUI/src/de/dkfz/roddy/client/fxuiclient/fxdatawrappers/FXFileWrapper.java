/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;

/**
 * Wrapper for single files.
 * Paths don't get displayed in toString()
 */
public class FXFileWrapper {
    private final File file;

    private StringProperty _fileName = new SimpleStringProperty();
    private StringProperty _filePath = new SimpleStringProperty();

    public FXFileWrapper(File file) {
        this.file = file;
        this._fileName.setValue(file.getName());
        this._filePath.setValue(file.getParent());
    }

    public String getPath() {
        return file.getPath();
    }

    public String getName() {
        return file.getName();
    }

    public StringProperty fileNameProperty() {
        return _fileName;
    }

    public StringProperty filePathProperty() {
        return _filePath;
    }

    @Override
    public String toString() {
        return file.getName();
    }

    public File getFile() {
        return file;
    }
}