package de.dkfz.roddy.client.fxuiclient.settingsviewer;

import de.dkfz.roddy.Constants;
import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.LocalExecutionService;
import de.dkfz.roddy.execution.io.SSHExecutionService;
import de.dkfz.roddy.execution.io.fs.FileSystemCommandSet;
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnGridPane;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.reflections.Reflections;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.dkfz.roddy.StringConstants.SPLIT_COMMA;

/**
 * Controller for the LogViewer component.
 * Registers to Roddies log stuff and create logging tabs for each logger.
 */
public class SettingsViewer extends CustomControlOnGridPane {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(SettingsViewer.class.getName());

    public static final String INVALID_CREDENTIALS = "Please provide proper credentials.";

    public enum Sections {
        Common,
        JobsAndExecution,
        Connection,
    }

    public ListView configurationPaths;
    public TextField txtCommandFactory;

    @FXML
    private ComboBox cbbCLIExecutionService;

    @FXML
    private ComboBox cbbGUIExecutionService;


    public SettingsViewer ROOT;
    public VBox settingsBox;

//    public GridPane remoteExecServiceCredentials;
//    public GridPane remoteExecServiceCredentials_Errors;
//    public VBox remoteExecServiceCredentials_ErrorsList;

    @FXML
    private GridPane overlayRoddySubSystemsSettings;

    @FXML
    private GridPane executionServiceSetup;

    private Map<ComboBox, SSHExecutionServiceSettingsPanelControl> createdSSHExecutionServicePanels = new LinkedHashMap<>();

