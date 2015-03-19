package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXICCWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * UI representation for a project entry in a treeview
 */
public class ProjectAnalysisTreeItemController extends CustomCellItemsHelper.CustomCellItemController<FXICCWrapper> {

    @FXML
    private Label analyisName;

    @Override
    public void itemSet(final FXICCWrapper item) {
        analyisName.setText(item.getAnalysisID());
    }
}
