/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXConfigurationValueWrapper;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXConfigurationWrapper;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.List;

/**
 */
public class ConfigurationGraphNode extends CustomControlOnBorderPane {
    private BooleanProperty grayedOut = new SimpleBooleanProperty(false);

    @FXML
    private Label txtCfgName;

    @FXML
    private Label txtCfgType;

    @FXML
    private GridPane selectionFrame;

    @FXML
    private GridPane borderFrame;

    @FXML
    private GridPane mainGrid;

    @FXML
    private GridPane grayOverlay;

    public ConfigurationGraphNode() {
    }

    public ConfigurationGraphNode(FXConfigurationWrapper config) {
        txtCfgName.setText(config.nameProperty().get());
        txtCfgType.setText("<< " + config.typeProperty().get() + " >>");
        mainGrid.setStyle("-fx-background-color: " + config.colorProperty().get() + ";");
        config.selectionStatusProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                int value = newValue.intValue();
                switch(value) {
                    case FXConfigurationWrapper.SelectionStatus.NONE:
                        selectionFrame.setVisible(false);
                        grayOverlay.setVisible(false);
                        grayedOut.set(false);
                        break;
                    case FXConfigurationWrapper.SelectionStatus.SELECTED:
                        selectionFrame.setVisible(true);
                        grayOverlay.setVisible(false);
                        grayedOut.set(false);
                        break;
                    case FXConfigurationWrapper.SelectionStatus.UNSELECTED:
                        selectionFrame.setVisible(false);
                        grayOverlay.setVisible(true);
                        grayedOut.set(true);
                        break;
                }
            }
        });
    }
    public BooleanProperty grayedOutProperty() {
        return grayedOut;
    }

}
