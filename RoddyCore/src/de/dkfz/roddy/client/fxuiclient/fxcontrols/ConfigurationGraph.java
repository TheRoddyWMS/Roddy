package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import de.dkfz.roddy.Constants;
import de.dkfz.roddy.client.fxuiclient.RoddyUITask;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXConfigurationWrapper;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import static de.dkfz.roddy.StringConstants.BRACE_LEFT;
import static de.dkfz.roddy.StringConstants.BRACE_RIGHT;
import static de.dkfz.roddy.StringConstants.SPLIT_WHITESPACE;


/**
 * Controller for the ConfigurationGraph javafx element for displaying a relationship graph between configurations.
 */
public class ConfigurationGraph extends CustomControlOnBorderPane {

    private double SCALE_FACTOR = 80;
    final private ObservableList<FXConfigurationWrapper> selectedConfigs = FXCollections.observableArrayList();

    @FXML
    private Pane paneConfigurationNodes;

    public ConfigurationGraph() {
    }


    /**
     * Set configurations to be displayed.
     *
     * @param configs - a mapping from the configuration's id to its wrapper
     */
    public void setConfigurations(final Map<String, FXConfigurationWrapper> configs) {
        RoddyUITask.runTask(new RoddyUITask<Void>("calculate configuration viewer graph") {

            GraphDefinition graphDefinition;

            @Override
            public Void _call() throws Exception {
                graphDefinition = calculateNodeCoordinates(configs);
                graphDefinition.scale(SCALE_FACTOR);
                return null;
            }

            @Override
            public void _succeeded() {
                renderGraph(graphDefinition, configs);
            }
        });
    }

    /**
     * @return the ObservableList of selected configurations
     */
    public ObservableList<FXConfigurationWrapper> getSelectedConfigs() {
        return selectedConfigs;
    }

