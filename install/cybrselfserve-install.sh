#!/bin/bash

# Apache Tomcat download page: https://tomcat.apache.org/download-90.cgi
export TOMCAT_MINOR_VERSION=50

main() {
#  install-ssh
  install-java
  install-tomcat
  install-mysql
  sudo apt install -y ant curl jq python3-tk
  cd ..
  ./ant.sh publish	# compile & copy servlet code to tomcat
}

#############################
install-ssh() {
  sudo apt update -y
  sudo apt install -y openssh-server
  sudo systemctl status ssh
  sudo ufw allow ssh
}

#############################
install-java() {
  sudo apt update -y
  sudo apt install -y default-jdk
  JAVA_DIR=$(ls -ld /usr/lib/jvm/java*jdk* | grep -v ^l | awk '{print $9}')
  export JAVA_HOME=$JAVA_DIR 
  export JRE_HOME=$JAVA_DIR 
  echo "export JAVA_HOME="$JAVA_HOME"" >> ~/.bashrc
  echo "export JRE_HOME="$JRE_HOME"" >> ~/.bashrc
}

#############################
install-tomcat() {
  sudo groupadd tomcat
  sudo useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat
  wget http://www-us.apache.org/dist/tomcat/tomcat-9/v9.0.$TOMCAT_MINOR_VERSION/bin/apache-tomcat-9.0.$TOMCAT_MINOR_VERSION.tar.gz
  tar xzf apache-tomcat-9.0.$TOMCAT_MINOR_VERSION.tar.gz
  rm apache-tomcat-9.0.$TOMCAT_MINOR_VERSION.tar.gz
  sudo mv apache-tomcat-9.0.$TOMCAT_MINOR_VERSION /usr/local/tomcat9
  echo "export CATALINA_HOME="/usr/local/tomcat9"" >> ~/.bashrc
  source ~/.bashrc

  # copy tomcat-users.xml file w/ admin rights for UI (see end of file)
  cp ./tomcat-users.xml /usr/local/tomcat9/conf/tomcat-users.xml

  # copy mysql jdbc connector to tomcat lib before starting (saves a restart)
  cp ../dependencies/mysql-connector-java-8.0.25.jar /usr/local/tomcat9/lib

  # start tomcat
  cd /usr/local/tomcat9/bin
  chmod +x ./*.sh
  ./startup.sh
}

#############################
# It is weirdly difficult to initialize the mysql root user password.
# If this process doesn't work for some reason, check out what others tried:
# https://www.google.com/search?q=mysql+server+5.7.34+set+root+password+ubuntu+18.04
#
install-mysql() {
  sudo apt update -y
  sudo apt install -y mysql-server
  sudo service mysql stop
  while [[ "$(ps -aux | grep mysql | grep -v grep)" != "" ]]; do
    sleep 3
  done
  sudo service mysql status
  sudo mkdir -p /var/run/mysqld; sudo chown mysql:mysql /var/run/mysqld
  sudo mysqld_safe --skip-grant-tables &
  echo
  echo
  echo "waiting 10 seconds for db to initialize..."
  sleep 10
  echo
  echo
  echo
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  echo "                         READ      THIS"
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  echo
  echo "At the mysql prompt cut/paste, the following commands:"
  echo
  echo "  update user set authentication_string=PASSWORD('Cyberark1') where user='root';"
  echo "  update user set plugin=\"mysql_native_password\" where User='root';"
  echo "  flush privileges;"
  echo
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  echo "           LOOK     HERE  ^^^^^"
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  echo
  echo
  sudo mysql --user=root mysql 
  for i in $(ps -aux | grep mysql | grep -v grep | awk '{print $2}'); do
    sudo kill -9 $i
  done
  sudo service mysql start
  sudo service mysql status
}

main "$@"
