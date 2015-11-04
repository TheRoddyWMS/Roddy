package de.dkfz.roddy.client.fxuiclient;

import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.StringConstants;
import de.dkfz.roddy.config.AnalysisConfiguration;
import de.dkfz.roddy.config.ConfigurationFactory;
import de.dkfz.roddy.config.InformationalConfigurationContent;
import de.dkfz.roddy.config.TestDataOption;
import de.dkfz.roddy.core.*;
import de.dkfz.roddy.execution.io.ExecutionResult;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.io.ExecutionServiceListener;
import de.dkfz.roddy.execution.jobs.Command;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXDataSetWrapper;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXICCWrapper;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.GenericListViewItemCellImplementation;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.ProjectTreeItemCellImplementation;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.ProjectTreeItemCellListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
public class RoddyUIController extends BorderPane implements ExecutionServiceListener, Initializable, ProjectTreeItemCellListener {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(RoddyUIController.class.getSimpleName());
    private static final String APP_PROPERTY_LAST_OPEN_PROJECT_PATH = "lastOpenProjectPath";
    private static final String APP_PROPERTY_FILTER_ANALYSISID = "projectFilterAnalysisID";
    private static final String APP_PROPERTY_FILTER_PROJECTID = "projectFilterProjectID";
    private static final String APP_PROPERTY_FILTER_HIDE_UNPROCESSABLE = "projectFilterHideUnprocessable";
    private static final String APP_PROPERTY_PROJECT_SETTINGS_OPENED = "titlePaneProjectSettingsOpened";
    private static final String APP_PROPERTY_PROJECT_DATASET_FILTER_OPENED = "titlePaneProjectDataSetFilterOpened";
    private static final String APP_PROPERTY_PROJECT_DATASET_PROCESSING_OPENED = "titlePaneProjectDataSetProcessingOpened";
    private static final String APP_PROPERTY_PROJECT_FILTER_SETTINGS_OPENED = "titlePaneProjectFilterSettingsOpened";

    public static enum TabType {
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
    private TabPane appTabs;

    @FXML
    private Tab appTabConfiguration;

    @FXML
    private VBox vboxAvailableAnalyses;

    @FXML
    private VBox vboxProcessingMode;

    @FXML
    private TextField txtAnalysisInputDirectory;

    @FXML
    private TextField txtProjectBaseOutputDirectory;

    @FXML
    private TextField txtAnalysisOutputDirectory;

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

//    @FXML
//    private TitledPane tpDataSetProcessing;

    @FXML
    public TitledPane tpProjectFilterSettings;


    private FXICCWrapper currentProjectWrapper;
    private Project currentProject;
    private Analysis currentAnalysis;
    private ObservableList<FXDataSetWrapper> currentListOfDataSets = FXCollections.observableArrayList();
    private boolean currentListOfDataSetsIsLocked = false;

    private TreeItem<FXICCWrapper> allProjectTreeItemsRoot = null;

    private Map<Analysis, Map<FXDataSetWrapper, DataSetView>> openDataSetViews = new HashMap<>();

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
        ExecutionService.getInstance().registerExecutionListener(this);
        runCheckConnectionTask();

        txtDataSetFilter.textProperty().addListener((observableValue, s, s2) -> refillListOfDataSets());
        currentListOfDataSets.addListener((ListChangeListener<FXDataSetWrapper>) change -> refillListOfDataSets());

