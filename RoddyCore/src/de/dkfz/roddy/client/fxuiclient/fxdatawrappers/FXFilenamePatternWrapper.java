package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.config.FilenamePattern;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Created with IntelliJ IDEA.
 * User: martin
 * Date: 02/12/13
 * Time: 14:39
 * To change this template use File | Settings | File Templates.
 */
public class FXFilenamePatternWrapper implements Comparable<FXFilenamePatternWrapper> {
    //filesAndPaths.add(fpp.getID() + " " + fpp.getCls() + " " + fpp.getFilenamePatternDependency());

    private StringProperty id = new SimpleStringProperty();
    private StringProperty cls = new SimpleStringProperty();
    private StringProperty dependency = new SimpleStringProperty();

    public FXFilenamePatternWrapper() {}
    public FXFilenamePatternWrapper(FilenamePattern pattern) {
        id.set(pattern.getID());
        cls.set(pattern.getCls().toString());
//        dependency.set(pattern.getFilenamePatternDependency().toString());
    }

    public final StringProperty idProperty() {
        return id;
    }
    public final String getId() {
        return id.get();
    }
    public final StringProperty clsProperty() {
        return cls;
    }
    public final String getCls() {
        return cls.get();
    }
    public final StringProperty dependencyProperty() {
        return dependency;
    }
    public final String getDependency() {
        return dependency.get();
    }

    @Override
    public String toString() {
        return id.get() + cls.get() + dependency.get();
    }

    @Override
    public int compareTo(FXFilenamePatternWrapper other) {
        return id.get().compareTo(other.getId());
    }
}
