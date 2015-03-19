package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXConfigurationValueWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;

public class ConfigurationValueCell<T> extends TableCell<FXConfigurationValueWrapper, T> {
    @Override
    protected void updateItem(T s, boolean b) {
        super.updateItem(s, b);
        if(s == null)
            return;
        FXConfigurationValueWrapper item = (FXConfigurationValueWrapper) getTableRow().getItem();
        TableRow row = getTableRow();
    }
//
//    public static class Factory implements Callback<TableColumn<FXConfigurationValueWrapper, String>, TableCell<FXConfigurationValueWrapper, String>>  {
//        @Override
//        public TableCell<FXConfigurationValueWrapper, String> call(TableColumn<FXConfigurationValueWrapper, String> fxConfigurationValueWrapperStringTableColumn) {
//            return new ConfigurationValueCell();
//        }
//    }
}
