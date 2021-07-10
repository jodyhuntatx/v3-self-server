#!/bin/bash
source ./mysql.config
$DOCKERI mysql-server mysqladmin -u root --password=Cyberark1 processlist
