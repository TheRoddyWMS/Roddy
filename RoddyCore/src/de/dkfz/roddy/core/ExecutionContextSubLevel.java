package de.dkfz.roddy.core;

/**
 * Detailed execution context level
 */
public enum ExecutionContextSubLevel {
    RUN_UNINITIALIZED("Context is not running"),
    RUN_SETUP_INIT("Primary writeConfigurationFile"),
    RUN_SETUP_COPY_CONFIG("Create and copy configuration"),
    RUN_SETUP_COPY_TOOLS("Copy tools and set access rights"),
    RUN_RUN("Execute workflow"),
    RUN_FINALIZE_CREATE_JOBFILES("Create job call files"),
    RUN_FINALIZE_CREATE_BINARYFILES("Create binary files");

    public final String message;

    private ExecutionContextSubLevel(String message) {
        this.message = message;
    }
}
