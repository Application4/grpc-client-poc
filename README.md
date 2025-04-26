# grpc-client-poc

```
grpcurl -d @ \
  -plaintext \
  -import-path  ~/Downloads/grpc-unary-server/src/main/proto  \
  -proto stock_trading.proto \
  localhost:9090 \
  com.javatechie.grpc.StockTradingService/PlaceBulkOrder < order.txt
```
