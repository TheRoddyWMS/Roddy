/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient;

import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.RunMode;
import de.dkfz.roddy.StringConstants;
import de.dkfz.roddy.client.fxuiclient.settingsviewer.SettingsViewer;
import de.dkfz.roddy.client.rmiclient.RoddyRMIClientConnection;
import de.dkfz.roddy.client.rmiclient.RoddyRMIInterfaceImplementation;
import de.dkfz.roddy.config.AppConfig;
import de.dkfz.roddy.config.ConfigurationFactory;
import de.dkfz.roddy.config.InformationalConfigurationContent;
import de.dkfz.roddy.core.*;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXDataSetWrapper;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXICCWrapper;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.GenericListViewItemCellImplementation;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.ProjectTreeItemCellImplementation;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.ProjectTreeItemCellListener;
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;
import de.dkfz.roddy.tools.RoddyIOHelperMethods;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Controller implementation for RoddyUI.fxml
 * This is the JavaFX replacement for the RoddyUI class.
 */
public class RoddyUIController extends BorderPane implements Initializable, ProjectTreeItemCellListener {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(RoddyUIController.class.getSimpleName());
    private static final String APP_PROPERTY_LAST_OPEN_PROJECT_PATH = "lastOpenProjectPath";
    private static final String APP_PROPERTY_FILTER_ANALYSISID = "projectFilterAnalysisID";
    private static final String APP_PROPERTY_FILTER_PROJECTID = "projectFilterProjectID";
    private static final String APP_PROPERTY_FILTER_HIDE_UNPROCESSABLE = "projectFilterHideUnprocessable";
    private static final String APP_PROPERTY_PROJECT_SETTINGS_OPENED = "titlePaneProjectSettingsOpened";
    private static final String APP_PROPERTY_PROJECT_DATASET_FILTER_OPENED = "titlePaneProjectDataSetFilterOpened";
    private static final String APP_PROPERTY_PROJECT_DATASET_PROCESSING_OPENED = "titlePaneProjectDataSetProcessingOpened";
    private static final String APP_PROPERTY_PROJECT_FILTER_SETTINGS_OPENED = "titlePaneProjectFilterSettingsOpened";

    public enum TabType {
        Common,
        Project,
        Dataset
    }

    /**
     * The UI controller is a singleton
     */
    private static RoddyUIController instance;

    @FXML
    private Image iconApplicationSpecific;
    @FXML
    private Image iconApplicationInformal;
    @FXML
    private Image iconProjectSpecific;
    @FXML
    private Image iconDatasetSpecific;

    @FXML
    private ToggleGroup dataSetFilterViewToggleGroup;

    @FXML
    private TextField txtDataSetFilter;

    @FXML
    private TreeView projectTree;

    @FXML
    private TextField txtProjectFilterByID;

    @FXML
    private TextField txtProjectFilterByAnalysis;

    @FXML
    private CheckBox cbProjectFilterHideUnprocessable;

    @FXML
    private Accordion projectDatasetAccordion;

    @FXML
    private TitledPane tpDatasets;

    @FXML
    private ListView listViewDataSets;

    @FXML
    private ComboBox comboBoxApplicationIniFiles;

    @FXML
    private TabPane appTabs;

    //    @FXML
//    private Tab appTabConfiguration;
//
    @FXML
    public SettingsViewer appIniViewer;

    @FXML
    public ListView listViewOfActiveUITasks;

    @FXML
    private VBox vboxAvailableAnalyses;

    @FXML
    private VBox vboxProcessingMode;

//    @FXML
//    private TextField txtAnalysisInputDirectory;
//
//    @FXML
//    private TextField txtProjectBaseOutputDirectory;
//
//    @FXML
//    private TextField txtAnalysisOutputDirectory;

    @FXML
    private ConfigurationViewer configurationViewer;

    @FXML
    private Label lblMemory;

    @FXML
    private ProgressBar pgbMemory;

    @FXML
    private ProgressIndicator executionService_activityIndicator;

    @FXML
    private Circle stateImage_active;

    @FXML
    private Circle stateImage_inactive;

    @FXML
    private Circle stateImage_connecting;

    @FXML
    private TitledPane tpProjectSettings;

    @FXML
    private TitledPane tpProjectDataSetFilter;

    @FXML
    public TitledPane tpProjectFilterSettings;


    private FXICCWrapper currentProjectWrapper;

    private File currentIniFile;
    private String currentProject;
    private String currentAnalysis;
    private RoddyRMIClientConnection currentConnection;

    private ObservableList<FXDataSetWrapper> currentListOfDataSets = FXCollections.observableArrayList();

