package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import javafx.scene.control.ListCell;


/**
 * Custom list view cell implementation which is loading an fxml file and a controller for its style.
 */
public abstract class CustomListCellImplementation<T, C extends CustomCellItemsHelper.CustomCellItemController<T>> extends ListCell<T> implements CustomCellItemsHelper.CustomCellItem {

    private C controller;

    public CustomListCellImplementation() {
    }

    @Override
    public void registerController(CustomCellItemsHelper.CustomCellItemController pc) {
        controller = (C)pc;
    }

    protected void updateItem(T itemWrapper) {};

    protected C getController() {
        return controller;
    }

    @Override
    protected void updateItem(T itemWrapper, boolean b) {
        super.updateItem(itemWrapper, b);
        if (itemWrapper == null) return;

        CustomCellItemsHelper.LoaderResult loaderResult = new CustomCellItemsHelper().loadCustomCell(this, itemWrapper);
        setGraphic(loaderResult.node);

        ((C)loaderResult.controller).setItem(itemWrapper);

        updateItem(itemWrapper);
    }

}
