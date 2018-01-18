/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient;

import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Controller for the LogViewer component.
 * Registers to Roddies log stuff and create logging tabs for each logger.
 */
public class LogViewer extends CustomControlOnBorderPane {
    public static final String UITASK_ADDLOGMESSAGE = "Add log message";

    @FXML
    private ListView allEntriesList;

    @FXML
    private TabPane tabPane;

    private ObservableList<String> allEntries = FXCollections.observableArrayList();
    private Map<String, ListView<String>> loggerLists = new HashMap<>();

    public LogViewer() {

        allEntriesList.setItems(allEntries);

        LogManager.getLogManager().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                throw new UnsupportedOperationException("Not supported yet!"); //  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        Logger.getLogger("").addHandler(new Handler() {
            @Override
            public void publish(final LogRecord record) {
                if (record.getLoggerName().contains(RoddyUITask.class.getName())) return;
                RoddyUITask.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!record.getLoggerName().startsWith("de.dkfz.roddy")) return;
                        if (!loggerLists.containsKey(record.getLoggerName())) {
                            ListView<String> lv = new ListView<>();
                            String[] name = record.getLoggerName().split("[.]");
                            Tab tab = new Tab(name[name.length - 1]);
                            tab.setContent(lv);
                            tabPane.getTabs().add(tab);
                            loggerLists.put(record.getLoggerName(), lv);
                        }
                        String message = record.getLevel() + " " + record.getMessage();
                        loggerLists.get(record.getLoggerName()).getItems().add(message);
                        allEntriesList.getItems().add(message);
                    }
                }, UITASK_ADDLOGMESSAGE, false);
            }

            @Override
            public void flush() {
//                throw new UnsupportedOperationException("Not supported yet!"); //  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void close() throws SecurityException {
//                throw new UnsupportedOperationException("Not supported yet!"); //  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

    }
}
