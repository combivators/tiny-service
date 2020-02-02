#!/bin/sh
SD_TOKEN="J1jrWpc0i-pQlgDDArGgBtcGzM4gkgPHUIgOA5SwWUc"
SD_PID=80904
SD_JOB=buildpack_stg
TAG_NAME=v103
JWT=`curl -s "https://api-cd.screwdriver.corp.yahoo.co.jp/v4/auth/token?api_token=$SD_TOKEN" | jq -r ".token"`
curl -s -X POST https://api-cd.screwdriver.corp.yahoo.co.jp/v4/events \
-H "Authorization: Bearer $JWT" \
-H "Content-Type: application/json" \
-d "{ \"pipelineId\": $SD_PID, \"startFrom\": \"$SD_JOB\", \"meta\": { \"tag\": \"$TAG_NAME\" } }"
