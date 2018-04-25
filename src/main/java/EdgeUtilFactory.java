import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

import java.util.List;
import java.util.stream.Collectors;

public class EdgeUtilFactory {
    public static synchronized EdgeDecorator createEdge(mxGraph graph, Object parent, Object intersection, Object neighboringIntersection, boolean isEntry, boolean isExit, List<EdgeDecorator> edges) {
        Object edge = graph.insertEdge(parent, null, "", intersection, neighboringIntersection, Styles.BLUE.getStyle());

        List<EdgeDecorator> edgeOfNearIntersection = edges.stream().filter(edgeDecorator -> edgeDecorator.getSource().equals(intersection) || edgeDecorator.getTarget().equals(intersection)
                || edgeDecorator.getSource().equals(neighboringIntersection) || edgeDecorator.getTarget().equals(neighboringIntersection)).collect(Collectors.toList());
        if (!isEntry) {
            isEntry = edgeOfNearIntersection.stream()
                    .anyMatch(EdgeDecorator::isEntry);
        }
        if (!isExit) {
            isExit = edgeOfNearIntersection.stream()
                    .anyMatch(EdgeDecorator::isExit);
        }
        return new EdgeDecorator((mxCell) edge, intersection, neighboringIntersection, isEntry, isExit);
    }
}
