/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.execution.jobs.JobStatusListener;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXJobWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

/**
 */
public class JobListViewItemController extends CustomCellItemsHelper.CustomCellItemController<FXJobWrapper> implements JobStatusListener {

    @FXML
    public ImageView indicatorAborted;

    @FXML
    private ImageView indicatorDummyJob;

    @FXML
    private ImageView indicatorDummyRerunJob;

    @FXML
    private ImageView indicatorError;

    @FXML
    private ImageView indicatorOnHold;

    @FXML
    private ImageView indicatorOK;

    @FXML
    private ImageView indicatorQueued;

    @FXML
    private ImageView indicatorRunning;

    @FXML
    private ImageView indicatorUnknown;

    @FXML
    private ImageView indicatorUnknownSubmitted;

    @FXML
    private ImageView indicatorUnprocessed;


    @FXML
    private Label indicatorLogFileIsMissing;

    @FXML
    private Label jobID;

    @FXML
    private Label toolID;

    @FXML
    private GridPane iconsPane;

    public FXJobWrapper jobWrapper;

    private Job job;

    private synchronized void setVisibleIcon(JobState jobState) {
        indicatorOK.setVisible(jobState == JobState.OK);
        indicatorError.setVisible(jobState == JobState.FAILED);
        indicatorUnknown.setVisible(jobState == JobState.UNKNOWN || jobState == null);
        indicatorUnknownSubmitted.setVisible(jobState == JobState.UNKNOWN_SUBMITTED || jobState == null);
//        indicatorUnknownReadout.setVisible(jobState == JobState.UNKNOWN_READOUT || jobState == null);
        indicatorOnHold.setVisible(jobState == JobState.HOLD);
        indicatorQueued.setVisible(jobState == JobState.QUEUED);
        indicatorRunning.setVisible(jobState == JobState.RUNNING);
        indicatorDummyJob.setVisible(jobState == JobState.DUMMY);
        indicatorAborted.setVisible(jobState == JobState.ABORTED);
        indicatorUnprocessed.setVisible(jobState == JobState.UNSTARTED);

        indicatorDummyRerunJob.setVisible(jobState == JobState.DUMMY && !job.areAllFilesValid());

    }

    @Override
    public void itemSet(final FXJobWrapper item) {
        this.jobWrapper = item;
        this.job = item.getJob();
        item.getJob().addJobStatusListener(this, true);

        if (!job.getJobState().isDummy())
            jobID.setText(job.getJobID());
        else
            jobID.setText("-");

        toolID.setText(job.getToolID());

        setStateIconVisibility();
    }

    private void setStateIconVisibility() {
        setVisibleIcon(job.getJobState());
        boolean isUnstarted = false;
        boolean isDummy = false;
        boolean isPlannedOrRunning = false;
        boolean hasLogFile = false;
        try {
            isUnstarted = job.getJobState() == JobState.UNSTARTED;
            isDummy = job.getJobState().isDummy();
            isPlannedOrRunning = job.getJobState().isPlannedOrRunning();
            if (!isUnstarted) {
                hasLogFile = job.hasLogFile();
            }
            if (isDummy || isPlannedOrRunning || hasLogFile) {
                iconsPane.getChildren().remove(indicatorLogFileIsMissing);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void jobStatusChanged(Job job, JobState oldState, JobState newState) {
        RoddyUITask.invokeLater(new Runnable() {
            @Override
            public void run() {
                setStateIconVisibility();
            }
        }, "Job icon change", false);
    }
}
