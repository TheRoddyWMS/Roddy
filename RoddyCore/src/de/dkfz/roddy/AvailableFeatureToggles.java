package de.dkfz.roddy;

/**
 * A list of available feature toggles.
 * Will be modified e.g. if something new comes up or something is removed or finally kept and enabled.
 */
public enum AvailableFeatureToggles {

    XMLValidation(true),
    ForbidSubmissionOnRunning(false),
    BreakSubmissionOnError(false),
    RollbackOnSubmissionOnError(false),
    // Modify bash arrays and pass them with a different separator. (comma instead of whitespace)
    ModifiedVariablePassing(true),
    UseOldDataSetIDExtraction(true),
    AutoFilenames(false),
    UnzipZippedPlugins(false);

    public final boolean defaultValue;

    AvailableFeatureToggles(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }
}
