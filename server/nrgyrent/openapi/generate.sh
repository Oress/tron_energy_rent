#!/bin/bash
npx @openapitools/openapi-generator-cli generate -i openapi.yaml -c config.yaml -g java -o ./out --group-id org.ipan --artifact-id nrgyrent.trongrid