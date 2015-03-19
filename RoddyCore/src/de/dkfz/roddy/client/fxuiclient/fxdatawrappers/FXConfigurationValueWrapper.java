package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationValue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.LinkedList;
import java.util.List;

/**
 * Encapsulates DataSets (like i.e. PIDs)
 */
public class FXConfigurationValueWrapper implements Comparable<FXConfigurationValueWrapper> {
    private final ConfigurationValue configurationValue;
    private final Configuration configuration;
    private StringProperty configurationID;
    private StringProperty name;
    private StringProperty value;
    private StringProperty color;
    private StringProperty type;
    private BooleanProperty isOverriding;
    private List<ConfigurationValue> inheritanceList = new LinkedList<>();
    private boolean inheritanceListIsSet = false;
    private StringProperty inheritanceID;

    public FXConfigurationValueWrapper(ConfigurationValue configurationValue, int inheritanceID) {
        this(configurationValue);
        this.inheritanceID.setValue(""+inheritanceID);
    }

    public FXConfigurationValueWrapper(ConfigurationValue configurationValue) {
        this.configurationValue = configurationValue;
        this.configuration = configurationValue.getConfiguration();
        this.configurationID = new SimpleStringProperty(configuration.getID());
        this.isOverriding = new SimpleBooleanProperty(overridesOtherValues());
        this.inheritanceID = new SimpleStringProperty("");
        this.name = new SimpleStringProperty(configurationValue.id);
        this.value = new SimpleStringProperty(configurationValue.getValue());
        this.color = new SimpleStringProperty();

        this.type = new SimpleStringProperty();
        if (configurationValue.getType().isEmpty()) {
            this.type.set("unknown");
        } else {
            this.type.set(configurationValue.getType());
        }
    }

    public final StringProperty configurationIDProperty() {
        return configurationID;
    }

    public final StringProperty nameProperty() {
        return name;
    }

    public final String getName() {
        return name.get();
    }

    public final StringProperty valueProperty() {
        return value;
    }

    public final String getValue() {
        return value.get();
    }

    public final StringProperty colorProperty() {
        return color;
    }

    public final String getColor() {
        return color.get();
    }

    public final StringProperty typeProperty() {
        return type;
    }

    public final String getType() {
        return type.get();
    }

    public final StringProperty inheritanceIDProperty() {
        return inheritanceID;
    }

    public final String getInheritanceID() {
        return inheritanceID.get();
    }

    public final BooleanProperty isOverridingProperty() {
        return isOverriding;
    }

    public final Boolean getIsOverriding() {
        return isOverriding.get();
    }

    @Override
    public String toString() {
        return configurationID.get() + name.get() + value.get() + color.get();
    }

    @Override
    public int compareTo(FXConfigurationValueWrapper o) {
        return configurationID.get().compareTo(o.configurationIDProperty().get());
    }

    public ConfigurationValue getConfigurationValue() {
        return configurationValue;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public boolean overridesOtherValues() {
        getInheritanceList();
        return inheritanceList.size() > 1;
    }

    public List<ConfigurationValue> getInheritanceList() {
        synchronized (inheritanceList) {
            if (!inheritanceListIsSet) {
                inheritanceList.addAll(configuration.getConfigurationValues().getInheritanceList(configurationValue.id));
                inheritanceListIsSet = true;
            }
        }
        return inheritanceList;
    }
}
