/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient;

import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXMeasurementInfoWrapper;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the LogViewer component.
 * Registers to Roddies log stuff and create logging tabs for each logger.
 */
public class FXUIPerformanceTab extends CustomControlOnBorderPane implements Initializable {

    @FXML
    private TableView performanceTable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ConfigurationViewer.setCellValueFactories(performanceTable, "taskID", "noOfCalls", "meanDuration");
    }

    public void refreshView(ActionEvent actionEvent) {
        performanceTable.getItems().clear();

        List<RoddyUITask.TaskMeasurementInfo> listOfAllMeasurementObjects = RoddyUITask.getListOfAllMeasurementObjects();

        for (RoddyUITask.TaskMeasurementInfo listOfAllMeasurementObject : listOfAllMeasurementObjects) {
            FXMeasurementInfoWrapper wrapper = new FXMeasurementInfoWrapper(listOfAllMeasurementObject);
            performanceTable.getItems().add(wrapper);
        }
    }
}
