#!/bin/bash
source ./cybrtest.config
tail -f $TOMCAT_HOME/logs/catalina.out