        projectTree.setCellFactory(treeView -> new ProjectTreeItemCellImplementation(projectTree, instance));
        projectTree.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
            TreeItem<FXICCWrapper> pWrapper = (TreeItem<FXICCWrapper>) projectTree.getSelectionModel().getSelectedItem();
            if (pWrapper == null) return;
            changeSelectedProject(pWrapper.getValue());//, pWrapper.getValue().getCurrentlySelectedAnalysisID());
        });

        listViewDataSets.setCellFactory(listView -> new GenericListViewItemCellImplementation());
        listViewDataSets.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listViewDataSets.getSelectionModel().selectedItemProperty().addListener((observableValue, old, newValue) -> selectedDataSetsChanged());

        String txtAnalysisIDFilter = Roddy.getApplicationProperty(Roddy.RunMode.UI, APP_PROPERTY_FILTER_ANALYSISID, StringConstants.EMPTY);
        String txtIDFilter = Roddy.getApplicationProperty(Roddy.RunMode.UI, APP_PROPERTY_FILTER_PROJECTID, StringConstants.EMPTY);
        Boolean filterHideUnprocessable = Boolean.parseBoolean(Roddy.getApplicationProperty(Roddy.RunMode.UI, APP_PROPERTY_FILTER_HIDE_UNPROCESSABLE, Boolean.FALSE.toString()));

        txtProjectFilterByAnalysis.setText(txtAnalysisIDFilter);
        txtProjectFilterByID.setText(txtIDFilter);
        txtProjectFilterByAnalysis.textProperty().addListener((observableValue, oldValue, newValue) -> refreshProjectView(null));
        txtProjectFilterByID.textProperty().addListener((observableValue, oldValue, newValue) -> refreshProjectView(null));
        cbProjectFilterHideUnprocessable.setSelected(filterHideUnprocessable);

        txtProjectFilterByAnalysis.textProperty().addListener((observableValue, oldValue, newValue) -> Roddy.setApplicationProperty(Roddy.RunMode.UI, APP_PROPERTY_FILTER_ANALYSISID, newValue));
        txtProjectFilterByID.textProperty().addListener((observableValue, oldValue, newValue) -> Roddy.setApplicationProperty(Roddy.RunMode.UI, APP_PROPERTY_FILTER_ANALYSISID, newValue));
        cbProjectFilterHideUnprocessable.selectedProperty().addListener((observableValue, aBoolean, newValue) -> Roddy.setApplicationProperty(Roddy.RunMode.UI, APP_PROPERTY_FILTER_HIDE_UNPROCESSABLE, newValue.toString()));

        setupTitlePaneExpansionProcessing(tpProjectFilterSettings, APP_PROPERTY_PROJECT_FILTER_SETTINGS_OPENED, Boolean.TRUE);
        setupTitlePaneExpansionProcessing(tpProjectSettings, APP_PROPERTY_PROJECT_SETTINGS_OPENED, Boolean.TRUE);
        setupTitlePaneExpansionProcessing(tpProjectDataSetFilter, APP_PROPERTY_PROJECT_DATASET_FILTER_OPENED, Boolean.TRUE);
//        setupTitlePaneExpansionProcessing(tpDataSetProcessing, APP_PROPERTY_PROJECT_DATASET_PROCESSING_OPENED, Boolean.TRUE);

        loadProjects();
    }

    public static void setupTitlePaneExpansionProcessing(TitledPane tp, String id, Boolean defaultValue) {
        tp.setExpanded(Boolean.parseBoolean(Roddy.getApplicationProperty(Roddy.RunMode.UI, id, defaultValue.toString())));
        tp.expandedProperty().addListener((obs, oldV, newV) -> Roddy.setApplicationProperty(Roddy.RunMode.UI, id, "" + tp.isExpanded()));
    }

    public ConfigurationViewer getConfigurationViewer() {
        return configurationViewer;
    }

    /**
     * Creates a daemon thread which updates ui info components such as the used memory bar.
     */
    private void runUIApplicationInfoUpdateDaemon() {
        Task<Void> fxUIApplicationInfoUpdateTask = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                while (App.instance.isRunning()) {
                    final Runtime runtime = Runtime.getRuntime();
//                    final double maxMem = 1.0 / (double) runtime.totalMemory();

//                    RoddyUITask.invokeLater(new Runnable() {
//                        @Override
//                        public void run() {
//                            double number = (runtime.totalMemory() - runtime.freeMemory()) * maxMem;
////                            pgbMemory.setProgress(number);
////                            lblMemory.setText(String.format("%8.0f", number * 100.0) + " %");
////                            lblMemory.setText("" + runtime.freeMemory() + " / " + runtime.totalMemory() + " / " + runtime.maxMemory());
//                        }
//                    }, null);//UIINVOKE_SET_APPINFO);
                    Thread.sleep(5000);
                }
                return null;
            }


        };
        Thread t = new Thread(fxUIApplicationInfoUpdateTask, UIConstants.UITASK_APPINFO_UPDATE_DAEMON);
        t.setDaemon(true);
