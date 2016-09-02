/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.examples;

import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.knowledge.files.FileStage;
import de.dkfz.roddy.knowledge.files.FileStageSettings;

/**
 * Todo Think about, how to setup a generic class for file stages.
 */
public class SimpleFileStageSettings extends FileStageSettings<SimpleFileStageSettings> {

    private final String valueLevel0;
    private final String valueLevel1;

    public SimpleFileStageSettings(DataSet dataSet, FileStage stage, String valueLevel0, String valueLevel1) {
        super(dataSet, stage);
        this.valueLevel0 = valueLevel0;
        this.valueLevel1 = valueLevel1;
    }

    public SimpleFileStageSettings(DataSet dataSet, String valueLevel0, String valueLevel1) {
        this(dataSet, SimpleFileStage.SIMPLE_STAGE_LVL_2, valueLevel0, valueLevel1);
    }

    public SimpleFileStageSettings(DataSet dataSet, String valueLevel0) {
        this(dataSet, SimpleFileStage.SIMPLE_STAGE_LVL_1, valueLevel0, null);
    }

    protected SimpleFileStageSettings(DataSet dataSet) {
        this(dataSet, SimpleFileStage.SIMPLE_STAGE_LVL_0, null, null);

    }

    @Override
    public SimpleFileStageSettings copy() {
        return new SimpleFileStageSettings(dataSet, stage, valueLevel0, valueLevel1);
    }

    @Override
    public SimpleFileStageSettings decreaseLevel() {
        if (stage == SimpleFileStage.SIMPLE_STAGE_LVL_2)
            return new SimpleFileStageSettings(dataSet, valueLevel0);
        if (stage == SimpleFileStage.SIMPLE_STAGE_LVL_1)
            return new SimpleFileStageSettings(dataSet);

        return copy();
    }

    @Override
    public String getIDString() {
        if (stage == SimpleFileStage.SIMPLE_STAGE_LVL_2)
            return String.format("%s_%s_%s", valueLevel0, valueLevel1, dataSet);
        if (stage == SimpleFileStage.SIMPLE_STAGE_LVL_1)
            return String.format("%s_%s", valueLevel0, dataSet);
        return String.format("%s", dataSet);
    }

    @Override
    public String fillStringContent(String temp) {
        temp = temp.replace("${val0}", valueLevel0);
        temp = temp.replace("${val1}", valueLevel1);
        return temp;
    }

    @Override
    public String fillStringContentWithArrayValues(int index, String temp) {
        temp = temp.replace(String.format("${val0[%d]}", index), valueLevel0);
        temp = temp.replace(String.format("${val1[%d]}", index), valueLevel1);
        return temp;
    }
}
