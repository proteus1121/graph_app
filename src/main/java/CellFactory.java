import com.mxgraph.view.mxGraph;

public class CellFactory {
    public static final int WEIGHT = 20;
    public static final int HEIGHT = 20;
    public static final int THRESHOLD = 20;

    public Object createCell(mxGraph graph, Object parent, String value, int x, int y) {
        return graph.insertVertex(parent, null, value, x, y,
                WEIGHT, HEIGHT);
    }
}
