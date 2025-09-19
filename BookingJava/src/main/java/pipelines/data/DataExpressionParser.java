package pipelines.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataExpressionParser {
    public static Iterable<SortField> parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) return null;

        List<SortField> sortFields = new ArrayList<>();
        for (String part : sortParam.split(",")) {
            String[] tokens = part.trim().split("\\s+");
            String field = tokens[0].trim();
            boolean descending = tokens.length == 2 && tokens[1].equalsIgnoreCase("DESC");
            sortFields.add(new SortField(field, !descending));
        }
        return sortFields;
    }

    // regex: field operator 'value', value can have '' escaped quote
    private static final Pattern CONDITION_PATTERN = Pattern.compile(
            "(\\w+)\\s+(eq|neq|has|gt|lt|gte|lte)\\s+'((?:[^']|'')*)'",
            Pattern.CASE_INSENSITIVE
    );

    //TODO make generic to recognize field types from T
    public static Map<String, DataFilter> parseFilter(String filterParam) {
        if (filterParam == null || filterParam.isBlank()) return null;

        var result = new java.util.HashMap<String, DataFilter>();


        String[] conditions = filterParam.split("(?i)\\s+AND\\s+");// Split by AND (case-insensitive)
        for (String condition : conditions) {
            Matcher matcher = CONDITION_PATTERN.matcher(condition.trim());
            if (!matcher.matches()) continue; // skip invalid

            String field = matcher.group(1);
            String op = matcher.group(2);
            String valueRaw = matcher.group(3).replace("''", "'"); // unescape ''

            switch (field) {
                case "hotelName", "guestName", "email" -> result.put(field, new StringFilter(valueRaw, parseStringOperator(op)));
                case "checkIn", "checkOut" -> result.put(field, new DateFilter(LocalDate.parse(valueRaw), parseDateOperator(op)));
                default -> throw new IllegalArgumentException("Unknown field: " + field);
            }
        }
        return result;
    }

    private static Operator parseStringOperator(String op) {
        return switch (op.toLowerCase()) {
            case "eq" -> Operator.EQ;
            case "neq" -> Operator.NEQ;
            case "has" -> Operator.IN;
            default -> throw new IllegalArgumentException("Unknown string operator: " + op);
        };
    }

    private static Operator parseDateOperator(String op) {
        return switch (op.toLowerCase()) {
            case "eq" -> Operator.EQ;
            case "neq" -> Operator.NEQ;
            case "gt" -> Operator.GT;
            case "lt" -> Operator.LT;
            case "gte" -> Operator.GTE;
            case "lte" -> Operator.LTE;
            default -> throw new IllegalArgumentException("Unknown date operator: " + op);
        };
    }
}
