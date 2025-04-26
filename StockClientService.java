package com.javatechie.service;

import com.javatechie.grpc.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockClientService {

//   @GrpcClient("stockService") // This should match the client name in application.yml
//    private StockTradingServiceGrpc.StockTradingServiceBlockingStub stockServiceStub;

    @GrpcClient("stockService") // This should match the client name in application.yml
    private StockTradingServiceGrpc.StockTradingServiceStub stockServiceStub;


    public void subscribeStockPrice(String stockSymbol) {
        StockRequest request = StockRequest.newBuilder()
                .setStockSymbol(stockSymbol)
                .build();
        stockServiceStub.subscribeStockPrice(request, new StreamObserver<StockResponse>() {
            @Override
            public void onNext(StockResponse response) {
                System.out.println("Stock Price Update: " + response.getStockSymbol() +
                        " Price: " + response.getPrice() + " " +
                        " Time: " + response.getTimestamp());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Stock price streaming completed.");
            }
        });
    }

    public void sendBulkOrders() {

        StreamObserver<OrderSummary> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(OrderSummary summary) {
                System.out.println("Order Summary Received from Server:");
                System.out.println("Total Orders: " + summary.getTotalOrders());
                System.out.println("Successful Orders: " + summary.getSuccessCount());
                System.out.println("Total Amount: $" + summary.getTotalAmount());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error receiving summary from server: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed. Server is done sending summary.");
            }
        };

        StreamObserver<StockOrder> requestObserver = stockServiceStub.placeBulkOrder(responseObserver);

        try {
            // Send multiple StockOrder messages
            requestObserver.onNext(StockOrder.newBuilder()
                    .setOrderId("1")
                    .setStockSymbol("AAPL")
                    .setOrderType("BUY")
                    .setPrice(150.5)
                    .setQuantity(10)
                    .build());

            requestObserver.onNext(StockOrder.newBuilder()
                    .setOrderId("2")
                    .setStockSymbol("GOOGL")
                    .setOrderType("SELL")
                    .setPrice(2700.0)
                    .setQuantity(5)
                    .build());

            requestObserver.onNext(StockOrder.newBuilder()
                    .setOrderId("3")
                    .setStockSymbol("TSLA")
                    .setOrderType("BUY")
                    .setPrice(700.0)
                    .setQuantity(8)
                    .build());

            // Done sending orders
            requestObserver.onCompleted();

        } catch (Exception e) {
            requestObserver.onError(e);
        }
    }

}