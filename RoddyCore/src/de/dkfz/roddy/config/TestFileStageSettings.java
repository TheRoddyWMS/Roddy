/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.knowledge.files.FileStage;
import de.dkfz.roddy.knowledge.files.FileStageSettings;

/**
 * Created by michael on 28.10.14.
 */
public class TestFileStageSettings extends FileStageSettings {
    public TestFileStageSettings() {
        super(null, null);
    }

    @Override
    public FileStageSettings copy() {
        return new TestFileStageSettings();
    }

    @Override
    public FileStageSettings decreaseLevel() {
        return new TestFileStageSettings();
    }

    @Override
    public String getIDString() {
        return "testid";
    }

    @Override
    public String fillStringContent(String temp) {
        return null;
    }

    @Override
    public String fillStringContentWithArrayValues(int index, String temp) {
        return null;
    }
}
