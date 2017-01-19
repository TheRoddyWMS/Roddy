/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.config.Configuration;
import javafx.beans.property.*;
import javafx.collections.FXCollections;

import java.util.HashMap;
import java.util.Map;


public class FXConfigurationWrapper {
    private final Configuration config;

    public static abstract class SelectionStatus {
        public static final int UNSELECTED = -1;
        public static final int NONE = 0;
        public static final int SELECTED = 1;
    }
    private static String colorForConfigType(Configuration.ConfigurationType type) {
        switch(type) {
            case ANALYSIS:
                return "cornflowerblue";
            case PROJECT:
                return "moccasin";
            case OTHER:
                return "silver";
            default:
                return "red";
        }
    }

    public static Map<String, FXConfigurationWrapper> wrapConfigTree(Configuration rootNode) {
        Map<String, FXConfigurationWrapper> configMap = new HashMap<>();
        FXConfigurationWrapper wrappedNode = new FXConfigurationWrapper(rootNode);
        configMap.put(wrappedNode.getName(), wrappedNode);
        for(Configuration parent : rootNode.getContainerParents()) {
            configMap.putAll(wrapConfigTree(parent));
        }
        return configMap;
    }

    private StringProperty name = new SimpleStringProperty();
    private StringProperty id = new SimpleStringProperty();
    private IntegerProperty selectionStatus = new SimpleIntegerProperty(SelectionStatus.NONE);
    private StringProperty type = new SimpleStringProperty();
    private StringProperty color = new SimpleStringProperty();

    // this bloat is ridiculous:
    private ListProperty<StringProperty> parentConfigs = new SimpleListProperty<>(FXCollections.<StringProperty>observableArrayList());

    public FXConfigurationWrapper() {
        config = null;
    }

    public FXConfigurationWrapper(Configuration config) {
        this.config = config;
        name.set(config.getName());
        id.set(config.getID());
        color.set(colorForConfigType(config.getConfigurationLevel()));
        type.set(config.getConfigurationLevel().toString());
        for(Configuration parent : config.getContainerParents()) {
            parentConfigs.add(new SimpleStringProperty(parent.getName()));
        }
    }

    public final StringProperty nameProperty() {
        return name;
    }
    public final String getName() {
        return name.get();
    }
    public final StringProperty idProperty() {
        return id;
    }
    public final String getID() {
        return id.get();
    }
    public final IntegerProperty selectionStatusProperty() {
        return selectionStatus;
    }
    public final ListProperty<StringProperty> parentConfigsProperty() {
        return parentConfigs;
    }
    public final StringProperty colorProperty() {
        return color;
    }
    public final StringProperty typeProperty() {
        return type;
    }
    public final Configuration getConfiguration() {
        return config;
    }
}
