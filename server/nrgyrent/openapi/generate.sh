#!/bin/bash
npx @openapitools/openapi-generator-cli generate -i openapi.yaml -c config.yaml -g java -o ./out --group-id org.ipan --artifact-id nrgyrent.trongrid
# cd into the generated directory
cd out
#  build the project
mvn clean install
