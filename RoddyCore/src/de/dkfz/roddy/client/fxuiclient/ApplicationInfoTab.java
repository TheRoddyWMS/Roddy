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
public class ApplicationInfoTab extends CustomControlOnBorderPane {

}
