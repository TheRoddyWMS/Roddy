package de.dkfz.roddy.core;

import de.dkfz.roddy.config.*;
import de.dkfz.roddy.knowledge.files.BaseFile;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

/**
 * Created by heinold on 09.11.15.
 */
public class RuntimeServiceTest {

    private static RuntimeService mockedService;

    private static ExecutionContext mockedContext;

    @BeforeClass
    public static void setupClass() {
        // Mock a runtime service instance

        mockedService = new RuntimeService() {
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
            public boolean initialize() {
                return false;
            }

            @Override
            public void destroy() {

            }
        };

        final Configuration mockupConfig = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "test", "", "", null, "", ResourceSetSize.l, null, null, null, null));

        mockupConfig.getConfigurationValues().add(new ConfigurationValue(RuntimeService.RODDY_CENTRAL_EXECUTION_DIRECTORY, "/tmp/roddyCentralDirectory"));

        mockedContext = MockupExecutionContextBuilder.createSimpleContext(RuntimeServiceTest.class, mockupConfig, mockedService);

    }

    @Test
    public void testGetCommonExecutionDirectory() throws Exception {
        assert mockedService.getCommonExecutionDirectory(mockedContext).getAbsolutePath().equals("/tmp/roddyCentralDirectory");
    }

    @Test
    public void testGetAnalysedMD5OverviewFile() throws Exception {
        assert mockedService.getAnalysedMD5OverviewFile(mockedContext).getAbsolutePath().equals("/tmp/roddyCentralDirectory/zippedAnalysesMD5.txt");
    }
}