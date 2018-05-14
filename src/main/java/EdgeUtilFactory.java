import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxEvent;
import com.mxgraph.view.mxGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EdgeUtilFactory
{
  public static synchronized EdgeDecorator createEdge(mxGraph graph, Object parent, Object intersection, Object neighboringIntersection,
      List<Object> firstRow, List<Object> lastRow, List<EdgeDecorator> edges)
  {
    Object edge = graph.insertEdge(parent, null, "", intersection, neighboringIntersection, Styles.BLUE.getStyle());

    EdgeDecorator currentEdge = new EdgeDecorator((mxCell) edge, intersection, neighboringIntersection, false, false);
    Set<EdgeDecorator> resultPath = new HashSet<>();
    resultPath.add(currentEdge);
    edges.add(currentEdge);
    Set<EdgeDecorator> fullPath = getFullLine(intersection, neighboringIntersection, edges, resultPath);

    boolean isEntry = fullPath.stream()
        .anyMatch(
            edgeDecorator -> firstRow.contains(edgeDecorator.getSource()) || firstRow.contains(edgeDecorator.getTarget()) || edgeDecorator.isEntry()
                || firstRow.contains(intersection) || firstRow.contains(neighboringIntersection));

    boolean isExit = fullPath.stream()
        .anyMatch(
            edgeDecorator -> lastRow.contains(edgeDecorator.getSource()) || lastRow.contains(edgeDecorator.getTarget()) || edgeDecorator.isExit()
                || lastRow.contains(intersection) || lastRow.contains(neighboringIntersection));

    fullPath.stream().filter(edgeDecorator -> !edgeDecorator.isEntry()).forEach(edgeDecorator -> edgeDecorator.setEntry(isEntry));
    fullPath.stream().filter(edgeDecorator -> !edgeDecorator.isExit()).forEach(edgeDecorator -> edgeDecorator.setExit(isExit));

    currentEdge.setEntry(isEntry);
    currentEdge.setExit(isExit);

    return currentEdge;
  }

  //recursive search full path
  private static synchronized Set<EdgeDecorator> getFullLine(Object intersection, Object neighboringIntersection, List<EdgeDecorator> edges,
      Set<EdgeDecorator> resultEdges)
  {
    Set<EdgeDecorator> newResult = new HashSet<>(resultEdges);
    Set<EdgeDecorator> connectedEdges = edges.stream()
        .filter(edgeDecorator -> edgeDecorator.getSource().equals(intersection) || edgeDecorator.getTarget().equals(intersection)
            || edgeDecorator.getSource().equals(neighboringIntersection) || edgeDecorator.getTarget().equals(neighboringIntersection))
        .filter(edgeDecorator -> !edgeDecorator.getSource().equals(intersection) && !edgeDecorator.getTarget().equals(neighboringIntersection))
        .collect(Collectors.toSet());

    resultEdges.addAll(connectedEdges);

    connectedEdges
        .forEach(edgeDecorator -> {
              if (!newResult.equals(resultEdges))
              {
                Set<EdgeDecorator> fullLine = getFullLine(edgeDecorator.getSource(), edgeDecorator.getTarget(), edges, resultEdges);
                resultEdges.addAll(fullLine);
                fullLine = getFullLine(edgeDecorator.getTarget(), edgeDecorator.getSource(), edges, resultEdges);
                resultEdges.addAll(fullLine);
              }
            }
        );
    return resultEdges;
  }
}
