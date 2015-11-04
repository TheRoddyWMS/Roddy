//package de.dkfz.roddy.core;
//
//import de.dkfz.roddy.config.AnalysisConfiguration;
//import de.dkfz.roddy.config.TestDataOption;
//
//import java.io.File;
//import java.util.List;
//import java.util.logging.Logger;
//
///**
//* A test data analysis encapsulates a productive analysis and extends that enable the creation and processing of test data.
//*/
//public class TestDataAnalysis extends Analysis {
//
//    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(TestDataAnalysis.class.getSimpleName());
//    private final Analysis parentProductiveAnalysis;
//    private final TestDataOption testDataOption;
//
//    private File inputBaseDirectory;
//
//    private File outputBaseDirectory;
//
//    private List<TestDataSet> listOfTestDataSets;
//
//    public TestDataAnalysis(Analysis parentProductiveAnalysis, TestDataOption testDataOption) {
//        super(parentProductiveAnalysis.getName(), parentProductiveAnalysis.getProject(), parentProductiveAnalysis.getWorkflow(), (AnalysisConfiguration) parentProductiveAnalysis.getConfiguration());
//        this.parentProductiveAnalysis = parentProductiveAnalysis;
//        this.testDataOption = testDataOption;
//    }
//
////    public String getName() {
////        return name;
////    }
////
////    public Project getProject() {
////        return project;
////    }
////
////    public Workflow getWorkflow() {
////        return workflow;
////    }
////
////    public Configuration getConfiguration() {
////        return configuration;
////    }
////
////    public Workflow createWorkflowInstance() {
////        return workflow;
////    }
//
//    /**
//     * Fetches information to the projects data sets and various analysis related additional information for those data sets.
//     * Retrieves the data sets for this analysis from it's project and appends the data for this analysis (if not already set).
//     *
//     * @return
//     */
//    public List<DataSet> getListOfDataSets() {
//        return parentProductiveAnalysis.getListOfDataSets();
//    }
//
////    @Override
////    public String toString() {
////        return name;
////    }
////
////    /**
////     * Returns the base input directory for this analysis object.
////     *
////     * @return
////     */
////    public File getInputBaseDirectory() {
////        if (inputBaseDirectory == null)
////            inputBaseDirectory = project.getRuntimeService().getInputFolderForAnalysis(this);
////        return inputBaseDirectory;
////    }
////
////    /**
////     * Returns the base output directory for this analysis object.
////     *
////     * @return
////     */
////    public File getOutputBaseDirectory() {
////        return project.getOutputBaseDirectory();
////    }
////
////    /**
////     * Returns the base output directory for this analysis object.
////     *
////     * @return
////     */
////    public File getOutputAnalysisBaseDirectory() {
////        if (outputBaseDirectory == null)
////            outputBaseDirectory = getConfiguration().getConfigurationValue(ConfigurationConstants.CFG_OUTPUT_ANALYSIS_BASE_DIRECTORY).toFile(this);
////        return outputBaseDirectory;
////    }
////
////    public DataSet getDataSet(String dataSetID) {
////        for (DataSet d : listOfAnalysisDataSets)
////            if (d.getId().equals(dataSetID))
////                return d;
////        return null;
////    }
////
////    public RuntimeService getRuntimeService() {
////        return project.getRuntimeService();
////    }
////
////    public List<ExecutionContext> context(List<String> pidFilters, ExecutionContext.ExecutionContextLevel runLevel) {
////        List<DataSet> listOfDataSets = getListOfDataSets();
////        List<ExecutionContext> runs = new LinkedList<ExecutionContext>();
////
////        WildcardFileFilter wff = new WildcardFileFilter(pidFilters);
////        for (DataSet ds : listOfDataSets) {
////            File inputFolder = ds.getInputFolderForAnalysis(this);
////            if (!wff.accept(inputFolder))
////                continue;
////            ExecutionContext ec = new ExecutionContext(this, ds, runLevel);
////            executeRun(runLevel, ec);
////            runs.add(ec);
////        }
////        return runs;
////    }
////
////
////    protected void executeRun(ExecutionContext.ExecutionContextLevel runLevel, ExecutionContext context) {
////        try {
////            try {
////                Roddy.getInstance().writeFilesForExecution(context);
////                context.execute();
////            } finally {
////                if (runLevel == ExecutionContext.ExecutionContextLevel.QUERY_STATUS) { //Clean up
//////                    Roddy.getInstance().removeDirectory(runtimeService.getExecutionDirectory(context));
////                } else {
////                    Roddy.getInstance().writeAdditionalFilesAfterExecution(context);
////                }
////            }
////        } catch (Exception e) {
////            logger.log(Level.SEVERE, e.toString());
////            for (Object o : e.getStackTrace()) {
////                logger.log(Level.SEVERE, o.toString());
////            }
////        }
////    }
//
//
//}