//        t.start();
    }


    private void runCheckConnectionTask() {
        RoddyUITask.runTask(new RoddyUITask<Boolean>(UIConstants.UITASK_CHECKCONN) {

            private boolean result = false;

            @Override
            public Boolean _call() throws Exception {
                Thread.sleep(UIConstants.UITASK_CHECKCONN_WAIT);
                if (!ExecutionService.getInstance().needsPassword())
                    result = true;
                if (ExecutionService.getInstance().testConnection())
                    result = true;
                return result;
            }

            @Override
            public void _succeeded() {
                show();
            }

            @Override
            public void _failed() {
                show();
            }

            private void show() {
                if (result) return;
//                overlayDialogSetup.addErrorMessage(SettingsViewer.Sections.Connection, SettingsViewer.INVALID_CREDENTIALS);
//                showOverlayDialog();
            }
        });
    }

    /**
     * Load all projects from accessible configuration files.
     * This is done in a JavaFX Task.
     */
    @FXML
    private void loadProjects() {
        RoddyUITask task = new RoddyUITask<TreeItem<FXICCWrapper>>(UIConstants.UITASK_LOAD_PROJECTS) {
            @Override
            public TreeItem<FXICCWrapper> _call() throws Exception {
                TreeItem<FXICCWrapper> root = new TreeItem<>(null);
                List<InformationalConfigurationContent> availableProjectConfigurations = ConfigurationFactory.getInstance().getAvailableProjectConfigurations();
                availableProjectConfigurations.sort(new Comparator<InformationalConfigurationContent>() {
                    @Override
                    public int compare(InformationalConfigurationContent o1, InformationalConfigurationContent o2) {
                        return o1.name.compareTo(o2.name);
                    }
                });
                loadProjectsRec(root, availableProjectConfigurations);
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
    private void loadProjectsRec(final TreeItem<FXICCWrapper> root, List<InformationalConfigurationContent> availableProjectConfigurations) {
        int count = 0;
        String path = Roddy.getApplicationProperty(Roddy.RunMode.UI, RoddyUIController.APP_PROPERTY_LAST_OPEN_PROJECT_PATH, "");

        for (InformationalConfigurationContent icc : availableProjectConfigurations) {
            FXICCWrapper fpw = new FXICCWrapper(icc, count++);
            TreeItem<FXICCWrapper> newItem = new TreeItem<>(fpw);
            root.getChildren().add(newItem);
            for (String analysisID : fpw.getAnalyses()) {
                FXICCWrapper fpwAnalysis = new FXICCWrapper(icc, analysisID, count++);
                newItem.getChildren().add(new TreeItem<>(fpwAnalysis));
            }
            loadProjectsRec(newItem, icc.getSubContent());
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
                            Roddy.setApplicationProperty(Roddy.RunMode.UI, RoddyUIController.APP_PROPERTY_LAST_OPEN_PROJECT_PATH, treeItem.getValue().getID());

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
            Analysis newAnalysis = null;
            List<Analysis> analysesList = null;

            @Override
            public Void _call() throws Exception {
                long t1 = ExecutionService.measureStart();
                currentProjectWrapper = pWrapper;
                long t2 = ExecutionService.measureStart();
                currentProject = ProjectFactory.getInstance().loadConfiguration(currentProjectWrapper.getICC());
                ExecutionService.measureStop(t2, UIConstants.UITASK_MP_LOADCONFIGURATION);

                analysesList = currentProject.getAnalyses();
                String analysisID = pWrapper.getAnalysisID();
                ExecutionService.measureStop(t2, UIConstants.UITASK_MP_LOAD_ANALYSIS_LIST);
                if (analysisID != null) {
                    newAnalysis = currentProject.getAnalysis(analysisID);
                }
                if (newAnalysis == null && analysesList.size() > 0) {
                    newAnalysis = analysesList.get(0);
                }
//                ConfigurationFactory.getInstance().validateConfiguration(newAnalysis.getConfiguration());
                ExecutionService.measureStop(t1, UIConstants.UITASK_CHANGE_PROJECT);
                return null;
            }

            @Override
            public void _succeeded() {

                vboxAvailableAnalyses.getChildren().clear();
                ToggleGroup tgAnalyses = new ToggleGroup();
                for (Analysis analysis : analysesList) {
                    RadioButton rb = new RadioButton(analysis.getName());
                    rb.setUserData(analysis);
                    rb.setToggleGroup(tgAnalyses);
                    if (analysis == newAnalysis)
                        rb.setSelected(true);
                    vboxAvailableAnalyses.getChildren().add(rb);
                }
                tgAnalyses.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                    @Override
                    public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle toggle2) {
                        changeSelectedAnalysis((Analysis) toggle2.getUserData());
                    }
                });
                changeSelectedAnalysis(newAnalysis);

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
        changeSelectedProject(pWrapper);
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

    /**
     * This method is called when a value in the analyses combo box was selected.
     *
     * @param analysis
     */
    private void changeSelectedAnalysis(final Analysis analysis) {
        RoddyUITask.runTask(new RoddyUITask<Void>(UIConstants.UITASK_ANALYSIS_SELECTED) {

            @Override
            public Void _call() throws Exception {
                long t = ExecutionService.measureStart();
                if (currentAnalysis == analysis) //Don't call twice
                    return null;

                currentAnalysis = analysis;

                long t2 = ExecutionService.measureStart();
                configurationViewer.setConfiguration(currentAnalysis.getConfiguration());
                ExecutionService.measureStop(t2, UIConstants.UITASK_ANALYSIS_SELECTED_UPD_CFGVIEWER);
                return null;
            }

            @Override
            public void _succeeded() {
                long t = ExecutionService.measureStart();

                RadioButton rbProductive = (RadioButton) vboxProcessingMode.getChildren().get(0);
                vboxProcessingMode.getChildren().clear();
                vboxProcessingMode.getChildren().add(rbProductive);
                for (TestDataOption tdo : ((AnalysisConfiguration) currentAnalysis.getConfiguration()).getTestdataOptions()) {
                    RadioButton rbPM = new RadioButton("Debug: " + tdo.getId());
                    rbPM.setUserData(tdo);
                    rbPM.setToggleGroup(rbProductive.getToggleGroup());
                    vboxProcessingMode.getChildren().add(rbPM);
                }
                String string = UIConstants.ERRSTR_PATHNOTFOUND;
                fillTextFieldWithObjectValue(txtAnalysisOutputDirectory, currentAnalysis.getOutputAnalysisBaseDirectory(), string);
                fillTextFieldWithObjectValue(txtAnalysisInputDirectory, currentAnalysis.getInputBaseDirectory(), string);
                fillTextFieldWithObjectValue(txtProjectBaseOutputDirectory, currentAnalysis.getOutputBaseDirectory(), string);

                if (!openDataSetViews.containsKey(analysis))
                    openDataSetViews.put(analysis, new LinkedHashMap<FXDataSetWrapper, DataSetView>());
                ExecutionService.measureStop(t, UIConstants.UITASK_ANALYSIS_SELECTED);

                loadDataSetsForProject();

            }
        });


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
                List<FXDataSetWrapper> _dataSetWrappers = new LinkedList<>();
                //Fetch information about the states for the current analysis
                for (DataSet dataSet : currentAnalysis.getListOfDataSets()) {
                    FXDataSetWrapper wrapper = new FXDataSetWrapper(dataSet, currentAnalysis);
                    _dataSetWrappers.add(wrapper);
                }
                Collections.sort(_dataSetWrappers);

                synchronized (currentListOfDataSets) {
//                    currentListOfDataSetsIsLocked = true;
//                    for (int i = 0; i < _dataSetWrappers.size() - 1; i++) {
//                        currentListOfDataSets.add(_dataSetWrappers.get(i));
//                    }
//                    currentListOfDataSetsIsLocked = false;
                    currentListOfDataSets.addAll(_dataSetWrappers);//.get(_dataSetWrappers.size() - 1));

                }

//                List<FXDataSetWrapper> dataSetWrappers = new LinkedList<>();
//                wrapperPackages.add(dataSetWrappers);
//                for (FXDataSetWrapper dsw : _dataSetWrappers) {
//                    if (dataSetWrappers.add(dsw)) ;
//                    if (dataSetWrappers.size() == 1) {
//                        dataSetWrappers = new LinkedList<>();
//                        wrapperPackages.add(dataSetWrappers);
//                    }
//                }
//
//                for (final List<FXDataSetWrapper> ldsw : wrapperPackages) {
//                    final boolean isFirst = ldsw == wrapperPackages.get(0);
////                    RoddyUITask.invokeLater(new Runnable() {
////                        @Override
////                        public void context() {
////                            if (isFirst)
////                                listViewDataSets.getItems().clear();
////                            listViewDataSets.getItems().addAll(ldsw);
//
////                        }
////                    }, UIINVOKE_ADD_DATA_SETS_TO_LISTVIEW);
//                }

                return _dataSetWrappers;
            }

//            @Override
//            public void _succeeded() {
//                listViewDataSets.getItems().clear();
//                listViewDataSets.getItems().addAll(dataSetWrappers);
//            }
        });
    }

    private void refillListOfDataSets() {
        //TODO Think about a good update mechanism Maybe a counter which only allowes update if the count fits?
//        if (currentListOfDataSetsIsLocked)
//            return;
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
                if (wff.accept(new File(dsw.getID()))) {
                    acceptedItems.add(dsw);
                }
            }
