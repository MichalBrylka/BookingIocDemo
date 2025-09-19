package pipelines.data;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public record StringFilter(String value, Operator operator) implements DataFilter<String> {
    public StringFilter {
        if (operator != Operator.EQ &&
            operator != Operator.NEQ &&
            operator != Operator.IN) {
            throw new IllegalArgumentException("Invalid operator for String: " + operator);
        }
    }

    @Override
    public <TEntity> Predicate<TEntity> getPredicate(Function<TEntity, String> getter) {
        return switch (operator) {
            case EQ -> b -> Objects.equals(getter.apply(b), value);
            case NEQ -> b -> !Objects.equals(getter.apply(b), value);
            case IN -> b -> getter.apply(b) != null && getter.apply(b).contains(value);
            default -> throw new IllegalArgumentException("Unsupported operator for string field: " + operator);
        };
    }
}