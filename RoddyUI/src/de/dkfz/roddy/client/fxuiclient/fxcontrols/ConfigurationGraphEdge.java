/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxcontrols;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;

public class ConfigurationGraphEdge extends Path {
    private static final Color GRAYED_OUT_COLOR = Color.rgb(204, 204, 204);
    private static final Color NORMAL_COLOR = Color.BLACK;

    private BooleanProperty grayedOut = new SimpleBooleanProperty(false);

    public BooleanProperty grayedOutProperty() {
        return grayedOut;
    }
    public ConfigurationGraphEdge() {
        setStrokeWidth(2.0);
        grayedOut.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    setStroke(GRAYED_OUT_COLOR);
                } else {
                    setStroke(NORMAL_COLOR);
                }
            }
        });
    }
}
