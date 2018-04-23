import com.google.common.collect.Lists;
import com.mxgraph.analysis.mxGraphAnalysis;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class MainFrame extends JFrame
{

  private static final int COUNT_OF_ROWS = 200;
  private static final int COUNT_OF_ELEMENTS_IN_ROW = 200;
  private static final int ITERATION_COUNT = 70000;
  private List<List<Object>> objects = new ArrayList<>();

  MainFrame()
  {
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
    try
    {
      //init 2D field
      int count = 0;
      int allCount = COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW;
      for (int x = 0; x < COUNT_OF_ROWS; x++)
      {
        List<Object> row = new ArrayList<>();
        objects.add(row);
        for (int y = 0; y < COUNT_OF_ELEMENTS_IN_ROW; y++)
        {
          Object cell = objectFactory.createCell(graph,
              parent,
              String.valueOf(count),
              x * (CellFactory.WEIGHT + CellFactory.THRESHOLD),
              y * (CellFactory.HEIGHT + CellFactory.THRESHOLD));
          row.add(cell);
          count++;
          System.out.println("Generate points... progress: " + count + "/" + allCount);
        }
      }

      //rand links between objects
      count = 0;
      while (count < ITERATION_COUNT)
      {
        Random rand = new Random();
        int numberOfRow = rand.nextInt(COUNT_OF_ROWS);
        int numberOfElement = rand.nextInt(COUNT_OF_ELEMENTS_IN_ROW);
        Object intersection = objects.get(numberOfRow).get(numberOfElement);

        int direction = rand.nextInt(3);
        Object neighboringIntersection = getNeighboringIntersection(numberOfRow, numberOfElement, direction);

        if (!neighboringIntersection.equals(intersection))
        {
          graph.insertEdge(parent, null, "", intersection, neighboringIntersection, Styles.BLUE.getStyle());
        }
        System.out.println("Generate random edges... progress: " + count + "/" + ITERATION_COUNT);
        count++;
      }
      List<Object> shortestPath = findShortestPath(graph);
      graph.setCellStyle(Styles.GREEN.getStyle(), shortestPath.toArray());

    }
    finally
    {
      graph.getModel().endUpdate();
    }

    mxGraphComponent graphComponent = new mxGraphComponent(graph);
    getContentPane().add(graphComponent);
  }

  private List<Object> findShortestPath(mxGraph graph)
  {
    final AtomicInteger count = new AtomicInteger(0);
    mxGraphAnalysis analysis = mxGraphAnalysis.getInstance();
    List<Object> firstRow = this.objects.get(0);
    List<Object> lastRow = this.objects.get(objects.size() - 1);
    final int allCount = firstRow.size() * lastRow.size();
    final List<Object> result = new ArrayList<>();

    firstRow.parallelStream().forEach(o -> lastRow.parallelStream().forEach(o1 -> {
      System.out.println("Try to find shortest path... progress: " + count.getAndIncrement() + " / " + allCount);
      Object[] shortestPath = analysis.getShortestPath(graph, o, o1, null, COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW, false);
      if (shortestPath.length != 0)
      {
        result.addAll(Arrays.asList(shortestPath));
        return;
      }
    }));
  return result;
//    List<List<Object>> shortestPaths = firstRow.stream().parallel().map(o -> Arrays.asList(lastRow.stream().parallel().map(o2 ->
//    {
////      System.out.println(graph.getModel().getValue(o) +" -> " + graph.getModel().getValue(o2));
////      System.out.println("Try to find shortest path... progress: " + count.getAndIncrement() + " / " + allCount);
//      return analysis.getShortestPath(graph, o, o2, null, COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW, false);
//    }).filter(objects1 -> objects1.length != 0).min(Comparator.comparingInt(o1 -> o1.length)).orElse(new Object[0]))).collect(Collectors.toList());
//    return shortestPaths.stream().parallel().min(Comparator.comparingInt(List::size)).orElse(Lists.newArrayList());
  }

  private Object getNeighboringIntersection(int numberOfRow, int numberOfElement, int direction)
  {
    int i;
    switch (direction)
    {
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
