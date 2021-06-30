#!/bin/bash
export CONJUR_HOME=~/Conjur/cybrsm-demos
source $CONJUR_HOME/config/azure.config

SSH_KEY=$AZURE_SSH_KEY
PUB_DNS=$AZURE_PUB_DNS

pids=$(echo "sudo docker exec -i mysql-server mysqladmin -u root --password=Cyberark1 processlist" | ssh -i $SSH_KEY $LOGIN_USER@$PUB_DNS | grep Sleep | cut -d " " -f2)
for pid in $pids; do
  echo "sudo docker exec -i mysql-server mysqladmin -u root --password=Cyberark1 kill $pid" \
	| ssh -i $SSH_KEY $LOGIN_USER@$PUB_DNS
done
echo "sudo docker exec -i mysql-server mysqladmin -u root --password=Cyberark1 processlist" | ssh -i $SSH_KEY $LOGIN_USER@$PUB_DNS 
