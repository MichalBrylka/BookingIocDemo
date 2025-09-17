package pipelines;

import an.awesome.pipelinr.Pipelinr;

import java.util.stream.Stream;

 class TestPipeline {
     static Pipelinr create(BookingRepository repository) {
        return new Pipelinr().with(() -> Stream.of(
                new BookHotelHandler(repository),
                new GetBookingsHandler(repository),
                new DeleteBookingsHandler(repository),
                new UpdateBookingHandler(repository),
                new PatchBookingHandler(repository),
                new GetBookingsByIdHandler(repository)
        ));
    }
}
