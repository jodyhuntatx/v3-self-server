cat db_query_appgovdb.sql \
| docker exec -i mysql-server mysql -u root --password=Cyberark1 appgovdb
