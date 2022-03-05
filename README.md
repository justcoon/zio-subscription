# zio subscription

simple subscription service, which managing subscription data (email and address) for users

demo implementation of outbox pattern with change data capture (CDC)

# outbox pattern with CDC

sources:
* https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern/
* https://medium.com/@sohan_ganapathy/resilient-eventing-in-microservices-using-the-outbox-pattern-ed0b10ea3ef8

# required
* postgres
* kafka

## postgres wal setup (CDC)

setup

```sql
ALTER SYSTEM SET wal_level = logical;
```
and then restart and check

```sql
SHOW wal_level;
```

## config

app mode:
* all - default, svc and cdc features
* svc - service features - api, domain impl.
* cdc - change data capture processing, just one instance/replica of service should run that