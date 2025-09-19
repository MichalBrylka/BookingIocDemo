package pipelines.data;

public sealed interface DataFilter permits StringFilter, DateFilter {
}
