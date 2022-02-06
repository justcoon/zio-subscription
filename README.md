# zio subscription

# CDC

https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern/
https://medium.com/@sohan_ganapathy/resilient-eventing-in-microservices-using-the-outbox-pattern-ed0b10ea3ef8
https://medium.com/swlh/change-data-capture-cdc-with-embedded-debezium-and-springboot-6f10cd33d8ec

# required
* postgres
* kafka

### application setup

multi nodes VMs arguments

node 1

```
-Drest-api.port=8030 -Dgrpc-api.port=8040 -Dprometheus.port=9050
```
node 2

``` 
 -Drest-api.port=8031 -Dgrpc-api.port=8041 -Dprometheus.port=9051
```
node 3

``` 
 -Drest-api.port=8032 -Dgrpc-api.port=8042 -Dprometheus.port=9052
```