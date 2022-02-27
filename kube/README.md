
kafka
https://strimzi.io/quickstarts/


kubectl apply -f 'https://strimzi.io/install/latest?namespace=default'
kubectl apply -f kube/strimzi-kafka.yaml

kafka-topics.sh --bootstrap-server kafka-kafka-brokers:9092 --create --topic c-subscription --partitions 3

postgres

https://devopscube.com/deploy-postgresql-statefulset/