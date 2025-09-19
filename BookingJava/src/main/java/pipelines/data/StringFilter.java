package pipelines.data;

public record StringFilter(String value, Operator operator) implements DataFilter {}


