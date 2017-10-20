#!/usr/bin/env bash


CONTAINER_NAME=morgarothserver
VERSION=$1
echo "Deploying version $VERSION"
A=`docker inspect -f {{.State.Running}} ${CONTAINER_NAME}`
B=`docker inspect -f {{.State}} ${CONTAINER_NAME}`
echo "'$A' '$B' '$?'"
if [ "$A" = "true" ]; then
    echo "Docker $CONTAINER_NAME is running, killing them..."
    docker kill ${CONTAINER_NAME}
else
    echo "Docker $CONTAINER_NAME not found."
fi
sleep 3
if [ "$B" != "" ]; then
    echo "Docker $CONTAINER_NAME exists, removing them..."
    docker rm ${CONTAINER_NAME}
else
    echo "Docker $CONTAINER_NAME not found."
fi

docker run --detach \
            --restart=always \
            --name ${CONTAINER_NAME} \
            --user `stat -c "%u:%g" .` \
            --env KOKPIT_BOT_API_KEY \
            --env GPBETTINGLEAGUE_USERNAME \
            --env GPBETTINGLEAGUE_PASSWORD \
            --env "MONGODB_URI=mongodb://172.17.0.1:28017" \
            --env "REMOTE_SELENIUM_SERVER=http://172.17.0.1:4444/wd/hub" \
            --publish 8888:8080 \
            --expose 8888 \
            morgarothserver:${VERSION}

echo "Docker fired!"
