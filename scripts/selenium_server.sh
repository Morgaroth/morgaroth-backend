#!/usr/bin/env bash

docker run -d --restart=always -p 4444:4444 --name selenium -v /dev/shm:/dev/shm selenium/standalone-chrome