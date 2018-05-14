import com.mxgraph.model.mxCell;
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
}
