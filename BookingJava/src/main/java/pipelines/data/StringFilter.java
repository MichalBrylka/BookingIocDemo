package pipelines.data;

public record StringFilter(String value, Operator operator) implements DataFilter {
    public StringFilter {
        if (operator != Operator.EQ &&
            operator != Operator.NEQ &&
            operator != Operator.IN) {
            throw new IllegalArgumentException("Invalid operator for String: " + operator);
        }
    }
}