#! /bin/bash

# Node is the general purpose one that all other images are based on
docker build -t simofa-alpine -f simofa-alpine.dockerfile .
docker build -t simofa-jekyll -f simofa-jekyll.dockerfile .