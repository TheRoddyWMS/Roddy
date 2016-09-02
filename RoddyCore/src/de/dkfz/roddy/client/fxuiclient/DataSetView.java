/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient;

import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import de.dkfz.roddy.execution.jobs.JobManager;
import de.dkfz.roddy.tools.RoddyIOHelperMethods;
import de.dkfz.roddy.core.*;
import de.dkfz.roddy.execution.io.ExecutionHelper;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.execution.jobs.ReadOutJob;
import de.dkfz.roddy.client.fxuiclient.fxcontrols.ExecutionContextOverviewControl;
import de.dkfz.roddy.client.fxuiclient.fxcontrols.JobOverviewControl;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXAnalysisProcessingInformationWrapper;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXFileWrapper;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXJobWrapper;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXWorkflowFileWrapper;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.GenericListViewItemCellImplementation;
import de.dkfz.roddy.knowledge.files.BaseFile;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;


/**
 * Controller for the LogViewer component.
 * Registers to Roddys log stuff and create logging tabs for each logger.
 */
@groovy.transform.CompileStatic
public class DataSetView extends CustomControlOnBorderPane implements Initializable, ExecutionContextListener, DataSetListener {


    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(DataSetView.class.getSimpleName());

    @FXML
    private ListView lstRuns;

    @FXML
    private ListView lstJobsOfRun;

    @FXML
    private ListView lstAdditionalLogFiles;

    @FXML
    private TableView lstFilesCreatedByWorkflow;

    @FXML
    private WebView txtAdditionalLogFile;

    @FXML
    private JobOverviewControl jobOverview;

    @FXML
    private Accordion accordion;

    @FXML
    private ExecutionContextOverviewControl executionContextOverview;

    @FXML
    private TitledPane tpExecutionContexts;

    @FXML
    private TitledPane tpSubmittedJob;

    @FXML
    private TitledPane tpECFilterSettings;

    @FXML
    private TitledPane tpJobFilterSettings;

    @FXML
    private TitledPane tpJobSettings;

    @FXML
    private TabPane tabpaneECContents;

    @FXML
    private Tab tabJobDetails;

    @FXML
    private Image fileIsMissing;

    @FXML
    private Image fileIsTemporary;

    private DataSet dataSet;
    private ExecutionContext currentContext;
    private FXJobWrapper currentSelectedJob;
    private boolean initialThreadIsRunning = true;

