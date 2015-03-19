package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import de.dkfz.roddy.client.fxuiclient.DataSetView;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import javafx.fxml.FXML;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;

import java.io.File;


/**
 * Controller for the ConfigurationGraph javafx element for displaying a relationship graph between configurations.
 */
public class ScriptEditorComponent extends CustomControlOnBorderPane {

    @FXML
    private HTMLEditor editorComponent;

    WebView webView = new WebView();

    public ScriptEditorComponent() {
    }

    public ScriptEditorComponent(File file) {
//        editorComponent.getEngine().

        loadScript(file);
    }

    private void loadScript(File file) {
        RoddyUITask.runTask(new DataSetView.LoadFileToTextAreaTask(editorComponent, file));
    }
}