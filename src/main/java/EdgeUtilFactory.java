import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxEvent;
import com.mxgraph.view.mxGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EdgeUtilFactory {
    public static synchronized EdgeDecorator createEdge(mxGraph graph, Object parent, Object intersection, Object neighboringIntersection,
        List<Object> firstRow, List<Object> lastRow, List<EdgeDecorator> edges) {
        Object edge = graph.insertEdge(parent, null, "", intersection, neighboringIntersection, Styles.BLUE.getStyle());

        EdgeDecorator currentEdge = new EdgeDecorator((mxCell) edge, intersection, neighboringIntersection, false, false);
        Set<EdgeDecorator> edgesFP = new HashSet<>();
        edgesFP.add(currentEdge);
        Set<EdgeDecorator> fullPath = getFullLine(intersection, neighboringIntersection, edges, edgesFP, firstRow, lastRow);

        boolean isEntry = fullPath.stream()
                .anyMatch(edgeDecorator -> firstRow.contains(edgeDecorator.getSource()) || firstRow.contains(edgeDecorator.getTarget()) ||  edgeDecorator.isEntry()
                || firstRow.contains(intersection) || firstRow.contains(neighboringIntersection));

        boolean isExit = fullPath.stream()
                .anyMatch(edgeDecorator -> lastRow.contains(edgeDecorator.getSource()) || lastRow.contains(edgeDecorator.getTarget()) ||  edgeDecorator.isExit()
                    || lastRow.contains(intersection) || lastRow.contains(neighboringIntersection));

        fullPath.stream().filter(edgeDecorator -> !edgeDecorator.isEntry()).forEach(edgeDecorator -> edgeDecorator.setEntry(isEntry));
        fullPath.stream().filter(edgeDecorator -> !edgeDecorator.isExit()).forEach(edgeDecorator -> edgeDecorator.setExit(isExit));

        currentEdge.setEntry(isEntry);
        currentEdge.setExit(isExit);

        return currentEdge;
    }

    //recursive search full path
    private static synchronized Set<EdgeDecorator> getFullLine(Object intersection, Object neighboringIntersection, List<EdgeDecorator> edges, Set<EdgeDecorator> resultEdges, List<Object> firstRow, List<Object> lastRow) {

        Set<EdgeDecorator> connectedEdges = edges.stream()
                .filter(edgeDecorator -> edgeDecorator.getSource().equals(intersection) || edgeDecorator.getTarget().equals(intersection)
                        || edgeDecorator.getSource().equals(neighboringIntersection) || edgeDecorator.getTarget().equals(neighboringIntersection))
            .filter(edgeDecorator -> !edgeDecorator.getSource().equals(intersection) && !edgeDecorator.getTarget().equals(neighboringIntersection))
            .collect(Collectors.toSet());

        Set<EdgeDecorator> currentEdge = edges.stream()
            .filter(edgeDecorator -> edgeDecorator.getSource().equals(intersection) && edgeDecorator.getTarget().equals(neighboringIntersection))
            .collect(Collectors.toSet());

        resultEdges.addAll(connectedEdges);
        resultEdges.addAll(currentEdge);

      boolean isEntry = connectedEdges.stream()
                .allMatch(edgeDecorator -> firstRow.contains(edgeDecorator.getSource()) || firstRow.contains(edgeDecorator.getTarget()) &&
                edges.stream().anyMatch(inn -> inn.getSource().equals(edgeDecorator.getSource()) || inn.getTarget().equals(edgeDecorator.getSource()) ||
                    inn.getSource().equals(edgeDecorator.getTarget()) || inn.getTarget().equals(edgeDecorator.getTarget())
                && !inn.equals(edgeDecorator))
                );

        boolean isExit = connectedEdges.stream()
                .allMatch(edgeDecorator -> lastRow.contains(edgeDecorator.getTarget()) || lastRow.contains(edgeDecorator.getSource()) &&
                    edges.stream().anyMatch(inn -> inn.getSource().equals(edgeDecorator.getTarget()) || inn.getTarget().equals(edgeDecorator.getSource()) ||
                        inn.getSource().equals(edgeDecorator.getTarget()) || inn.getTarget().equals(edgeDecorator.getTarget())
                        && !inn.equals(edgeDecorator))
                );

        connectedEdges
                .forEach(edgeDecorator -> {
                            try {
                                 if (!isEntry && !isExit) {

                                ArrayList<EdgeDecorator> edgeDecorators = new ArrayList<>(edges);

                                edgeDecorators.removeAll(currentEdge);
                                Set<EdgeDecorator> fullLine = getFullLine(edgeDecorator.getSource(), edgeDecorator.getTarget(), edgeDecorators, resultEdges, firstRow, lastRow);
                                resultEdges.addAll(fullLine);
                                }

                            } catch (Error e) {
                                e.printStackTrace();
                            }
                        }
                 );
        return resultEdges;
    }
}
