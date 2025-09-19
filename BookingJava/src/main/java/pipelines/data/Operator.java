package pipelines.data;

public enum Operator {
    EQ, NEQ, IN, GT, LT, GTE, LTE;

    public static Operator fromString(String op) {
        return switch (op.toLowerCase()) {
            case "eq" -> EQ;
            case "neq" -> NEQ;
            case "has", "in" -> IN;
            case "gt" -> GT;
            case "lt" -> LT;
            case "gte" -> GTE;
            case "lte" -> LTE;
            default -> throw new IllegalArgumentException("Unknown operator: " + op);
        };
    }
}
