import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mxgraph.analysis.mxGraphAnalysis;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import javafx.util.Pair;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class MainFrame extends JFrame
{

  private static final int COUNT_OF_ROWS = 10;
  private static final int COUNT_OF_ELEMENTS_IN_ROW = 10;
  private List<List<Object>> objects = new ArrayList<>();
  private final List<EdgeDecorator> edges = new ArrayList<>();

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

    graph.getModel().beginUpdate();
    try
    {
      //init 2D field
      generatePoints(graph, parent);

      List<Object> firstRow = this.objects.get(0);
      List<Object> lastRow = this.objects.get(objects.size() - 1);

      //rand links between objects
      generateLinks(graph, parent, firstRow, lastRow);

      //remove first dangling links
      removeFirstDanglingLinks(graph);

      //find shortest path
      List<EdgeDecorator> shortestPath = findShortestPath(graph);

      if (shortestPath.size() > 0)
      {
        System.out.println("Shortest path found!");
      }
      else
      {
        System.out.println("None path found!");
      }

      graph.setCellStyle(Styles.GREEN.getStyle(), shortestPath.stream().map(EdgeDecorator::getEdge).toArray());
      graph.setCellStyle(Styles.GREEN.getStyle(), shortestPath.stream().map(EdgeDecorator::getSource).toArray());
      graph.setCellStyle(Styles.GREEN.getStyle(), shortestPath.stream().map(EdgeDecorator::getTarget).toArray());

      //remove second dangling links
      removeSecondDanglingLinks(graph, shortestPath, firstRow, lastRow);

    }
    finally
    {
      graph.getModel().endUpdate();
    }

    mxGraphComponent graphComponent = new mxGraphComponent(graph);
    getContentPane().add(graphComponent);
    graphComponent.getGraphControl().addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseReleased(MouseEvent e)
      {
        mxCell cell = (mxCell) graphComponent.getCellAt(e.getX(), e.getY());
        Optional<EdgeDecorator> any = edges.stream().filter(edgeDecorator -> edgeDecorator.getEdge().equals(cell)).findAny();
        if (cell != null && any.isPresent())
        {
          System.err.println("entry: " + any.get().isEntry());
          System.err.println("exit: " + any.get().isExit());
        }
      }
    });
  }

  private void removeFirstDanglingLinks(mxGraph graph)
  {
    List<EdgeDecorator> edgeDecoratorsForDelete = edges.parallelStream()
        .filter(edgeDecorator -> !edgeDecorator.isEntry() || !edgeDecorator.isExit())
        .collect(Collectors.toList());
    graph.removeCells(edgeDecoratorsForDelete.stream().map(EdgeDecorator::getEdge).toArray());
    edges.removeAll(edgeDecoratorsForDelete);
  }

  private void removeSecondDanglingLinks(mxGraph graph, List<EdgeDecorator> shortestPath, List<Object> firstRow, List<Object> lastRow)
  {
    Boolean matrix[][] = new Boolean[COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW][COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW];

    List<Object> allIntersections = objects.stream().flatMap(Collection::stream).collect(Collectors.toList());
    for (int i = 0; i < allIntersections.size(); i++)
    {
      for (int y = 0; y < allIntersections.size(); y++)
      {
        int finalI = i;
        int finalY = y;
        matrix[i][y] = edges.stream()
            .anyMatch(edgeDecorator ->
                    edgeDecorator.getSource().equals(allIntersections.get(finalI))
                        && edgeDecorator.getTarget().equals(allIntersections.get(finalY))
//                    || edgeDecorator.getSource().equals(allIntersections.get(finalY))
//                    && edgeDecorator.getTarget().equals(allIntersections.get(finalI))
            );
//        System.out.println(matrix[i][y] + " -> " +graph.getModel().getValue(allIntersections.get(i)) + " -> " + graph.getModel().getValue(allIntersections.get(y)));
      }
    }
    List<Object> shortestPathNodes = shortestPath.stream().flatMap(edgeDecorator -> Sets.newHashSet(edgeDecorator.getSource()
        , edgeDecorator.getTarget()).stream()).collect(Collectors.toList());

    Set<Integer> shortestPathIndexes = shortestPathNodes.stream().map(allIntersections::indexOf).collect(Collectors.toSet());

    List<Object> trueIntersections = (Stream.concat(Stream.concat(firstRow.stream(), lastRow.stream()), shortestPathNodes.stream()))
        .collect(Collectors.toList());

    for (int i = 0; i < matrix.length; i++)
    {
      Set<Pair<Integer, Integer>> collected = new HashSet<>();
      collected = recursiveAnalise(i, matrix, shortestPathIndexes, allIntersections, graph, collected);
      List<EdgeDecorator> collect1 = new ArrayList<>();
      collected.forEach(o1 ->
          {
            Object key = allIntersections.get((int) ((Pair) o1).getKey());
            Object value = allIntersections.get((int) ((Pair) o1).getValue());

            collect1.addAll(edges.stream().filter(edgeDecorator ->
                edgeDecorator.getSource().equals(key) &&
                    edgeDecorator.getTarget().equals(value)
                    ||
                    edgeDecorator.getTarget().equals(key) &&
                        edgeDecorator.getSource().equals(value)
            ).collect(Collectors.toList()));
          }
      );

      Set<EdgeDecorator> fullPath = new HashSet<>();
      collect1.forEach(edgeDecorator -> {
        Set<EdgeDecorator> resultPath = new HashSet<>();
        resultPath.add(edgeDecorator);
        fullPath.addAll(EdgeUtilFactory.getFullLine(edgeDecorator.getSource(), edgeDecorator.getTarget(), collect1, resultPath));
      });

      fullPath.forEach(edgeDecorator -> {
        edgeDecorator.setExit(false);
        edgeDecorator.setEntry(false);
      });

      boolean isEntry = fullPath.stream()
          .anyMatch(
              edgeDecorator2 -> trueIntersections.contains(edgeDecorator2.getSource()));

      boolean isExit = fullPath.stream()
          .anyMatch(
              edgeDecorator2 -> trueIntersections.contains(edgeDecorator2.getTarget()));

      fullPath.stream().filter(edgeDecorator2 -> !edgeDecorator2.isEntry()).forEach(edgeDecorator2 -> {
        edgeDecorator2.setEntry(isEntry);
        if (isEntry)
          trueIntersections.add(edgeDecorator2.getTarget());
      });
      fullPath.stream().filter(edgeDecorator2 -> !edgeDecorator2.isExit()).forEach(edgeDecorator2 -> {
        edgeDecorator2.setExit(isExit);
        if (isExit)
          trueIntersections.add(edgeDecorator2.getSource());
      });

      System.out.println(i + ": " + fullPath.stream().map(edgeDecorator -> edgeDecorator.toString(graph)).collect(Collectors.toList()));
//      graph.removeCells(collect1.stream().map(EdgeDecorator::getEdge).collect(Collectors.toList()).toArray());
//      graph.setCellStyle(Styles.RED.getStyle(), collect1.stream().map(EdgeDecorator::getEdge).toArray());
    }

    //analyze path from each element in shortestPath to start or end
//    List<Object> shortestPathNodes = shortestPath.stream()
//        .flatMap(edgeDecorator -> Sets.newHashSet(edgeDecorator.getSource(), edgeDecorator.getTarget()).stream())
//        .collect(Collectors.toList());
//
//    List<EdgeDecorator> allEdgesWithoutShortestPath = new ArrayList<>(edges);
//    allEdgesWithoutShortestPath.removeAll(shortestPath);
//
//    shortestPath.forEach(eg ->
//    {
//      recursiveSearchRedundantLinks(graph, shortestPathNodes, allEdgesWithoutShortestPath, eg);
//    });
  }

  private Set<Pair<Integer, Integer>> recursiveAnalise(int i, Boolean[][] matrix, Set<Integer> shortestPathIndexes,
      List<Object> allIntersections, mxGraph graph, Set<Pair<Integer, Integer>> collected)
  {
    for (int y = 0; y < matrix[i].length; y++)
    {
      Pair<Integer, Integer> pair = new Pair<>(i, y);
      if (matrix[i][y] && !alredy.contains(pair) && !alredy.contains(new Pair<>(y, i)) && !shortestPathIndexes.contains(i))
      {
        collected.add(pair);
        recursiveAnalise(y, matrix, shortestPathIndexes, allIntersections, graph, collected);
      }
    }
    return collected;
  }

  static Set<Pair<Integer, Integer>> alredy = new HashSet<>();
