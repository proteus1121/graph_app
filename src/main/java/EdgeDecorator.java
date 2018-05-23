import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
class EdgeDecorator {
    private mxCell edge;
    private Object source;
    private Object target;
    private boolean isEntry;
    private boolean isExit;

    public String toString(mxGraph graph){
        return graph.getModel().getValue(source) + " -> " + graph.getModel().getValue(target);
    }
}
