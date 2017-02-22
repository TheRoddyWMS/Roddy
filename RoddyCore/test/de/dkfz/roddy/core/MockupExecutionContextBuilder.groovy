/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.config.AnalysisConfiguration;
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ProjectConfiguration
import de.dkfz.roddy.knowledge.files.BaseFile;

import java.io.File;
import java.util.Map;

/**
 * Created by heinold on 01.07.16.
 */
@groovy.transform.CompileStatic
public class MockupExecutionContextBuilder {

    public static final String DIR_PREFIX = "RoddyTests_";

    public static File getTestBaseDirectory(String testID) {
        final File testBaseDirectory = File.createTempDir(DIR_PREFIX, "_" + testID)
        testBaseDirectory.deleteOnExit();
        testBaseDirectory
    }

    public static File getDirectory(String testID, String id) {
        def file = new File(getTestBaseDirectory(testID), id)
        if(!file.exists()) file.mkdirs();
        file.deleteOnExit();
        return file;
    }

    public static File getTestLoggingDirectory(String testID) {
        return getDirectory(testID, "logdir");
    }

    public static File getTestExecutionDirectory(String testID) {
        return getDirectory(testID, "exec_dir");
    }

    public static File getTestOutputDirectory(String testID) {
        return getDirectory(testID, "output");
    }

    public static File getTestInputDirectory(String testID) {
        return getDirectory(testID, "input");
    }

    public static RuntimeService createSimpleRuntimeService(final String testClassName) {
        return new RuntimeService() {
            @Override
            public Map<String, Object> getDefaultJobParameters(ExecutionContext context, String TOOLID) {
                return null;
            }

            @Override
            public String createJobName(ExecutionContext executionContext, BaseFile file, String TOOLID, boolean reduceLevel) {
                return null;
            }

            @Override
            public boolean isFileValid(BaseFile baseFile) {
                return false;
            }

            @Override
            public void releaseCache() {

            }

            @Override
            File getLoggingDirectory(ExecutionContext context) {
                return getTestLoggingDirectory(testClassName)
            }

            @Override
            public boolean initialize() {
                return false;
            }

            @Override
            public void destroy() {

            }

        }
    }

    public static ExecutionContext createSimpleContext(final Class testClass, final Configuration testConfig = new Configuration(null), final RuntimeService testRuntimeService = createSimpleRuntimeService(testClass.name)) {
        return createSimpleContext(testClass.name, testConfig, testRuntimeService);
    }

    public static ExecutionContext createSimpleContext(final String testID, final Configuration testConfig = new Configuration(null), final RuntimeService testRuntimeService = createSimpleRuntimeService(testID)) {

        final File testInputDirectory = getTestInputDirectory(testID)
        final File testOutputDirectory = getTestOutputDirectory(testID)
        final File testExecutionDirectory = getTestExecutionDirectory(testID)
        final File testLoggingDirectory = getTestLoggingDirectory(testID)

        for (File f : [testInputDirectory, testOutputDirectory, testExecutionDirectory, testLoggingDirectory]) {
            if (!f.exists()) f.mkdirs();
        }
        final Project project = new Project(new ProjectConfiguration(null, null, null, testConfig), testRuntimeService, null, null) {
            @Override
            String getName() {
                return "TestProject";
            }
        }
        final Analysis analysis = new Analysis("Test", project, null, null, new AnalysisConfiguration(null, "", "", null, null, null, null))

        return new ExecutionContext(System.getProperty("user.name"), analysis, null, ExecutionContextLevel.UNSET, testOutputDirectory, testInputDirectory, testExecutionDirectory, System.nanoTime(), true) {
            @Override
            public Configuration getConfiguration() {
                return testConfig;
            }

            @Override
            public RuntimeService getRuntimeService() {
                return testRuntimeService;
            }

            @Override
            DataSet getDataSet() {
                DataSet ds = new DataSet(getAnalysis(), "TEST_PID", new File(getOutputDirectory(), "TEST_PID"));
                return ds;
            }

            @Override
            Map<String, Object> getDefaultJobParameters(String TOOLID) {
                return [:];
            }

            @Override
            File getLoggingDirectory() {
                return testLoggingDirectory
            }

            @Override
            public String toString() {
                return "TestContext";
            }
        };
    }

}
