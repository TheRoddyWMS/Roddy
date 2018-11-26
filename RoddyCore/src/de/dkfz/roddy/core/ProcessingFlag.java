/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

/**
 * Processing flags are there to select what can be stored for a workflow.
 * I.e. Dummy jobs for input files can be prevented or i.e. jobs which are used to prepare an workflows flow (SNV, BamFile selection).
 * TODO Maybe split out the better parts to a generic flags class.
 */
public enum ProcessingFlag {

    STORE_DUMMY_JOBS(0b0000000000001),
    STORE_FILES(0b0000000000010),
    CREATE_FAKE_JOBS(0b0000000010000),
    STORE_NOTHING(0b0000000000000),
    STORE_EVERYTHING(STORE_DUMMY_JOBS, STORE_FILES);

    private final int value;

    private ProcessingFlag(int value) {
        this.value = value;
    }

    private ProcessingFlag(ProcessingFlag... values) {
        int tempValue = 0;
        for (ProcessingFlag processingFlag : values) {
            tempValue |= processingFlag.value;
        }
        this.value = tempValue;
    }

    public boolean contains(ProcessingFlag flag) {
        return (flag.value & value) > 0;
    }

    public boolean contains(int flag) {
        return (value & flag) > 0;
    }

    public boolean isIn(ProcessingFlag flag) {
        return (flag.value & value) > 0;
    }

    public boolean isIn(int flag) {
        return (value & flag) > 0;
    }
}
