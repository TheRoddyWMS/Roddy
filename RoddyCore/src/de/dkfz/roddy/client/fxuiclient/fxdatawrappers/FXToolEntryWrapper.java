package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

/**
 * Created with IntelliJ IDEA.
 * User: martin
 * Date: 02/12/13
 * Time: 14:25
 * To change this template use File | Settings | File Templates.
 */

import de.dkfz.roddy.config.AnalysisConfiguration;
import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ToolEntry;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FXToolEntryWrapper implements Comparable<FXToolEntryWrapper> {
    private final Configuration configuration;
    private final ToolEntry toolEntry;
    private StringProperty id = new SimpleStringProperty();
    private StringProperty path = new SimpleStringProperty();
    private StringProperty basePathId = new SimpleStringProperty();
    private BooleanProperty isListedInAnalysisHeader = new SimpleBooleanProperty();
    private BooleanProperty isUsedInRun = new SimpleBooleanProperty();

    public FXToolEntryWrapper() {
        configuration = null;
        toolEntry = null;
    }

    public FXToolEntryWrapper(Configuration cfg, ToolEntry toolEntry) {
        configuration = cfg;
        this.toolEntry = toolEntry;
        id.set(toolEntry.id);
        path.set(toolEntry.path);
        basePathId.set(toolEntry.basePathId);
        if(cfg instanceof AnalysisConfiguration) {
            AnalysisConfiguration ac = ((AnalysisConfiguration)cfg);
            isListedInAnalysisHeader.setValue(ac.getListOfUsedTools().contains(toolEntry.id));
        }
    }

    public final StringProperty idProperty() {
        return id;
    }

    public final String getId() {
        return id.get();
    }

    public final StringProperty pathProperty() {
        return path;
    }

    public final String getPath() {
        return path.get();
    }

    public final StringProperty basePathIdProperty() {
        return basePathId;
    }

    public final String getBasePathId() {
        return basePathId.toString();
    }

    public boolean getIsListedInAnalysisHeader() {
        return isListedInAnalysisHeader.get();
    }

    public BooleanProperty isListedInAnalysisHeaderProperty() {
        return isListedInAnalysisHeader;
    }

    public boolean getIsUsedInRun() {
        return isUsedInRun.get();
    }

    public BooleanProperty isUsedInRunProperty() {
        return isUsedInRun;
    }

    @Override
    public String toString() {
        return id.get() + path.get() + basePathId.get();
    }

    @Override
    public int compareTo(FXToolEntryWrapper other) {
        return id.get().compareTo(other.getId());
    }
}
