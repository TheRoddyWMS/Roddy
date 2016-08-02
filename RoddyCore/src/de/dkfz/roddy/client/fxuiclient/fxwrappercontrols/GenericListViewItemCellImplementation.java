/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

/**
 */
public class GenericListViewItemCellImplementation<T> extends ListCell<T> implements CustomCellItemsHelper.CustomCellItem {

    private CustomCellItemsHelper.CustomCellItemController<T> controller;

    @Override
    public void registerController(CustomCellItemsHelper.CustomCellItemController controller) {
        this.controller = controller;
    }


    @Override
    protected void updateItem(T itemWrapper, boolean empty) {
        super.updateItem(itemWrapper, empty);
        if (empty || itemWrapper == null) {
            setGraphic(new Label());
            return;
        }

        CustomCellItemsHelper.LoaderResult loaderResult = new CustomCellItemsHelper().loadCustomCell(this, itemWrapper);
        setGraphic(loaderResult.node);

        CustomCellItemsHelper.CustomCellItemController<T> _controller = (CustomCellItemsHelper.CustomCellItemController<T>) loaderResult.controller;

            _controller.setItem(itemWrapper);

//        if (controller != null)
//            controller.setItem(itemWrapper);
//        else
//            throw new RuntimeException("The controller must be set.");
    }
}
