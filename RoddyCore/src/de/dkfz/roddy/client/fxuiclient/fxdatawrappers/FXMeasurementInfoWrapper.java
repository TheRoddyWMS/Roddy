package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;

/**
 * Wrapper for single files.
 * Paths don't get displayed in toString()
 */
public class FXMeasurementInfoWrapper {
    private final RoddyUITask.TaskMeasurementInfo infoObject;

    private StringProperty _taskIDProperty = new SimpleStringProperty();
    private StringProperty _meanDurationProperty = new SimpleStringProperty();
    private StringProperty _noOfCallsProperty = new SimpleStringProperty();

    public FXMeasurementInfoWrapper(RoddyUITask.TaskMeasurementInfo infoObject) {
        this.infoObject = infoObject;
        update();
    }

    public void update() {
        this._taskIDProperty.setValue(infoObject.getId());
        this._meanDurationProperty.setValue(String.format("%8.2f", infoObject.getMeanValueInMicros()));
        this._noOfCallsProperty.setValue("" + infoObject.getNumberOfCalls());
    }

    public StringProperty meanDurationProperty() {
        return _meanDurationProperty;
    }

    public StringProperty noOfCallsProperty() {
        return _noOfCallsProperty;
    }

    public StringProperty taskIDProperty() {
        return _taskIDProperty;
    }
}