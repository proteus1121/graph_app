import lombok.Getter;

public enum Styles {
    BLUE("defaultVertex;strokeColor=blue;fillColor=blue;"),
    GREEN("defaultVertex;strokeColor=green;fillColor=green;"),
    RED("defaultVertex;strokeColor=red;fillColor=red;");

    @Getter
    private String style;
    Styles(String s) {
        style = s;
    }
}
