package pipelines.data;

import java.util.function.Function;
import java.util.function.Predicate;

public sealed interface DataFilter<TValue> permits DateFilter, StringFilter, UuidFilter {
    <TEntity> Predicate<TEntity> getPredicate(Function<TEntity, TValue> getter);
}
