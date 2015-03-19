package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXICCWrapper;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.ListResourceBundle;
import java.util.Map;


/**
 * Custom tree cell implementation which is loading an fxml file and a controller for its style.
 */
public class ProjectTreeItemCellImplementation extends TreeCell<FXICCWrapper> implements ProjectTreeItemCellListener, CustomCellItemsHelper.CustomCellItem {

    public static final Background BG_LIGHT_BLUE = new Background(new BackgroundFill(Color.LIGHTBLUE, new CornerRadii(0), new Insets(0)));
    public static final Background BG_ORANGE = new Background(new BackgroundFill(Color.ORANGE, new CornerRadii(0), new Insets(0)));
    public static final Background BG_TRANSPARENT = new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(0), new Insets(0)));
    private final TreeView parentTreeView;
    /**
     * One listener is allowed.
     * This is to handle custom events within the item controller class.
     */
    private ProjectTreeItemCellListener listener;

    private static Map<Integer, String> colMap = new LinkedHashMap<>();
    private FXICCWrapper itemWrapper;

    public ProjectTreeItemCellImplementation(TreeView parentTreeView, ProjectTreeItemCellListener listener) {
        this.parentTreeView = parentTreeView;
        if (colMap.size() == 0) {
            synchronized (colMap) {
                colMap.put(0, "0F");
                colMap.put(1, "2F");
                colMap.put(2, "4F");
                colMap.put(3, "6F");
                colMap.put(4, "8F");
                colMap.put(5, "AF");
                colMap.put(6, "CF");
                colMap.put(7, "EF");
//                colMap.put(8, "8F");
//                colMap.put(9, "9F");
//                colMap.put(10, "AF");
//                colMap.put(11, "BF");
//                colMap.put(12, "CF");
//                colMap.put(13, "DF");
//                colMap.put(14, "EF");
//                colMap.put(15, "FF");
                colMap.put(15 - 115, "18");
                colMap.put(15 - 114, "38");
                colMap.put(15 - 113, "58");
                colMap.put(15 - 112, "78");
                colMap.put(15 - 111, "98");
                colMap.put(15 - 110, "B8");
                colMap.put(15 - 109, "D8");
                colMap.put(15 - 108, "F8");
//                colMap.put(15 - 107, "88");
//                colMap.put(15 - 106, "98");
//                colMap.put(15 - 105, "A8");
//                colMap.put(15 - 104, "B8");
//                colMap.put(15 - 103, "C8");
//                colMap.put(15 - 102, "D8");
//                colMap.put(15 - 101, "E8");
//                colMap.put(15 - 100, "F8");
            }
        }
        this.listener = listener;
        this.hoverProperty().addListener((observableValue, aBoolean, newValue) -> {
            setColorOfItem(newValue);
        });
    }

    @Override
    public void registerController(CustomCellItemsHelper.CustomCellItemController pc) {
//        controller =  pc;
    }

    private static Map<FXICCWrapper, Node> nodeCache = new LinkedHashMap<>();

    /**
     * There are so many funny issues with JavaFX :). So this map stores a node to controller relation, so we can reset that on demand.
     * The controller is on a per node base, but "this" is on some sort of per view. A treeview for example has several cell implementations
     * which are changed and updated during runtime. So a controller / node mismatch can occur if the node and item is updated but not the controller.
     */
    private static Map<FXICCWrapper, CustomCellItemsHelper.CustomCellItemController<FXICCWrapper>> controllerCache = new LinkedHashMap<>();

    @Override
    protected void updateItem(FXICCWrapper itemWrapper, boolean empty) {
        this.itemWrapper = itemWrapper;
        CustomCellItemsHelper.CustomCellItemController controller = null;
        super.updateItem(itemWrapper, empty);
        if (empty || itemWrapper == null) {
            setGraphic(new Label());
            setBackground(Background.EMPTY);
            return;
        }

        try {
            Node node = null;
//            System.out.println("Selecting for itemWrapper " + itemWrapper.getID() + " : " + itemWrapper.getAnalysisID());
            synchronized (nodeCache) {
                if (nodeCache.containsKey(itemWrapper)) {
                    node = nodeCache.get(itemWrapper);
                    controller = controllerCache.get(itemWrapper);
                }
            }
            if (node == null) {
                final ProjectTreeItemCellImplementation THIS = this;
                FXMLLoader loader = new FXMLLoader();
                loader.setResources(new ListResourceBundle() {
                    @Override
                    protected Object[][] getContents() {
                        return new Object[][]{
                                {"Parent", THIS},
                        };
                    }
                });
                String className = "ProjectTreeItem.fxml";
                if (itemWrapper.isAnalysisWrapper()) {
                    className = "ProjectAnalysisTreeItem.fxml";
                }

                URL url = getClass().getResource(className);
                loader.setLocation(url);
                node = loader.load(url.openStream());
                Object controller1 = loader.getController();
                controller = (CustomCellItemsHelper.CustomCellItemController) controller1;

                synchronized (nodeCache) {
                    if (!nodeCache.containsKey(itemWrapper)) {
                        nodeCache.put(itemWrapper, node);
                        controllerCache.put(itemWrapper, (CustomCellItemsHelper.CustomCellItemController<FXICCWrapper>) controller1);
                    }
                }
            }
            setGraphic(node);
//                setGraphic(new CustomCellItemsHelper().loadCustomCell(this, itemWrapper));
            setColorOfItem(false);
            if (controller != null)
                controller.setItem(itemWrapper);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setColorOfItem(boolean hovering) {

        if (itemWrapper == null) {
            setStyle("-fx-border-width: 0 0 0 0;");
            setBackground(BG_TRANSPARENT);
            return;
        }

        if (hovering) {
            if (itemWrapper.isAnalysisWrapper())
                setBackground(BG_LIGHT_BLUE);
            else
                setBackground(BG_ORANGE);
            return;
        }

        final int MDEPTH = colMap.size() / 2;
        int depth = this.itemWrapper.getDepth();
        if (depth > MDEPTH) depth = MDEPTH;

        int ordinal = itemWrapper.getUIPosition();// getOrdinalOfTreeItem(parentTreeView.getRoot());
        if (ordinal % 2 == 1) {
            depth += 100;
        }
        String val = colMap.get(MDEPTH - depth);
        if (val == null) val = "FF";
        String color = String.format("#%s%s%s", val, val, val);
        setBackground(BG_TRANSPARENT);

        if (itemWrapper.isAnalysisWrapper())
            setStyle("-fx-border-width: 0 0 0 0; -fx-border-color: " + color + ";");
        else
            setStyle("-fx-border-width: 0 0 1 0; -fx-border-color: " + color + ";");
    }

    @Override
    public void analysisSelected(FXICCWrapper pWrapper, String analysisID) {
        listener.analysisSelected(pWrapper, analysisID);
    }
}
