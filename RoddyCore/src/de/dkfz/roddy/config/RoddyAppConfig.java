package de.dkfz.roddy.config;

import de.dkfz.roddy.RunMode;
import de.dkfz.roddy.tools.AppConfig;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;

import java.io.File;

public class RoddyAppConfig extends AppConfig {

    public RoddyAppConfig() {
        super();
    }

    public RoddyAppConfig(File file) {
        super(file);
    }

    public String getOrSetApplicationProperty(RunMode runMode, String pName) {
        return getOrSetApplicationProperty(runMode, pName, "");
    }

    public String getOrSetApplicationProperty(RunMode runMode, String pName, String defaultValue) {
        return getOrSetApplicationProperty(runMode.name() + "." + pName,
                getOrSetApplicationProperty(pName, defaultValue));
    }

    public boolean getOrSetBooleanApplicationProperty(String pName, boolean defaultValue) throws ConfigurationError {
        try {
            return Boolean.parseBoolean(getOrSetApplicationProperty(pName, String.valueOf(defaultValue), "boolean"));
        } catch (IllegalArgumentException e) {
            throw new ConfigurationError(e.getMessage(), pName, e);
        }
    }

    public void setApplicationProperty(RunMode runMode, String appPropertyExecutionServiceClass, String text) {
        setApplicationProperty(runMode.name() + "." + appPropertyExecutionServiceClass, text);
    }

    public String getOrSetApplicationProperty(String pName) {
        return getOrSetApplicationProperty(pName, "");
    }

    public String getOrSetApplicationProperty(String pName, String defaultValue) {
        try {
            return getOrSetApplicationProperty(pName, defaultValue, "string");
        } catch (ConfigurationError e) {
            throw new RuntimeException("type = 'string' should never throw ConfigurationError", e);
        }
    }

    public String getOrSetApplicationProperty(String pName, String defaultValue, String type) throws ConfigurationError {
        assert(null != pName);
        assert(null != type);
        String value;
        switch (type) {
            case "string":
                value = uncheckedGetOrSetApplicationProperty(pName, defaultValue);
                break;
            case "integer":
                if (!RoddyConversionHelperMethods.isInteger(defaultValue)) {
                    throw new ConfigurationError(String.format("The value for '%s' is not of type '%s'", pName, type), pName);
                }
                value = uncheckedGetOrSetApplicationProperty(pName, defaultValue);
                break;
            case "path":
                value = uncheckedGetOrSetApplicationProperty(pName, defaultValue);
                break;
            case "boolean":
                if (!RoddyConversionHelperMethods.isBoolean(defaultValue)) {
                    throw new RuntimeException("Trying to get boolean via getOrSetApplicationProperty with non-boolean defaultValue: " + defaultValue);
                }
                value = uncheckedGetOrSetApplicationProperty(pName, defaultValue);
                if (!RoddyConversionHelperMethods.isBoolean(value)) {
                    throw new ConfigurationError("applicationProperty value '" + value + "' cannot be interpreted as boolean", pName);
                }
                break;
            default:
                throw new RuntimeException("Unknown property value type: ${type}");
        }
        return value;
    }

    public void setApplicationProperty(String pName, String value) {
        setProperty(pName, value);
    }

    public String uncheckedGetOrSetApplicationProperty(String pName, String defaultValue) {

        // TODO Oops, getting an AppProp value with defaultValue has the side effect of setting it globally! Dangerous!
        if (!containsKey(pName))
            setProperty(pName, defaultValue);

        return getProperty(pName, defaultValue);
    }
}
