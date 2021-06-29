#!/bin/bash
export CONJUR_HOME=~/Conjur/cybrsm-demos
source $CONJUR_HOME/config/azure.config
source ../mysql/mysql.config

SSH_KEY=$AZURE_SSH_KEY
PUB_DNS=$AZURE_PUB_DNS

echo "sudo docker exec -i $MYSQL_SERVER 				\
	mysqladmin -u root --password=$MYSQL_ROOT_PASSWORD processlist" \
| ssh -i $SSH_KEY $LOGIN_USER@$PUB_DNS
