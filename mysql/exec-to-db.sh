#!/bin/bash
source ./mysql.config
$DOCKER exec -it $MYSQL_SERVER mysql -h $MYSQL_HOSTNAME -u root --password=$MYSQL_ROOT_PASSWORD
