package pipelines.data;

import java.time.LocalDate;

public record DateFilter(LocalDate value, Operator operator) implements DataFilter {
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
}