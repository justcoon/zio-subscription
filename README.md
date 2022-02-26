# zio subscription

# CDC

sources:
* https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern/
* https://medium.com/@sohan_ganapathy/resilient-eventing-in-microservices-using-the-outbox-pattern-ed0b10ea3ef8
* https://medium.com/swlh/change-data-capture-cdc-with-embedded-debezium-and-springboot-6f10cd33d8ec
* https://www.baeldung.com/debezium-intro

# required
* postgres
* kafka

### postgres wal setup

setup

```sql
ALTER SYSTEM SET wal_level = logical;
```
and then restart and check

```sql
SHOW wal_level;
```