//            ExecutorService es = Executors.newFixedThreadPool(64);
//            for (final FXDataSetWrapper dataSetWrapper : acceptedItems) {
//                es.execute(new Runnable() {
//                    @Override
//                    public void context() {
            listViewDataSets.getItems().addAll(acceptedItems);
//                    }
//                });
//            }
//            es.shutdown();
//            es.awaitTermination(3, TimeUnit.SECONDS);
//            listViewDataSets.getItems().addAll(acceptedItems);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void selectedDataSetsChanged() {
        List<FXDataSetWrapper> dataSets = listViewDataSets.getSelectionModel().getSelectedItems();
        Map<FXDataSetWrapper, DataSetView> currentListOfViews = openDataSetViews.get(currentAnalysis);
        Map<FXDataSetWrapper, DataSetView> newMap = new HashMap<>();

        //TODO Rework that. Close all dataset tabs. Not more Tag them with Common / Project / Dataset?
        for (int i = appTabs.getTabs().size() - 1; i > 0; i--) {
            Tab appTab = appTabs.getTabs().get(i);
            if(appTab.isClosable())
                appTabs.getTabs().remove(appTab);
        }

        activeDataSetViews.clear();

        for (FXDataSetWrapper dsw : dataSets) {
            if (dsw == null) continue; //This case can occur, if the analysis changed. Then dsw can be null which leads to an exception..
            if (currentListOfViews.containsKey(dsw)) {
                newMap.put(dsw, currentListOfViews.get(dsw));
            } else {
                DataSet ds = dsw.getDataSet();
                DataSetView dsv = new DataSetView(currentAnalysis, ds);
                newMap.put(dsw, dsv);
            }
        }

        List<FXDataSetWrapper> keysSorted = new LinkedList<>(newMap.keySet());
        Collections.sort(keysSorted);
        for (FXDataSetWrapper dsw : keysSorted) {
            DataSet ds = dsw.getDataSet();
            addTab(newMap.get(dsw), ds.getId(), TabType.Dataset, true);
        }
        openDataSetViews.put(currentAnalysis, newMap);
        appTabs.getSelectionModel().select(3);
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

    private void runDataSetsDeferred(ExecutionContextLevel level) {
        ObservableList selectedItems = listViewDataSets.getSelectionModel().getSelectedItems();
        for (Object o : selectedItems) {
            final FXDataSetWrapper fxDataSetWrapper = (FXDataSetWrapper) o;
            ExecutionContext deferredContext = currentAnalysis.runDeferredContext(fxDataSetWrapper.getID(), level);
            if (deferredContext == null) {
                logger.warning(UIConstants.ERRTXT_WORKFLOWNOTEXECUTED + fxDataSetWrapper.getID());
                continue;
            }
        }
    }

    /**
     * Executes a workflow for a list of data sets
     *
     * @param actionEvent
     */
    public void executeWorkflowForDataSets(ActionEvent actionEvent) {
        runDataSetsDeferred(ExecutionContextLevel.RUN);
    }

    /**
     * If the workflow supports it create test data here.
     *
     * @param actionEvent
     */
    public void createTestDataForDataSets(ActionEvent actionEvent) {

    }

    /**
     * Reruns several data sets without a check...
     *
     * @param actionEvent
     */
    public void rerunWorkflowForDataSets(ActionEvent actionEvent) {

        ObservableList selectedItems = listViewDataSets.getSelectionModel().getSelectedItems();
        final List<String> dataSets = new LinkedList<>();
        for (Object o : selectedItems) {
            final FXDataSetWrapper fxDataSetWrapper = (FXDataSetWrapper) o;
            dataSets.add(fxDataSetWrapper.getDataSet().getId());
        }

        RoddyUITask.runTask(new RoddyUITask(UIConstants.UITASK_RERUN_DATASETS) {
            @Override
            protected Object _call() throws Exception {
                long creationCheckPoint = System.nanoTime();
                List<ExecutionContext> listOfContexts = currentAnalysis.run(dataSets, ExecutionContextLevel.QUERY_STATUS);
                for (ExecutionContext context : listOfContexts) {
                    currentAnalysis.rerunDeferredContext(context, ExecutionContextLevel.RERUN, creationCheckPoint, false);
                }

                return null;
            }

            @Override
            protected void _succeeded() {
                super._succeeded();
            }
        });
    }

