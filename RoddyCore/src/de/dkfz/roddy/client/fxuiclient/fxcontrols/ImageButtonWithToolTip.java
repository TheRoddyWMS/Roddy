package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Controller for the LogViewer component.
 * Registers to Roddies log stuff and create logging tabs for each logger.
 */
public class ImageButtonWithToolTip extends Button {

    private StringProperty imagePath = new SimpleStringProperty();

    private StringProperty tooltipText = new SimpleStringProperty();

    public ImageButtonWithToolTip() {
        this(null);
    }

    public ImageButtonWithToolTip(String imageURL) {
        this.setMnemonicParsing(true);
        this.setMaxWidth(1000000);
        this.setStyle("-fx-alignment: center-left; ");
        if (imageURL != null && imageURL.trim().length() > 0) {
            Image img = new Image(imageURL);
            setGraphic(new ImageView(img));
        } else
            setGraphic(null);
    }

    public String getImagePath() {
        return imagePath.get();
    }

    public void setImagePath(String imagePath) {
        this.imagePath.set(imagePath);
        this.setGraphic(new ImageView(new Image(imagePath)));
    }

//    }
//        return imagePath;
//    public StringProperty imagePathProperty() {
//
public void setTooltipText(String tooltipText) {
    this.tooltipText.set(tooltipText);
    this.setTooltip(new Tooltip(tooltipText));
}

    public String getTooltipText() {
        return tooltipText.get();
    }

    public StringProperty tooltipTextProperty() {
        return tooltipText;
    }

}
