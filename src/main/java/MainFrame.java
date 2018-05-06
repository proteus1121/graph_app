import com.mxgraph.analysis.mxGraphAnalysis;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class MainFrame extends JFrame {

    private static final int COUNT_OF_ROWS = 10;
    private static final int COUNT_OF_ELEMENTS_IN_ROW = 10;
    private static int iterationCount = 0;
    private List<List<Object>> objects = new ArrayList<>();
    private final List<EdgeDecorator> edges = new ArrayList<>();

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

        graph.getModel().beginUpdate();
        try {
            //init 2D field
            generatePoints(graph, parent);

            //rand links between objects
            generateLinks(graph, parent);

            //analyze all links
            //TODO: analyze all links after each step
//            analyzeLinks();

            //remove first dangling links
            removeFirstDanglingLinks(graph, parent);

            //find shortest path
            Object[] shortestPath = findShortestPath(graph);
            if (shortestPath.length > 0) {
                System.out.println("Shortest path found!");
            } else {
                System.out.println("None path found!");
            }
//            graph.setCellStyle(Styles.GREEN.getStyle(), shortestPath);

        } finally {
            graph.getModel().endUpdate();
        }

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent);
    }

    private void analyzeLinks() {
        List<Object> firstRow = this.objects.get(0);
        List<Object> lastRow = this.objects.get(objects.size() - 1);

        List<EdgeDecorator> edgeOfFirstRow = edges.stream()
                .filter(edgeDecorator -> firstRow.contains(edgeDecorator.getSource()) || firstRow.contains(edgeDecorator.getTarget()))
                .collect(Collectors.toList());

        List<EdgeDecorator> edgeOfLastRow = edges.stream()
                .filter(edgeDecorator -> lastRow.contains(edgeDecorator.getSource()) || lastRow.contains(edgeDecorator.getTarget()))
                .collect(Collectors.toList());

//        if (!isEntry) {
//            isEntry = edgeOfFirstRow.stream()
//                    .anyMatch(EdgeDecorator::isEntry);
//        }
//        if (!isExit) {
//            isExit = edgeOfFirstRow.stream()
//                    .anyMatch(EdgeDecorator::isExit);
//        }
    }

    private void removeFirstDanglingLinks(mxGraph graph, Object parent) {
        List<mxCell> collect = edges.parallelStream().filter(edgeDecorator -> !edgeDecorator.isEntry() && !edgeDecorator.isExit()).map(EdgeDecorator::getEdge).collect(Collectors.toList());
//        edges.removeAll(collect);
        collect.forEach(edge -> edge.setStyle(Styles.RED.getStyle()));

//        AtomicInteger count = new AtomicInteger();
//        mxAnalysisGraph mxAnalysisGraphInst = new mxAnalysisGraph();
//        mxAnalysisGraphInst.setGraph(graph);
//        List<Object> lastRowEdges = this.objects.get(objects.size() - 1).stream().map(graph::getEdges).collect(Collectors.toList());
//        edges.forEach(o ->
//        {
//            AtomicBoolean isEnded = new AtomicBoolean(false);
//            mxTraversal.bfs(mxAnalysisGraphInst, o, (vertex, edge) ->
//            {
//                boolean contains = lastRowEdges.contains(edge);
//                if (contains)
//                    isEnded.set(contains);
//                return false;
//            });
//            if (!isEnded.get()) {
//                System.out.println("Remove first dangling links... progress: " + count + "/" + edges.size());
//                count.getAndIncrement();
//                graph.getModel().remove(o);
//            }

        //      boolean isLinkDangling = lastRow.stream()
//          .anyMatch(o1 -> mxAnalysisGraphInst.get(o, o1, false, false).length == 0);

//      if (isLinkDangling)
//      {
//        System.out.println("Remove first dangling links... progress: " + count + "/" + edges.size());
//        count.getAndIncrement();
//        graph.getModel().remove(o);
//      }
//    });
    }

    private void generateLinks(mxGraph graph, Object parent) {
        AtomicInteger count = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>();
        iterationCount = (COUNT_OF_ROWS - 1) * COUNT_OF_ELEMENTS_IN_ROW + (COUNT_OF_ELEMENTS_IN_ROW - 1) * COUNT_OF_ROWS;
        while (count.get() < iterationCount) {
//            Thread t = new Thread(() ->
//            {
                Random rand = new Random();
                int numberOfRow = rand.nextInt(COUNT_OF_ROWS);
                int numberOfElement = rand.nextInt(COUNT_OF_ELEMENTS_IN_ROW);
                List<Object> firstRow = this.objects.get(0);
                List<Object> lastRow = this.objects.get(objects.size() - 1);
                Object intersection = objects.get(numberOfRow).get(numberOfElement);

                int direction = rand.nextInt(3);
                Object neighboringIntersection = getNeighboringIntersection(numberOfRow, numberOfElement, direction);

                if (!neighboringIntersection.equals(intersection)) {
                    synchronized (edges) {
                        EdgeDecorator edge = EdgeUtilFactory.createEdge(graph, parent, intersection, neighboringIntersection, firstRow, lastRow, edges);
                        edges.add(edge);
                        if (edge.isExit() && edge.isEntry()) {
                            count.set(iterationCount);
                        }
                    }
                }
//            });
//            threads.add(t);
//            t.start();
            System.out.println("Generate random edges... progress: " + count.get() + "/" + iterationCount);
            count.getAndIncrement();
        }

        threads.forEach(thread ->
        {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        threads.clear();
    }

    private void generatePoints(mxGraph graph, Object parent) {
        AtomicInteger count = new AtomicInteger();
        int allCount = COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW;
        for (int x = 0; x < COUNT_OF_ROWS; x++) {
            List<Object> row = new ArrayList<>();
            objects.add(row);
            for (int y = 0; y < COUNT_OF_ELEMENTS_IN_ROW; y++) {
                Object cell = CellUtilFactory.createCell(graph,
                        parent,
                        String.valueOf(count.get()),
                        x * (CellUtilFactory.WEIGHT + CellUtilFactory.THRESHOLD),
                        y * (CellUtilFactory.HEIGHT + CellUtilFactory.THRESHOLD));
                row.add(cell);
                count.getAndIncrement();
                System.out.println("Generate points... progress: " + count + "/" + allCount);
            }
        }
    }

    private Object[] findShortestPath(mxGraph graph) {
        final AtomicInteger count = new AtomicInteger(0);
        mxGraphAnalysis analysis = mxGraphAnalysis.getInstance();
        List<Object> firstRow = this.objects.get(0);
        List<Object> lastRow = this.objects.get(objects.size() - 1);
        final int allCount = firstRow.size() * lastRow.size();

// if you want to find min path between any points, from left side to right side:
        return firstRow.parallelStream()
                .map(o -> lastRow.parallelStream().map(o1 ->
                {
                    System.out.println("Try to find shortest path... progress: " + count.getAndIncrement() + " / " + allCount);
                    return analysis.getShortestPath(graph, o, o1, null, COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW, false);
                }).filter(objects1 -> objects1.length > 0).findAny())
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.of(new Object[0]))
                .orElse(new Object[0]);

// if you want to find min path between points with min path in the all matrix, from left side to right side:
//    List<List<Object>> shortestPaths = firstRow.stream().parallel().map(o -> Arrays.asList(lastRow.stream().parallel().map(o2 ->
//    {
//      System.out.println(graph.getModel().getValue(o) +" -> " + graph.getModel().getValue(o2));
//      System.out.println("Try to find shortest path... progress: " + count.getAndIncrement() + " / " + allCount);
//      return analysis.getShortestPath(graph, o, o2, null, COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW, false);
//    }).filter(objects1 -> objects1.length != 0).min(Comparator.comparingInt(o1 -> o1.length)).orElse(new Object[0]))).collect(Collectors.toList());
//    return shortestPaths.stream().parallel().min(Comparator.comparingInt(List::size)).orElse(Lists.newArrayList());
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
