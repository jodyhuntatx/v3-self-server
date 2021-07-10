#!/bin/bash
source ./mysql.config
$DOCKERI mysqladmin -u root --password=Cyberark1 processlist
