/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.plugins

import org.junit.BeforeClass;
import org.junit.Test;
import de.dkfz.roddy.plugins.LibrariesFactory.*;

import static org.junit.Assert.*;

/**
 * Test class for the BuildInfoFile Helper class
 * Created by heinold on 01.03.16.
 */
public class BuildInfoFileHelperTest {

    public static BuildInfoFileHelper setupValidHelperObject() {
        List<String> entries = [
                LibrariesFactory.BUILDINFO_RUNTIME_APIVERSION + "=2.3",
                LibrariesFactory.BUILDINFO_RUNTIME_GROOVYVERSION + "=2.3",
                LibrariesFactory.BUILDINFO_RUNTIME_JDKVERSION + "=1.7",
                LibrariesFactory.BUILDINFO_DEPENDENCY + "=PluginBase:1.0.24",
                LibrariesFactory.BUILDINFO_DEPENDENCY + "=Invalid:1.x.24",
                "betfa=true", //Spell error
                LibrariesFactory.BUILDINFO_COMPATIBILITY + "=1.0.10"
        ]
        return new BuildInfoFileHelper("MasterMax", "1.0.11", entries);
    }

    @Test
    public void constructObjectWithNullInput() {
        BuildInfoFileHelper buildInfoFileHelper = new BuildInfoFileHelper("Maximaxi", "1.0.11", null);
    }

    @Test
    public void constructObject() {
        def object = setupValidHelperObject()
        assert object.betaPlugin == false; //Spelled wrong!
        assert object.getEntries().size() == 6;
        assert object.getEntries().containsKey(LibrariesFactory.BUILDINFO_RUNTIME_APIVERSION)
        assert object.getEntries().containsKey(LibrariesFactory.BUILDINFO_RUNTIME_GROOVYVERSION)
        assert object.getEntries().containsKey(LibrariesFactory.BUILDINFO_RUNTIME_JDKVERSION)
        assert object.getEntries().containsKey(LibrariesFactory.BUILDINFO_DEPENDENCY)
        assert object.getEntries().containsKey(LibrariesFactory.BUILDINFO_STATUS_BETA)
        assert object.getEntries().containsKey(LibrariesFactory.BUILDINFO_COMPATIBILITY)
    }

    @Test
    public void testGetDependencies() {
        assert setupValidHelperObject().getDependencies() == ["PluginBase": "1.0.24"]
    }

    @Test
    public void testIsCompatibleTo() {
        assert !setupValidHelperObject().isCompatibleTo(new PluginInfo("MasterMax", null, null, null, "1.0.10-0", "2.3", "1.8", "2.3", null));
        assert !setupValidHelperObject().isCompatibleTo(new PluginInfo("MasterMax", null, null, null, "1.0.9-0", "2.3", "1.7", "2.3", null));
        //Actually the following case will not be upcoming, because the PluginInfo objects are created with values from the BuildInfoHelper object
        //assert setupValidHelperObject().isCompatibleTo(new PluginInfo("MasterMax", null, null, null, "1.0.10-0", "2.3", "1.7", "2.4.5", null));
        assert setupValidHelperObject().isCompatibleTo(new PluginInfo("MasterMax", null, null, null, "1.0.10-0", "2.3", "1.7", "2.3", null));
    }

    @Test
    public void testCheckMatchingAPIVersions() {
        assert !setupValidHelperObject().checkMatchingAPIVersions(new PluginInfo("MasterMax", null, null, null, "1.0.10-0", "2.3", "1.8", "2.4.5", null));
        assert !setupValidHelperObject().checkMatchingAPIVersions(new PluginInfo("MasterMax", null, null, null, "1.0.10-0", "2.3", "1.7", "2.3.5", null));
        assert setupValidHelperObject().checkMatchingAPIVersions(new PluginInfo("MasterMax", null, null, null, "1.0.9-0", "2.3", "1.7", "2.3", null));
        //Actually the following case will not be upcoming, because the PluginInfo objects are created with values from the BuildInfoHelper object
        assert !setupValidHelperObject().checkMatchingAPIVersions(new PluginInfo("MasterMax", null, null, null, "1.0.9-0", "2.3", "1.7", "2.3.5", null));

    }

    @Test
    public void testGetJDKVersion() {
        assert setupValidHelperObject().getJDKVersion() == "1.7";
    }

    @Test
    public void testGetGroovyVersion() {
        assert setupValidHelperObject().getGroovyVersion() == "2.3";
    }

    @Test
    public void testGetRoddyAPIVersion() {
        assert setupValidHelperObject().getRoddyAPIVersion() == "2.3";
    }

    @Test
    public void testIsBetaPlugin() {
        assert setupValidHelperObject().isBetaPlugin() == false;
    }
}