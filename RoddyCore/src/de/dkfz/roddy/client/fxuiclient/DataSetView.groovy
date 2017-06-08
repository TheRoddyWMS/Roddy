/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient

import de.dkfz.roddy.execution.jobs.FakeBEJob
import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.*;
import de.dkfz.roddy.client.rmiclient.RoddyRMIClientConnection;
import de.dkfz.roddy.client.rmiclient.RoddyRMIInterfaceImplementation
import de.dkfz.roddy.client.rmiclient.RoddyRMIInterfaceImplementation.ExecutionContextInfoObject;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import de.dkfz.roddy.core.*;
import de.dkfz.roddy.execution.io.ExecutionHelper;
import de.dkfz.roddy.execution.jobs.BEJob
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.roddy.execution.jobs.ReadOutJob;
import de.dkfz.roddy.client.fxuiclient.fxcontrols.ExecutionContextOverviewControl;
import de.dkfz.roddy.client.fxuiclient.fxcontrols.JobOverviewControl;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.GenericListViewItemCellImplementation;
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import groovy.transform.CompileStatic
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView
import javafx.util.Callback

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;


/**
 * Controller for the LogViewer component.
 * Registers to Roddys log stuff and create logging tabs for each logger.
 */
@groovy.transform.CompileStatic
public class DataSetView extends CustomControlOnBorderPane implements Initializable {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(DataSetView.class.getSimpleName());

    @FXML
    private ListView lstRuns;

    @FXML
    private ListView lstJobsOfRun;

//    @FXML
//    private ListView lstAdditionalLogFiles;
//
//    @FXML
//    private TableView lstFilesCreatedByWorkflow;
//
//    @FXML
//    private WebView txtAdditionalLogFile;

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

    private boolean initialThreadIsRunning = true;

    private String analysisLongId;

    private FXDataSetWrapper dataSetWrapper;

    private List<FXJobInfoObjectWrapper> allJobWrappersForEC = new LinkedList<>();

    private Map<String, FXJobInfoObjectWrapper> jobInfoWrappersByJobId = [:];


    public DataSetView(final String analysis, final FXDataSetWrapper dataSetWrapper) {
        this.analysisLongId = dataSetWrapper.getLongAnalysisId();
        this.dataSetWrapper = dataSetWrapper;
        loadAPIList(analysis, dataSetWrapper);
    }

    public String getDataSetId() {
        return dataSetWrapper.getId();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lstRuns.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            ListCell call(ListView param) {
                new GenericListViewItemCellImplementation();
            }
        });
        lstRuns.getSelectionModel().selectedItemProperty().addListener((new ChangeListener<FXExecutionContextInfoObjectWrapper>() {
            @Override
            void changed(ObservableValue<? extends FXExecutionContextInfoObjectWrapper> observable, FXExecutionContextInfoObjectWrapper oldValue, FXExecutionContextInfoObjectWrapper newValue) {

                selectedExecutionContextChanged((FXExecutionContextInfoObjectWrapper) newValue);
                accordion.setExpandedPane(tpSubmittedJob);
            }
        }));
        tpSubmittedJob.expandedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                tpExecutionContexts.setExpanded(!newValue)
            }
        });

        lstJobsOfRun.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            ListCell call(ListView param) {
                new GenericListViewItemCellImplementation()
            }
        });
        lstJobsOfRun.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            void changed(ObservableValue observable, Object oldValue, Object newValue) {
                selectedJobChanged((FXJobInfoObjectWrapper) newValue);
            }
        });
