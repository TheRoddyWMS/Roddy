package de.dkfz.roddy.core;

/**
 */
public enum TriState {
    TRUE(true),
    FALSE(false),
    UNKNOWN(false);
    private final boolean defaultsTo;

    private TriState(boolean defaultsTo) {

        this.defaultsTo = defaultsTo;
    }

    public boolean toBoolean() {
        return defaultsTo;
    }
}
