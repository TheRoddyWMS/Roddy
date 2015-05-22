package de.dkfz.roddy;

/**
 * A list of available feature toggles.
 * Will be modified e.g. if something new comes up or something is removed or finally kept and enabled.
 */
public enum AvailableFeatureToggles {

    XMLValidation(true),
    BreakSubmissionOnError(false),
    RollbackOnSubmissionOnError(false);

    public final boolean defaultValue;

    AvailableFeatureToggles(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }
}
