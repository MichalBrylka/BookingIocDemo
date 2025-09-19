package pipelines.commands;

import an.awesome.pipelinr.Command;
import pipelines.data.*;
import pipelines.domain.Booking;

import java.util.*;


public record GetBookingsQuery(Map<String, DataFilter<?>> filter, Iterable<SortField> sort) implements Command<List<Booking>> {
    public GetBookingsQuery() {this(null, null);}
}