    private TreeItem<FXICCWrapper> allProjectTreeItemsRoot = null;

    private Map<String, Map<FXDataSetWrapper, DataSetView>> openDataSetViews = new HashMap<>();

    /**
     * A map which contains the currently open data set view objects.
     */
    private Map<FXDataSetWrapper, DataSetView> activeDataSetViews = new LinkedHashMap<>();


    public static RoddyUIController getMainUIController() {
        return instance;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        instance = this;

        txtDataSetFilter.textProperty().addListener((observableValue, s, s2) -> refillListOfDataSets());

        comboBoxApplicationIniFiles.getSelectionModel().selectedItemProperty().addListener((observableValue, old, newValue) -> selectedApplicationIniChanged(newValue.toString()));
        comboBoxApplicationIniFiles.getItems().addAll(RoddyIOHelperMethods.readTextFile(getUiSettingsFileForIniFiles()));

        projectTree.setCellFactory(treeView -> new ProjectTreeItemCellImplementation(projectTree, instance));
        projectTree.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
            TreeItem<FXICCWrapper> pWrapper = (TreeItem<FXICCWrapper>) projectTree.getSelectionModel().getSelectedItem();
            if (pWrapper == null) return;
            changeSelectedProject(pWrapper.getValue());
        });

        listViewDataSets.setCellFactory(listView -> new GenericListViewItemCellImplementation());
        listViewDataSets.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listViewDataSets.getSelectionModel().selectedItemProperty().addListener((observableValue, old, newValue) -> selectedDataSetsChanged());

        currentListOfDataSets.addListener((ListChangeListener<FXDataSetWrapper>) change -> refillListOfDataSets());

        String txtAnalysisIDFilter = Roddy.getApplicationProperty(RunMode.UI, APP_PROPERTY_FILTER_ANALYSISID, StringConstants.EMPTY);
        String txtIDFilter = Roddy.getApplicationProperty(RunMode.UI, APP_PROPERTY_FILTER_PROJECTID, StringConstants.EMPTY);
        Boolean filterHideUnprocessable = Boolean.parseBoolean(Roddy.getApplicationProperty(RunMode.UI, APP_PROPERTY_FILTER_HIDE_UNPROCESSABLE, Boolean.FALSE.toString()));

        txtProjectFilterByAnalysis.setText(txtAnalysisIDFilter);
        txtProjectFilterByID.setText(txtIDFilter);
        txtProjectFilterByAnalysis.textProperty().addListener((observableValue, oldValue, newValue) -> refreshProjectView(null));
        txtProjectFilterByID.textProperty().addListener((observableValue, oldValue, newValue) -> refreshProjectView(null));
        cbProjectFilterHideUnprocessable.setSelected(filterHideUnprocessable);

        txtProjectFilterByAnalysis.textProperty().addListener((observableValue, oldValue, newValue) -> Roddy.setApplicationProperty(RunMode.UI, APP_PROPERTY_FILTER_ANALYSISID, newValue));
        txtProjectFilterByID.textProperty().addListener((observableValue, oldValue, newValue) -> Roddy.setApplicationProperty(RunMode.UI, APP_PROPERTY_FILTER_ANALYSISID, newValue));
        cbProjectFilterHideUnprocessable.selectedProperty().addListener((observableValue, aBoolean, newValue) -> Roddy.setApplicationProperty(RunMode.UI, APP_PROPERTY_FILTER_HIDE_UNPROCESSABLE, newValue.toString()));

        setupTitlePaneExpansionProcessing(tpProjectFilterSettings, APP_PROPERTY_PROJECT_FILTER_SETTINGS_OPENED, Boolean.TRUE);
        setupTitlePaneExpansionProcessing(tpProjectSettings, APP_PROPERTY_PROJECT_SETTINGS_OPENED, Boolean.TRUE);
        setupTitlePaneExpansionProcessing(tpProjectDataSetFilter, APP_PROPERTY_PROJECT_DATASET_FILTER_OPENED, Boolean.TRUE);

        RoddyUITask.activeListOfTasksProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                RoddyUITask.invokeASAP(new Runnable() {
                    @Override
                    public void run() {
                        listViewOfActiveUITasks.getItems().clear();
                        listViewOfActiveUITasks.getItems().addAll(RoddyUITask.activeListOfTasksProperty().values());
                    }
                }, "donttrack::Update tasklist", false);
            }
        });

        startUIUpdateThread();
    }

    private void startUIUpdateThread() {
        Thread t = new Thread(() -> {
            while (App.instance.isRunning) {
                try {
                    invokeDataUpdate();

//                    final Runtime runtime = Runtime.getRuntime();
//                    final double maxMem = 1.0 / (double) runtime.totalMemory();
//
//                    RoddyUITask.invokeLater(() -> {
//                        double number = (runtime.totalMemory() - runtime.freeMemory()) * maxMem;
//                        pgbMemory.setProgress(number);
//                        lblMemory.setText(String.format("%8.0f", number * 100.0) + " %");
//                        lblMemory.setText("" + runtime.freeMemory() + " / " + runtime.totalMemory() + " / " + runtime.maxMemory());
//                    }, "app info update");

                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private void invokeDataUpdate() {
        // Query open datasets
        if (currentAnalysis != null) {
            RoddyRMIClientConnection connection = getRMIConnection(currentAnalysis);

            for (Object _dsw : listViewDataSets.getItems()) {
                FXDataSetWrapper dsw = (FXDataSetWrapper) _dsw;
                connection.queryDataSetState(dsw.getId(), dsw.getAnalysis());
            }

            // Query open data set views
            for (DataSetView dsv : getListOfOpenDataSetViewsForAnalysis().values()) {
                dsv.updateDataSetInformation();
            }
        }
    }

    private String getUiSettingsFileForIniFiles() {
        String _f = Roddy.getSettingsDirectory() + "/uiRecentleyUsedIniFiles.lst";
        File f = new File(_f);
        if (!f.exists())
            RoddyIOHelperMethods.writeTextFile(f, "");
        return _f;
    }

    private void stopRMITasks() {
        for (RoddyRMIClientConnection clientConnection : rmiConnectionPool.values()) {
            try {
                clientConnection.closeServer();
            } catch (Exception ex) {

            }
        }
    }

    public static void setupTitlePaneExpansionProcessing(TitledPane tp, String id, Boolean defaultValue) {
        tp.setExpanded(Boolean.parseBoolean(Roddy.getApplicationProperty(RunMode.UI, id, defaultValue.toString())));
        tp.expandedProperty().addListener((obs, oldV, newV) -> Roddy.setApplicationProperty(RunMode.UI, id, "" + tp.isExpanded()));
    }


    public ConfigurationViewer getConfigurationViewer() {
        return configurationViewer;
    }

    /**
     * Helper method to fill the content of a text field specifically to an input objects type.
     * If this cannot be done, the default value will be used.
     */
    private void fillTextFieldWithObjectValue(TextField textField, Object o, String alternative) {
        String text = null;
        if (o != null) {
            if (o instanceof File) {
                text = ((File) o).getAbsolutePath();
            } else {
                text = o.toString();
            }
        } else {
            text = alternative != null ? alternative : "";
        }
        textField.setText(text);
    }

    public void addTab(Pane component, String title, TabType tabType, boolean closable) {
        Tab t = new Tab(title);
        t.setStyle("TabHeader" + tabType.name());
        t.setClosable(true);
        t.setContent(component);
        if (tabType == TabType.Dataset)
            t.setGraphic(new ImageView(iconDatasetSpecific));
        appTabs.getTabs().add(t);
    }

    private void setStateImageVisibility(boolean f1, boolean f2, boolean f3) {
        stateImage_active.setVisible(f1);
        stateImage_inactive.setVisible(f2);
        stateImage_connecting.setVisible(f3);
    }


    /**
     * Load an ini file, add it to the list of recently used ini files.
     * TODO Store this info somewhere!
     *
     * @param actionEvent
     */
    public void loadApplicationIniFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File iniFile = fileChooser.showOpenDialog(App.instance.primaryStage);
        if (iniFile != null && iniFile.exists()) {
            comboBoxApplicationIniFiles.getItems().add(iniFile);
            selectedApplicationIniChanged(iniFile.getAbsolutePath());
        }
    }

    private void selectedApplicationIniChanged(String _newIni) {
        currentIniFile = new File(_newIni);
        RoddyIOHelperMethods.writeTextFile(getUiSettingsFileForIniFiles(), comboBoxApplicationIniFiles.getItems());
        appIniViewer.loadSettingsToScreen(currentIniFile);
        loadProjects(currentIniFile);
    }


    /**
     * Load a list of available projects related to the current ini file
     * Load all projects from accessible configuration files.
     * This is done in a JavaFX Task.
     *
     * @param iniFile
     */
    private void loadProjects(File iniFile) {
        RoddyUITask task = new RoddyUITask<TreeItem<FXICCWrapper>>(UIConstants.UITASK_LOAD_PROJECTS) {
            @Override
            public TreeItem<FXICCWrapper> _call() throws Exception {
                TreeItem<FXICCWrapper> root = new TreeItem<>(null);
                AppConfig appConfig = new AppConfig(iniFile);
                String[] allConfigDirectories = appConfig.getProperty("configurationDirectories", "").split(StringConstants.SPLIT_COMMA);
                List<File> folders = new LinkedList<>();
                for (String s : allConfigDirectories) {
                    File f = new File(s);
                    if (f.exists())
                        folders.add(f);
                }
                ConfigurationFactory.initialize(folders);
                List<InformationalConfigurationContent> availableProjectConfigurations = ConfigurationFactory.getInstance().getAvailableProjectConfigurations();
                availableProjectConfigurations.sort(new Comparator<InformationalConfigurationContent>() {
                    @Override
                    public int compare(InformationalConfigurationContent o1, InformationalConfigurationContent o2) {
                        return o1.name.compareTo(o2.name);
                    }
                });
                loadProjectsRecursivelyFromXMLFiles(root, availableProjectConfigurations);
                return root;
            }

            @Override
            public void _failed() {
                logger.log(Level.WARNING, UIConstants.ERRTXT_PROJECTSNOTLOADED, getException());
            }

            @Override
            protected void _succeeded() {
                allProjectTreeItemsRoot = valueProperty().get();
                refreshProjectView(null);
            }
        };
        RoddyUITask.runTask(task);
    }

    /**
     * Recursive helper method to load projects from configuration files.
     */
    private void loadProjectsRecursivelyFromXMLFiles(final TreeItem<FXICCWrapper> root, List<InformationalConfigurationContent> availableProjectConfigurations) {
        int count = 0;
        String path = Roddy.getApplicationProperty(RunMode.UI, RoddyUIController.APP_PROPERTY_LAST_OPEN_PROJECT_PATH, "");

        for (InformationalConfigurationContent icc : availableProjectConfigurations) {
            FXICCWrapper fpw = new FXICCWrapper(icc, count++);
            TreeItem<FXICCWrapper> newItem = new TreeItem<>(fpw);
            root.getChildren().add(newItem);
            try {

                Map<String, String> analyses = fpw.getAnalyses();
                for (String analysisID : analyses.keySet()) {
                    FXICCWrapper fpwAnalysis = new FXICCWrapper(icc, analysisID, count++);
                    newItem.getChildren().add(new TreeItem<>(fpwAnalysis));
                }
                loadProjectsRecursivelyFromXMLFiles(newItem, icc.getSubContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Add an expand listener to the topmost nodes
        if (root.getValue() != null) {
            for (final TreeItem<FXICCWrapper> treeItem : root.getChildren()) {
                treeItem.setExpanded(true);
            }
            return;
        }
        for (final TreeItem<FXICCWrapper> treeItem : root.getChildren()) {

            treeItem.expandedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean newValue) {
                    if (!newValue) return;

                    //Upon expand close all other nodes.
                    for (TreeItem<FXICCWrapper> sister : root.getChildren()) {
                        if (treeItem == sister) {
                            Roddy.setApplicationProperty(RunMode.UI, RoddyUIController.APP_PROPERTY_LAST_OPEN_PROJECT_PATH, treeItem.getValue().getID());

                        } else {
                            sister.setExpanded(false);
                        }
                    }
                }
            });
            if (treeItem.getValue().getID().equals(path))
                treeItem.setExpanded(true);
        }

    }

    /**
     * Called when the selected project is changed.
     */
    private void changeSelectedProject(final FXICCWrapper pWrapper) {
        projectDatasetAccordion.setExpandedPane(tpDatasets);

        RoddyUITask.runTask(new RoddyUITask<Void>(UIConstants.UITASK_CHANGE_PROJECT) {
            List<String> analysesList = null;

            @Override
            public Void _call() throws Exception {
                long t1 = ExecutionService.measureStart();
                currentProjectWrapper = pWrapper;
                currentProject = pWrapper.getID();
                long t2 = ExecutionService.measureStart();

                ExecutionService.measureStop(t2, UIConstants.UITASK_MP_LOADCONFIGURATION);

                analysesList = currentProjectWrapper.getICC().getListOfAnalyses();

                cleanRMIPool();

                ExecutionService.measureStop(t2, UIConstants.UITASK_MP_LOAD_ANALYSIS_LIST);
                return null;
            }

            @Override
            public void _succeeded() {

                vboxAvailableAnalyses.getChildren().clear();
                ToggleGroup tgAnalyses = new ToggleGroup();

                for (String analysis : analysesList) {
                    List fullAnalysisID = ProjectFactory.dissectFullAnalysisID(analysis);
                    String id = analysis.split("[:][:]")[0];
                    String plugin = fullAnalysisID.size() > 0 ? "\n - " + fullAnalysisID.get(0).toString() : "";

                    RadioButton rb = new RadioButton(id + plugin);
                    rb.setUserData(analysis);
                    rb.setToggleGroup(tgAnalyses);
                    vboxAvailableAnalyses.getChildren().add(rb);
                }
                tgAnalyses.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                    @Override
                    public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle toggle2) {
                        changeSelectedAnalysis((String) toggle2.getUserData());
                    }
                });

                if (analysesList.size() == 1) {
                    tgAnalyses.getToggles().get(0).setSelected(true);
                }
            }
        });
    }


    /**
     * This method is called if an analysis hyperlink in the projects tree view was clicked.
     *
     * @param pWrapper
     * @param analysisID
     */
    @Override
    public void analysisSelected(FXICCWrapper pWrapper, String analysisID) {
        changeSelectedAnalysis(analysisID);
    }

    /**
     * This method is called when a value in the analyses combo box was selected.
     *
     * @param analysis
     */
    private void changeSelectedAnalysis(final String analysis) {
        RoddyUITask.runTask(new RoddyUITask<Void>(UIConstants.UITASK_ANALYSIS_SELECTED) {

            @Override
            public Void _call() throws Exception {
                long t = ExecutionService.measureStart();
                if (currentAnalysis == analysis) //Don't call twice
                    return null;

                currentAnalysis = analysis;

                long t2 = ExecutionService.measureStart();
                ExecutionService.measureStop(t2, UIConstants.UITASK_ANALYSIS_SELECTED_UPD_CFGVIEWER);
                return null;
            }

            @Override
            public void _succeeded() {
                long t = ExecutionService.measureStart();

                String string = UIConstants.ERRSTR_PATHNOTFOUND;
//                Analysis activeAnalysis = currentProject.getAnalysis(currentAnalysis);
//                fillTextFieldWithObjectValue(txtAnalysisOutputDirectory, activeAnalysis.getOutputAnalysisBaseDirectory(), string);
//                fillTextFieldWithObjectValue(txtAnalysisInputDirectory, activeAnalysis.getInputBaseDirectory(), string);
//                fillTextFieldWithObjectValue(txtProjectBaseOutputDirectory, activeAnalysis.getOutputBaseDirectory(), string);
//
//                if (!openDataSetViews.containsKey(analysis))
//                    openDataSetViews.put(activeAnalysis, new LinkedHashMap<FXDataSetWrapper, DataSetView>());
                ExecutionService.measureStop(t, UIConstants.UITASK_ANALYSIS_SELECTED);

                loadDataSetsForProject();

            }
        });
    }

    private Map<String, ReentrantLock> rmiLocks = new LinkedHashMap<>();

    private Map<String, RoddyRMIClientConnection> rmiConnectionPool = new LinkedHashMap<>();

    private synchronized void cleanRMIPool() {
        for (RoddyRMIClientConnection c : rmiConnectionPool.values()) {
            c.closeServer();
        }
        rmiConnectionPool.clear();
    }

    /**
     * Check and (re-)open / get an (active) rmi server + connection
     *
     * @return
     */
    public RoddyRMIClientConnection getRMIConnection(String analysis) {
        if (RoddyConversionHelperMethods.isNullOrEmpty(analysis) && !analysis.contains("::"))
            System.err.println("Malformed analysis " + analysis + " for getRMIConnection(), needs to be fully specified.");
        List<String> dissected = ProjectFactory.dissectFullAnalysisID(analysis);
        String pluginID = dissected.get(0);

        String shortAnalysisId = analysis.split("[:][:]")[0];
        ReentrantLock myLock = null;
        synchronized (rmiLocks) {
            if (!rmiLocks.containsKey(pluginID)) {
                rmiLocks.put(pluginID, new ReentrantLock());
            }
            myLock = rmiLocks.get(pluginID);
        }

        RoddyRMIClientConnection connection = null;
        logger.postAlwaysInfo("Locking for " + analysis);
        myLock.lock();
        try {
            if (!rmiConnectionPool.containsKey(pluginID) || !rmiConnectionPool.get(pluginID).pingServer()) {
                logger.postAlwaysInfo("Creating connection for " + analysis);
                RoddyRMIClientConnection clientConnection = new RoddyRMIClientConnection();
                clientConnection.startLocalRoddyRMIServerAndConnect(currentIniFile.getAbsolutePath(), currentProjectWrapper.getName(), shortAnalysisId);
                rmiConnectionPool.put(pluginID, clientConnection);
            }
            logger.postAlwaysInfo("Retrieving connection for " + analysis);
            connection = rmiConnectionPool.get(pluginID);
        } finally {
            myLock.unlock();
        }
        return connection;
    }

    /**
     * Load all datasets for an analysis / project
     */
    private void loadDataSetsForProject() {
        try {
            listViewDataSets.getItems().clear();
        } catch (Exception e) {
            logger.severe(e.toString());
        }
        synchronized (currentListOfDataSets) {

            currentListOfDataSets.clear();
        }
        RoddyUITask.runTask(new RoddyUITask<List<FXDataSetWrapper>>(UIConstants.UITASK_LOAD_PROJECTS_DATASETS) {
            List<FXDataSetWrapper> dataSetWrappers = new LinkedList<>();
            List<List<FXDataSetWrapper>> wrapperPackages = new LinkedList<>();

            @Override
            public List<FXDataSetWrapper> _call() throws Exception {
                if (currentAnalysis == null) return null;
                List<FXDataSetWrapper> _dataSetWrappers = new LinkedList<>();

                // Run a remote RMI Roddy task and load datasets from the new instance.
                List<String> dissected = ProjectFactory.dissectFullAnalysisID(currentAnalysis);
                String shortAnalysisId = currentAnalysis.split("[:][:]")[0];
                String pluginID = dissected.get(0);
                if (RoddyConversionHelperMethods.isNullOrEmpty(pluginID))
                    return null;

                RoddyRMIClientConnection clientConnection = getRMIConnection(currentAnalysis);

                assert clientConnection.pingServer();
                List<RoddyRMIInterfaceImplementation.DataSetInfoObject> listOfDatasets = clientConnection.getListOfDatasets(shortAnalysisId);
                for (RoddyRMIInterfaceImplementation.DataSetInfoObject infoObject : listOfDatasets) {
                    _dataSetWrappers.add(new FXDataSetWrapper(currentProjectWrapper.getName(), shortAnalysisId, currentAnalysis, infoObject.getId(), infoObject.getPath().getAbsolutePath()));
                }

                Collections.sort(_dataSetWrappers);

                dataSetWrappers.clear();
                dataSetWrappers.addAll(_dataSetWrappers);

                return _dataSetWrappers;
            }

            @Override
            public void _succeeded() {
                listViewDataSets.getItems().clear();
                listViewDataSets.getItems().addAll(dataSetWrappers);

//                for (final FXDataSetWrapper dataSetWrapper : dataSetWrappers) {
//
//                }
            }
        });
    }

    private void refillListOfDataSets() {
        //TODO Think about a good update mechanism Maybe a counter which only allowes update if the count fits?
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    refillListOfDataSets();
                }
            });
            return;
        }

        List<FXDataSetWrapper> newList;
        List<FXDataSetWrapper> acceptedItems = new LinkedList<>();

        synchronized (currentListOfDataSets) {
            newList = new LinkedList<>(currentListOfDataSets);
        }
        try {
            WildcardFileFilter wff = new WildcardFileFilter(txtDataSetFilter.getText());
            listViewDataSets.getSelectionModel().select(-1);
            listViewDataSets.getItems().clear();
            for (FXDataSetWrapper dsw : newList) {
                if (wff.accept(new File(dsw.getId()))) {
                    acceptedItems.add(dsw);
                }
            }
            listViewDataSets.getItems().addAll(acceptedItems);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectedDataSetsChanged() {
        List<FXDataSetWrapper> dataSets = listViewDataSets.getSelectionModel().getSelectedItems();
        Map<FXDataSetWrapper, DataSetView> currentListOfViews = getListOfOpenDataSetViewsForAnalysis();
        Map<FXDataSetWrapper, DataSetView> newMap = new HashMap<>();
        if (currentListOfViews == null) currentListOfViews = new LinkedHashMap<>();

        //TODO Rework that. Close all dataset tabs. Not more Tag them with Common / Project / Dataset?
        for (int i = appTabs.getTabs().size() - 1; i > 0; i--) {
            Tab appTab = appTabs.getTabs().get(i);
            if (appTab.isClosable())
                appTabs.getTabs().remove(appTab);
        }

        activeDataSetViews.clear();

        for (FXDataSetWrapper dsw : dataSets) {
            if (dsw == null) continue; //This case can occur, if the analysis changed. Then dsw can be null which leads to an exception..
            if (currentListOfViews.containsKey(dsw)) {
                newMap.put(dsw, currentListOfViews.get(dsw));
            } else {
                DataSetView dsv = new DataSetView(currentAnalysis, dsw);
                newMap.put(dsw, dsv);
            }
        }

        List<FXDataSetWrapper> keysSorted = new LinkedList<>(newMap.keySet());
        Collections.sort(keysSorted);
        for (FXDataSetWrapper dsw : keysSorted) {
            addTab(newMap.get(dsw), dsw.getId(), TabType.Dataset, true);
        }
        synchronized (openDataSetViews) {
            openDataSetViews.put(currentAnalysis, newMap);
        }
        appTabs.getSelectionModel().select(1);
    }

    private Map<FXDataSetWrapper, DataSetView> getListOfOpenDataSetViewsForAnalysis() {
        Map<FXDataSetWrapper, DataSetView> currentListOfViews;
        synchronized (openDataSetViews) {
            currentListOfViews = openDataSetViews.get(currentAnalysis);
        }
        return currentListOfViews;
    }


    private void runDataSetsDeferred(boolean rerun, boolean test) {
        RoddyUITask.runTask(new RoddyUITask(UIConstants.UITASK_RERUN_DATASETS) {

            private List<RoddyRMIInterfaceImplementation.ExecutionContextInfoObject> executionContextInfoObjects;

            @Override
            protected Object _call() throws Exception {
                RoddyRMIClientConnection rmiConnection = getRMIConnection(currentAnalysis);

                ObservableList selectedItems = listViewDataSets.getSelectionModel().getSelectedItems();
                List<String> datasetIds = new LinkedList<>();
                for (Object selectedItem : selectedItems) {
                    FXDataSetWrapper dsw = (FXDataSetWrapper) selectedItem;
                    datasetIds.add(dsw.getId());
                }

                if (rerun)
                    executionContextInfoObjects = rmiConnection.rerun(datasetIds, currentAnalysis, test);
                else {
                    executionContextInfoObjects = rmiConnection.run(datasetIds, currentAnalysis, test);
                }
                return null;
            }

            @Override
            protected void _succeeded() {
                for (RoddyRMIInterfaceImplementation.ExecutionContextInfoObject contextInfoObject : executionContextInfoObjects) {
                    String dsId = contextInfoObject.getDatasetId();
                    Map<FXDataSetWrapper, DataSetView> listOfOpenDataSetViewsForAnalysis = getListOfOpenDataSetViewsForAnalysis();
                    for (DataSetView dataSetWrapper : listOfOpenDataSetViewsForAnalysis.values()) {
                        if (dataSetWrapper.getDataSetId().equals(dsId)) {
                            dataSetWrapper.addContextInfo(contextInfoObject);
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Executes a workflow for a list of data sets
     *
     * @param actionEvent
     */
    public void executeWorkflowForDataSets(ActionEvent actionEvent) {
        runDataSetsDeferred(false, false);
    }

    /**
     * Reruns several data sets without a check...
     *
     * @param actionEvent
     */
    public void rerunWorkflowForDataSets(ActionEvent actionEvent) {
        runDataSetsDeferred(true, false);
    }


    private Map<Long, String> activeExecutionTasks = new LinkedHashMap<>();

    private ReentrantLock taskCountLock = new ReentrantLock();

    private void switchExecutionStateProcessIndicator() {
        boolean runningTasks = RoddyUITask.getActiveTaskCount() > 0;
        executionService_activityIndicator.setVisible(runningTasks);
        if (runningTasks)
            App.instance.primaryStage.getScene().setCursor(Cursor.WAIT);
        else
            App.instance.primaryStage.getScene().setCursor(Cursor.DEFAULT);
    }

    public void dataSetFilterChanged(KeyEvent keyEvent) {
        try {
            refillListOfDataSets();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    @Override
    public void executionStarted(final long id, final String text) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                taskCountLock.lock();
                activeExecutionTasks.put(id, text);
                switchExecutionStateProcessIndicator();
                taskCountLock.unlock();
            }
        });
    }

    //    @Override
    public void executionFinished(final long id, String text) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                taskCountLock.lock();
                activeExecutionTasks.remove(id);
                switchExecutionStateProcessIndicator();
                taskCountLock.unlock();
            }
        });
    }

    public void refreshDatasetsForWorkflow(ActionEvent actionEvent) {
        loadDataSetsForProject();
    }

    public void reloadProjectView(ActionEvent actionEvent) {
        ConfigurationFactory.getInstance().initialize();
        loadProjects(currentIniFile);
    }

    public void refreshProjectView(ActionEvent actionEvent) {

        TreeItem<FXICCWrapper> filteredRoot = null;
        boolean filtersApplied = false;
        boolean hideUnprocessable = cbProjectFilterHideUnprocessable.isSelected();
        String analysisIDFilter = txtProjectFilterByAnalysis.getText();
        String idFilter = txtProjectFilterByID.getText();
        analysisIDFilter = "*" + analysisIDFilter.trim() + "*";
        idFilter = "*" + idFilter.trim() + "*";
        filtersApplied = hideUnprocessable;
        filteredRoot = new TreeItem<>(allProjectTreeItemsRoot.getValue());


        TreeItem<FXICCWrapper> root = filteredRoot;
        for (TreeItem<FXICCWrapper> currentNode : allProjectTreeItemsRoot.getChildren()) {
            TreeItem<FXICCWrapper> addable = isProcessable(currentNode, hideUnprocessable, idFilter, analysisIDFilter);
            if (addable != null)
                root.getChildren().add(addable);
        }
        projectTree.setRoot(filteredRoot);
    }

    private TreeItem<FXICCWrapper> isProcessable(TreeItem<FXICCWrapper> currentNode, boolean hideUnprocessable, String idFilter, String analysisIDFilter) {
        WildcardFileFilter wffID = new WildcardFileFilter(idFilter);
        WildcardFileFilter wffAID = new WildcardFileFilter(analysisIDFilter);
        FXICCWrapper cWrapper = currentNode.getValue();
        boolean isVisible = false;
        TreeItem<FXICCWrapper> copyOfTreeItem = new TreeItem<>(currentNode.getValue());
        copyOfTreeItem.setExpanded(currentNode.isExpanded());
//        System.out.println(currentNode.getValue().getID() + " " + currentNode.getChildren().size());
        //At first: Check, if the node has children and if one of those children is visible.
        for (TreeItem<FXICCWrapper> treeItem : currentNode.getChildren()) {
            TreeItem<FXICCWrapper> childVisible = isProcessable(treeItem, hideUnprocessable, idFilter, analysisIDFilter);
            if (childVisible != null)
                copyOfTreeItem.getChildren().add(childVisible);
        }

        //If there are no visible children, then check the node itself.
        if (copyOfTreeItem.getChildren().size() == 0) {
//            System.out.println(cWrapper.getID());
            //Is this a project node or an analysis node?
            isVisible = wffID.accept(new File(cWrapper.getID()));
            if (!isVisible)
                return null;
            if (cWrapper.isAnalysisWrapper()) {
                isVisible = wffAID.accept(new File(cWrapper.getAnalysisID()));
                if (!isVisible)
                    return null;
            } else {
                if (hideUnprocessable) {
                    isVisible = false;
                    return null;
                } else {
                    if (cWrapper.hasAnalyses())
                        return null;
                }
            }
//            if (isVisible && !cWrapper.hasAnalyses()) {
//                if (currentNode.getChildren().size() > 0)
//                    isVisible = false;
//            }
        } else {
            isVisible = true;
        }

        if (isVisible)
            return copyOfTreeItem;
        return null;
    }

    public void switchActiveTabToConfiguration() {
//        appTabs.getSelectionModel().select(appTabConfiguration);
    }

    public static class App extends Application {

        public static final String UI_HEADER = "Roddy - Cluster workflow development and management";
        public static final String UI_ICON_16PX = "/imgs/roddy_16.png";
        public static final String UI_ICON_32PX = "/imgs/roddy_32.png";
        static App instance;

        private Stage primaryStage;

        private boolean isRunning;

        public App() {
            instance = this;
            isRunning = true;
        }

        @Override
        public void start(final Stage primaryStage) throws Exception {
            try {
                this.primaryStage = primaryStage;
                Parent root = FXMLLoader.load(getClass().getResource("RoddyUI.fxml"));
                primaryStage.setTitle(UI_HEADER);
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream(UI_ICON_16PX)));
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream(UI_ICON_32PX)));
                SystemColor systemColor = SystemColor.control;
                primaryStage.setScene(new Scene(root, new Color(systemColor.getRed() / 255.0f, systemColor.getGreen() / 255.0f, systemColor.getBlue() / 255.0f, 1)));
                primaryStage.show();
                primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

                    @Override
                    public void handle(WindowEvent windowEvent) {
                        RoddyUIController.getMainUIController().stopRMITasks();
                        primaryStage.close();
                    }
                });


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void main(String[] args) {
            try {
                launch(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void stop() throws Exception {
            super.stop();    //To change body of overridden methods use File | Settings | File Templates.
            Roddy.exit();
        }

        public void exit() {
            primaryStage.close();
            isRunning = false;
        }

        public boolean isRunning() {
            return isRunning;
        }
    }
}
