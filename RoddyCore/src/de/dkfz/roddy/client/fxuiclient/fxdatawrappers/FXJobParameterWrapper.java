package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Encapsulates DataSets (like i.e. PIDs)
 */
public class FXJobParameterWrapper implements Comparable<FXJobParameterWrapper> {

    private StringProperty name;

    private StringProperty value;

    public FXJobParameterWrapper(String name, String value) {
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty valueProperty() {
        return value;
    }

    @Override
    public int compareTo(FXJobParameterWrapper o) {
        return name.getValue().compareTo(o.name.getValue());
    }
}
