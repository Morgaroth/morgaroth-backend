#!/usr/bin/env bash

docker run -d \
    -u `stat -c "%u:%g" $HOME` \
    --restart=always \
    -p 4444:4444 \
    --expose 4444 \
    --name selenium \
    -v /dev/shm:/dev/shm \
    selenium/standalone-chrome