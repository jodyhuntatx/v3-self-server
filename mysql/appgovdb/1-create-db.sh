#!/bin/bash
source ../mysql.config
cat db_create_appgovdb.sql \
| $DOCKERI mysql -h $MYSQL_HOSTNAME -u root --password=$MYSQL_ROOT_PASSWORD
