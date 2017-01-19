/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationValue;
import de.dkfz.roddy.config.FilenamePattern;
import de.dkfz.roddy.config.ToolEntry;
import de.dkfz.roddy.config.validation.ValidationError;
import de.dkfz.roddy.config.validation.WholeConfigurationValidator;
import de.dkfz.roddy.client.fxuiclient.fxcontrols.CodeEditor;
import de.dkfz.roddy.client.fxuiclient.fxcontrols.ConfigurationGraph;
import de.dkfz.roddy.client.fxuiclient.fxcontrols.ConfigurationValueCell;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXConfigurationValueWrapper;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXConfigurationWrapper;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXFilenamePatternWrapper;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXToolEntryWrapper;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;


public class ConfigurationViewer extends CustomControlOnBorderPane {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(ConfigurationViewer.class.getSimpleName());

    @FXML
    private TitledPane tpConfigurationGraph;

    @FXML
    private ConfigurationGraph configurationGraph;

    @FXML
    private CodeEditor configurationCodeEditor;

    @FXML
    private CodeEditor scriptCodeEditor;

    @FXML
    private TableView<FXConfigurationValueWrapper> configValuesTable;

    @FXML
    private TableView<FXToolEntryWrapper> toolEntriesTable;

    @FXML
    private TableView<FXFilenamePatternWrapper> filenamePatternsTable;

    @FXML
    private TextField filterTextField;

    @FXML
    private Button btnCloseCValueInheritanceTable;

    @FXML
    private TableView configValuesInheritanceTable;

    @FXML
    private GridPane configValueDetailsPane;

    @FXML
    private ListView lstConfigurationErrors;

    @FXML
    private Image imageCValueOverridesCValue;

    @FXML
    private Image imageToolListedInAnalysisHeader;

    @FXML
    private Image imageToolIsUsedInRun;

    @FXML
    private TabPane tabpaneConfigurationContents;

    @FXML
    private Tab tabXMLSources;

    @FXML
    private Tab tabScriptSources;
//
//    @FXML
//    ListView lstCValueHistory;

    private Map<String, FXConfigurationWrapper> configs;
    TableEntryList<FXConfigurationValueWrapper> configValues;
    TableEntryList<FXConfigurationValueWrapper> configValueInheritanceList;
    TableEntryList<FXToolEntryWrapper> toolEntries;
    TableEntryList<FXFilenamePatternWrapper> filenamePatterns;
    private Configuration currentConfiguration;
    private ObservableList<ValidationError> configurationValidationErrors;

    /**
     * Container for objects to be displayed in tables on this page.
     * TableEntryList contains a master list of all elements that could be displayed, and an ObservableList of objects
     * that should currently be displayed.
     * Each object must be added along with an IntegerProperty as 'dependency'. If and only if the integer is positive
     * is an object from the master list in the list for displayed objects. TableEntryList takes care of installing
     * callbacks so this is updated immediately when the IntegerProperty changes.
     * In addition to this, a filter can be set. Only objects that conform to the filter's pattern are added to the list
     * of displayed objects.
     */
    private static class TableEntryList<T extends Comparable> {
        private ObservableList<T> list;
        private Map<IntegerProperty, List<T>> allValues = new HashMap<>();
        private String filter;


        public TableEntryList(ObservableList<T> list) {
            this.list = list;
        }

        public ObservableList<T> getList() {
            return list;
        }

        public void addDependantValues(List<T> dependentObjects, IntegerProperty dependency) {
            List<T> objectsForKey = allValues.get(dependency);
            if (objectsForKey != null) {
                objectsForKey.addAll(dependentObjects);
            } else {
                allValues.put(dependency, new ArrayList<T>(dependentObjects));
                dependency.addListener(new ChangeListener<Number>() {

                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                        if (oldValue.intValue() < 0 && newValue.intValue() >= 0) {
                            list.addAll(filter(allValues.get(observableValue)));
                        } else if (oldValue.intValue() >= 0 && newValue.intValue() < 0) {
                            list.removeAll(allValues.get(observableValue));
                        }
                        Collections.sort(list);
                    }
                });
            }

            if (dependency.get() >= 0) {
                list.addAll(filter(dependentObjects));
                Collections.sort(list);
            }
        }

        public void addDependantValue(T dependantObject, IntegerProperty dependency) {
            addDependantValues(Arrays.asList(dependantObject), dependency);
        }

