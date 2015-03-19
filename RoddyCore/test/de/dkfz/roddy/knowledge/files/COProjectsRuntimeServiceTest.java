//package de.dkfz.roddy.knowledge.files;
//
//import de.dkfz.roddy.Roddy;
//import de.dkfz.roddy.config.ConfigurationFactory;
//import de.dkfz.roddy.config.ProjectConfiguration;
//import de.dkfz.roddy.core.Analysis;
//import de.dkfz.roddy.core.DataSet;
//import de.dkfz.roddy.core.Project;
//import de.dkfz.roddy.core.ProjectFactory;
//import org.junit.Test;
//import pipelines.common.COProjectsRuntimeService;
//
//import java.util.List;
//
///**
// */
//public class COProjectsRuntimeServiceTest {
//
//    @Test
//    public void testLoadListOfInputDataSets() {
//        Roddy.initializeServices();
//        de.dkfz.roddy.knowledge.files.RuntimeService runtimeService = new COProjectsRuntimeService();
//        ProjectConfiguration prostate = (ProjectConfiguration) ConfigurationFactory.getInstance().getConfiguration("prostate");
//        Project project = ProjectFactory.getInstance().loadConfiguration(prostate);
//        Analysis analysis = project.getAnalyses().get(0);
//        List<DataSet> dataSets = runtimeService.loadListOfInputDataSets(analysis);
//        assert dataSets != null;
//        assert dataSets.size() > 0;
//    }
//}