    public DataSetView(final Analysis analysis, final DataSet ds) {
        this.dataSet = ds;
        ds.addListener(this);
        loadAPIList(analysis, ds);
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lstRuns.setCellFactory(listView -> new GenericListViewItemCellImplementation());
        lstRuns.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
            selectedExecutionContextChanged((FXAnalysisProcessingInformationWrapper) newValue);
            accordion.setExpandedPane(tpSubmittedJob);
        });
        tpSubmittedJob.expandedProperty().addListener((observable, oldVal, newVal) -> tpExecutionContexts.setExpanded(!newVal));
        lstJobsOfRun.setCellFactory(listView -> new GenericListViewItemCellImplementation());
        lstJobsOfRun.getSelectionModel().selectedItemProperty().addListener((observableValue, o, o2) -> selectedJobChanged((FXJobWrapper) o2));
        lstAdditionalLogFiles.getSelectionModel().selectedItemProperty().addListener((observableValue, o, o2) -> selectedLogFileInAllLogFileListChanged((FXFileWrapper) o2));
        lstFilesCreatedByWorkflow.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> selectedWorkflowFileEntryChanged((FXWorkflowFileWrapper) newValue));

        ((TableColumn) lstFilesCreatedByWorkflow.getColumns().get(0)).setCellFactory(new TableCellIconCallBack(fileIsMissing));

        ((TableColumn) lstFilesCreatedByWorkflow.getColumns().get(1)).setCellFactory(new TableCellIconCallBack(fileIsTemporary));
    }


    private void loadAPIList(final Analysis analysis, final DataSet ds) {
        RoddyUITask.runTask(new RoddyUITask<List<AnalysisProcessingInformation>>("load list of api's") {
            List<AnalysisProcessingInformation> processingInformation;

            @Override
            public List<AnalysisProcessingInformation> _call() throws Exception {
                processingInformation = ds.getProcessingInformation(analysis);
                if (ds.getDummyAnalysisProcessingInformation(analysis) == null && ds.getActiveAnalysisProcessingInformation(analysis) == null)
                    analysis.run(Arrays.asList(ds.getId()), ExecutionContextLevel.QUERY_STATUS);


                processingInformation = ds.getProcessingInformation(analysis);
                for (AnalysisProcessingInformation api : processingInformation) {
                    api.getDetailedProcessingInfo();
                }
//                processingInformation.add(0, );
                initialThreadIsRunning = false;
                return processingInformation;
            }

            @Override
            public void _succeeded() {
                if (ds.hasDummyAnalysisProcessingInformation(analysis))
                    lstRuns.getItems().add(new FXAnalysisProcessingInformationWrapper(ds.getDummyAnalysisProcessingInformation(analysis)));
                if (ds.hasPlannedOrRunningAnalysisProcessingInformation(analysis))
                    lstRuns.getItems().add(new FXAnalysisProcessingInformationWrapper(ds.getActiveAnalysisProcessingInformation(analysis)));
                for (AnalysisProcessingInformation api : processingInformation) {

                    lstRuns.getItems().add(new FXAnalysisProcessingInformationWrapper(api));
                    Thread.yield();
                }
            }
        });
    }

    private void loadAdditionalLogFiles() {
        RoddyUITask.runTask(new RoddyUITask<List<File>>("load additional log files") {

            private List<File> files;

            @Override
            public List<File> _call() throws Exception {
                try {
                    files = currentContext.getAdditionalLogFiles();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
                return files;
            }

            @Override
            public void _succeeded() {
                lstAdditionalLogFiles.getItems().clear();
                if (files == null) return;
                for (File f : files) {
                    lstAdditionalLogFiles.getItems().add(new FXFileWrapper(f));
                }
            }
        });
    }

    @Override
    public void newExecutionContextEvent(ExecutionContext context) {

    }

    @Override
    public void jobStateChangedEvent(Job job) {
    }

    @Override
    public void jobAddedEvent(Job job) {
        if(job.isFakeJob())
            return;
        final FXJobWrapper jobWrapper = new FXJobWrapper(job);
        synchronized (allJobWrappersForEC) {
            allJobWrappersForEC.add(jobWrapper);
        }
        RoddyUITask.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    jobStateVisibilityChanged(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Job added", false);
    }

    @Override
    public void fileAddedEvent(File file) {
    }

    @Override
    public void detailedExecutionContextLevelChanged(ExecutionContext context) {
    }

    @Override
    public void processingInfoAddedEvent(DataSet dataSet, final AnalysisProcessingInformation pi) {
        if (initialThreadIsRunning) return;
        synchronized (lstRuns) {
            ObservableList items = lstRuns.getItems();
            for (Object o : items) {
                FXAnalysisProcessingInformationWrapper wrapper = (FXAnalysisProcessingInformationWrapper) o;
                pi.getDetailedProcessingInfo().registerContextListener(this);
                if (wrapper.getAnalysisProcessingInformation() != pi)
                    continue;
                return;
            }
            RoddyUITask.invokeLater(new Runnable() {
                @Override
                public void run() {
                    lstRuns.getItems().add(0, new FXAnalysisProcessingInformationWrapper(pi));
                    lstRuns.getSelectionModel().select(0);
                }
            }, UIConstants.UIINVOKE_APPEND_PROCESSINGINFO, false);
        }
    }

    @Override
    public void processingInfoRemovedEvent(DataSet dataSet, AnalysisProcessingInformation pi) {
        FXAnalysisProcessingInformationWrapper wrapperToRemove = null;
        synchronized (lstRuns) {
            ObservableList items = lstRuns.getItems();
            for (Object o : items) {
                FXAnalysisProcessingInformationWrapper wrapper = (FXAnalysisProcessingInformationWrapper) o;
                if (wrapper.getAnalysisProcessingInformation() != pi) continue;
                wrapperToRemove = wrapper;
            }
        }
        if (wrapperToRemove == null) return;
        final FXAnalysisProcessingInformationWrapper _wrapperToRemove = wrapperToRemove;
        RoddyUITask.invokeLater(new Runnable() {
            @Override
            public void run() {
                synchronized (lstRuns) {
                    lstRuns.getItems().remove(_wrapperToRemove);
                }
            }
        }, "remove api wrapper", false);
    }

    /**
     * Calls the Command factory and tells it to stop all the processing for the current pid.
     *
     * @param actionEvent
     */
    public void stopPIDProcessing(ActionEvent actionEvent) {
        currentContext.abortJobSubmission();
        JobManager.getInstance().queryJobAbortion(currentContext.getExecutedJobs());
    }

    /**
     * Just stops job submission
     *
     * @param actionEvent
     */
    public void stopJobSubmission(ActionEvent actionEvent) {
        currentContext.abortJobSubmission();
    }

//    public void resubmitSingleJob(ActionEvent actionEvent) {
//
//    }

    public static class LoadFileToTextAreaTask extends RoddyUITask<String> {
        private String finalString;
        private WebView textArea;
        private HTMLEditor htmlTextArea;
        private File file;
        private File resultFile;
        private Job job;

        public LoadFileToTextAreaTask(HTMLEditor textArea, File file) {
            super("load file to html editor");
            this.htmlTextArea = textArea;
            this.file = file;
        }

        public LoadFileToTextAreaTask(WebView textArea, File file) {
            super("load file to text area");
            this.textArea = textArea;
            this.file = file;
        }

        public LoadFileToTextAreaTask(WebView textArea, Job job) {
            super("load job to text area.");
            this.textArea = textArea;
            if (job.hasLogFile()) {
                this.file = job.getLogFile();
            } else {
                if (job.getJobState().isRunning()) {
                    this.job = job;
                }
            }
        }

        @Override
        protected String _call() throws Exception {
            String[] strings = new String[0];
            if (file != null) {
                if (file.exists())
                    strings = RoddyIOHelperMethods.loadTextFile(file);
                else
                    strings = FileSystemAccessProvider.getInstance().loadTextFile(file);
            } else if (job != null) {
                strings = JobManager.getInstance().peekLogFile(job);
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : strings) {
                stringBuilder.append(s);
                stringBuilder.append(System.lineSeparator());
            }
            finalString = stringBuilder.toString();

            File f = File.createTempFile("roddy", "syntaxhiliter.html");
            File dest = File.createTempFile("roddy", "syntaxhiliter_result.html");
            FileWriter fw = new FileWriter(f);
            fw.write(finalString);
            fw.flush();
            fw.close();

            //TODO Put this to a config file.
            String cmd = "dist/tools/linux_x64/highlight/highlight -I -i " + f.getAbsolutePath() + " -D dist/tools/linux_x64/highlight -S Bash -o " + dest.getAbsolutePath();

            try {
                String temp = ExecutionHelper.execute(cmd);
                if (temp.trim().length() > 0)
                    finalString = temp;
                resultFile = dest;
            } catch (Exception e) {
                finalString = "<html><body contenteditable='true'>" + finalString.replace("[" + System.lineSeparator() + "]", "<br />") + "</body></html>";
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            return finalString;
        }

        @Override
        public void _succeeded() {
            try {
                if (resultFile != null) {
                    File tgtFile = new File(resultFile.getParent() + File.separator + "highlight.css");
                    if (!tgtFile.exists()) {
                        File srcFile = new File("dist/tools/linux_x64/highlight/highlight.css");
                        Files.copy(srcFile.toPath(), tgtFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    try {
                        if (textArea != null) {
                            textArea.getEngine().load("file://localhost" + resultFile.getAbsolutePath());

                            Set<Node> nodes = textArea.lookupAll(".scroll-bar");

                            for (Node node : nodes) {
                                if (ScrollBar.class.isInstance(node)) {
                                    ScrollBar scroll = (ScrollBar) node;
                                    double scrollPos = scroll.getValue();
                                    scroll.setValue(Double.MAX_VALUE);
                                }
                            }
                        } else if (htmlTextArea != null) {
                            String htmlText = RoddyIOHelperMethods.loadTextFileEnblock(resultFile);
                            System.out.println(htmlText);
                            htmlTextArea.setHtmlText(htmlText);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            } catch (IOException e) {
                try {
                    textArea.getEngine().loadContent(finalString);
                } catch (Exception e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        /**
         * Load the contents of a file to a webview
         *
         * @param ta
         * @param f
         */
        public static final void createAndExecute(WebView ta, File f) {
            (new Thread(new LoadFileToTextAreaTask(ta, f), "Load file to webview")).start();
        }

        /**
         * Load a log file from a job to a webview
         *
         * @param ta
         * @param job
         */
        public static final void createAndExecute(WebView ta, Job job) {
            (new Thread(new LoadFileToTextAreaTask(ta, job), "Load job to webview")).start();
        }
    }

    private void selectedExecutionContextChanged(FXAnalysisProcessingInformationWrapper fxapi) {
        final AnalysisProcessingInformation api = fxapi.getAnalysisProcessingInformation();
        lstFilesCreatedByWorkflow.getItems().clear();
        final ExecutionContext ec = api.getDetailedProcessingInfo();
        executionContextOverview.setContext(ec);
        currentSelectedJob = null;
        currentContext = ec;

        loadFilesCreatedByWorkflow(ec);

        clearJobList();
        for (Job job : ec.getExecutedJobs()) {
            if(job instanceof Job.FakeJob || job.isFakeJob())
                continue; //Ignore fake jobs.
            synchronized (allJobWrappersForEC) {
                allJobWrappersForEC.add(new FXJobWrapper(job));
            }
        }

//        addMissingJobToList();
        jobStateVisibilityChanged(0);
        loadAdditionalLogFiles();
    }

    private void selectedWorkflowFileEntryChanged(FXWorkflowFileWrapper jobWrapper) {
        if (jobWrapper == null) {
            logger.warning("No FXJobWrapper object for selectedJobChanged()");
            return;
        }
        Job job = jobWrapper.getJob();
        if (job == null) {
            logger.warning("No Job object for selectedJobChanged()");
            return;
        }
        for (Object _jobWrapper : lstJobsOfRun.getItems()) {
            FXJobWrapper jWrapper = (FXJobWrapper) _jobWrapper;
            if (jWrapper.getJob() != job)
                continue;
            lstJobsOfRun.getSelectionModel().select(jWrapper);
            break;
        }
    }

    private void loadFilesCreatedByWorkflow(final ExecutionContext ec) {
        RoddyUITask.runTask(new RoddyUITask<List<FXWorkflowFileWrapper>>("load files created upon workflow execution") {
            List<FXWorkflowFileWrapper> lstOfFiles = new LinkedList<FXWorkflowFileWrapper>();

            @Override
            protected List<FXWorkflowFileWrapper> _call() throws Exception {
                for (Job job : ec.getExecutedJobs()) {
                    for (BaseFile file : job.getFilesToVerify()) {
                        lstOfFiles.add(new FXWorkflowFileWrapper(file, job));
                    }
                }
                return lstOfFiles;
            }

            @Override
            protected void _succeeded() {
                lstFilesCreatedByWorkflow.getItems().addAll(lstOfFiles);
            }
        });
    }

    /**
     * Removes all jobs from the listview and from other lists which contain references to a job.
     */
    private void clearJobList() {
        allJobWrappersForEC.clear();
        lstJobsOfRun.getItems().clear();
    }

    private List<FXJobWrapper> allJobWrappersForEC = new LinkedList<>();

//    private void addMissingJobToList() {
//        List<FXJobWrapper> listOfExistingJobs;
//        synchronized (lstJobsOfRun) {
//            listOfExistingJobs = new LinkedList<FXJobWrapper>(lstJobsOfRun.getItems());
//        }
//
//
//    }

    private void jobStateVisibilityChanged(int index) {
        synchronized (allJobWrappersForEC) {
            lstJobsOfRun.getItems().clear();

            JobState jState = null;
            if (index == 0) { //View all
                for (FXJobWrapper job : allJobWrappersForEC) {
//            for (Job job : currentContext.getExecutedJobs()) {
                    lstJobsOfRun.getItems().add(job);
                }
                return;
            } else if (index == 1) { //View running
                for (FXJobWrapper jobWrapper : allJobWrappersForEC) {
//            for (Job job : currentContext.getExecutedJobs()) {
                    if (jobWrapper.getJob().getJobState().isPlannedOrRunning()) {
                        lstJobsOfRun.getItems().add(jobWrapper);
                    }
                }
                return;
            } else if (index == 2) { //View unknown
                jState = JobState.UNKNOWN;
            } else if (index == 3) { //View with errors
                jState = JobState.FAILED;
            } else if (index == 4) { //View ok
                jState = JobState.OK;
            }
            for (FXJobWrapper jobWrapper : allJobWrappersForEC) {
//          for (Job job : currentContext.getExecutedJobs()) {
                Job job = jobWrapper.getJob();
                if (job instanceof ReadOutJob && job.getJobState() == jState) {
                    lstJobsOfRun.getItems().add(jobWrapper);
                }
            }
        }
    }

    private void selectedJobChanged(FXJobWrapper jobWrapper) {
        try {

//            txtClusterLogFile.getEngine().loadContent("<html><body>No file available.</body></html>");
            currentSelectedJob = jobWrapper;
            if (jobWrapper == null) {
                logger.warning("No FXJobWrapper object for selectedJobChanged()");
                return;
            }
            Job job = jobWrapper.getJob();
            if (job == null) {
                logger.warning("No Job object for selectedJobChanged()");
                return;
            }
//            LoadFileToTextAreaTask.createAndExecute(txtClusterLogFile, job);
            jobOverview.setJob(job);
            tabpaneECContents.getSelectionModel().select(tabJobDetails);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void selectedLogFileInAllLogFileListChanged(FXFileWrapper wrapper) {
        try {
            txtAdditionalLogFile.getEngine().loadContent("<html><body>No file available.</body></html>");
            LoadFileToTextAreaTask.createAndExecute(txtAdditionalLogFile, wrapper.getFile());
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void dependencyJobClicked(ActionEvent actionEvent) {
        FXJobWrapper selectedWrapper = null;
        for (Object o : lstJobsOfRun.getItems()) {
            FXJobWrapper fjw = (FXJobWrapper) o;
            if (((FXJobWrapper) o).getJob() != actionEvent.getSource())
                continue;
            selectedWrapper = fjw;
            break;
        }

        if (selectedWrapper != null)
            lstJobsOfRun.getSelectionModel().select(selectedWrapper);
    }

    private void resetJobState(Job job, JobState state) {
        job.setJobState(state);
        JobManager.getInstance().storeJobStateInfo(job);
    }

    public void resetJobState_OK(ActionEvent actionEvent) {
        resetJobState(currentSelectedJob.getJob(), JobState.OK);
    }

    public void resetJobState_FAILED(ActionEvent actionEvent) {
        resetJobState(currentSelectedJob.getJob(), JobState.FAILED);
    }

    public void resetJobState_UNPROCESSED(ActionEvent actionEvent) {
        resetJobState(currentSelectedJob.getJob(), JobState.UNSTARTED);
    }

    public void resetJobState_ABORTED(ActionEvent actionEvent) {
        resetJobState(currentSelectedJob.getJob(), JobState.ABORTED);
    }
}
