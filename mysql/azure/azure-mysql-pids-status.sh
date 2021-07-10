#!/bin/bash
export CONJUR_HOME=~/Conjur/cybrsm-demos
source $CONJUR_HOME/config/azure.config

SSH_KEY=$AZURE_SSH_KEY
PUB_DNS=$AZURE_PUB_DNS

echo "sudo docker exec -i mysql-server mysqladmin -u root --password=Cyberark1 processlist" | ssh -i $SSH_KEY $LOGIN_USER@$PUB_DNS 
