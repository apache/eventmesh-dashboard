/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.log;

import org.apache.eventmesh.dashboard.console.annotation.EmLog;
import org.apache.eventmesh.dashboard.console.entity.LogEntity;
import org.apache.eventmesh.dashboard.console.log.service.LogService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

@Aspect
@Service
public class OprLog implements Ordered {

    private int order = LOWEST_PRECEDENCE - 1000; // Specify the order of execution

    @Autowired
    private LogService logService;

    @Pointcut("within(org.apache.eventmesh.dashboard.console.service..*)")
    public void pointCut() {
    }

    @Around("pointCut()")
    public Object logStart(ProceedingJoinPoint joinPoint) throws Throwable {
        LogEntity logEntity = new LogEntity();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Method mostSpecificMethod = ClassUtils.getMostSpecificMethod(method, joinPoint.getTarget().getClass());
        EmLog declaredAnnotation = mostSpecificMethod.getAnnotation(EmLog.class);
        //Get the Emlog annotation on the method
        if (declaredAnnotation != null) {
            //It is a method of operation
            logEntity.setOperationType(declaredAnnotation.OprType());
            logEntity.setOperationType(declaredAnnotation.OprTarget());
            Object[] args = joinPoint.getArgs();
            Object model = args[0];
            //Obtaining the Input Parameter of the Operation Method (Specified as the First)
            Field clusterPhyId = model.getClass().getDeclaredField("clusterId");
            clusterPhyId.setAccessible(true);
            Long opClusterPhyId = (Long) clusterPhyId.get(model);
            String opDescription = model.toString();
            logEntity.setClusterId(opClusterPhyId);
            logEntity.setDescription(opDescription);
            //The clusterId is obtained from the parameter object, and the operation is described as the object itself
            logEntity.setStatus(1);
            logEntity.setCreateTime(new Timestamp(System.currentTimeMillis()));
            logService.addLog(logEntity);
            Object proceed = joinPoint.proceed();
            if (proceed == null)  {
                //An exception occurred with the target method
                logEntity.setStatus(3);
                logEntity.setEndTime(new Timestamp(System.currentTimeMillis()));
                Integer integer1 = logService.updateLog(logEntity);
                return proceed;
            } else {
                //The target approach is successful
                logEntity.setEndTime(new Timestamp(System.currentTimeMillis()));
                logEntity.setStatus(2);
                Integer integer1 = logService.updateLog(logEntity);
                return proceed;
            }
        } else {
            //It is not part of the operation method
            return joinPoint.proceed();
        }
    }

    @Override
    public int getOrder() {
        return order;
    }
}
