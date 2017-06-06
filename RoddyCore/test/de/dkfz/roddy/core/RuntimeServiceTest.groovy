/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.config.ResourceSetSize
import de.dkfz.roddy.config.*
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic
import org.junit.BeforeClass
import org.junit.Test

import java.util.Map

/**
 * Created by heinold on 09.11.15.
 */
@CompileStatic
class RuntimeServiceTest {

    private static RuntimeService mockedService

    private static ExecutionContext mockedContext

    @BeforeClass
    static void setupClass() {
        // Mock a runtime service instance

        mockedService = new RuntimeService() {
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

        }

        final Configuration mockupConfig = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "test", "", "", null, "", ResourceSetSize.l, null, null, null, null))

        mockupConfig.getConfigurationValues().add(new ConfigurationValue(RuntimeService.RODDY_CENTRAL_EXECUTION_DIRECTORY, "/tmp/roddyCentralDirectory"))

        mockedContext = MockupExecutionContextBuilder.createSimpleContext(RuntimeServiceTest.class, mockupConfig, mockedService)

    }

    @Test
    void testGetCommonExecutionDirectory() throws Exception {
        assert mockedService.getCommonExecutionDirectory(mockedContext).getAbsolutePath() == "/tmp/roddyCentralDirectory"
    }

    @Test
    void testGetAnalysedMD5OverviewFile() throws Exception {
        assert mockedService.getAnalysedMD5OverviewFile(mockedContext).getAbsolutePath() == "/tmp/roddyCentralDirectory/zippedAnalysesMD5.txt"
    }
}