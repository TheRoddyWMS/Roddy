/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.ResourceBundle;

public class CustomCellItemsHelper {

    public static abstract class CustomCellItemController<T> implements Initializable {

        private T item;

        private ObjectProperty _item = new SimpleObjectProperty();

        @Override
        public final void initialize(URL url, ResourceBundle resourceBundle) {
            if (resourceBundle != null) {
                item = (T) resourceBundle.getObject("Parent");
                if (item instanceof CustomCellItem) {
                    ((CustomCellItem) item).registerController(this);
                }
            }
            postInitiliazation(url, resourceBundle);
            initialize();
        }

        /**
         * Can be overriden and will be called within initialize(...)
         *
         * @param url
         * @param resourceBundle
         */
        protected void postInitiliazation(URL url, ResourceBundle resourceBundle) {

        }

        public T getItem() {
            return item;
        }

        public T get_item() {
            return (T) _item.get();
        }

        public ObjectProperty<T> itemProperty() {
            return _item;
        }

        public void initialize() {
        }

        public final void setItem(T item) {
            if(this.item == item) return; //Don't do it again...Can lea
            this.item = item;
            this._item.setValue(item);
            itemSet(item);
        }

        protected void itemSet(T item) {
        }
    }

    public static abstract interface CustomCellItem {

        public void registerController(CustomCellItemController controller);
    }

    public CustomCellItemsHelper() {
    }

    private static Map<CustomCellItem, Node> customCellItemsCache = new HashMap<>();

    private static Map<Node, Object> cellControllersByNodeCache = new HashMap<>();

    private static Map<CustomCellItem, Integer> customCellItemsCachReads = new HashMap<>();

    public static class LoaderResult  {
        public final Node node;
        public final Object controller;

        public LoaderResult(Node node, Object controller) {
            this.node = node;
            this.controller = controller;
        }
    }

    LoaderResult loadCustomCell(final CustomCellItem cellItem, final Object item) {
        return loadCustomCell(cellItem, null, item);
    }

    LoaderResult loadCustomCell(final CustomCellItem cellItem, String cellItemClassName, final Object item) {
        boolean contains = false;
        synchronized (customCellItemsCache) {
            contains = customCellItemsCache.containsKey(cellItem);
        }

//        String itemID = cellItem.getClass().getSimpleName() + ":" + cellItem.hashCode();

//        long t = ExecutionService.measureStart();
        if (!contains) {
            Node node = null;
            Object controller = null;
            try {

                FXMLLoader loader = new FXMLLoader();
                URL url;
                String name = "";
                if (cellItemClassName == null) {
                    ListResourceBundle lr = new ListResourceBundle() {
                        @Override
                        protected Object[][] getContents() {
                            return new Object[][]{
                                    {"Parent", cellItem},
                            };
                        }
                    };
                    Class itemClass = cellItem.getClass();
                    if (itemClass == GenericListViewItemCellImplementation.class) {
                        String itemClassName = item.getClass().getName();
                        int indexOfPoint = itemClassName.lastIndexOf(".");
                        int indexOfCell = itemClassName.indexOf("Wrapper");
                        try {
                            name = itemClassName.substring(indexOfPoint + 3, indexOfCell) + "ListViewItem.fxml";
                        } catch (Exception e) {
                            throw e;
                        }
                    } else {
                        String itemClassName = itemClass.getName();
                        int indexOfPoint = itemClassName.lastIndexOf(".");
                        int indexOfCell = itemClassName.indexOf("CellImplementation");
                        name = itemClassName.substring(indexOfPoint + 1, indexOfCell) + ".fxml";
                    }
                    loader.setResources(lr);
                } else {
                    name = cellItemClassName + ".fxml";
                }
                url = cellItem.getClass().getResource(name);
                loader.setLocation(url);
                node = loader.load();
                controller = loader.getController();

//                ExecutionService.measureStop(t, "cellItem " + itemID + " uncached, create and put to cache");
            } catch (Exception e) {
                VBox hbox = new VBox();
                hbox.getChildren().add(new Label("Fallback: (" + cellItem.getClass().getSimpleName() + " / " + item.getClass().getSimpleName() + ")"));
                hbox.getChildren().add(new Label(item.toString()));
                node = hbox;
                e.printStackTrace();
            }
            synchronized (customCellItemsCache) {
                customCellItemsCache.put(cellItem, node);
                cellControllersByNodeCache.put(node, controller);
                customCellItemsCachReads.put(cellItem, 0);
            }
        } else {
//            ExecutionService.measureStop(t, "cellItem " + itemID + " cached, took from cache");
        }
        Node node = null;
        Object controller = null;
        synchronized (customCellItemsCache) {
            node = customCellItemsCache.get(cellItem);
            controller = cellControllersByNodeCache.get(node);
            int noOfReads = customCellItemsCachReads.get(cellItem) + 1;
            customCellItemsCachReads.put(cellItem, noOfReads);
//            ExecutionService.measureStop(t, "\titem " + itemID + " read count: " + noOfReads);
        }
        return new LoaderResult(node, controller);
    }
}