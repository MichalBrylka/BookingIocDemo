package pipelines.data;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public record UuidFilter(@NotNull UUID value, @NotNull Operator operator) implements DataFilter<UUID> {
    public UuidFilter {
        if (operator != Operator.EQ && operator != Operator.NEQ)
            throw new IllegalArgumentException("Invalid operator for UUID: " + operator);
    }

    @Override
    public <TEntity> Predicate<TEntity> getPredicate(Function<TEntity, UUID> getter) {
        return switch (operator) {
            case EQ -> b -> Objects.equals(getter.apply(b), value);
            case NEQ -> b -> !Objects.equals(getter.apply(b), value);
            default -> throw new IllegalArgumentException("Unsupported operator for UUID field: " + operator);
        };
    }
}