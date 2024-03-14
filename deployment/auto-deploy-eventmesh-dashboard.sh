#!/bin/bash

# Git repository path
REPO_PATH=~/service/eventmesh-dashboard

# SpringBoot process ID file path
PID_LOG=./eventmesh-dashboard-pid.log

# Automatic deployment log file path
AUTO_DEPLOY_LOG=./auto-deploy-eventmesh-dashboard.log

# Jar file path
JAR_FILE_PATH=~/service/eventmesh-dashboard/eventmesh-dashboard-console/target/eventmesh-dashboard-console-0.0.1-SNAPSHOT.jar

# Check if the pid.log file exists, if not, create an empty file
touch $PID_LOG

# Update the git repository
cd $REPO_PATH
git fetch origin dev
LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse origin/dev)

if [ $LOCAL != $REMOTE ]; then
    # If the remote dev branch is newer than the local one, update the local dev branch code
    git pull origin dev

    # Log the event
    echo "$(date +"%Y-%m-%d %H:%M:%S") - change detected." >> $AUTO_DEPLOY_LOG
    
    # Terminate the old process
    if [ -f $PID_LOG ]; then
        PID=$(cat $PID_LOG)
        if [ -n "$PID" ]; then
            kill $PID
            # Log the event
            echo "$(date +"%Y-%m-%d %H:%M:%S") - kill running application." >> $AUTO_DEPLOY_LOG
        fi
    fi
    
    # Compile and package the Jar file using Maven
    mvn clean package
    
    # Start the springboot application and record the process id to pid.log file, redirect console logs to eventmesh-dashboard-<current time>.log file
    nohup java -jar $JAR_FILE_PATH > eventmesh-dashboard-$(date +"%Y-%m-%d-%H-%M-%S").log 2>&1 &
    echo $! > $PID_LOG
    
    # Log the event
    echo "$(date +"%Y-%m-%d %H:%M:%S") - start new application." >> $AUTO_DEPLOY_LOG
else
    # If there are no new changes in the remote dev branch
    
    # Log the event
    echo "$(date +"%Y-%m-%d %H:%M:%S") - no change detected." >> $AUTO_DEPLOY_LOG
    
    if [ -f $PID_LOG ]; then
        # If the pid.log file exists, no action is performed
        echo "$(date +"%Y-%m-%d %H:%M:%S") - application running, no operation performed." >> $AUTO_DEPLOY_LOG
    else
        # If the pid.log file does not exist, start the springboot application and record the process id to pid.log file, redirect console logs to eventmesh-dashboard-<current time>.log file
        nohup java -jar $JAR_FILE_PATH > eventmesh-dashboard-$(date +"%Y-%m-%d-%H-%M-%S").log 2>&1 &
        echo $! > $PID_LOG
        
        # Log the event
        echo "$(date +"%Y-%m-%d %H:%M:%S") - no pid.log file, start application." >> $AUTO_DEPLOY_LOG
    fi
fi
