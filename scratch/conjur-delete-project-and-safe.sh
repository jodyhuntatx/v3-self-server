#!/bin/bash
if [[ "$#" != 2 ]]; then
  echo "Usage: $0 <project-name> <safe-name>"
  exit -1
fi
cat delete.yml \
  | sed -e "s#{{ PROJECT_NAME }}#$1#g"		\
  | sed -e "s#{{ SAFE_NAME }}#$2#g"		\
  | docker exec -i conjur-cli conjur policy load --delete root -
