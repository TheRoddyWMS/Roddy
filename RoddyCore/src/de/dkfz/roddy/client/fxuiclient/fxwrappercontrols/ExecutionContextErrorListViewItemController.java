package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import de.dkfz.roddy.tools.RoddyIOHelperMethods;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXExecutionContextErrorWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import java.util.logging.Level;

/**
 */
public class ExecutionContextErrorListViewItemController extends CustomCellItemsHelper.CustomCellItemController<FXExecutionContextErrorWrapper> {

    public Tooltip lblErrorException;
    public Label lblErrorDetails;
    @FXML
    private BorderPane borderPane;

    @FXML
    private ImageView indicatorError;

    @FXML
    private ImageView indicatorWarning;

    @FXML
    private ImageView indicatorInfo;

    @FXML
    private Label lblErrorText;

    @FXML
    private Label lblErrorClass;

    @Override
    protected void itemSet(FXExecutionContextErrorWrapper item) {
        lblErrorText.setText(item.getErrorText());
        lblErrorClass.setText(item.getErrorLevel());
        lblErrorDetails.setText(item.getError().getAdditionalInfo());
        lblErrorException.setText(RoddyIOHelperMethods.getStackTraceAsString(item.getError().getException()));

        if (item.getErrorLevel() == Level.WARNING.getName()) {
            indicatorWarning.setVisible(true);
//        } else if (item.getErrorLevel() == Level.SEVERE.getName()) {
//            indicatorWarning.setVisible(true);
        } else
        if (item.getErrorLevel() == Level.INFO.getName()) {
            indicatorInfo.setVisible(true);
        }
        if (item.hasException()) {
            Label lblException = new Label(item.getError().getException().toString());
            borderPane.getChildren().add(lblException);
//            borderPane.setBottom(lblException);
        } else
            borderPane.setBottom(null);
    }
}
