/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

/**
 * Created by heinold on 15.02.17.
 */
public class ToolStringParameter extends ToolEntry.ToolParameter<ToolStringParameter> {

    /**
     * How this parameter is filled.
     * Can be set by the calling code, it must really be done there!
     * Can also be set by a configuration value, this is done during the call.
     */
    public enum ParameterSetbyOptions {
        callingCode,
        configurationValue
    }

    /**
     * The original idea was that the value that was given to the job via the script-parameter name was provided in a configuration value.
     * As now all configuration values are provided as is, this option is obsolete.
     */
    @Deprecated
    private final String cValueID;

    private final ParameterSetbyOptions setby;

    /**
     * Call this, if the value is set in code.
     *
     * @param scriptParameterName
     */
    public ToolStringParameter(String scriptParameterName) {
        super(scriptParameterName);
        cValueID = null;
        setby = ParameterSetbyOptions.callingCode;
    }

    /**
     * Call this, if you have the id of a configuration value.
     *
     * @param scriptParameterName    Name of the parameter as communicated to the job via the environment
     * @param cValueID               Possible configuration value name to fill in the values from upon calling the job.
     */
    public ToolStringParameter(String scriptParameterName, String cValueID) {
        super(scriptParameterName);
        this.cValueID = cValueID;
        setby = ParameterSetbyOptions.configurationValue;
    }

    private ToolStringParameter(String scriptParameterName, String cValueID, ParameterSetbyOptions setby) {
        super(scriptParameterName);
        this.cValueID = cValueID;
        this.setby = setby;
    }

    @Override
    public ToolStringParameter clone() throws CloneNotSupportedException {
        return new ToolStringParameter(scriptParameterName, cValueID, setby);
    }

}
