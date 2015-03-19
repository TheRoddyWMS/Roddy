package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import javafx.scene.layout.BorderPane;

/**
 */
public abstract class CustomControlOnBorderPane extends BorderPane {
    protected CustomControlOnBorderPane() {
        CustomControlHelper.loadFXML(this);
//        String className = getClass().getSimpleName();
//        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(className + ".fxml"));
//        fxmlLoader.setRoot(this);
//        fxmlLoader.setController(this);
////
//        try {
//            fxmlLoader.load();
//        } catch (Exception ex) {
//            throw new RuntimeException(ex);
//        }
    }
}
