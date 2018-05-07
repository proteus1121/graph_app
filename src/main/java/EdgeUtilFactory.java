import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EdgeUtilFactory {
    public static synchronized EdgeDecorator createEdge(mxGraph graph, Object parent, Object intersection, Object neighboringIntersection, List<Object> firstRow, List<Object> lastRow, List<EdgeDecorator> edges) {
        Object edge = graph.insertEdge(parent, null, "", intersection, neighboringIntersection, Styles.BLUE.getStyle());

        Set<EdgeDecorator> edgesFP = new HashSet<>();
        Set<EdgeDecorator> fullPath = getFullLine(intersection, neighboringIntersection, edges, edgesFP, firstRow, lastRow);

        boolean isEntry = fullPath.stream()
                .anyMatch(edgeDecorator -> firstRow.contains(edgeDecorator.getSource()) || firstRow.contains(edgeDecorator.getTarget()));

        boolean isExit = fullPath.stream()
                .anyMatch(edgeDecorator -> lastRow.contains(edgeDecorator.getSource()) || lastRow.contains(edgeDecorator.getTarget()));

        fullPath.forEach(edgeDecorator -> edgeDecorator.setEntry(isEntry));
        fullPath.forEach(edgeDecorator -> edgeDecorator.setEntry(isExit));

        return new EdgeDecorator((mxCell) edge, intersection, neighboringIntersection, isEntry, isExit);
    }

    //recursive search full path
    private static synchronized Set<EdgeDecorator> getFullLine(Object intersection, Object neighboringIntersection, List<EdgeDecorator> edges, Set<EdgeDecorator> resultEdges, List<Object> firstRow, List<Object> lastRow) {

        Set<EdgeDecorator> connectedEdges = edges.stream()
                .filter(edgeDecorator -> edgeDecorator.getSource().equals(intersection) || edgeDecorator.getTarget().equals(intersection)
                        || edgeDecorator.getSource().equals(neighboringIntersection) || edgeDecorator.getTarget().equals(neighboringIntersection)).collect(Collectors.toSet());

        Set<EdgeDecorator> connectedEdgesR = edges.stream()
                .filter(edgeDecorator3 -> edgeDecorator3.getSource().equals(intersection) && edgeDecorator3.getTarget().equals(neighboringIntersection)
                        || edgeDecorator3.getSource().equals(intersection) && edgeDecorator3.getTarget().equals(neighboringIntersection)).collect(Collectors.toSet());

        resultEdges.addAll(connectedEdges);
        connectedEdges.removeAll(connectedEdgesR);

        boolean isEntry = resultEdges.stream()
                .anyMatch(edgeDecorator -> firstRow.contains(edgeDecorator.getSource()) || firstRow.contains(edgeDecorator.getTarget()));

        boolean isExit = resultEdges.stream()
                .anyMatch(edgeDecorator -> lastRow.contains(edgeDecorator.getSource()) || lastRow.contains(edgeDecorator.getTarget()));

        connectedEdges
                .forEach(edgeDecorator -> {
                            try {
                                if (!isEntry && !isExit) {

                                ArrayList<EdgeDecorator> edgeDecorators = new ArrayList<>(edges);

                                edgeDecorators.removeAll(connectedEdges);
                                Set<EdgeDecorator> fullLine = getFullLine(edgeDecorator.getSource(), edgeDecorator.getTarget(), edgeDecorators, resultEdges, firstRow, lastRow);
                                fullLine.removeAll(resultEdges);
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
