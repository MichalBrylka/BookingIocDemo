package pipelines.data;

import java.time.LocalDate;

public record DateFilter(LocalDate value, Operator operator) implements DataFilter {}
