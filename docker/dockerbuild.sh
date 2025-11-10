#! /bin/bash
NAME=caternberg/cb-ci-controller-stress-ng
TAG=latest
#docker  buildx  build   -t $NAME  .
docker   build   -t $NAME  .
docker push $NAME
docker push $NAME:$TAG