#!/usr/bin/env bash


CONTAINER_ID=morgarothserver
VERSION=$1
echo "Deploying version $VERSION"
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
#            -e pass="$1" \
docker run -d \
            --restart=always \
            --name ${CONTAINER_ID} \
            -u `stat -c "%u:%g" .` \
            -e KOKPIT_BOT_API_KEY \
            -e GPBETTINGLEAGUE_USERNAME \
            -e GPBETTINGLEAGUE_PASSWORD \
            -e "MONGODB_URI=mongodb://172.17.0.1:28017" \
            -e "REMOTE_SELENIUM_SERVER=http://172.17.0.1:4444/wd/hub" \
            morgarothserver:${VERSION}

echo "Docker fired!"
