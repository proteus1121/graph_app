import com.google.common.collect.Lists;
import com.mxgraph.analysis.mxGraphAnalysis;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

class MainFrame extends JFrame {

    private static final int COUNT_OF_ROWS = 10;
    private static final int COUNT_OF_ELEMENTS_IN_ROW = 10;
    private List<List<Object>> objects = new ArrayList<>();

    MainFrame() {
        super("Discrete modeling of an object in 2D");

        mxGraph graph = new mxGraph();
        graph.setAllowDanglingEdges(false);
        graph.setAllowLoops(false);
        graph.setCellsResizable(false);
        graph.setCellsMovable(false);
        graph.setCellsEditable(false);
        graph.setEdgeLabelsMovable(false);
        graph.setCellsLocked(true);
        graph.setEnabled(false);

        Object parent = graph.getDefaultParent();

        CellFactory objectFactory = new CellFactory();

        graph.getModel().beginUpdate();
        try {
            //init 2D field
            int count = 0;
            for (int x = 0; x < COUNT_OF_ROWS; x++) {
                List<Object> row = new ArrayList<>();
                objects.add(row);
                for (int y = 0; y < COUNT_OF_ELEMENTS_IN_ROW; y++) {
                    Object cell = objectFactory.createCell(graph, parent, String.valueOf(count), x * (CellFactory.WEIGHT + CellFactory.THRESHOLD), y * (CellFactory.HEIGHT + CellFactory.THRESHOLD));
                    row.add(cell);
                    count++;
                }
            }

            //rand links between objects
            List<Object> shortestPath = getFindShortestPath(graph);
            while (shortestPath.isEmpty()){
                Random rand = new Random();
                int numberOfRow = rand.nextInt(COUNT_OF_ROWS);
                int numberOfElement = rand.nextInt(COUNT_OF_ELEMENTS_IN_ROW);
                Object intersection = objects.get(numberOfRow).get(numberOfElement);

                int direction = rand.nextInt(3);
                Object neighboringIntersection = getNeighboringIntersection(numberOfRow, numberOfElement, direction);

                if (!neighboringIntersection.equals(intersection)) {
                    graph.insertEdge(parent, null, "", intersection, neighboringIntersection, Styles.BLUE.getStyle());
                }
                shortestPath = getFindShortestPath(graph);
            }

           graph.setCellStyle(Styles.GREEN.getStyle(), shortestPath.toArray());

        } finally {
            graph.getModel().endUpdate();
        }

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent);
    }

    private List<Object> getFindShortestPath(mxGraph graph) {
        mxGraphAnalysis analysis = mxGraphAnalysis.getInstance();
        List<Object> firstRow = this.objects.get(0);
        List<Object> lastRow = this.objects.get(objects.size() - 1);
        List<List<Object>> shortestPaths = firstRow.stream()
                .map(o -> Arrays.asList(lastRow.stream()
                        .map(o2 -> analysis.getShortestPath(graph, o, o2, null, 999, false))
                        .filter(objects1 -> objects1.length != 0)
                        .min(Comparator.comparingInt(o1 -> o1.length)).orElse(new Object[0])))
                .collect(Collectors.toList());
        return shortestPaths.stream().min(Comparator.comparingInt(List::size)).orElse(Lists.newArrayList());
    }

    private Object getNeighboringIntersection(int numberOfRow, int numberOfElement, int direction) {
        int i;
        switch (direction) {
            case 0:
                i = numberOfElement - 1;
                if (objects.get(numberOfRow).size() > i && i >= 0)
                    return objects.get(numberOfRow).get(i);
            case 1:
                i = numberOfElement + 1;
                if (objects.get(numberOfRow).size() > i && i >= 0)
                    return objects.get(numberOfRow).get(i);
            case 2:
                i = numberOfRow - 1;
                if (objects.size() > i && i >= 0)
                    return objects.get(i).get(numberOfElement);
            case 3:
                i = numberOfRow + 1;
                if (objects.size() > i && i >= 0)
                    return objects.get(i).get(numberOfElement);
        }
        return objects.get(numberOfRow).get(numberOfElement);
    }
}
