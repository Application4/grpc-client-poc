package com.javatechie.service;

import com.javatechie.entity.Stock;
import com.javatechie.grpc.*;
import com.javatechie.repository.StockRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@GrpcService
public class StockTradingServiceImpl extends StockTradingServiceGrpc.StockTradingServiceImplBase {

    private final StockRepository stockRepository;

    public StockTradingServiceImpl(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public void getStockPrice(StockRequest request, StreamObserver<StockResponse> responseObserver) {
        String stockSymbol = request.getStockSymbol();
        Optional<Stock> stock = stockRepository.findByStockSymbol(stockSymbol);

        if (stock.isPresent()) {
            Stock stockEntity = stock.get();
            StockResponse response = StockResponse.newBuilder()
                    .setStockSymbol(stockEntity.getStockSymbol())
                    .setPrice(stockEntity.getPrice())
                    .setTimestamp(stockEntity.getLastUpdated() != null ? stockEntity.getLastUpdated().toString() : "N/A")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Stock " + stockSymbol + " is not available in system")
                            .asRuntimeException()
            );
        }
    }


    @Override
    public void subscribeStockPrice(StockRequest request, StreamObserver<StockResponse> responseObserver) {
        String stockSymbol = request.getStockSymbol();
        try {
            // Send price updates for 10 seconds
            for (int i = 0; i < 10; i++) {
                StockResponse response = StockResponse.newBuilder()
                        .setStockSymbol(stockSymbol)
                        .setPrice(new Random().nextDouble(200))
                        .setTimestamp(Instant.now().toString())
                        .build();

                responseObserver.onNext(response); // Send response to client
                TimeUnit.SECONDS.sleep(1); // Simulate real-time update every second
            }
            responseObserver.onCompleted(); // End the stream
        } catch (InterruptedException e) {
            responseObserver.onError(e);
        }
    }

    /// This is not swapped. It follows this rule:
    /// For client streaming, the method returns the stream that client will write to
    /// (the server, receive this), and it accepts a stream to write the final single response (OrderSummary) back.
    @Override
    public StreamObserver<StockOrder> placeBulkOrder(StreamObserver<OrderSummary> responseObserver) {

        return new StreamObserver<StockOrder>() {

            private int totalOrders = 0;
            private int successCount = 0;
            private double totalAmount = 0.0;

            @Override
            public void onNext(StockOrder stockOrder) {
                totalOrders++;
                totalAmount += stockOrder.getPrice() * stockOrder.getQuantity();
                successCount++; // Assume all orders succeed for simplicity
                System.out.println("Received order: " + stockOrder);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("server unable to process the request " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                OrderSummary summary = OrderSummary.newBuilder()
                        .setTotalOrders(totalOrders)
                        .setSuccessCount(successCount)
                        .setTotalAmount(totalAmount)
                        .build();
                responseObserver.onNext(summary);
                responseObserver.onCompleted();
            }
        };
    }
}
