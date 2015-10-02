package de.dkfz.roddy.knowledge.examples;

import de.dkfz.roddy.knowledge.files.FileStage;

/**
 */
public class SimpleFileStage extends FileStage {

    public static SimpleFileStage SIMPLE_STAGE_LVL_0 = new SimpleFileStage(0);
    public static SimpleFileStage SIMPLE_STAGE_LVL_1 = new SimpleFileStage(SIMPLE_STAGE_LVL_0, 1);
    public static SimpleFileStage SIMPLE_STAGE_LVL_2 = new SimpleFileStage(SIMPLE_STAGE_LVL_1, 2);

    protected SimpleFileStage(FileStage successor, int value) {
        super(successor, value);
    }

    protected SimpleFileStage(int value) {
        super(value);
    }
}