//  private void recursiveSearchRedundantLinks(mxGraph graph, List<Object> shortestPathNodes, List<EdgeDecorator> allEdgesWithoutShortestPath,
//      EdgeDecorator eg)
//  {
//    Set<EdgeDecorator> resultSet = new HashSet<>();
//
//    Set<EdgeDecorator> rs = Sets.union(EdgeUtilFactory.getFullLine(eg.getSource(), eg.getTarget(), edges, resultSet),
//        EdgeUtilFactory.getFullLine(eg.getTarget(), eg.getSource(), edges, resultSet));
//
//    boolean b = rs.stream()
//        .anyMatch(edgeDecorator -> shortestPathNodes.contains(edgeDecorator.getSource()) || shortestPathNodes.contains(edgeDecorator.getTarget()));
//    if (!b && rs.size() < 2)
//    {
////      edges.removeAll(rs);
////      graph.removeCells(rs.toArray());
//      rs.forEach(edge -> edge.getEdge().setStyle(Styles.RED.getStyle()));
//    }
////    else
////    if(b && rs.size() == 1)
////    {
////      rs.forEach(edge -> edge.getEdge().setStyle(Styles.BLUE.getStyle()));
////    }
//    else
//    {
//      rs.forEach(edgeDecorator ->
//      {
//        edges.remove(edgeDecorator);
//        recursiveSearchRedundantLinks(graph, shortestPathNodes, allEdgesWithoutShortestPath, edgeDecorator);
//      });
//    }
//  }

  private void generateLinks(mxGraph graph, Object parent, List<Object> firstRow, List<Object> lastRow)
  {
    AtomicInteger count = new AtomicInteger(0);
    List<Thread> threads = new ArrayList<>();
    int iterationCount = ((COUNT_OF_ROWS - 1) * COUNT_OF_ELEMENTS_IN_ROW + (COUNT_OF_ELEMENTS_IN_ROW - 1) * COUNT_OF_ROWS) - (2 * (
        COUNT_OF_ELEMENTS_IN_ROW - 1));
    while (count.get() < iterationCount)
    {
//            Thread t = new Thread(() ->
//            {
      Random rand = new Random();
      int numberOfRow = rand.nextInt(COUNT_OF_ROWS);
      int numberOfElement = rand.nextInt(COUNT_OF_ELEMENTS_IN_ROW);
      Object intersection = objects.get(numberOfRow).get(numberOfElement);

      int direction = rand.nextInt(3);
      Object neighboringIntersection = getNeighboringIntersection(numberOfRow, numberOfElement, direction);

      if (!neighboringIntersection.equals(intersection) && edges.stream()
          .noneMatch(edge -> edge.getSource().equals(intersection) && edge.getTarget().equals(neighboringIntersection)
              || edge.getSource().equals(neighboringIntersection) && edge.getTarget().equals(intersection)) && !(firstRow.contains(intersection)
          && firstRow.contains(neighboringIntersection)) && !(lastRow.contains(intersection) && lastRow.contains(neighboringIntersection)))
      {
        synchronized (edges)
        {
          EdgeDecorator edge = EdgeUtilFactory.createEdge(graph, parent, intersection, neighboringIntersection, firstRow, lastRow, edges);

          edges.add(edge);
          count.getAndIncrement();
          if (edge.isEntry() && edge.isExit())
          {
            break;
          }
        }
      }
//            });
//            threads.add(t);
//            t.start();
//      System.out.println("Generate random edges... progress: " + count.get() + "/" + iterationCount);
    }

    threads.forEach(thread ->
    {
      try
      {
        thread.join();
      }
      catch (InterruptedException e)
      {
        e.printStackTrace();
      }
    });
    threads.clear();
  }

  private void generatePoints(mxGraph graph, Object parent)
  {
    AtomicInteger count = new AtomicInteger();
    int allCount = COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW;
    for (int x = 0; x < COUNT_OF_ROWS; x++)
    {
      List<Object> row = new ArrayList<>();
      objects.add(row);
      for (int y = 0; y < COUNT_OF_ELEMENTS_IN_ROW; y++)
      {
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

  private List<EdgeDecorator> findShortestPath(mxGraph graph)
  {
    final AtomicInteger count = new AtomicInteger(0);
    mxGraphAnalysis analysis = mxGraphAnalysis.getInstance();
    List<Object> firstRow = this.objects.get(0);
    List<Object> lastRow = this.objects.get(objects.size() - 1);
    final int allCount = firstRow.size() * lastRow.size();

// if you want to find min path between any points, from left side to right side:
//    return firstRow.parallelStream()
//        .map(o -> lastRow.parallelStream().map(o1 ->
//        {
//          System.out.println("Try to find shortest path... progress: " + count.getAndIncrement() + " / " + allCount);
//          return analysis.getShortestPath(graph, o, o1, null, COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW, false);
//        }).filter(objects1 -> objects1.length > 0).findAny())
//        .filter(Optional::isPresent)
//        .findFirst()
//        .orElse(Optional.of(new Object[0]))
//        .orElse(new Object[0]);

// if you want to find min path between points with min path in the all matrix, from left side to right side:
    List<List<Object>> shortestPaths = firstRow.stream().parallel().map(o -> Arrays.asList(lastRow.stream().parallel().map(o2 ->
    {
      System.out.println("Try to find shortest path... progress: " + count.getAndIncrement() + " / " + allCount);
      return analysis.getShortestPath(graph, o, o2, null, COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW, false);
    }).filter(objects1 -> objects1.length != 0).min(Comparator.comparingInt(o1 -> o1.length)).orElse(new Object[0]))).collect(Collectors.toList());
    return shortestPaths.stream()
        .filter(shortestPath -> shortestPath.size() != 0)
        .parallel()
        .min(Comparator.comparingInt(List::size))
        .orElse(Lists.newArrayList())
        .stream()
        .map(edge -> edges.stream().filter(edgeDecorator -> edgeDecorator.getEdge().equals(edge)).findAny())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
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
