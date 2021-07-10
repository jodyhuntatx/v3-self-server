#!/bin/bash
source ./mysql.config
pids=$($DOCKERI mysqladmin -u root --password=Cyberark1 processlist | grep Sleep | cut -d " " -f2)
echo "Dangling PIDs to kill: " $pids
for pid in $pids; do
  $DOCKERI mysqladmin -u root --password=Cyberark1 kill $pid
done
$DOCKERI mysqladmin -u root --password=Cyberark1 processlist
