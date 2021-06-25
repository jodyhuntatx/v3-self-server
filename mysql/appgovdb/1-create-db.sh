#!/bin/bash
source ../mysql.config
cat db_create_appgovdb.sql \
| $DOCKER exec -i $MYSQL_SERVER mysql -h $MYSQL_HOSTNAME -u root --password=$MYSQL_ROOT_PASSWORD
