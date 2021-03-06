import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mxgraph.analysis.mxGraphAnalysis;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class MainFrame extends JFrame
{
  private static final int COUNT_OF_ROWS = 10;
  private static final int COUNT_OF_ELEMENTS_IN_ROW = 10;
  private static double resistance = 0;
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
      Set<Object> shortestPathNodes = shortestPath.stream()
          .flatMap(edgeDecorator -> Sets.newHashSet(edgeDecorator.getSource(), edgeDecorator.getTarget()).stream())
          .collect(Collectors.toSet());
      List<Object> trueNodes = new ArrayList<>(Sets.union(Sets.union(new HashSet<>(firstRow), new HashSet<>(lastRow)), shortestPathNodes));

      List<List<EdgeDecorator>> truePathLinks = new ArrayList<>();
      truePathLinks.add(shortestPath);
      graph.removeCells(shortestPath.stream().map(EdgeDecorator::getEdge).toArray());
      removeSecondDanglingLinks(graph, trueNodes, truePathLinks);

      List<EdgeDecorator> trueLinks = truePathLinks.stream().flatMap(Collection::stream).collect(Collectors.toList());

      Set<EdgeDecorator> danglingLinks = edges.stream().filter(e -> !trueLinks.contains(e)).collect(Collectors.toSet());
//      danglingLinks.forEach(edge -> edge.getEdge().setStyle(Styles.RED.getStyle()));
      graph.removeCells(danglingLinks.stream().map(EdgeDecorator::getEdge).toArray());

      Set<EdgeDecorator> trueLinksForRestore = edges.stream().filter(trueLinks::contains).collect(Collectors.toSet());

      trueLinksForRestore.forEach(edge -> edges.add(EdgeUtilFactory.createEdge(graph, parent, edge.getSource(), edge.getTarget(), firstRow, lastRow, edges)));

      double resistance = calcResistance(truePathLinks, shortestPath, graph);
      System.err.println("resistance: " + resistance);

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

  private double calcResistance(List<List<EdgeDecorator>> links, List<EdgeDecorator> shortestPath, mxGraph graph)
  {
    links.remove(shortestPath);
    List<List<List<EdgeDecorator>>> newLinksBig = new ArrayList<>();

    links.forEach(edgeDecoratorsList -> {
      List<List<EdgeDecorator>> newLinks = new ArrayList<>();

      EdgeDecorator firstEdge = edgeDecoratorsList.get(0);
      EdgeDecorator lastEdge = edgeDecoratorsList.get(edgeDecoratorsList.size() - 1);
      List<Object> objects = Arrays.asList(mxGraphAnalysis.getInstance()
          .getShortestPath(graph, firstEdge.getSource(), lastEdge.getTarget(), null, 999,
              false));

      if (!objects.isEmpty())
      {
        List<EdgeDecorator> collect = objects.stream().flatMap(o ->
            edges.stream().filter(edgeDecorator -> edgeDecorator.getEdge().equals(o)).collect(Collectors.toSet()).stream())
            .collect(Collectors.toList());
        newLinks.add(collect);
      }
//      List<List<EdgeDecorator>> collect = links.stream().filter(edgeDecorators -> edgeDecorators.get(0).getSource().equals(firstEdge.getSource())
//          && edgeDecorators.get(edgeDecorators.size() - 1).getTarget().equals(firstEdge.getTarget())).collect(Collectors.toList());
//      newLinks.addAll(collect);
      newLinks.add(edgeDecoratorsList);
      newLinksBig.add(newLinks);
    });
    return newLinksBig.stream().map(lists -> calcParallel(lists.stream().map(List::size).map(Integer::doubleValue).collect(Collectors.toList())))
        .reduce((aDouble, aDouble2) -> aDouble + aDouble2).orElse(0d);
  }

  public static double calcParallel(List<Double> collect)
  {
    double result = 0d;
    for (Double aDouble : collect)
    {
      result = result + (1d / aDouble);
    }
    double v = 1d / result;
    return v;
  }

  private void removeFirstDanglingLinks(mxGraph graph)
  {
    List<EdgeDecorator> edgeDecoratorsForDelete = edges.parallelStream()
        .filter(edgeDecorator -> !edgeDecorator.isEntry() || !edgeDecorator.isExit())
        .collect(Collectors.toList());
    graph.removeCells(edgeDecoratorsForDelete.stream().map(EdgeDecorator::getEdge).toArray());
    edges.removeAll(edgeDecoratorsForDelete);
  }

  private void removeSecondDanglingLinks(mxGraph graph, List<Object> trueNodes, List<List<EdgeDecorator>> trueLinks)
  {
    //analyze path from each element in shortestPath to start or end
    AtomicBoolean haveAlternativePath = new AtomicBoolean(false);

    List<EdgeDecorator> localTrueLinks = new ArrayList<>();
    trueNodes.forEach(o1 ->
        trueNodes.forEach(o2 -> {
          List<Object> objects = Arrays.asList(mxGraphAnalysis.getInstance().getShortestPath(graph, o1, o2, null, trueNodes.size(), false));
          if (!objects.isEmpty())
          {
            haveAlternativePath.set(true);
            Set<EdgeDecorator> collect = objects.stream().flatMap(o ->
                edges.stream().filter(edgeDecorator -> edgeDecorator.getEdge().equals(o)).collect(Collectors.toSet()).stream())
                .collect(Collectors.toSet());
            localTrueLinks.addAll(collect);
            trueLinks.add(new ArrayList<>(collect));
          }
        }));
    if (!localTrueLinks.isEmpty())
    {
      Set<EdgeDecorator> collect = edges.stream().filter(localTrueLinks::contains).collect(Collectors.toSet());
      graph.removeCells(collect.stream().map(EdgeDecorator::getEdge).toArray());
    }
    if (haveAlternativePath.get())
    {
      removeSecondDanglingLinks(graph, trueNodes, trueLinks);
    }
  }

  private void generateLinks(mxGraph graph, Object parent, List<Object> firstRow, List<Object> lastRow)
  {
    AtomicInteger count = new AtomicInteger(0);
    int iterationCount = ((COUNT_OF_ROWS - 1) * COUNT_OF_ELEMENTS_IN_ROW + (COUNT_OF_ELEMENTS_IN_ROW - 1) * COUNT_OF_ROWS) - (2 * (
        COUNT_OF_ELEMENTS_IN_ROW - 1));
    while (count.get() < iterationCount)
    {
      Random rand = new Random();
      int numberOfRow = rand.nextInt(COUNT_OF_ROWS);
      int numberOfElement = rand.nextInt(COUNT_OF_ELEMENTS_IN_ROW);
      Object intersection = objects.get(numberOfRow).get(numberOfElement);

      int direction = rand.nextInt(3);
      Object neighboringIntersection = getNeighboringIntersection(numberOfRow, numberOfElement, direction);

      if (!neighboringIntersection.equals(intersection) && edges.parallelStream()
          .noneMatch(edge -> edge.getSource().equals(intersection) && edge.getTarget().equals(neighboringIntersection)
              || edge.getSource().equals(neighboringIntersection) && edge.getTarget().equals(intersection)) && !(firstRow.contains(intersection)
          && firstRow.contains(neighboringIntersection)) && !(lastRow.contains(intersection) && lastRow.contains(neighboringIntersection)))
      {
        EdgeDecorator edge = EdgeUtilFactory.createEdge(graph, parent, intersection, neighboringIntersection, firstRow, lastRow, edges);

        edges.add(edge);
        count.getAndIncrement();
        if (edge.isEntry() && edge.isExit())
        {
          break;
        }
      }
      System.out.println("Generate random edges... progress: " + count.get() + "/" + iterationCount);
    }
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
    List<List<Object>> shortestPaths = firstRow.parallelStream().parallel().map(o -> Arrays.asList(lastRow.parallelStream().parallel().map(o2 ->
    {
      System.out.println("Try to find shortest path... progress: " + count.getAndIncrement() + " / " + allCount);
      return analysis.getShortestPath(graph, o, o2, null, COUNT_OF_ROWS * COUNT_OF_ELEMENTS_IN_ROW, false);
    }).filter(objects1 -> objects1.length != 0).min(Comparator.comparingInt(o1 -> o1.length)).orElse(new Object[0]))).collect(Collectors.toList());
    return shortestPaths.parallelStream()
        .filter(shortestPath -> shortestPath.size() != 0)
        .parallel()
        .min(Comparator.comparingInt(List::size))
        .orElse(Lists.newArrayList())
        .parallelStream()
        .map(edge -> edges.parallelStream().filter(edgeDecorator -> edgeDecorator.getEdge().equals(edge)).findAny())
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
