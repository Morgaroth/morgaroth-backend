#!/usr/bin/env bash


CONTAINER_ID=morgarothserver

./deploy_docker.sh ${@:1}
docker logs -f ${CONTAINER_ID}