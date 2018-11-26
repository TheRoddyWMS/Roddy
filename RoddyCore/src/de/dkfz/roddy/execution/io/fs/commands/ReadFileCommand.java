/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io.fs.commands;

import java.io.File;

/**
 * Read a file from the storage system.
 */
public class ReadFileCommand extends IOCommand {

    private File file;

    public ReadFileCommand(File file) {
        this.file = file;
    }
}
