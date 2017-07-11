/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.RunMode
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.ResourceSetSize
import de.dkfz.roddy.config.*
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.plugins.LibrariesFactory
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

    static File folderOfPids

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

    @BeforeClass
    public static void initialize() {
        ExecutionService.initializeService(LocalExecutionService, RunMode.CLI)
        FileSystemAccessProvider.initializeProvider(true)
        folderOfPids = new File(LibrariesFactory.getGroovyClassLoader().getResource("resources/testpids").file)
    }

    public static Analysis getTestAnalysis() {
        return new Analysis("Test", new Project(null, null, null, null), null, new RuntimeService() {

            @Override
            File getInputFolderForAnalysis(Analysis analysis) {
                return folderOfPids
            }

            @Override
            File getOutputFolderForAnalysis(Analysis analysis) {
                return folderOfPids
            }

            @Override
            File getOutputFolderForDataSetAndAnalysis(DataSet dataSet, Analysis analysis) {
                return new File(folderOfPids, dataSet.id);
            }
        }, null);
    }

    @Test
    public void testGetTestAnalysis() {
        def a = getTestAnalysis()
        assert a.getInputBaseDirectory() == folderOfPids
        assert a.getOutputBaseDirectory() == folderOfPids
    }

    @Test
    public void getDefaultJobParameters() throws Exception {
        def context = MockupExecutionContextBuilder.createSimpleContext(RuntimeService);
        def result = new RuntimeService().getDefaultJobParameters(context, "aTool")
        assert result["pid"] == context.getDataSet().getId()
        assert result["PID"] == context.getDataSet().getId()
        assert result["CONFIG_FILE"]
        assert result["ANALYSIS_DIR"]
    }

    @Test
    public void testValidateDataSetLoadingString() {
        def rs = new RuntimeService()

        assert rs.validateCohortDataSetLoadingString("s[c:ADDD]")
        assert rs.validateCohortDataSetLoadingString("s[c:PID_0]")
        assert rs.validateCohortDataSetLoadingString("s[c:PID_0;PID_1;Pid-1]")
        assert rs.validateCohortDataSetLoadingString("s[c:PID_0;PID_1]")
        assert rs.validateCohortDataSetLoadingString("s[c:PID_0;PID_1;Pid-1|c:PID_0;Pid1]")
        assert rs.validateCohortDataSetLoadingString("s[c:PID_0;PID_1;Pid-1|c:PID_0;Pid1|c:PID-2;p?*]")

        assert !rs.validateCohortDataSetLoadingString("s[c:PID_0;]")
        assert !rs.validateCohortDataSetLoadingString("c:PID_0")
        assert !rs.validateCohortDataSetLoadingString("PID_0;PID_1")
    }

    @Test
    public void loadDatasetsWithFilter() throws Exception {

        Analysis a = getTestAnalysis()

        // Try good cases
        assert a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:ADDD]"]).size() == 1
        assert a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:ADDD;BDDD]"]).size() == 1
        assert a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:CD*]"]).size() == 1
        assert a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:*DDD]"]).size() == 1

        // Two super cohorts
        assert a.getRuntimeService().loadDatasetsWithFilter(a, "s[c:ADDD;BDDD|c:ADDD|c:CD*],s[c:ADDD;BDDD|c:ADDD|c:CD*]".split(StringConstants.SPLIT_COMMA) as List<String>).size() == 2

        // Check one result.
        List<SuperCohortDataSet> result = a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:ADDD;BDDD;ACCD]"]) as List<SuperCohortDataSet>
        assert result[0].allCohorts.size() == 1 && result[0].allCohorts[0] instanceof CohortDataSet
        assert result[0].allCohorts[0].allCohortDatasets.size() == 3
        assert result[0].allCohorts[0].primarySet.id == "ADDD" // Make sure, sort order is right!
        assert result[0].allCohorts[0].allCohortDatasets[1].id == "ACCD" // Make sure, sort order is right!
        assert result[0].allCohorts[0].allCohortDatasets[2].id == "BDDD" // Make sure, sort order is right!
    }

    @Test
    public void loadInproperDatasetsWithFilterAndFail() throws Exception {

        Analysis a = getTestAnalysis()

        // Try erroneous cases first.
        assert !a.getRuntimeService().loadDatasetsWithFilter(a, ["ADDD;ADDD"]) // Missing prefix
        assert !a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:ADDF]"]) // Missing directory
        assert !a.getRuntimeService().loadDatasetsWithFilter(a, ["c:ADDF"]) // Missing directory
        assert !a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:AE??;AE*]"]) // Missing directory

    }
}