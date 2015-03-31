package de.dkfz.roddy.client;

/**
 * Created by michael on 31.03.15.
 */
public class RoddyStartupModeScopes {

    /**
     * Use this scope to leave options for command line processing.
     * They will not be used in the Roddy binary!
     * Instead, they need a script file and are fully handled in the roddy.sh
     * This is useful, if things can be handled in an easier way with scripts,
     * like i.e. file operations
     */
    public static final int SCOPE_CLI = 0x00;

    /**
     * This scope is for startup modes where only a reduced init is necessary.
     */
    public static final int SCOPE_REDUCED = 0x10;

    /**
     * This scope is for modes with a full init.
     */
    public static final int SCOPE_FULL = 0x20;

}
