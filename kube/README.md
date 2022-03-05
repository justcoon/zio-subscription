# kafka

https://strimzi.io/quickstarts/

```
kubectl apply -f 'https://strimzi.io/install/latest?namespace=default'
kubectl apply -f kube/strimzi-kafka.yaml
```

```
kubectl exec --stdin --tty kafka-kafka-0 -- /bin/bash
kafka-topics.sh --bootstrap-server kafka-kafka-brokers:9092 --create --topic c-subscription --partitions 3
```

# postgres

https://devopscube.com/deploy-postgresql-statefulset/

```
kubectl exec --stdin --tty postgres-sts-0 -- /bin/bash
```

# subscription service

stafulset - offsets for CDC are stored in file, replica with index 0 using `all` app mode, others `svc` app mode

minikube docker env
```
eval $(minikube -p minikube docker-env)
```

docker image
```
sbt docker:publishLocal
```

kube deployment
```
kubectl apply -f kube/subscription.yaml
```

forward service
```
kubectl port-forward service/subscription-svc 8040:8010
```