    /**
        The pipeline for generating the graph is:
        calculateNodeCoordinates (this is done asynchronously)
            generateGraphDescription (generate textual graph description for graphviz)
            execute graphviz and feed the description to it
            parseGraphVizOutput (parse graphviz output into a structure suitable for rendering)
        renderGraph
     */
    private static GraphDefinition calculateNodeCoordinates(Map<String, FXConfigurationWrapper> configs) {
        String graphVizInput = "digraph G " + BRACE_LEFT + Constants.ENV_LINESEPARATOR +
                "rankdir = LR;" + Constants.ENV_LINESEPARATOR +
                "rotate=90;" + Constants.ENV_LINESEPARATOR +
                generateGraphDescription(configs) + Constants.ENV_LINESEPARATOR +
                BRACE_RIGHT;

//        System.out.println(graphVizInput);

        ProcessBuilder pBuilder = new ProcessBuilder("dot", "-Tplain");
        try {
            Process proc = pBuilder.start();
            proc.getOutputStream().write(graphVizInput.getBytes());
            proc.getOutputStream().flush();
            proc.getOutputStream().close();
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            List<String> outputLines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                outputLines.add(line);
            }
            proc.waitFor();
            return parseGraphVizOutput(outputLines);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateGraphDescription(Map<String, FXConfigurationWrapper> configs) {
        StringBuilder result = new StringBuilder();

        for (FXConfigurationWrapper config : configs.values()) {
            result.append(config.getID() + " [shape=rectangle,width=1.4,height=0.4,fixedsize=true];\n");

            for (StringProperty parent : config.parentConfigsProperty()) {
                result.append(config.getID() + " -> " + parent.get() + " [dir=none];\n");
            }
        }

        return result.toString();
    }

    // java does not have a built in pair type, so we reinvent the wheel:
    private static class Point {
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Point() {
            this(0, 0);
        }

        public void scale(double factor) {
            x *= factor;
            y *= factor;
        }

        double x;
        double y;
    }

    private static class GraphNode {
        public String id;
        Point position;
    }

    private static class GraphEdge {
        // interpolation values for edgeCoordinates
        // apparently, graphviz gives 3n + 1 interpolation values for some n > 0,
        // so we can interpolate with cubic polynomials
        public List<Point> coordinates = new ArrayList<>();
        public String from;
        public String to;
    }

    private static class GraphDefinition {
        public List<GraphNode> nodes = new ArrayList<>();
        public List<GraphEdge> edges = new ArrayList<>();

        public void scale(double factor) {
            for (GraphNode node : nodes) {
                node.position.scale(factor);
            }
            for (GraphEdge edge : edges) {
                for (Point point : edge.coordinates) {
                    point.scale(factor);
                }
            }
        }
    }

    private static GraphDefinition parseGraphVizOutput(List<String> lines) {
        GraphDefinition result = new GraphDefinition();
        for (String line : lines) {
            List<String> words = Arrays.asList(line.split(SPLIT_WHITESPACE));
            if (words.isEmpty())
                continue;

            String edgeType = words.get(0);
            if (edgeType.equals("node")) {
                GraphNode node = new GraphNode();
                node.id = words.get(1);
                node.position = new Point(Double.parseDouble(words.get(2)), Double.parseDouble(words.get(3)));
                result.nodes.add(node);
            } else if (edgeType.equals("edge")) {
                GraphEdge edge = new GraphEdge();
                edge.from = words.get(1);
                edge.to = words.get(2);
                int startIndex = 4; // first index with interpolation values
                int interpolationPointNumber = Integer.parseInt(words.get(3));
                for (int i = startIndex; i < startIndex + interpolationPointNumber * 2; i += 2) {
                    double x = Double.parseDouble(words.get(i));
                    double y = Double.parseDouble(words.get(i + 1));
                    edge.coordinates.add(new Point(x, y));
                }
                result.edges.add(edge);
            }
        }
        return result;
    }

    private void renderGraph(GraphDefinition graphDefinition, Map<String, FXConfigurationWrapper> configs) {
        paneConfigurationNodes.getChildren().clear();
        selectedConfigs.clear();

        // save created javafx node elements so edges can be linked to them
        Map<String, ConfigurationGraphNode> nodeElements = new HashMap<>();

        for (GraphNode node : graphDefinition.nodes) {
            final FXConfigurationWrapper currentConfig = configs.get(node.id);
            ConfigurationGraphNode nodeElement = new ConfigurationGraphNode(currentConfig);

            nodeElement.setOnMouseClicked(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent mouseEvent) {
                    if (mouseEvent.isControlDown()) {
                        // just select or deselect the clicked config
                        if (selectedConfigs.contains(currentConfig)) {
                            selectedConfigs.remove(currentConfig);
                        } else {
                            selectedConfigs.add(currentConfig);
                        }
                    } else {
                        if (selectedConfigs.contains(currentConfig)) {
                            // if the config was already selected, remove it from selected configs
                            selectedConfigs.remove(currentConfig);
                        } else {
                            // otherwise, make it the sole selected config
                            selectedConfigs.setAll(Arrays.asList(currentConfig));
                        }
                    }

                    mouseEvent.consume();
                }
            });

            nodeElement.setLayoutX(node.position.x - nodeElement.getPrefWidth() / 2);
            nodeElement.setLayoutY(node.position.y - nodeElement.getPrefHeight() / 2);

            nodeElements.put(node.id, nodeElement);
            paneConfigurationNodes.getChildren().add(nodeElement);
        }
        for (GraphEdge edge : graphDefinition.edges) {
            ConfigurationGraphEdge edgeElement = new ConfigurationGraphEdge();
            ConfigurationGraphNode from = nodeElements.get(edge.from);
            ConfigurationGraphNode to = nodeElements.get(edge.to);

            // at least one of its nodes is grayed out => edge is grayed out
            edgeElement.grayedOutProperty().bind(from.grayedOutProperty().or(to.grayedOutProperty()));

            Iterator<Point> it = edge.coordinates.iterator();
            Point firstPoint = it.next();
            edgeElement.getElements().add(new MoveTo(firstPoint.x, firstPoint.y));

            while (it.hasNext()) {
                // this should be ok, see comment in definition of 'GraphEdge'
                Point p1 = it.next();
                Point p2 = it.next();
                Point p3 = it.next();
                edgeElement.getElements().add(new CubicCurveTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y));

            }
            paneConfigurationNodes.getChildren().add(0,edgeElement);
        }
        paneConfigurationNodes.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent mouseEvent) {
                selectedConfigs.clear();
            }
        });

    }
}