/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy;

/**
 * A list of available feature toggles.
 * Will be modified e.g. if something new comes up or something is removed or finally kept and enabled.
 */
public enum AvailableFeatureToggles {

    ForbidSubmissionOnRunning(false),
    BreakSubmissionOnError(false),
    QuoteSomeScalarConfigValues(true),
    UseDeclareFunctionalityForBashConverter(true);

    public final boolean defaultValue;

    AvailableFeatureToggles(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }
}
