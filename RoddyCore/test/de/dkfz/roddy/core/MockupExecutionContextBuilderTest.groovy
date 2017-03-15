/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic

/**
 * Created by heinold on 05.07.16.
 */
@CompileStatic
class MockupExecutionContextBuilderTest extends GroovyTestCase {

    void testGetTestBaseDirectory() {
        String testDir = MockupExecutionContextBuilder.getTestBaseDirectory(MockupExecutionContextBuilderTest.name).absolutePath
        assert testDir.startsWith("/tmp/" + MockupExecutionContextBuilder.DIR_PREFIX)
        assert testDir.endsWith("_" + MockupExecutionContextBuilderTest.name)
    }

    void testGetDirectory() {
        File file = MockupExecutionContextBuilder.getDirectory(MockupExecutionContextBuilderTest.class.name, "verification");
        assert file.exists() && file.name.endsWith("verification")
    }

    void testGetLoggingDirectory() {
        assert MockupExecutionContextBuilder.getTestLoggingDirectory(MockupExecutionContextBuilder.name).name == "logdir"
    }

    void testGetExecDirectory() {
        assert MockupExecutionContextBuilder.getTestExecutionDirectory(MockupExecutionContextBuilder.name).name == "exec_dir"
    }

    void testGetInputDirectory() {
        assert MockupExecutionContextBuilder.getTestInputDirectory(MockupExecutionContextBuilder.name).name == "input"
    }

    void testGetOutputDirectory() {
        assert MockupExecutionContextBuilder.getTestOutputDirectory(MockupExecutionContextBuilder.name).name == "output"
    }

    void testCreateSimpleRuntimeService() {
        def rs = MockupExecutionContextBuilder.createSimpleRuntimeService(MockupExecutionContextBuilderTest.class.name);
        // The following test will fail, because a new temporary file is created when accessing getTestLoggingDirectory.
        // We'll have to do that differently.
//        assert rs.getLoggingDirectory(null) == MockupExecutionContextBuilder.getTestLoggingDirectory(MockupExecutionContextBuilderTest.class.name)
        String logDir = rs.getLoggingDirectory(null).getAbsolutePath()
        assert logDir.startsWith("/tmp/RoddyTests_") && logDir.endsWith("_" + MockupExecutionContextBuilderTest.class.name + "/logdir")
    }

    void testCreateSimpleContextWithClass() {
        MockupExecutionContextBuilder.createSimpleContext(MockupExecutionContextBuilderTest);
    }

    void testCreateSimpleContextWithClassName() {
        MockupExecutionContextBuilder.createSimpleContext(MockupExecutionContextBuilderTest.name)
    }

    void testCreateSimpleContextWithClassNameAndNulls() {
        MockupExecutionContextBuilder.createSimpleContext(MockupExecutionContextBuilderTest.name, null, null)
    }

    void testCreateSimpleContextWithClassNameAndCustomConfig() {
        Configuration configuration = new Configuration(null);
        MockupExecutionContextBuilder.createSimpleContext(MockupExecutionContextBuilderTest.name, configuration);
    }

    void testCreateSimpleContextWithClassNameCustomConfigAndRuntimeService() {
        Configuration configuration = new Configuration(null);
        def rs = new RuntimeService() {
            @Override
            Map<String, Object> getDefaultJobParameters(ExecutionContext context, String TOOLID) {
                return null
            }

            @Override
            String createJobName(ExecutionContext executionContext, BaseFile file, String TOOLID, boolean reduceLevel) {
                return null
            }

            @Override
            boolean isFileValid(BaseFile baseFile) {
                return false
            }

            @Override
            void releaseCache() {

            }

            @Override
            boolean initialize() {
                return false
            }

            @Override
            void destroy() {

            }
        }
        MockupExecutionContextBuilder.createSimpleContext(MockupExecutionContextBuilderTest.name, configuration, rs);
    }

}
