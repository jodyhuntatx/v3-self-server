#!/bin/bash
source ../mysql.config
if [[ "$#" != 1 ]]; then
  echo "Usage: $0 <query-sql-file>"
  exit -1
fi
cat $1 \
| $DOCKERI $MYSQL_SERVER mysql -h $MYSQL_HOSTNAME -u root --password=$MYSQL_ROOT_PASSWORD docinabox
