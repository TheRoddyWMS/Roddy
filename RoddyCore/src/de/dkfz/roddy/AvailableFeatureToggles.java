package de.dkfz.roddy;

/**
 * A list of available feature toggles.
 * Will be modified e.g. if something new comes up or something is removed or finally kept and enabled.
 */
public enum AvailableFeatureToggles {
    XMLValidation(true);

    public final boolean defaultValue;

    private AvailableFeatureToggles(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }
}
