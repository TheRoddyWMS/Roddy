package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXICCWrapper;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

/**
 * UI representation for a project entry in a treeview
 */
public class ProjectTreeItemController extends CustomCellItemsHelper.CustomCellItemController<FXICCWrapper> {

    @FXML
    private Label projectName;

    @FXML
    private VBox analyses;

    @FXML
    private Label imgAnalysis;

    @FXML
    private ImageView projectIcon;

    @FXML
    private Image projectIconEnabled;

    @FXML
    private Image projectAnalysis;

    private ProjectTreeItemCellImplementation pi;

    @Override
    public void initialize() {
    }

    @Override
    public void itemSet(final FXICCWrapper item) {
        projectName.setText(item.getName());
        item.setCurrentlySelectedAnalysisID(item.getAnalyses().size() > 0 ? item.getAnalyses().get(0) : "");

        if (item.getAnalyses().size() == 0) {
//            analyses.getChildren().remove(imgAnalysis);
//            analyses.setMaxHeight(0);
//            analyses.setMinHeight(0);
//            analyses.setPrefHeight(0);
        } else {
            projectIcon.setImage(projectIconEnabled);
//            analyses.getChildren().clear();
            for (int i = 0; i < item.getAnalyses().size(); i++) {
                if (i >= 0)
                    continue;
                final String analysisStr = item.getAnalyses().get(i);
                final String[] analysisArr = analysisStr.split("::");
                final Label hl = new Label(analysisArr[0]);
                hl.setId("AnalysisHyperlink");
                //OnMouseClicked is handled by the tree click controller directly in RoddyUIController
//                hl.setOnMouseClicked(new EventHandler<MouseEvent>() {
//                    @Override
//                    public void handle(MouseEvent actionEvent) {
//                        pi.analysisSelected(item, hl.getText());
//                    }
//                });
                hl.setOnMouseExited(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(final MouseEvent mouseEvent) {
                        RoddyUITask.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                Label hl = (Label) mouseEvent.getSource();
                                hl.setId("AnalysisHyperlink");
                            }
                        }, "id update task out", false);
                    }
                });
                hl.setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(final MouseEvent mouseEvent) {
                        item.setCurrentlySelectedAnalysisID(hl.getText());
                        RoddyUITask.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                Label hl = (Label) mouseEvent.getSource();
                                hl.setId("AnalysisHyperlinkHover");
                            }
                        }, "id update task in", false);
                    }
                });
                hl.setOnMousePressed(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        item.setCurrentlySelectedAnalysisID(hl.getText());
                        hl.setId("AnalysisHyperlinkPressed");
                    }
                });
                hl.setOnMouseReleased(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        item.setCurrentlySelectedAnalysisID(hl.getText());
                        hl.setId("AnalysisHyperlinkReleased");
                    }
                });
//                hl.setOnMouseMoved(new EventHandler<MouseEvent>() {
//                    @Override
//                    public void handle(MouseEvent mouseEvent) {
//                        hl.setId("AnalysisHyperlinkHover");
//                    }
//                });
                hl.setGraphic(new ImageView(projectAnalysis));
                analyses.getChildren().add(hl);
            }
        }
    }

    private void checkAndSetLabelHeight(Control control, String item) {
        if (item == null || item.trim().equals("")) {
            if (control instanceof Label) ((Label) control).setGraphic(null);
            control.setMinHeight(0);
            control.setMaxHeight(0);
            control.setPrefHeight(0);
        }
    }
}
