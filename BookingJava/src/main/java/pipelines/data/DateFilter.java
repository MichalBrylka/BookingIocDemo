package pipelines.data;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public record DateFilter(LocalDate value, Operator operator) implements DataFilter<LocalDate> {
    public DateFilter {
        if (operator != Operator.EQ &&
            operator != Operator.NEQ &&
            operator != Operator.GT &&
            operator != Operator.LT &&
            operator != Operator.GTE &&
            operator != Operator.LTE) {
            throw new IllegalArgumentException("Invalid operator for LocalDate: " + operator);
        }
    }

    @Override
    public <TEntity> Predicate<TEntity> getPredicate(Function<TEntity, LocalDate> getter) {
        return switch (operator) {
            case EQ -> b -> Objects.equals(getter.apply(b), value);
            case NEQ -> b -> !Objects.equals(getter.apply(b), value);
            case GT -> b -> getter.apply(b).isAfter(value);
            case LT -> b -> getter.apply(b).isBefore(value);
            case GTE -> b -> !getter.apply(b).isBefore(value);
            case LTE -> b -> !getter.apply(b).isAfter(value);
            default -> throw new IllegalArgumentException("Unsupported operator for date field: " + operator);
        };
    }
}