//        lstAdditionalLogFiles.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
//            @Override
//            void changed(ObservableValue observable, Object oldValue, Object newValue) {
//                selectedLogFileInAllLogFileListChanged((FXFileWrapper) newValue);
//            }
//        });
//        lstFilesCreatedByWorkflow.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
//            @Override
//            void changed(ObservableValue observable, Object oldValue, Object newValue) {
//                selectedWorkflowFileEntryChanged((FXWorkflowFileWrapper) newValue)
//            }
//        });
//
//        ((TableColumn) lstFilesCreatedByWorkflow.getColumns().get(0)).setCellFactory(new TableCellIconCallBack(fileIsMissing));
//
//        ((TableColumn) lstFilesCreatedByWorkflow.getColumns().get(1)).setCellFactory(new TableCellIconCallBack(fileIsTemporary));
    }

    private void loadAPIList(final String analysis, final FXDataSetWrapper ds) {
        RoddyUITask.runTask(new RoddyUITask<List<RoddyRMIInterfaceImplementation.ExtendedDataSetInfoObject>>("load list of api's") {
            RoddyRMIInterfaceImplementation.ExtendedDataSetInfoObjectCollection dataSetInfoObjects;

            @Override
            @CompileStatic
            public List<RoddyRMIInterfaceImplementation.ExtendedDataSetInfoObject> _call() throws Exception {
                RoddyRMIClientConnection rmiConnection = RoddyUIController.getMainUIController().getRMIConnection(ds.getLongAnalysisId());
                dataSetInfoObjects = rmiConnection.queryExtendedDataSetInfo(ds.getId(), ds.getAnalysis());
                initialThreadIsRunning = false;
                return null;
            }

            @Override
            @CompileStatic
            public void _succeeded() {
                if (!dataSetInfoObjects)
                    return;
                for (RoddyRMIInterfaceImplementation.ExecutionContextInfoObject contextInfoObject : dataSetInfoObjects.list) {
                    lstRuns.getItems().add(new FXExecutionContextInfoObjectWrapper(contextInfoObject));
                }
//                dataSetInfoObjects
//                if (ds.hasDummyAnalysisProcessingInformation(analysis))
//                    lstRuns.getItems().add(new FXExecutionContextInfoObjectWrapper(ds.getDummyAnalysisProcessingInformation(analysis)));
//                if (ds.hasPlannedOrRunningAnalysisProcessingInformation(analysis))
//                    lstRuns.getItems().add(new FXExecutionContextInfoObjectWrapper(ds.getActiveAnalysisProcessingInformation(analysis)));
//                for (AnalysisProcessingInformation api : dataSetInfoObjects) {
//
//                    lstRuns.getItems().add(new FXExecutionContextInfoObjectWrapper(api));
//                    Thread.yield();
//                }
            }
        });
    }

//    private void loadAdditionalLogFiles() {
//        RoddyUITask.runTask(new RoddyUITask<List<File>>("load additional log files") {
//
//            private List<File> files;
//
//            @Override
//            @CompileStatic
//            public List<File> _call() throws Exception {
//                try {
////                    files = currentContext.getAdditionalLogFiles();
//                } catch (Exception ex) {
//                    System.out.println(ex);
//                }
//                return files;
//            }
//
//            @Override
//            @CompileStatic
//            public void _succeeded() {
//                lstAdditionalLogFiles.getItems().clear();
//                if (files == null) return;
//                for (File f : files) {
//                    lstAdditionalLogFiles.getItems().add(new FXFileWrapper(f));
//                }
//            }
//        });
//    }

