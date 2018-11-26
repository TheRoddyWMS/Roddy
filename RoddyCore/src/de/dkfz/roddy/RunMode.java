/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy;

/**
 * Run modes for Roddy
 * Currently there are UI (Graphical user interface)
 * and CLI (Command line interface)
 * Created by heinold on 10.11.15.
 */
public enum RunMode {
    UI(false, false),
    CLI(true, false),
    RMI(true, true);

    private boolean isCommandLineMode;

    private boolean isDetachedMode;

    RunMode(boolean isCommandLineMode, boolean isDetachedMode) {
        this.isCommandLineMode = isCommandLineMode;
    }

    public boolean isCommandLineMode() {
        return isCommandLineMode;
    }

    public boolean isDetachedMode() {
        return isDetachedMode;
    }
}
