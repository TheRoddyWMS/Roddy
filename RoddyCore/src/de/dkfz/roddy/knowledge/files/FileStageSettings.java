/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.core.DataSet;

import java.io.Serializable;

/**
 * File stages contain information about the "detail" level / stage of a file.
 * i.e. Raw sequence files belong to the lowest stage and contain the highest level of detail (even the numeric index).
 * i.e. paired bam files are one level above raw sequences and do not have the index set but they still belong to a lane and a run
 * i.e. Merged bam files only contain information about the sample and the data set.
 * @author michael
 */
public abstract class FileStageSettings<T extends FileStageSettings> implements Serializable {

    protected final DataSet dataSet;

    protected final FileStage stage;

    protected FileStageSettings(DataSet dataSet, FileStage stage) {
        this.dataSet = dataSet;
        this.stage = stage;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public FileStage getStage() {
        return stage;
    }

    public abstract T copy();

    public abstract T decreaseLevel();

    public abstract String getIDString();

    public abstract String fillStringContent(String temp);

    public abstract String fillStringContentWithArrayValues(int index, String temp);
}
