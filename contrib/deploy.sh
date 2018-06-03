#!/bin/bash
# contrib/deploy.sh
# tesshucom/jpsonic
#
# Helper script to shorten dev/build/deployment
#

sudo systemctl stop tomcat
sudo rm /var/lib/tomcat/webapps/jpsonic* -rf
sudo cp jpsonic-main/target/jpsonic.war /var/lib/tomcat/webapps/
sudo systemctl start tomcat

