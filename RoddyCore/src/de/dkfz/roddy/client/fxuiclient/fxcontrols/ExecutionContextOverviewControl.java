/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextError;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXExecutionContextErrorWrapper;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.GenericListViewItemCellImplementation;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Callback;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

/**
 */
public class ExecutionContextOverviewControl extends CustomControlOnBorderPane implements Initializable {

    @FXML
    private TextField txtExecutionDirectory;

    @FXML
    private TextField txtInputDirectory;

    @FXML
    private TextField txtOutputDirectory;

    @FXML
    private ListView inputDirectoryContentList;

    @FXML
    private ListView outputDirectoryContentList;

    @FXML
    private ListView errorList;

    public ExecutionContextOverviewControl() {

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        errorList.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView listView) {
                return new GenericListViewItemCellImplementation();
            }
        });
    }

    public void setContext(final ExecutionContext context) {
        txtExecutionDirectory.setText(context.getExecutionDirectory().getAbsolutePath());
        txtInputDirectory.setText(context.getInputDirectory().getAbsolutePath());
        txtOutputDirectory.setText(context.getOutputDirectory().getAbsolutePath());
        RoddyUITask.runTask(new RoddyUITask("Get additional execution context information") {
            List<File> filesIn;
            List<File> filesOut;

            @Override
            protected Object _call() throws Exception {
                try {
                    filesIn = FileSystemAccessProvider.getInstance().listDirectoriesInDirectory(context.getInputDirectory());
                } catch (Exception e) {
                    filesIn = new LinkedList<File>();
                }
                try {
                    filesOut = FileSystemAccessProvider.getInstance().listDirectoriesInDirectory(context.getOutputDirectory());
                } catch (Exception e) {
                    filesOut = new LinkedList<File>();
                }
                return null;
            }

            @Override
            protected void _succeeded() {
                inputDirectoryContentList.getItems().clear();
                inputDirectoryContentList.getItems().addAll(filesIn);
                outputDirectoryContentList.getItems().clear();
                outputDirectoryContentList.getItems().addAll(filesOut);

                errorList.getItems().clear();
                for (ExecutionContextError ece : context.getErrors()) {
                    FXExecutionContextErrorWrapper wrapper = new FXExecutionContextErrorWrapper(ece);
                    errorList.getItems().add(wrapper);
                }
            }
        });
    }

}
