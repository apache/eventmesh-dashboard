#!/bin/bash
#
# Licensed to Apache Software Foundation (ASF) under one or more contributor
# license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright
# ownership. Apache Software Foundation (ASF) licenses this file to you under
# the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Git repository path
REPO_PATH=~/service/eventmesh-dashboard

# SpringBoot process ID file path
PID_LOG=~/service/eventmesh-dashboard/deployment/eventmesh-dashboard-pid.log

# Automatic deployment shell script log file path
AUTO_DEPLOY_LOG=~/service/eventmesh-dashboard/deployment/auto-deploy-eventmesh-dashboard.log

# EventMesh Dashboard log file path
APP_LOG=~/service/eventmesh-dashboard/deployment/eventmesh-dashboard-$(date +"%Y-%m-%d-%H-%M-%S").log

# Jar file path
JAR_FILE_PATH=~/service/eventmesh-dashboard/eventmesh-dashboard-console/target/eventmesh-dashboard-console-0.0.1-SNAPSHOT.jar

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
    if [ -s $PID_LOG ]; then
        PID=$(cat $PID_LOG)
        if [ -n "$PID" ]; then
            kill $PID
            # Log the event
            echo "$(date +"%Y-%m-%d %H:%M:%S") - kill running application." >> $AUTO_DEPLOY_LOG
        fi
    fi
    
    # Compile and package the Jar file
    mvn clean package
    
    # Start the springboot application and record the process id to pid.log file, redirect console logs to eventmesh-dashboard-<current time>.log file
    nohup java -jar $JAR_FILE_PATH > $APP_LOG 2>&1 &
    echo $! > $PID_LOG
    
    # Log the event
    echo "$(date +"%Y-%m-%d %H:%M:%S") - start new application." >> $AUTO_DEPLOY_LOG
else
    # If there are no new changes in the remote dev branch
    
    # Log the event
    echo "$(date +"%Y-%m-%d %H:%M:%S") - no change detected." >> $AUTO_DEPLOY_LOG
    
    if [ -s $PID_LOG ]; then
        # If the pid.log file exists, no action is performed
        echo "$(date +"%Y-%m-%d %H:%M:%S") - application running, no operation performed." >> $AUTO_DEPLOY_LOG
    else
        # If the pid.log file does not exist, compile and package the Jar file
        mvn clean package

        # Start the springboot application and record the process id to pid.log file, redirect console logs to eventmesh-dashboard-<current time>.log file
        nohup java -jar $JAR_FILE_PATH > $APP_LOG 2>&1 &
        echo $! > $PID_LOG

        # Log the event
        echo "$(date +"%Y-%m-%d %H:%M:%S") - no pid.log file, start application." >> $AUTO_DEPLOY_LOG
    fi
fi
