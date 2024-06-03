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
PID_LOG=$REPO_PATH/deployment/eventmesh-dashboard-pid.log

# Automatic deployment shell script log file path
AUTO_DEPLOY_LOG=$REPO_PATH/deployment/auto-deploy-eventmesh-dashboard.log

# Jar file path
JAR_FILE_PATH=$REPO_PATH/eventmesh-dashboard-console/target/eventmesh-dashboard-console-0.0.1-SNAPSHOT.jar

# Load environment variables from external file
ENV_FILE=$REPO_PATH/deployment/.env
source $ENV_FILE

# Function to check if a process with given PID is running
is_process_running() {
    local pid=$1
    if ps -p $pid > /dev/null; then
        return 0
    else
        return 1
    fi
}

# Update the git repository
cd $REPO_PATH
git fetch origin main
LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse origin/main)

if [ $LOCAL != $REMOTE ]; then
    # If the remote branch is newer than the local one, update the local branch code
    git pull origin main

    # Log the event
    echo "$(date +"%Y-%m-%d %H:%M:%S") - change detected." >> $AUTO_DEPLOY_LOG
    
    # Terminate the old process if it's running
    if [ -s $PID_LOG ]; then
        PID=$(cat $PID_LOG)
        if is_process_running $PID; then
            kill $PID
            # Log the event
            echo "$(date +"%Y-%m-%d %H:%M:%S") - kill running application." >> $AUTO_DEPLOY_LOG
        fi
    fi
    
    # Compile and package the Jar file
    $REPO_PATH/mvnw clean package -DskipTests -Dcheckstyle.skip=true
    
    # Start the springboot application and record the process id to pid.log file
    nohup java -DDB_ADDRESS=$DB_ADDRESS -DDB_USERNAME=$DB_USERNAME -DDB_PASSWORD=$DB_PASSWORD -jar $JAR_FILE_PATH > /dev/null 2>&1 &
    echo $! > $PID_LOG
    
    # Log the event
    echo "$(date +"%Y-%m-%d %H:%M:%S") - start new application." >> $AUTO_DEPLOY_LOG
else
    # If there are no new changes in the remote branch
    
    # Log the event
    echo "$(date +"%Y-%m-%d %H:%M:%S") - no change detected." >> $AUTO_DEPLOY_LOG
    
    if [ ! -s $PID_LOG ] || ! is_process_running $(cat $PID_LOG); then
        # If the pid.log file does not exist or the process is not running, compile and package the Jar file
        $REPO_PATH/mvnw clean package -DskipTests -Dcheckstyle.skip=true

        # Start the springboot application and record the process id to pid.log file
        nohup java -DDB_ADDRESS=$DB_ADDRESS -DDB_USERNAME=$DB_USERNAME -DDB_PASSWORD=$DB_PASSWORD -jar $JAR_FILE_PATH > /dev/null 2>&1 &
        echo $! > $PID_LOG

        # Log the event
        echo "$(date +"%Y-%m-%d %H:%M:%S") - no pid.log file or process not running, start application." >> $AUTO_DEPLOY_LOG
    else
        # If the pid.log file exists and the process is running, no action is performed
        echo "$(date +"%Y-%m-%d %H:%M:%S") - application running, no operation performed." >> $AUTO_DEPLOY_LOG
    fi
fi
