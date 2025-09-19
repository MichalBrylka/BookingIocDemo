package pipelines.data;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record UuidFilter(@NotNull UUID value, @NotNull Operator operator) implements DataFilter {
    public UuidFilter {
        if (operator != Operator.EQ && operator != Operator.NEQ)
            throw new IllegalArgumentException("Invalid operator for UUID: " + operator);
    }
}