package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextError;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import de.dkfz.roddy.client.fxuiclient.fxcontrols.ExecutionContextPresenter;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXAnalysisProcessingInformationWrapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.*;

/**
 */
public class AnalysisProcessingInformationListViewItemController extends ExecutionContextPresenter<FXAnalysisProcessingInformationWrapper> {
    public static final String UITASK_UPDATE_JOBCOUNT = "update job count";

    @FXML
    public Label pidID;

    @FXML
    public Label noOfAbortedProcesses;

    @FXML
    public Label noOfDummyProcesses;

    @FXML
    public Label noOfFailedProcesses;

    @FXML
    public Label noOfFinishedProcesses;

    @FXML
    public Label noOfProcessesOnHold;

    @FXML
    public Label noOfProcessesWithUnknownState;

    @FXML
    public Label noOfQueuedProcesses;

    @FXML
    public Label noOfRunningProcesses;

    @FXML
    public Label noOfSubmittedProcesses;

    @FXML
    public Label noOfUnstartedProcesses;

    @FXML
    public GridPane hiddenIconsContainer;

    @FXML
    public ImageView unknownImage;

    @FXML
    public GridPane runInfoPane;

    @FXML
    public BorderPane borderPane;

    @FXML
    public Label furtherInfo;

    @FXML
    public HBox errorInfo;

    @FXML
    private GridPane ecDetails;

    @FXML
    private Label lblContextLevel;

    @FXML
    private Label lblContextState;

    private Map<JobState, Label> labelsByJobState = new LinkedHashMap<>();

    private FXAnalysisProcessingInformationWrapper item;

    @Override
    public void postInitiliazation(URL url, ResourceBundle resourceBundle) {
        labelsByJobState.put(JobState.ABORTED, noOfAbortedProcesses);
        labelsByJobState.put(JobState.DUMMY, noOfDummyProcesses);
        labelsByJobState.put(JobState.FAILED, noOfFailedProcesses);
        labelsByJobState.put(JobState.HOLD, noOfProcessesOnHold);
        labelsByJobState.put(JobState.OK, noOfFinishedProcesses);
        labelsByJobState.put(JobState.QUEUED, noOfQueuedProcesses);
        labelsByJobState.put(JobState.RUNNING, noOfRunningProcesses);
        labelsByJobState.put(JobState.UNKNOWN_SUBMITTED, noOfSubmittedProcesses);
        labelsByJobState.put(JobState.UNKNOWN, noOfProcessesWithUnknownState);
        labelsByJobState.put(JobState.UNSTARTED, noOfUnstartedProcesses);
    }

    @Override
    public void itemSet(final FXAnalysisProcessingInformationWrapper item) {
        this.item = item;
        final ExecutionContext context = item.getExecutionContext();
        context.registerContextListener(this);
        lblContextLevel.setText(context.getExecutionContextLevel().toString());
        lblContextState.setText(context.getDetailedExecutionContextLevel().toString());
        pidID.setText(item.getID());
        RoddyUITask.runTask(new RoddyUITask<ExecutionContext>("set states map for api wrapper", false) {
            ExecutionContext ec = null;


            @Override
            public ExecutionContext _call() throws Exception {
                try {
                    ec = context;

                } catch (Exception ex) {
                    System.out.println(ex);
                    for (Object o : ex.getStackTrace())
                        System.out.println(o.toString());
                } finally {
                    return ec;
                }
            }

            @Override
            public void _succeeded() {
                invokeUpdateJobStateIcons();
            }

        });
    }

    private void invokeUpdateJobStateIcons() {
        RoddyUITask.invokeLater(new Runnable() {
            @Override
            public void run() {
                synchronized (item) {
                    updateJobStateIcons();
                }
            }
        }, UITASK_UPDATE_JOBCOUNT, false);

    }

    private void updateJobStateIcons() {
        ExecutionContext ec = item.getExecutionContext();
        String ecUser = ec.getExecutingUser();
        Map<JobState, Integer> stateMap = new HashMap<>();

        if (ec == null) return;

        for (JobState js : JobState.values())
            stateMap.put(js, 0);

        for (Job job : ec.getExecutedJobs()) {
            if(job.isFakeJob()) continue;
            JobState s = job.getJobState();
            if (s == null) s = JobState.UNKNOWN;
            stateMap.put(s, stateMap.get(s) + 1);
        }

        errorInfo.getChildren().clear();

        if (ec.getErrors().size() > 0) {
            for (ExecutionContextError ece : ec.getErrors()) {
                errorInfo.getChildren().add(createErrorIcon(ece));
            }
        }

        furtherInfo.setText(String.format("Executed by %s", ecUser));

        List<Node> visibleChildren = new LinkedList<>(runInfoPane.getChildren());
        runInfoPane.getChildren().clear();
        runInfoPane.add(unknownImage, 0, 0); //Let that image stay in the run info pane
        visibleChildren.remove(unknownImage);

        for (Node n : visibleChildren)
            hiddenIconsContainer.add(n, 0, 0);

        int row = 0;
        int col = 0;
        for (JobState js : labelsByJobState.keySet()) {
            int count = stateMap.get(js);
            Label label = labelsByJobState.get(js);
            if (count > 0) {
                label.setText("" + count);
                hiddenIconsContainer.getChildren().remove(label);
                runInfoPane.add(label, col, row);
                row++;
                if (row >= 4) {
                    row -= 4;
                    col++;
                }
            }
        }
    }

    @Override
    public void jobStateChangedEvent(Job job) {
        invokeUpdateJobStateIcons();
    }

    @Override
    public void detailedExecutionContextLevelChanged(final ExecutionContext context) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                lblContextLevel.setText(context.getExecutionContextLevel().toString());
                lblContextState.setText(context.getDetailedExecutionContextLevel().toString());
            }
        });
    }

    @Override
    public void jobAddedEvent(final Job job) {
        invokeUpdateJobStateIcons();
    }
//
//    public void runDummyContext(ActionEvent actionEvent) {
//        item.getExecutionContext().getAnalysis().runDeferredContext(ExecutionContextLevel.RERUN, item.getExecutionContext());
//    }

}
