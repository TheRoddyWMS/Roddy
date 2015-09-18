//package de.dkfz.roddy.execution;
//
//import de.dkfz.roddy.Roddy;
//import de.dkfz.roddy.config.ConfigurationFactory;
//import de.dkfz.roddy.core.*;
//import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider;
//import org.junit.Test;
//
//import java.io.File;
//import java.util.Arrays;
//
///**
// */
//public class SSHExecutionServiceTest {
//    @Test
//    public void testSerializeObjectToTempFile() {
//        Roddy.loadPropertiesFile();
//        Roddy.initializeServices(false);
//        Project project = ProjectFactory.getInstance().loadConfiguration(ConfigurationFactory.getInstance().getProjectConfiguration("ICGCeval.forAppDebug"));
//        Analysis analysis = project.getAnalysis("genome");
//        analysis.run(Arrays.asList("*SANGER*"), ExecutionContextLevel.RUN);
//        String[] testString = new String[]{"abcdefg", "test1", "test2"};
//        File f = FileSystemInfoProvider.serializeObjectToTempFile(testString);
//        assert f != null;
//        Object o = FileSystemInfoProvider.deserializeObjectFromFile(f);
//        assert o != null;
//        o.toString().equals(testString);
//    }
//}
