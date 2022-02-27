
#kafka
https://strimzi.io/quickstarts/

```
kubectl apply -f 'https://strimzi.io/install/latest?namespace=default'
kubectl apply -f kube/strimzi-kafka.yaml
```

```
kubectl exec --stdin --tty kafka-kafka-0 -- /bin/bash
kafka-topics.sh --bootstrap-server kafka-kafka-brokers:9092 --create --topic c-subscription --partitions 3
```

#postgres

https://devopscube.com/deploy-postgresql-statefulset/

```
kubectl exec --stdin --tty postgres-sts-0 -- /bin/bash
```
# docker env
```
eval $(minikube -p minikube docker-env)
```
# docker image
```
sbt docker:publishLocal
```


kubectl apply -f kube/subscription.yaml