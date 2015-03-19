package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import de.dkfz.roddy.core.*;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import de.dkfz.roddy.client.fxuiclient.fxcontrols.ExecutionContextPresenter;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXDataSetWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class DataSetListViewItemController extends ExecutionContextPresenter<FXDataSetWrapper> {

//    @FXML
//    private Label lblCurrentExecutionInformation;

    @FXML
    private ImageView indicatorNotExecutedByRoddy;

    @FXML
    private ImageView indicatorOK;

    @FXML
    private ImageView indicatorError;

    @FXML
    private ImageView indicatorUnknown;

    @FXML
    private ImageView indicatorRunning;

    @FXML
    private ImageView indicatorAborted;

    @FXML
    private ProgressIndicator indicatorQueryIsActive;

    @FXML
    private Label pidID;

//    @FXML
//    private Label dbgLbl;

//    @FXML
//    private GridPane runInfoPane;

//    @FXML
//    private HBox numberOfRunsPane;

    @FXML
    private BorderPane borderPane;

//    @FXML
//    private GridPane dataSetInfo;

//    @FXML
//    private GridPane runViews;

//    @FXML
//    private Label runErrorLogStatesMissing;
//
//    @FXML
//    private Label runErrorJobCallsMissing;

    @FXML
    private Label furtherInfo;

    @FXML
    private Label furtherInfo2;

    @FXML
    private HBox errorInfo;

    @FXML
    private HBox pidExecInfo;

//
//    @FXML
//    private GridPane ecDetails;


    @Override
    public void initialize() {
    }

    private synchronized void setIconVisiblity(Node icon) {
        indicatorNotExecutedByRoddy.setVisible(indicatorNotExecutedByRoddy == icon);
        indicatorOK.setVisible(indicatorOK == icon);
        indicatorError.setVisible(indicatorError == icon);
        indicatorQueryIsActive.setVisible(indicatorQueryIsActive == icon);
        indicatorUnknown.setVisible(indicatorUnknown == icon);
        indicatorRunning.setVisible(indicatorRunning == icon);
        indicatorAborted.setVisible(indicatorAborted == icon);
//        furtherInfo2.setText(
//                indicatorNotExecutedByRoddy.isVisible() + ", " +
//                        indicatorOK.isVisible() + ", " +
//                        indicatorError.isVisible() + ", " +
//                        indicatorQueryIsActive.isVisible() + ", " +
//                        indicatorUnknown.isVisible() + ", " +
//                        indicatorAborted.isVisible()
//        );
    }

    @Override
    public void itemSet(final FXDataSetWrapper item) {
//        this.item = item;
        item.getDataSet().addListener(this, true);

        pidID.setText(item.getID());

//        runViews.getChildren().remove(runInfoPane);

        setIconVisiblity(indicatorNotExecutedByRoddy);
        updateUI();
    }

    private void updateUI() {
        final FXDataSetWrapper item = getItem();
//        RoddyUITask.invokeLater(() -> setIconVisiblity(indicatorQueryIsActive), "set icon visibility", false);

        RoddyUITask.runTask(new RoddyUITask<ExecutionContext>("set states map", false) {
            public String ecUser;
            public String ecTimeStamp;
            public boolean isExecutable;
            ExecutionContext ec = null;
            Map<JobState, Integer> stateMap = new HashMap<>();
            private AnalysisProcessingInformation api;

            @Override
            public ExecutionContext _call() throws Exception {
                try {
                    isExecutable = item.isExecutable();
                    api = item.getRunningOrPlannedProcessingInfo();
                    if (api == null && item.getProcessingInfo().size() > 0)
                        api = item.getProcessingInfo().get(0);

                    if (api == null) // || !api.getDetailedProcessingInfo().hasRunningJobs())
//                        if (item.getProcessingInfo().size() > 0)
//                            for (AnalysisProcessingInformation _api : item.getProcessingInfo()) {
//                                if (_api.getDetailedProcessingInfo().getExecutionContextLevel() == ExecutionContextLevel.QUERY_STATUS)
//                                    continue;
//                                api = item.getProcessingInfo().get(0);
//                                break;
//                            }
//                        else
                        return null;

                    ec = api.getDetailedProcessingInfo();
                    ecUser = ec.getExecutingUser();
                    ecTimeStamp = ec.getTimeStampString();
                    for (JobState js : JobState.values())
                        stateMap.put(js, 0);
                    ec.getExecutedJobs().forEach((job) -> {
                        if (job.isFakeJob()) return; //Do not count Fake jobs
                        JobState s = job.getJobState();
                        if (s == null) s = JobState.UNKNOWN;
                        stateMap.put(s, stateMap.get(s) + 1);
                    });
//                    for (Job job : ec.getExecutedJobs()) {
//                        JobState s = job.getJobState();
//                        if (s == null) s = JobState.UNKNOWN;
//                        stateMap.put(s, stateMap.get(s) + 1);
//                    }
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
                RoddyUITask.invokeLater(() -> {
                    try {

                        if (!item.isExecutable()) {
                            pidID.setId("InactiveItem");
                        } else
                            pidID.setId("ActiveItem");

                        setIconVisiblity(indicatorUnknown);
                        if (errorInfo.getChildren().size() > 0)
                            errorInfo.getChildren().clear();

                        if (api == null) {
                            setIconVisiblity(indicatorNotExecutedByRoddy);
                            return;
                        }

                        if (ec == null) return;

                        if (ec.getErrors().size() > 0) {
                            for (ExecutionContextError ece : ec.getErrors()) {
                                errorInfo.getChildren().add(createErrorIcon(ece));
                            }
                        }

                        if (api != null && ecUser != null)
                            furtherInfo.setText(String.format("%s / %s", api.getExecutionDateHumanReadable(), ecUser));

                        pidExecInfo.setVisible(true);

                        boolean isRunning = stateMap.get(JobState.RUNNING) > 0 || stateMap.get(JobState.HOLD) > 0 || stateMap.get(JobState.QUEUED) > 0;
                        boolean hasFailedJobs = stateMap.get(JobState.FAILED) > 0;
                        boolean hasUnknownJobs = stateMap.get(JobState.UNKNOWN) > 0;
                        boolean hasAbortedJobs = stateMap.get(JobState.ABORTED) > 0;
                        boolean isOk = !hasFailedJobs && !hasUnknownJobs && !hasAbortedJobs;

                        setIconVisiblity(indicatorError);
                        if (isRunning) {
                            setIconVisiblity(indicatorRunning);
                        } else if (hasFailedJobs) {
                            setIconVisiblity(indicatorError);
                        } else if (hasUnknownJobs) {
                            setIconVisiblity(indicatorUnknown);
                        } else if (hasAbortedJobs) {
                            setIconVisiblity(indicatorAborted);
                        } else if (isOk) {
                            setIconVisiblity(indicatorOK);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }, "Invoke ds lv icon update", false);

            }

            @Override
            public void _failed() {
                super.failed();
//                setIconVisiblity(indicatorUnknown);
            }
        });

    }

    private void succ() {

    }


    @Override
    public void processingInfoAddedEvent(DataSet dataSet, AnalysisProcessingInformation pi) {
//        if(pi.getDetailedProcessingInfo().getExecutionContextLevel().canSubmitJobs)
        updateUI();
    }

    @Override
    public void jobStateChangedEvent(Job job) {
        updateUI();
    }

    @Override
    public void jobAddedEvent(Job job) {
        updateUI();
    }

}