//    @Override
//    public void jobAddedEvent(RoddyRMIInterfaceImplementation.JobInfoObject job) {
//        if (job.isFakeJob())
//            return;
//        final FXJobInfoObjectWrapper jobWrapper = new FXJobInfoObjectWrapper(job);
//        synchronized (allJobWrappersForEC) {
//            allJobWrappersForEC.add(jobWrapper);
//        }
//        RoddyUITask.invokeLater(new Runnable() {
//
//            @Override
//            public void run() {
//                try {
//                    jobStateVisibilityChanged(0);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }, "BEJob added", false);
//    }
//
//    @Override
//    public void processingInfoAddedEvent(DataSet dataSet, final AnalysisProcessingInformation pi) {
//        if (initialThreadIsRunning) return;
//        synchronized (lstRuns) {
//            ObservableList items = lstRuns.getItems();
//            for (Object o : items) {
//                FXExecutionContextInfoObjectWrapper wrapper = (FXExecutionContextInfoObjectWrapper) o;
////                pi.getDetailedProcessingInfo().registerContextListener(this);
//                if (wrapper.getExecutionContextInfoObject() != pi)
//                    continue;
//                return;
//            }
//            RoddyUITask.invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    lstRuns.getItems().add(0, new FXExecutionContextInfoObjectWrapper(pi));
//                    lstRuns.getSelectionModel().select(0);
//                }
//            }, UIConstants.UIINVOKE_APPEND_PROCESSINGINFO, false);
//        }
//    }
//
//    @Override
//    public void processingInfoRemovedEvent(DataSet dataSet, RoddyRMIInterfaceImplementation.ExecutionContextInfoObject pi) {
//        FXExecutionContextInfoObjectWrapper wrapperToRemove = null;
//        synchronized (lstRuns) {
//            ObservableList items = lstRuns.getItems();
//            for (Object o : items) {
//                FXExecutionContextInfoObjectWrapper wrapper = (FXExecutionContextInfoObjectWrapper) o;
//                if (wrapper.getExecutionContextInfoObject() != pi) continue;
//                wrapperToRemove = wrapper;
//            }
//        }
//        if (wrapperToRemove == null) return;
//        final FXExecutionContextInfoObjectWrapper _wrapperToRemove = wrapperToRemove;
//        RoddyUITask.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (lstRuns) {
//                    lstRuns.getItems().remove(_wrapperToRemove);
//                }
//            }
//        }, "remove api wrapper", false);
//    }

    /**
     * Calls the Command factory and tells it to stop all the processing for the current pid.
     *
     * @param actionEvent
     */
    public void stopPIDProcessing(ActionEvent actionEvent) {
//        currentContext.abortJobSubmission();
//        Roddy.getJobManager().queryJobAbortion(currentContext.getExecutedJobs());
    }

    /**
     * Just stops job submission
     *
     * @param actionEvent
     */
    public void stopJobSubmission(ActionEvent actionEvent) {
//        currentContext.abortJobSubmission();
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
        private BEJob job;

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

        public LoadFileToTextAreaTask(WebView textArea, BEJob job) {
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
                strings = Roddy.getJobManager().peekLogFile(job);
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
                e.printStackTrace();
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
                        ex.printStackTrace();
                    }
                }
            } catch (IOException e) {
                try {
                    textArea.getEngine().loadContent(finalString);
                } catch (Exception e1) {
                    e1.printStackTrace();
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
        public static final void createAndExecute(WebView ta, BEJob job) {
            (new Thread(new LoadFileToTextAreaTask(ta, job), "Load job to webview")).start();
        }
    }

    private void selectedExecutionContextChanged(FXExecutionContextInfoObjectWrapper fxapi) {
        final RoddyRMIInterfaceImplementation.ExecutionContextInfoObject api = fxapi.getExecutionContextInfoObject();
//        lstFilesCreatedByWorkflow.getItems().clear();
//        final ExecutionContext ec = api.getDetailedProcessingInfo();
        executionContextOverview.setContext(api);
//        currentSelectedJob = null;
//        currentContext = ec;

//        loadFilesCreatedByWorkflow(ec);

        clearJobList();
        for (RoddyRMIInterfaceImplementation.JobInfoObject job : api.getExecutedJobs()) {
            if (job instanceof FakeBEJob || job.isFakeJob())
                continue; //Ignore fake jobs.
            synchronized (allJobWrappersForEC) {
                if (jobInfoWrappersByJobId[job.jobId] == null) {
                    def wrapper = new FXJobInfoObjectWrapper(job)
                    allJobWrappersForEC.add(wrapper);
                    jobInfoWrappersByJobId[job.jobId] = wrapper;
                }
            }
        }

//        addMissingJobToList();
        jobStateVisibilityChanged(0);
//        loadAdditionalLogFiles();
    }

    private void selectedWorkflowFileEntryChanged(FXWorkflowFileWrapper jobWrapper) {
        if (jobWrapper == null) {
            logger.warning("No FXJobInfoObjectWrapper object for selectedJobChanged()");
            return;
        }
        BEJob job = jobWrapper.getJob();
        if (job == null) {
            logger.warning("No BEJob object for selectedJobChanged()");
            return;
        }
        for (Object _jobWrapper : lstJobsOfRun.getItems()) {
            FXJobInfoObjectWrapper jWrapper = (FXJobInfoObjectWrapper) _jobWrapper;
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
            @CompileStatic
            protected List<FXWorkflowFileWrapper> _call() throws Exception {
                for (Job job : ec.getExecutedJobs()) {
                    for (BaseFile file : job.getFilesToVerify()) {
                        lstOfFiles.add(new FXWorkflowFileWrapper(file, job));
                    }
                }
                return lstOfFiles;
            }

            @Override
            @CompileStatic
            protected void _succeeded() {
//                lstFilesCreatedByWorkflow.getItems().addAll(lstOfFiles);
            }
        });
    }

    /**
     * Removes all jobs from the listview and from other lists which contain references to a job.
     */
    private void clearJobList() {
        synchronized (allJobWrappersForEC) {
            allJobWrappersForEC.clear();
            lstJobsOfRun.getItems().clear();
        }
    }

