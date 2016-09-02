/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

/**
 */
public final class CustomControlHelper {
    private CustomControlHelper() {
    }

    public static <T extends Pane> void loadFXML(T object) {
        Class cls = object.getClass();
        String className = cls.getSimpleName();
        FXMLLoader fxmlLoader = new FXMLLoader(cls.getResource(className + ".fxml"));
        fxmlLoader.setRoot(object);
        fxmlLoader.setController(object);

        try {
            fxmlLoader.load();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
