package de.dkfz.roddy.client.fxuiclient;

import de.dkfz.roddy.core.CacheProvider;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for the LogViewer component.
 * Registers to Roddies log stuff and create logging tabs for each logger.
 */
public class CacheProviderViewer extends CustomControlOnBorderPane implements CacheProvider.CacheProviderListener, ListChangeListener<CacheProvider> {

    @FXML
    private TabPane tabPane;

    private Map<CacheProvider, ObservableMap<String, String[]>> valuesByCacheProviders = new HashMap<>();
    private Map<CacheProvider, VBox> vboxByCacheProvider = new HashMap<>();
    private Map<CacheProvider, Map<String, Label>> idLabelByCacheProviderAndValueID = new LinkedHashMap<>();
    private Map<CacheProvider, Map<String, Label>> valueLabelByCacheProviderAndValueID = new LinkedHashMap<>();
    private Map<CacheProvider, Map<String, Label>> countLabelByCacheProviderAndValueID = new LinkedHashMap<>();

    public CacheProviderViewer() {
        CacheProvider.getAllCacheProviders().addListener(this);
        for (Object cp : CacheProvider.getAllCacheProviders()) {
            ((CacheProvider) cp).addListener(this);
            addNewProvider(((CacheProvider)cp));
        }
    }


    private void checkContainers(CacheProvider newValue) {
        if (valuesByCacheProviders.containsKey(newValue)) return;
        valuesByCacheProviders.put(newValue, FXCollections.<String, String[]>observableHashMap());
        idLabelByCacheProviderAndValueID.put(newValue, new LinkedHashMap<String, Label>());
        valueLabelByCacheProviderAndValueID.put(newValue, new LinkedHashMap<String, Label>());
        countLabelByCacheProviderAndValueID.put(newValue, new LinkedHashMap<String, Label>());
    }

    @Override
    public void cacheValueAdded(final CacheProvider source, final String name, final String value) {
        checkContainers(source);
        valuesByCacheProviders.get(source).put(name, new String[]{value, "0"});
        RoddyUITask.runTask(new RoddyUITask("add cache value", false) {
            private HBox hbox;

            @Override
            public Object _call() throws Exception {
                hbox = new HBox();

//        hbox.setStyle("-fx-background-color: yellow;");
                Label idLabel = new Label(name);
                idLabel.setMinHeight(22);
                idLabel.setMinWidth(200);
                Label valueLabel = new Label(value);
                valueLabel.setMinHeight(22);
                valueLabel.setMinWidth(220);
                Label countLabel = new Label("0");
                countLabel.setMinHeight(22);
                countLabel.setMinWidth(50);
                hbox.getChildren().add(countLabel);
                hbox.getChildren().add(idLabel);
                hbox.getChildren().add(valueLabel);
                hbox.setPadding(new Insets(6));
//                hbox.setStyle("-fx-border-color: black;");
                idLabelByCacheProviderAndValueID.get(source).put(name, idLabel);
                valueLabelByCacheProviderAndValueID.get(source).put(name, valueLabel);
                countLabelByCacheProviderAndValueID.get(source).put(name, countLabel);
                return true;
            }

            @Override
            public void _succeeded() {
                ObservableList<Node> children = vboxByCacheProvider.get(source).getChildren();
                children.add(hbox);
                int i = children.indexOf(hbox);
                if (i % 2 == 1) {
                    hbox.setStyle("-fx-background-color: white;");
                } else {
                    hbox.setStyle("-fx-background-color: lightgray;");
                }
            }
        });
    }

    @Override
    public void cacheValueChanged(CacheProvider source, String name, String value) {
        checkContainers(source);
        valueLabelByCacheProviderAndValueID.get(source).get(name).setText(value);
    }

    @Override
    public void cacheValueRead(CacheProvider source, String name, int noOfReads) {
        checkContainers(source);
        Label l = countLabelByCacheProviderAndValueID.get(source).get(name);
        int count = 0;
        try {
            count = Integer.parseInt(l.getText()) + 1;
            l.setText("" + count);
        } catch (Exception ex) {
            //Wrong text...
        }

    }

    @Override
    public void onChanged(final Change<? extends CacheProvider> change) {
        if(!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    _onChanged(change);
                }
            });
        }
        else
            _onChanged(change);
    }

    private void _onChanged(Change<? extends CacheProvider> change) {
        for (CacheProvider newValue : change.getAddedSubList()) {
            addNewProvider(newValue);
        }    }

    private void addNewProvider(CacheProvider newValue) {
        newValue.addListener(this);
        checkContainers(newValue);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        GridPane gridPane = new GridPane();
//            gridPane.setStyle("-fx-background-color: purple;");
        scrollPane.setContent(gridPane);
//  gridPane.setAlignment(Pos.CENTER);
        VBox vbox = new VBox();
        vbox.setFillWidth(true);
//            vbox.setStyle("-fx-background-color: blue;");
        gridPane.getChildren().add(vbox);
        GridPane.setHgrow(vbox, Priority.ALWAYS);
        vboxByCacheProvider.put(newValue, vbox);


        String name = newValue.getID();
        Tab tab = new Tab(name);
        tab.setContent(scrollPane);
//            scrollPane.setStyle("-fx-background-color: red;");
        tabPane.getTabs().add(tab);
    }
}