//    private void addMissingJobToList() {
//        List<FXJobInfoObjectWrapper> listOfExistingJobs;
//        synchronized (lstJobsOfRun) {
//            listOfExistingJobs = new LinkedList<FXJobInfoObjectWrapper>(lstJobsOfRun.getItems());
//        }
//
//
//    }

    private void jobStateVisibilityChanged(int index) {
        synchronized (allJobWrappersForEC) {
            lstJobsOfRun.getItems().clear();

            JobState jState = null;
            if (index == 0) { //View all
                for (FXJobInfoObjectWrapper job : allJobWrappersForEC) {
                    lstJobsOfRun.getItems().add(job);
                }
                return;
            } else if (index == 1) { //View running
                for (FXJobInfoObjectWrapper jobWrapper : allJobWrappersForEC) {
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
            for (FXJobInfoObjectWrapper jobWrapper : allJobWrappersForEC) {
                RoddyRMIInterfaceImplementation.JobInfoObject job = jobWrapper.getJob();
                if (job instanceof ReadOutJob && job.getJobState() == jState) {
                    lstJobsOfRun.getItems().add(jobWrapper);
                }
            }
        }
    }

    private void selectedJobChanged(FXJobInfoObjectWrapper jobWrapper) {
        try {
            if (jobWrapper == null) {
                logger.warning("No FXJobInfoObjectWrapper object for selectedJobChanged()");
                return;
            }
            RoddyRMIInterfaceImplementation.JobInfoObject job = jobWrapper.getJob();
            if (job == null) {
                logger.warning("No BEJob object for selectedJobChanged()");
                return;
            }
            jobOverview.setJob(job, analysisLongId);
            tabpaneECContents.getSelectionModel().select(tabJobDetails);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void selectedLogFileInAllLogFileListChanged(FXFileWrapper wrapper) {
//        try {
//            txtAdditionalLogFile.getEngine().loadContent("<html><body>No file available.</body></html>");
//            LoadFileToTextAreaTask.createAndExecute(txtAdditionalLogFile, wrapper.getFile());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void addContextInfo(ExecutionContextInfoObject newContext) {
        lstRuns.getItems().add(0, new FXExecutionContextInfoObjectWrapper(newContext));
    }

    public void updateDataSetInformation() {

        // Check a running context and add missing jobs

        // Check existing jobs and update their status
        RoddyUITask.runTask(new RoddyUITask<List<FXJobInfoObjectWrapper>>("load files created upon workflow execution") {

            List<String> jobsToCheck = []
            Map<String, JobState> newStates

            @Override
            protected List<FXJobInfoObjectWrapper> _call() throws Exception {
                synchronized (allJobWrappersForEC) {
                    jobsToCheck += jobInfoWrappersByJobId.values().collect { FXJobInfoObjectWrapper wrapper -> wrapper.job.jobId }
                }
                logger.severe("Querying the jobstate from DataSetView is currently not possible.")
//                newStates = RoddyUIController.getMainUIController().getRMIConnection(analysisLongId).queryJobState(jobsToCheck);
                newStates = [:]
                return null;
            }

            @Override
            protected void _failed() {
                super._failed()
            }

            @Override
            protected void _succeeded() {
                synchronized (allJobWrappersForEC) {
                    newStates.each {
                        String id, JobState state ->
                            if (jobInfoWrappersByJobId[id]) {
                                FXJobInfoObjectWrapper wrapper = (FXJobInfoObjectWrapper) jobInfoWrappersByJobId[id];
                                wrapper.jobStateProperty().set(state);
                            }
                    }
                }
                super._succeeded()
            }
        });
    }

    public void dependencyJobClicked(ActionEvent actionEvent) {
        FXJobInfoObjectWrapper selectedWrapper = null;
        for (Object o : lstJobsOfRun.getItems()) {
            FXJobInfoObjectWrapper fjw = (FXJobInfoObjectWrapper) o;
            if (((FXJobInfoObjectWrapper) o).getJob() != actionEvent.getSource())
                continue;
            selectedWrapper = fjw;
            break;
        }

        if (selectedWrapper != null)
            lstJobsOfRun.getSelectionModel().select(selectedWrapper);
    }
}
