/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.Constants
import de.dkfz.roddy.RunMode
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.execution.BindSpec
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import groovy.transform.CompileStatic
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

import java.nio.file.Paths

/**
 * Created by heinold on 01.02.17.
 */
@CompileStatic
class ExecutionContextTest {

    @Rule
    final public ContextResource contextResource = new ContextResource()

    private ExecutionContext createEmptyContext() {
        contextResource.createSimpleContext(this.getClass())
    }

    @BeforeClass
    static void setup() {
        FileSystemAccessProvider.initializeProvider(true)
        ExecutionService.initializeService(LocalExecutionService.class, RunMode.CLI)
    }

    @Test
    void valueIsEmpty() throws Exception {
        def context = createEmptyContext()
        assert context.valueIsEmpty(null, "var")
        assert context.getErrors().size() == 1
        assert !context.valueIsEmpty(false, "var")
    }

    @Test
    void fileIsAccessible() throws Exception {
        File f = File.createTempFile("roddy", "fileIsAccessibleTest")
        f << "abc"
        def context = createEmptyContext()
        assert context.fileIsAccessible(f, "var")
        assert !context.fileIsAccessible(new File(f.getAbsolutePath() + "_abc"), "var")
        assert context.getErrors().size() == 1

    }

    @Test
    void directoryIsAccessible() throws Exception {
        File f = File.createTempFile("roddy", "fileIsAccessibleTest")
        f << "abc"
        def context = createEmptyContext()
        assert context.directoryIsAccessible(f.parentFile, "var")
        assert !context.directoryIsAccessible(new File(f.parent + "_invalidDirectory"), "var")
        assert context.getErrors().size() == 1
    }

    @Test
    void roddyScratchBaseDirectory() throws Exception {
        def context = createEmptyContext()
        String testScratch = "/testScratch"
        context.configurationValues.add(new ConfigurationValue(Constants.APP_PROPERTY_SCRATCH_BASE_DIRECTORY,
                                                               testScratch))
        assert context.roddyScratchBaseDir == new File(testScratch)
    }

    @Test
    void roddyBindSpecs() throws Exception {
        def context = createEmptyContext()
        String testScratch = "/testScratch"
        context.configurationValues.add(new ConfigurationValue(Constants.APP_PROPERTY_SCRATCH_BASE_DIRECTORY,
                                                               testScratch))
        String outBase = "/outBase"
        context.configurationValues.add(new ConfigurationValue(ConfigurationConstants.CFG_OUTPUT_BASE_DIRECTORY,
                                                               outBase))
        String inBase = "/inBase"
        context.configurationValues.add(new ConfigurationValue(ConfigurationConstants.CFG_INPUT_BASE_DIRECTORY,
                                                               inBase))

        List<BindSpec> roddyMounts = context.roddyMounts

        assert roddyMounts.size() == 4

        assert roddyMounts[0].hostPath == Paths.get(outBase)
        assert roddyMounts[0].containerPath == Paths.get(outBase)
        assert roddyMounts[0].mode == BindSpec.Mode.RW

        assert roddyMounts[1].hostPath == Paths.get(inBase)
        assert roddyMounts[1].containerPath == Paths.get(inBase)
        assert roddyMounts[1].mode == BindSpec.Mode.RO

        assert roddyMounts[2].hostPath == Paths.get(testScratch)
        assert roddyMounts[2].containerPath == Paths.get(testScratch)
        assert roddyMounts[2].mode == BindSpec.Mode.RW

        // In this test, the value comes from createSimpleRuntimeService.
        assert roddyMounts[3].hostPath == Paths.get("/some/analysisToolsDirectory")
        assert roddyMounts[3].containerPath == Paths.get("/some/analysisToolsDirectory")
        assert roddyMounts[3].mode == BindSpec.Mode.RO
    }

    @Test
    void userBindSpecs() throws Exception {
        def context = createEmptyContext()
        String testScratch = "/testScratch"
        context.configurationValues.add(new ConfigurationValue(Constants.APP_PROPERTY_SCRATCH_BASE_DIRECTORY,
                                                               testScratch))
        String outBase = "/outBase"
        context.configurationValues.add(new ConfigurationValue(ConfigurationConstants.CFG_OUTPUT_BASE_DIRECTORY,
                                                               outBase))
        String inBase = "/inBase"
        context.configurationValues.add(new ConfigurationValue(ConfigurationConstants.CFG_INPUT_BASE_DIRECTORY,
                                                               inBase))

        context.configurationValues.add(new ConfigurationValue(ConfigurationConstants.CVALUE_CONTAINER_MOUNTS,
                                                               "(/hostPathA /hostPathB:/containerPathB /hostPathC:/containerPathC:rw)",
                                                               "bashArray"))

        List<BindSpec> userMounts = context.userContainerMounts

        assert userMounts.size() == 3

        assert userMounts[0].hostPath == Paths.get("/hostPathA")
        assert userMounts[0].containerPath == Paths.get("/hostPathA")
        assert userMounts[0].mode == BindSpec.Mode.RO

        assert userMounts[1].hostPath == Paths.get("/hostPathB")
        assert userMounts[1].containerPath == Paths.get("/containerPathB")
        assert userMounts[1].mode == BindSpec.Mode.RO

        assert userMounts[2].hostPath == Paths.get("/hostPathC")
        assert userMounts[2].containerPath == Paths.get("/containerPathC")
        assert userMounts[2].mode == BindSpec.Mode.RW
    }

    @Test
    void fileAccessRetryDefaults() {
        ExecutionContext context = createEmptyContext()

        assert context.maxFileAppearanceAttempts == 3
        assert context.fileAppearanceRetryWaitTimeMS == 100
    }

    @Test
    void fileAccessRetryCustomConfig() {
        ExecutionContext context = createEmptyContext()
        context.configurationValues.add(
                new ConfigurationValue(ConfigurationConstants.MAX_FILE_APPEARANCE_ATTEMPTS, "5"))
        context.configurationValues.add(
                new ConfigurationValue(ConfigurationConstants.CFG_FILE_APPEARANCE_RETRY_WAIT_TIME_MS, "250"))

        assert context.maxFileAppearanceAttempts == 5
        assert context.fileAppearanceRetryWaitTimeMS == 250
    }

}
