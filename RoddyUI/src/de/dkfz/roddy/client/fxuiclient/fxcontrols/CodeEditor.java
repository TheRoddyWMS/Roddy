/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.File;
import java.util.List;

/**
 * A syntax highlighting code editor for JavaFX created by wrapping a
 * CodeMirror code editor in a WebView.
 * <p>
 * See http://codemirror.net for more information on using the codemirror editor.
 */
public class CodeEditor extends CustomControlOnBorderPane {

    @FXML
    private Button btnSaveScript;

    @FXML
    private Button btnReloadScript;

    @FXML
    private Button btnRunScript;

    @FXML
    private Button btnUndoStep;

    @FXML
    private Button btnRedoStep;

    @FXML
    private ObservableList listOfOpenFiles;

    @FXML
    private TabPane tabEditors;

    public CodeEditor() {

    }

    public CodeEditor(Configuration configuration, File file) {
        loadFile(configuration, file);
    }

    public void loadFiles(List<File> files) {
        clearFiles();
        for (File file : files) {
            loadFile(file);
            listOfOpenFiles.add(file.getName());
        }
    }

    private void clearFiles() {
        tabEditors.getTabs().clear();
    }

    public void loadFile(Configuration cfg, String toolID, File file) {
        String idx = file.getName();
        Tab newTab = null;
        for (Tab tab : tabEditors.getTabs()) {
            if (tab.getText().equals(idx)) {
                newTab = tab;
                newTab.setContent(new CodeEditorWithErrorList(cfg, toolID, file));
            }
        }
        newTab = new Tab(file.getName());
        addTab(file, newTab);
    }

    public void loadFile(Configuration cfg, File file) {
        Tab t = new Tab(file.getName());
        t.setContent(new CodeEditorWithErrorList(cfg, file));
        addTab(file, t);
    }

    public void loadFile(File file) {
        Tab t = new Tab(file.getName());
        t.setContent(new CodeEditorWithErrorList(file));
        addTab(file, t);
    }

    private void addTab(File file, Tab t) {
        tabEditors.getTabs().add(t);
        listOfOpenFiles.add(file.getName());
        invokeTab(t);
    }

    private void invokeTab(final Tab t) {
        RoddyUITask.invokeASAP(new Runnable() {
            @Override
            public void run() {
                tabEditors.getSelectionModel().select(t);
            }
        }, "select editor tab", false);
    }

    private void init() {
    }

    private void loadCode(String editingCode) {
    }

    public void saveFile(ActionEvent actionEvent) {
        ((CodeEditorWithErrorList) tabEditors.getSelectionModel().getSelectedItem().getContent()).saveFile(actionEvent);
    }

    public void reloadFile(ActionEvent actionEvent) {
    }
}
