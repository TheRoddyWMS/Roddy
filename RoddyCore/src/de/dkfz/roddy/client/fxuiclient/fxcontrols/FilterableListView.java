package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.util.Callback;

/**
 * Controller for the LogViewer component.
 * Registers to Roddies log stuff and create logging tabs for each logger.
 */
public class FilterableListView extends CustomControlOnBorderPane {

    public TextField txtFilter;
    public ComboBox cbbSortBy;
    public Label lblHeader;
    public ListView listView;

    public FilterableListView() {
    }

    public void setHeader(String text) {
        this.lblHeader.setText(text);
    }

    public void setFilter(String filter) {
        this.txtFilter.setText(filter);
    }

    public ObservableList getItems() {
        return listView.getItems();
    }

    public void addItem(Object item) {
        listView.getItems().add(item);
    }

    public void setCellFactory(Callback<ListView, ListCell> callBack) {
        listView.setCellFactory(callBack);
    }

    public MultipleSelectionModel getSelectionModel() {
        return listView.getSelectionModel();
    }
}
