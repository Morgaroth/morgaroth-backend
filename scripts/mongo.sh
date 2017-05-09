#!/usr/bin/env bash

docker run --restart=always --name mongo -p 27017:27017 -v $HOME/mongo:/data/db -d mongo:latest