    public SettingsViewer() {
        super();
        loadSettingsToScreen();

//        visibleProperty().addListener(new ChangeListener<Boolean>() {
//            @Override
//            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
//                if (aBoolean2 == Boolean.TRUE)
//
//            }
//        });

        cbbCLIExecutionService.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observableValue, Object oldObject, Object newObject) {
                Roddy.RunMode mode = Roddy.RunMode.CLI;
                if(newObject.equals(SSHExecutionService.class.getName())) {
                    SSHExecutionServiceSettingsPanelControl cPanel = new SSHExecutionServiceSettingsPanelControl(mode);
                    settingsBox.getChildren().add(settingsBox.getChildren().indexOf(overlayRoddySubSystemsSettings) + 1, cPanel);
                    createdSSHExecutionServicePanels.put(cbbCLIExecutionService, cPanel);
                } else {
                    if(createdSSHExecutionServicePanels.containsKey(cbbCLIExecutionService)) {
                        settingsBox.getChildren().remove(createdSSHExecutionServicePanels.get(cbbCLIExecutionService));
                        createdSSHExecutionServicePanels.remove(cbbCLIExecutionService);
                    }
                }
            }
        });

        cbbGUIExecutionService.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observableValue, Object oldObject, Object newObject) {
                Roddy.RunMode mode = Roddy.RunMode.UI;
                if(newObject.equals(SSHExecutionService.class.getName())) {
                    SSHExecutionServiceSettingsPanelControl cPanel = new SSHExecutionServiceSettingsPanelControl(mode);
                    settingsBox.getChildren().add(settingsBox.getChildren().indexOf(executionServiceSetup), cPanel);
                    createdSSHExecutionServicePanels.put(cbbGUIExecutionService, cPanel);
                } else {
                    if(createdSSHExecutionServicePanels.containsKey(cbbGUIExecutionService)) {
                        settingsBox.getChildren().remove(createdSSHExecutionServicePanels.get(cbbGUIExecutionService));
                        createdSSHExecutionServicePanels.remove(cbbGUIExecutionService);
                    }
                }
            }
        });

        cbbCLIExecutionService.getSelectionModel().select(Roddy.getApplicationProperty(Roddy.RunMode.CLI, Constants.APP_PROPERTY_EXECUTION_SERVICE_CLASS, LocalExecutionService.class.getName()));
        cbbGUIExecutionService.getSelectionModel().select(Roddy.getApplicationProperty(Roddy.RunMode.UI, Constants.APP_PROPERTY_EXECUTION_SERVICE_CLASS, LocalExecutionService.class.getName()));

        RoddyUITask.runTask(new RoddyUITask("Load settings to settings viewer.", false) {
            @Override
            protected Object _call() throws Exception {
                Reflections collect = Reflections.collect();
                Set<Class<? extends ExecutionService>> lstOfExecutionServices = collect.getSubTypesOf(ExecutionService.class);
                Set<Class<? extends FileSystemInfoProvider>> lstOfFileSystemInfoProviders = collect.getSubTypesOf(FileSystemInfoProvider.class);
                Set<Class<? extends FileSystemCommandSet>> lstOfFileSystemCommandSets = collect.getSubTypesOf(FileSystemCommandSet.class);
//                collect.getSubTypesOf(de.dkfz.roddy.execution.io.RuntimeService.class);
                return null;
            }
        });
    }

    public void addErrorMessage(Sections section, String errMsg) {
        VBox listOfErrors = null;
        if (section == Sections.Connection) {
//            listOfErrors = remoteExecServiceCredentials_ErrorsList;
//            ObservableList<Node> rootChildren = ROOT.getChildren();
//            if (rootChildren.contains(remoteExecServiceCredentials_Errors)) rootChildren.remove(remoteExecServiceCredentials_Errors);
//            ObservableList<Node> settingsBoxChildren = settingsBox.getChildren();
//            settingsBoxChildren.add(settingsBoxChildren.indexOf(remoteExecServiceCredentials) + 1, remoteExecServiceCredentials_Errors);
//            remoteExecServiceCredentials_Errors.setVisible(true);
        }
        try {
            Label l = new Label(errMsg);
            l.setId("ErrorListContainer");

            listOfErrors.getChildren().add(l);
            listOfErrors.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void loadSettingsToScreen() {
        String cmdFactory = Roddy.getApplicationProperty(Constants.APP_PROPERTY_COMMAND_FACTORY_CLASS);
//        String execService = Roddy.getApplicationProperty(Constants.APP_PROPERTY_EXECUTION_SERVICE_CLASS);
        String cfgDirectories = Roddy.getApplicationProperty(Constants.APP_PROPERTY_CONFIGURATION_DIRECTORIES);

        configurationPaths.getItems().clear();
        configurationPaths.getItems().add("pipelineConfigurationFiles");
        if (cfgDirectories.trim().length() > 0)
            configurationPaths.getItems().addAll(Arrays.asList(cfgDirectories.split(SPLIT_COMMA)));
        txtCommandFactory.setText(cmdFactory);
//        execServiceSettingsWrite();
    }

    private void writeSettings() {
        ObservableList pathList = configurationPaths.getItems();
        pathList.remove(0);
        String workflowPaths = "";
         for (Object entry : pathList)
            workflowPaths += "," + entry.toString();

        if (workflowPaths.length() > 0)
            workflowPaths = workflowPaths.substring(1);
        Roddy.setApplicationProperty(Constants.APP_PROPERTY_CONFIGURATION_DIRECTORIES, workflowPaths);
        Roddy.setApplicationProperty(Constants.APP_PROPERTY_COMMAND_FACTORY_CLASS, txtCommandFactory.getText());
        Roddy.setApplicationProperty(Roddy.RunMode.CLI, Constants.APP_PROPERTY_EXECUTION_SERVICE_CLASS, cbbCLIExecutionService.getSelectionModel().getSelectedItem().toString());
        Roddy.setApplicationProperty(Roddy.RunMode.UI, Constants.APP_PROPERTY_EXECUTION_SERVICE_CLASS, cbbGUIExecutionService.getSelectionModel().getSelectedItem().toString());
        for(SSHExecutionServiceSettingsPanelControl pnl : createdSSHExecutionServicePanels.values()) {
            pnl.writeToApplicationSettings();
        }

        try {

            FileSystemInfoProvider.initializeProvider(true);
            ExecutionService.initializeService(true);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
//            throw new RuntimeException(ex);
        }
    }

    public void addConfigurationPath(ActionEvent actionEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select configuration directory");
        File file = chooser.showDialog(null);
        configurationPaths.getItems().add(file);
    }

    public void removeConfigurationPath(ActionEvent actionEvent) {
        try {
            Object o = configurationPaths.getSelectionModel().getSelectedItem();
            configurationPaths.getItems().remove(o);
        } catch (Exception ex) {

        }
    }

    public void setRemoteCredentialSettings(ActionEvent actionEvent) {
        writeSettings();
//        hideConfigurationView(true);
    }

    public void rejectRemoteCredentialSettings(ActionEvent actionEvent) {

//        hideConfigurationView(true);
    }

//    public void hideConfigurationView(boolean eraseErrors) {
//        setVisible(false);
//        if (!eraseErrors) return;
//        remoteExecServiceCredentials_ErrorsList.getChildren().clear();
//        remoteExecServiceCredentials_Errors.setVisible(false);
//        settingsBox.getChildren().remove(remoteExecServiceCredentials_Errors);
//    }
}
