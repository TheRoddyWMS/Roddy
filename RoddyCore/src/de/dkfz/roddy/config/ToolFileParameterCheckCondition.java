/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;

/**
 * The class defines a state for the check tag of tool output parameters
 * check can hold:
 * true                   - The value will be checked on rerun
 * false                  - The value will NOT be checked
 * conditionial:somevalue - The check is true or false depending on the somevalue named variable
 */
public class ToolFileParameterCheckCondition {

    private String condition;

    private Boolean booleanValue;

    public ToolFileParameterCheckCondition(boolean value) {
        this.booleanValue = Boolean.valueOf(value);
    }

    public ToolFileParameterCheckCondition(String condition) {
        if (condition.startsWith("conditional:"))
            this.condition = condition.substring(12);
        else {
            this.booleanValue = RoddyConversionHelperMethods.toBoolean(condition, true);
        }
    }

    public boolean isBoolean() {
        return booleanValue != null;
    }

    public String getCondition() {
        return condition;
    }

    public boolean evaluate(ExecutionContext context) {
        if (booleanValue != null)
            return booleanValue;
        return context.getConfiguration().getConfigurationValues().getBoolean(condition, true);
    }
}
