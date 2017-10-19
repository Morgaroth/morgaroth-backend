#!/usr/bin/env bash


CONTAINER_ID=some-mongo

A=`docker inspect -f {{.State.Running}} ${CONTAINER_ID}`
B=`docker inspect -f {{.State}} ${CONTAINER_ID}`
echo "'$A' '$B' '$?'"
if [ "$A" = "true" ]; then
    echo "Docker $CONTAINER_ID is running, killing them..."
    docker kill ${CONTAINER_ID}
else
    echo "Docker $CONTAINER_ID not found."
fi
sleep 3
if [ "$B" != "" ]; then
    echo "Docker $CONTAINER_ID exists, removing them..."
    docker rm ${CONTAINER_ID}
else
    echo "Docker $CONTAINER_ID not found."
fi

docker run -d \
    -u `stat -c "%u:%g" $HOME` \
    --restart=always \
    --name ${CONTAINER_ID} \
    -p 28017:27017 \
    --expose=28017 \
    -v $HOME/mongo:/data/db \
    mongo:latest