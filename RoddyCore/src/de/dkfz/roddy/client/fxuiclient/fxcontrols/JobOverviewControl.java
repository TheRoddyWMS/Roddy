package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.execution.jobs.JobStatusListener;
import de.dkfz.roddy.client.fxuiclient.ConfigurationViewer;
import de.dkfz.roddy.client.fxuiclient.DataSetView;
import de.dkfz.roddy.client.fxuiclient.RoddyUIController;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXJobParameterWrapper;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import de.dkfz.roddy.knowledge.files.BaseFile;
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
public class JobOverviewControl extends CustomControlOnBorderPane implements Initializable, JobStatusListener {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(JobOverviewControl.class.getName());

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
    private Job currentJob;

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

    public synchronized void setJob(Job job) {
        try {
            if (currentJob != null)
                currentJob.removeJobStatusListener(this);
            currentJob = job;
            lblToolID.setText(job.getToolID());
            lblJobID.setText(job.jobName);
            lblProcessID.setText(job.getJobID());
            File logFile = job.getLogFile();
            if (logFile != null)
                lblLogFilePath.setText(logFile.getAbsolutePath());
            tblJobsParameters.getItems().clear();
            dependsOnHyperlinksContainer.getChildren().clear();

            for (String key : job.getParameters().keySet()) {
                tblJobsParameters.getItems().add(new FXJobParameterWrapper(key, job.getParameters().get(key)));
            }

            for (BaseFile bf : job.getParentFiles()) {
                if (bf == null || bf.getCreatingJobsResult() == null || bf.getCreatingJobsResult().getJob() == null) {
                    logger.info("No log file info is available for basefile " + bf.getAbsolutePath());
                } else {
                    final Job jP = bf.getCreatingJobsResult().getJob();
                    Hyperlink hl = new Hyperlink(jP.getJobID());//
                    hl.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            jobHyperlinkClicked(jP);
                        }
                    });
                    dependsOnHyperlinksContainer.getChildren().add(hl);
                    dependsOnHyperlinksContainer.getChildren().add(new Label(", "));
                }
            }
            if (dependsOnHyperlinksContainer.getChildren().size() > 0)
                dependsOnHyperlinksContainer.getChildren().remove(dependsOnHyperlinksContainer.getChildren().size() - 1);

            refreshLogfile();
            job.addJobStatusListener(this);

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void refreshLogfile() {
        RoddyUITask.invokeASAP(new Runnable() {
            @Override
            public void run() {
                Job job = currentJob;
                txtClusterLogFile.getEngine().loadContent("<html><body>No file available.</body></html>");
                if (currentJob == null) return;
                DataSetView.LoadFileToTextAreaTask.createAndExecute(txtClusterLogFile, job);
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
        refreshLogfile();
    }

    @Override
    public void jobStatusChanged(Job job, JobState oldState, JobState newState) {
        refreshLogfile();
    }
}
