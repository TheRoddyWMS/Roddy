/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import RoddyRMIInterfaceImplementation;
import Job;
import JobState;
//import de.dkfz.roddy.execution.jobs.JobStatusListener;
import ConfigurationViewer;
import DataSetView;
import RoddyUIController;
import RoddyUITask;
import FXJobParameterWrapper;
import CustomControlOnBorderPane;
import BaseFile
import groovy.transform.CompileStatic;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 */
@CompileStatic
public class JobOverviewControl extends CustomControlOnBorderPane implements Initializable {
//        , JobStatusListener {
    private static final LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(JobOverviewControl.class.getSimpleName());

    @FXML
    private Label lblJobID;

    @FXML
    private Hyperlink lblToolID;

    @FXML
    private Label lblProcessID;

    @FXML
    private TextField lblLogFilePath;

    @FXML
    private TableView tblJobsParameters;

    @FXML
    private HBox dependsOnHyperlinksContainer;

    @FXML
    private WebView txtClusterLogFile;
    private JobInfoObject currentJob;

    private String analysisLongId;

    public JobOverviewControl() {

    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tblJobsParameters.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private ObjectProperty<EventHandler<ActionEvent>> propertyOnAction = new SimpleObjectProperty<EventHandler<ActionEvent>>();

    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return propertyOnAction;
    }

    public final void setOnAction(EventHandler<ActionEvent> handler) {
        propertyOnAction.set(handler);
    }

    public final EventHandler<ActionEvent> getOnAction() {
        return propertyOnAction.get();

    }

    public synchronized void setJob(JobInfoObject job, String analysisLongId) {
        try {
            this.analysisLongId = analysisLongId;
            currentJob = job;
            lblToolID.setText(job.getToolId());
            lblJobID.setText(job.getJobName());
            lblProcessID.setText(job.getJobId());
            File logFile = job.getLogFile();
            if (logFile != null)
                lblLogFilePath.setText(logFile.getAbsolutePath());
            tblJobsParameters.getItems().clear();
            dependsOnHyperlinksContainer.getChildren().clear();

            for (String key : job.getParameters().keySet()) {
                tblJobsParameters.getItems().add(new FXJobParameterWrapper(key, job.getParameters().get(key)));
            }

//            for (BaseFile bf : job.getParentJobs()) {
//                if (bf == null || bf.getCreatingJobsResult() == null || bf.getCreatingJobsResult().getJob() == null) {
//                    logger.info("No log file info is available for basefile " + bf.getAbsolutePath());
//                } else {
//                    final Job jP = bf.getCreatingJobsResult().getJob();
//                    Hyperlink hl = new Hyperlink(jP.getJobID());//
//                    hl.setOnAction(new EventHandler<ActionEvent>() {
//                        @Override
//                        public void handle(ActionEvent actionEvent) {
//                            jobHyperlinkClicked(jP);
//                        }
//                    });
//                    dependsOnHyperlinksContainer.getChildren().add(hl);
//                    dependsOnHyperlinksContainer.getChildren().add(new Label(", "));
//                }
//            }
//            if (dependsOnHyperlinksContainer.getChildren().size() > 0)
//                dependsOnHyperlinksContainer.getChildren().remove(dependsOnHyperlinksContainer.getChildren().size() - 1);

            refreshLogfile(analysisLongId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshLogfile(String analysisLongId) {
        RoddyUITask.invokeASAP(new Runnable() {
            @Override
            public void run() {
                JobInfoObject job = currentJob;
                String text = RoddyUIController.getMainUIController().getRMIConnection(analysisLongId).readRemoteFile(job.getLogFile().getAbsolutePath()).join("<br>");
                txtClusterLogFile.getEngine().loadContent("<html><body>${text}</body></html>");
                if (currentJob == null) return;
            }

        }, "Refresh logfile task", false);
    }

    private void jobHyperlinkClicked(Job jP) {
        onActionProperty().get().handle(new ActionEvent(jP, null));
    }

    @FXML
    private void toolIDClicked(ActionEvent event) {
        if (lblToolID.getText().trim().length() == 0)
            return;

        ConfigurationViewer cViewer = RoddyUIController.getMainUIController().getConfigurationViewer();
        cViewer.openToolInSourceCodeViewer(lblToolID.getText());
    }

    @FXML
    private void reloadJobLogClicked(ActionEvent event) {
        refreshLogfile(analysisLongId);
    }

}
