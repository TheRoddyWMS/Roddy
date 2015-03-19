package de.dkfz.roddy.execution.io.fs.commands;

/**
 * This is the basic command for file based io operations like copy, read, write
 * This is necessary to separate the execution service from the file system service as there should not be file operations in the execution service.
 */
public abstract class IOCommand {

}