//    private void showOverlayDialog() {
//        overlayDialogSetup.setVisible(true);
//    }

    @Override
    public void stringExecuted(String commandString, ExecutionResult result) {
    }

    @Override
    public void commandExecuted(Command result) {
    }

    @Override
    public void changeExecutionServiceState(final TriState state) {
        //Only important if the execution service works on remote machines.
        if (ExecutionService.getInstance().isLocalService()) {
            setStateImageVisibility(false, false, false);
            return;
        }

        RoddyUITask.invokeLater(new Runnable() {
            @Override
            public void run() {
                setStateImageVisibility(state == TriState.TRUE, state == TriState.FALSE, state == TriState.UNKNOWN);
            }
        }, UIConstants.UIINVOKE_CHANGE_STATE_IMAGES);
    }

    private void setStateImageVisibility(boolean f1, boolean f2, boolean f3) {
        stateImage_active.setVisible(f1);
        stateImage_inactive.setVisible(f2);
        stateImage_connecting.setVisible(f3);
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

    @Override
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

    @Override
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
        ConfigurationFactory.getInstance().refresh();
        loadProjects();
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
        appTabs.getSelectionModel().select(appTabConfiguration);
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
                        primaryStage.close();
                    }
                });


            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        public static void main(String[] args) {
            try {
                launch(args);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
