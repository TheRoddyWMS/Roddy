//package de.dkfz.roddy.core;
//
//import de.dkfz.roddy.config.Configuration;
//import de.dkfz.roddy.config.ConfigurationFactory;
//import de.dkfz.roddy.config.ProjectConfiguration;
//import org.junit.Test;
//
//import java.io.File;
//
///**
// */
//public class ProjectFactoryTest {
//    @Test
//    public void testLoadConfiguration() throws Exception {
//        Configuration configuration = ConfigurationFactory.getInstance().getConfiguration("testProject");
//        Project project = ProjectFactory.getInstance().loadConfiguration((ProjectConfiguration)configuration);
//        assert (project != null);
//    }
//
//    @Test
//    public void testLoadComplexConfiguration() throws Exception {
//        Configuration configuration = ConfigurationFactory.getInstance().getConfiguration("testProject");
//        Project project = ProjectFactory.getInstance().loadConfiguration((ProjectConfiguration)configuration);
//        assert (project != null);
//        Analysis analysis = project.getAnalysis("test");
//        File outputAnalysisBaseDirectory = analysis.getOutputAnalysisBaseDirectory();
//        assert outputAnalysisBaseDirectory != null;
//        File inputBaseDirectory = analysis.getInputBaseDirectory();
//        assert inputBaseDirectory != null;
//        File outputBaseDirectory = analysis.getOutputBaseDirectory();
//        assert outputBaseDirectory != null;
//    }
//}
