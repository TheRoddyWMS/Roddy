/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

/**
 * A custom table cell callback implementation which can take up to two icons.
 * One if the table cell value (Boolean) is empty or null
 * One if the table cell value is true
 * Both images can be set to null.
*/
class TableCellIconCallBack implements Callback<TableColumn, TableCell> {

    private Image image;

    private Image falseIcon;

    TableCellIconCallBack(Image image) {
        this.image = image;
    }

    TableCellIconCallBack(Image image, Image falseIcon) {
        this.image = image;
        this.falseIcon = falseIcon;
    }

    public TableCell call(TableColumn tableColumn) {
        TableCell cell = new TableCell<String, Boolean>() {
            @Override
            protected void updateItem(Boolean tableCellValue, boolean empty) {
                super.updateItem(tableCellValue, empty);
                if(empty)
                    setGraphic(null);
                if (tableCellValue == null || tableCellValue == Boolean.FALSE)
                    setGraphic(falseIcon == null ? null : new ImageView(falseIcon));
                else
                    setGraphic(image == null ? null : new ImageView(image));
            }
        };
        return cell;
    }
}