        public void setFilter(String filter) {
            this.filter = filter;

            List<T> activatedObjects = new ArrayList<>();
            for (Map.Entry<IntegerProperty, List<T>> entry : allValues.entrySet()) {
                if (entry.getKey().getValue() >= 0) {
                    activatedObjects.addAll(entry.getValue());
                }
            }
            list.setAll(filter(activatedObjects));
        }

        private List<T> filter(List<T> objects) {
            List<T> filteredObjects = new ArrayList<>();
            for (T object : objects) {
                if (filter == null || object.toString().contains(filter)) {
                    filteredObjects.add(object);
                }
            }
            return filteredObjects;
        }
    }

    public ConfigurationViewer() {
        setUpTables();
        filterTextField.textProperty().addListener(new ChangeListener<String>() {

            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                configValues.setFilter(newValue);
                toolEntries.setFilter(newValue);
                filenamePatterns.setFilter(newValue);
            }
        });
        toolEntriesTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<FXToolEntryWrapper>() {
            @Override
            public void changed(ObservableValue<? extends FXToolEntryWrapper> observableValue, FXToolEntryWrapper fxToolEntryWrapper, FXToolEntryWrapper fxToolEntryWrapper2) {
                openToolInSourceCodeViewer(fxToolEntryWrapper2.getId());
            }
        });
        configValuesTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<FXConfigurationValueWrapper>() {
            @Override
            public void changed(ObservableValue<? extends FXConfigurationValueWrapper> whatever, FXConfigurationValueWrapper oldItem, FXConfigurationValueWrapper newItem) {
                if (newItem.overridesOtherValues())
                    fillAndShowDetailsPane(newItem);
            }
        });
        btnCloseCValueInheritanceTable.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                configValueDetailsPane.setVisible(false);
            }
        });
        RoddyUIController.setupTitlePaneExpansionProcessing(tpConfigurationGraph, "configurationViewGraphOpened", false);
    }

    private void fillAndShowDetailsPane(FXConfigurationValueWrapper item) {
        configValueDetailsPane.setVisible(true);

        List<ConfigurationValue> inheritanceList = item.getInheritanceList();

        configValuesInheritanceTable.getItems().clear();
        int i = 1;
        for (ConfigurationValue configurationValue : inheritanceList) {
            configValuesInheritanceTable.getItems().add(new FXConfigurationValueWrapper(configurationValue, i++));
        }

    }

    // load configurations asynchronously, then display stuff
    public void setConfiguration(final Configuration unwrappedRootConfig) {
        currentConfiguration = unwrappedRootConfig;
        RoddyUITask.runTask(new RoddyUITask<Boolean>("load configuration to viewer") {
            Map<String, FXConfigurationWrapper> loadedConfigs;
            TableEntryList<FXConfigurationValueWrapper> loadedConfigValues;
            TableEntryList<FXToolEntryWrapper> loadedToolEntries;
            TableEntryList<FXFilenamePatternWrapper> loadedFilenamePatterns;
            ObservableList<ValidationError> loadedConfigurationValidationErrors;

            @Override
            public Boolean _call() throws Exception {
                // build tree of configurations
                loadedConfigs = FXConfigurationWrapper.wrapConfigTree(unwrappedRootConfig);

                // load all configuration values
                loadedConfigValues = new TableEntryList<>(FXCollections.<FXConfigurationValueWrapper>observableArrayList());
                Map<String, ConfigurationValue> unwrappedValues = unwrappedRootConfig.getConfigurationValues().getAllValues();
                for (ConfigurationValue value : unwrappedValues.values()) {
                    FXConfigurationValueWrapper wrappedValue = new FXConfigurationValueWrapper(value);
                    String configID = wrappedValue.configurationIDProperty().get();
                    FXConfigurationWrapper configForValue = loadedConfigs.get(configID);
                    if (configForValue != null) { // why can 'configForValue == null' happen?
                        IntegerProperty dependency = configForValue.selectionStatusProperty();
                        loadedConfigValues.addDependantValue(wrappedValue, dependency);
                        wrappedValue.colorProperty().bind(configForValue.colorProperty());
                    }
                }

                try {
                    // load all tool entries
                    loadedToolEntries = new TableEntryList<>(FXCollections.<FXToolEntryWrapper>observableArrayList());
                    for (ToolEntry entry : unwrappedRootConfig.getTools().getAllValuesAsList()) {
                        FXToolEntryWrapper wrappedEntry = new FXToolEntryWrapper(unwrappedRootConfig, entry);
                        loadedToolEntries.addDependantValue(wrappedEntry, new SimpleIntegerProperty(1));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // load all filename patterns
                loadedFilenamePatterns = new TableEntryList<>(FXCollections.<FXFilenamePatternWrapper>observableArrayList());
                for (FilenamePattern pattern : unwrappedRootConfig.getFilenamePatterns().getAllValuesAsList()) {
                    FXFilenamePatternWrapper wrappedPattern = new FXFilenamePatternWrapper(pattern);
                    loadedFilenamePatterns.addDependantValue(wrappedPattern, new SimpleIntegerProperty(1));
                }

                try {
                    loadedConfigurationValidationErrors = FXCollections.observableArrayList();
                    WholeConfigurationValidator cfgValidator = new WholeConfigurationValidator(currentConfiguration);
                    cfgValidator.validate();
                    for (ValidationError validationError : cfgValidator.getValidationErrors()) {
                        loadedConfigurationValidationErrors.add(validationError);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return Boolean.TRUE;
            }

            @Override
            public void _succeeded() {
                configs = loadedConfigs;
                configValues = loadedConfigValues;
                toolEntries = loadedToolEntries;
                filenamePatterns = loadedFilenamePatterns;
                configurationValidationErrors = loadedConfigurationValidationErrors;
                resetContent();
                List<File> configs = new LinkedList<>();
                for (FXConfigurationWrapper fxConfigurationWrapper : loadedConfigs.values()) {
                    configs.add(fxConfigurationWrapper.getConfiguration().getInformationalConfigurationContent().file);
                }
                configurationCodeEditor.loadFiles(configs);
            }
        });
    }

    private void resetContent() {
        for (FXConfigurationWrapper config : configs.values()) {
            config.selectionStatusProperty().setValue(FXConfigurationWrapper.SelectionStatus.NONE);
        }

        configValuesTable.setItems(configValues.getList());
        toolEntriesTable.setItems(toolEntries.getList());
        filenamePatternsTable.setItems(filenamePatterns.getList());
        lstConfigurationErrors.setItems(configurationValidationErrors);

        configurationGraph.setConfigurations(configs);
        configurationGraph.getSelectedConfigs().addListener(new ListChangeListener<FXConfigurationWrapper>() {

            @Override
            public void onChanged(Change<? extends FXConfigurationWrapper> change) {
                if (change.getList().isEmpty()) {
                    for (FXConfigurationWrapper config : configs.values()) {
                        config.selectionStatusProperty().set(FXConfigurationWrapper.SelectionStatus.NONE);
                    }
                } else {
                    for (FXConfigurationWrapper config : configs.values()) {
                        if (change.getList().contains(config)) {
                            config.selectionStatusProperty().setValue(FXConfigurationWrapper.SelectionStatus.SELECTED);
                        } else {
                            config.selectionStatusProperty().setValue(FXConfigurationWrapper.SelectionStatus.UNSELECTED);
                        }
                    }
                }
            }
        });
    }

    //TODO Move to a ui helper class
    public static <T> void setCellValueFactories(TableView<T> tableView, String... propertyNames) {
        for (int i = 0; i != propertyNames.length; ++i) {
            TableColumn column = tableView.getColumns().get(i);
            String propertyName = propertyNames[i];

            column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        }
    }


    private void setUpTables() {
        setCellValueFactories(configValuesTable, "isOverriding", "configurationID", "name", "value", "type");
        setCellValueFactories(toolEntriesTable, "isListedInAnalysisHeader", "isUsedInRun", "id", "path", "basePathId");
        setCellValueFactories(filenamePatternsTable, "id", "cls", "dependency");
        setCellValueFactories(configValuesInheritanceTable, "inheritanceID", "configurationID", "value", "type");

        TableColumn toolIsListedInAnalysisHeader = toolEntriesTable.getColumns().get(0);
        TableColumn toolIsUsedInSelectedRun = toolEntriesTable.getColumns().get(1);
        toolIsListedInAnalysisHeader.setCellFactory(createBooleanImageTableCellFactory(imageToolListedInAnalysisHeader));
        toolIsUsedInSelectedRun.setCellFactory(createBooleanImageTableCellFactory(imageToolIsUsedInRun));
        for (int i = 2; i < toolEntriesTable.getColumns().size(); i++) {
            TableColumn column = toolEntriesTable.getColumns().get(i);
            column.setCellFactory(new Callback<TableColumn<FXToolEntryWrapper, String>, TableCell<FXToolEntryWrapper, String>>() {
                @Override
                public TableCell<FXToolEntryWrapper, String> call(TableColumn<FXToolEntryWrapper, String> fxToolEntryWrapperStringTableColumn) {
                    TableCell<FXToolEntryWrapper, String> cell = new TableCell<FXToolEntryWrapper, String>() {

                        @Override
                        protected void updateItem(String s, boolean b) {
                            try {
                                super.updateItem(s, b);
                                if (s == null || b) return;
                                Label tf = new Label(s);
                                FXToolEntryWrapper item = (FXToolEntryWrapper) getTableRow().getItem();
                                if (!(item.getIsListedInAnalysisHeader() || item.getIsUsedInRun()))
                                    tf.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                                setGraphic(tf);
                            } catch (Exception ex) {
                                System.err.println(ex);
                            }
                        }
                    };
                    return cell;
                }
            });
        }

        TableColumn configOverridesColumn = configValuesTable.getColumns().get(0);
        TableColumn configIdColumn = configValuesTable.getColumns().get(1);
        configOverridesColumn.setCellFactory(createBooleanImageTableCellFactory(imageCValueOverridesCValue));
        configIdColumn.setCellFactory(new Callback<TableColumn<FXConfigurationValueWrapper, String>, TableCell<FXConfigurationValueWrapper, String>>() {

            @Override
            public TableCell<FXConfigurationValueWrapper, String> call(TableColumn<FXConfigurationValueWrapper, String> fxConfigurationValueWrapperStringTableColumn) {
                try {
                    TableCell<FXConfigurationValueWrapper, String> cell = new ConfigurationValueCell<String>() {

                        @Override
                        protected void updateItem(String s, boolean b) {
                            try {
                                super.updateItem(s, b);
                                if (s == null) return;
                                Label tf = new Label(s);
                                Pane pane = new Pane();
                                pane.setPrefHeight(16);
                                pane.setPrefWidth(16);
                                FXConfigurationValueWrapper item = (FXConfigurationValueWrapper) getTableRow().getItem();
                                pane.setStyle("-fx-background-color: " + item.colorProperty().get() + ";");
                                tf.setGraphic(pane);
                                setGraphic(tf);
                            } catch (Exception ex) {
                                System.err.println(ex);
                            }
                        }
                    };
                    return cell;
                } catch (Exception ex) {
                    System.err.println(ex);
                    return null;
                }
            }
        });
    }

    private Callback<TableColumn<FXToolEntryWrapper, Boolean>, TableCell<FXToolEntryWrapper, Boolean>> createBooleanImageTableCellFactory(final Image image) {
        return new Callback<TableColumn<FXToolEntryWrapper, Boolean>, TableCell<FXToolEntryWrapper, Boolean>>() {
            @Override
            public TableCell<FXToolEntryWrapper, Boolean> call(TableColumn<FXToolEntryWrapper, Boolean> fxToolEntryWrapperBooleanTableColumn) {
                try {
                    TableCell<FXToolEntryWrapper, Boolean> cell = new TableCell<FXToolEntryWrapper, Boolean>() {

                        @Override
                        protected void updateItem(Boolean s, boolean b) {
                            try {
                                super.updateItem(s, b);
                                if (s == null) return;
                                Label tf = new Label();
//                                FXToolEntryWrapper item = (FXToolEntryWrapper) getTableRow().getItem();
                                if (s)
                                    tf.setGraphic(new ImageView(image));
                                setGraphic(tf);
                            } catch (Exception ex) {
                                System.err.println(ex);
                            }
                        }
                    };
                    return cell;
                } catch (Exception ex) {
                    System.err.println(ex);
                    return null;
                }
            }
        };
    }

    public void openToolInSourceCodeViewer(String toolID) {
        File path = currentConfiguration.getSourceToolPath(toolID);

//                CodeEditor ce = new CodeEditor(currentConfiguration, path);
        scriptCodeEditor.loadFile(currentConfiguration, toolID, path);
        tabpaneConfigurationContents.getSelectionModel().select(tabScriptSources);
        RoddyUIController.getMainUIController().switchActiveTabToConfiguration();
    }
}
