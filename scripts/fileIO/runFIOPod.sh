#!/bin/bash

kubectl apply -f k8s-fio.yaml
kubectl wait pod/fio-benchmark --for condition=ready  --timeout=120s
kubectl logs -f fio-benchmark
kubectl delete -f k8s-fio.yaml
