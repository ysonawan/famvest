#!/bin/bash

echo "Starting deployment..."

echo "Copying JAR to remote server..."
scp target/famvest-app-2.0.0.jar root@172.105.40.137:~
if [ $? -ne 0 ]; then
  echo "Error: SCP failed."
  exit 1
fi

echo "Moving JAR to /opt/app/famvest-app and restarting service..."
ssh root@172.105.40.137 "sudo mv ~/famvest-app-2.0.0.jar /opt/app/famvest-app/ && sudo systemctl restart famvest-app.service"
if [ $? -ne 0 ]; then
  echo "Error: SSH command failed."
  exit 1
fi

echo "Deployment completed successfully."
