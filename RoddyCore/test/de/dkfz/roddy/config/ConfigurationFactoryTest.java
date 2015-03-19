package de.dkfz.roddy.config;

import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.client.RoddyStartupModes;
import de.dkfz.roddy.client.cliclient.RoddyCLIClient;
import de.dkfz.roddy.core.Analysis;
import de.dkfz.roddy.core.ExecutionContextLevel;
import de.dkfz.roddy.core.Project;
import de.dkfz.roddy.core.ProjectFactory;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

/**
 * Tests for ConfigurationFactory
 */
public class ConfigurationFactoryTest {
    @Test
    public void testGetAvailableWorkflows() throws Exception {
        assert (ConfigurationFactory.getInstance().getAvailableAnalysisConfigurations() != null);
        assert (ConfigurationFactory.getInstance().getAvailableProjectConfigurations() != null);
    }

    @Test
    public void testGetConfiguration() throws Exception {
        assert (ConfigurationFactory.getInstance().getConfiguration("default") != null);
        ProjectConfiguration prostate = (ProjectConfiguration)ConfigurationFactory.getInstance().getConfiguration("testProject");
        assert (prostate != null);
        assert (prostate.getImportConfigurations().size() >= 1);
        assert (prostate.getListOfAnalysisIDs().size() > 0);

    }

    @Test
    public void testValidateConfiguration() throws Exception {
//        Configuration cfg = ConfigurationFactory.getInstance().getConfiguration("default");
//        assert (ConfigurationFactory.getInstance().validateConfiguration(cfg) == true);
    }

    @Test
    public void testComplexConfigurationLoading() throws Exception {
        Roddy.performInitialSetup(new String[0], RoddyStartupModes.help);
        Roddy.loadPropertiesFile();
        Roddy.initializeServices(true);

        RoddyCLIClient.testrun(new String[]{"testrun", "testProject.sub_inherit@test", "A100"});
        ProjectConfiguration cfg = ConfigurationFactory.getInstance().getProjectConfiguration("testProject");

        assert cfg != null;
        assert cfg.getListOfAnalysisIDs().size() == 1;
        assert cfg.getListOfAnalysisIDs().get(0).equals("test");

        cfg = ConfigurationFactory.getInstance().getProjectConfiguration("testProject.sub_inherit");
//        AnalysisConfiguration acfg = ConfigurationFactory.getInstance().getAnalysisConfiguration("test");
        assert cfg.getListOfAnalysisIDs().size() == 1;
        Project project = ProjectFactory.getInstance().loadConfiguration(cfg);
        Analysis analysis = project.getAnalysis("test");

        File inputBaseDirectory = analysis.getInputBaseDirectory();
        File outputBaseDirectory = analysis.getOutputBaseDirectory();
        File outputAnalysisBaseDirectory = analysis.getOutputAnalysisBaseDirectory();
        analysis.getName();

//        assert cfg.getConfigurationValues().getString("testInheritanceValue").equals("string 0");
//        cfg = ConfigurationFactory.getInstance().getProjectConfiguration("testProject.testSubProject");
//        assert cfg != null;
//        assert cfg.getListOfAnalysisIDs().size() == 1;
//        AnalysisConfiguration test = cfg.getAnalysis("test");
//        assert test != null;
//        assert cfg.getConfigurationValues().getString("testInheritanceValue").equals("string 1");
    }

    @Test
    public void testConvertConfigurationToXML() throws Exception {
        ProjectConfiguration cfg = ConfigurationFactory.getInstance().getProjectConfiguration("testProject");
        ConfigurationFactory.getInstance().convertConfigurationToXML(cfg);
    }
//
//    @Test
//    public void testReadDefaultConfiguratonFile() throws Exception {
//        ConfigurationFactory.getInstance().readDefaultConfiguratonFile();
//    }
//
//    @Test
//    public void testReadUserConfigurationFile() throws Exception {
//        ConfigurationFactory.getInstance().readUserConfigurationFile(false);
//        ConfigurationFactory.getInstance().readUserConfigurationFile(true);
//    }
}
