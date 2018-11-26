/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io.fs.commands;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Write a file to the storage system.
 */
public class StoreFileCommand extends IOCommand {

    /**
     * The data to write.
     */
    private byte[] data;

    /**
     * The target file to write to.
     */
    private File target;

    public StoreFileCommand(byte[] data, File target) {
        this.data = data;
        this.target = target;
    }
}
