#!/bin/bash
source ../mysql.config
cat db_load_petstore.sql \
| $DOCKERI mysql -h $MYSQL_HOSTNAME -u root --password=$MYSQL_ROOT_PASSWORD
