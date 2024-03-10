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
import org.apache.eventmesh.dashboard.console.entity.log.LogEntity;
import org.apache.eventmesh.dashboard.console.service.log.LogService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

@Aspect
@Service
public class OprLog implements Ordered, ApplicationContextAware {

    private int order = LOWEST_PRECEDENCE - 1000; // Specify the order of execution

    private LogService logService;

    private ApplicationContext applicationContext;


    @Pointcut("within(org.apache.eventmesh.dashboard.console.service..*)")
    public void pointCut() {
    }


    @Around("pointCut()")
    public Object logStart(ProceedingJoinPoint joinPoint) throws Throwable {
        if (Objects.isNull(this.logService)) {
            this.logService = applicationContext.getBean(LogService.class);
        }
        EmLog declaredAnnotation = this.getTargetEmlog(joinPoint);
        //Get the Emlog annotation on the method
        if (Objects.isNull(declaredAnnotation)) {
            return joinPoint.proceed();
        }
        LogEntity logEntity = this.productLoEntity(declaredAnnotation, joinPoint);
        logService.addLog(logEntity);
        logEntity.setEndTime(new Timestamp(System.currentTimeMillis()));
        Object proceed = null;
        try {
            proceed = joinPoint.proceed();
            logEntity.setState(2);
            logEntity.setResult(Objects.isNull(proceed) ? "" : proceed.toString());
            return proceed;
        } catch (Throwable e) {
            logEntity.setState(3);
            throw new RuntimeException(e);
        } finally {
            logEntity.setResult(proceed.toString());
            logService.updateLog(logEntity);
        }


    }

    public LogEntity productLoEntity(EmLog declaredAnnotation, ProceedingJoinPoint joinPoint) throws NoSuchFieldException, IllegalAccessException {
        LogEntity logEntity = new LogEntity();
        Object[] args = joinPoint.getArgs();
        Object model = args[0];
        //Obtaining the Input Parameter of the Operation Method (Specified as the First)
        Field clusterPhyId = model.getClass().getDeclaredField("clusterId");
        clusterPhyId.setAccessible(true);
        Long opClusterPhyId = (Long) clusterPhyId.get(model);
        //The clusterId is obtained from the parameter object, and the operation is described as the object itself
        logEntity.setClusterId(opClusterPhyId);
        logEntity.setContent(model.toString());
        logEntity.setOperationType(declaredAnnotation.OprType());
        logEntity.setTargetType(declaredAnnotation.OprTarget());
        logEntity.setState(1);
        logEntity.setCreateTime(new Timestamp(System.currentTimeMillis()));
        return logEntity;
    }

    public EmLog getTargetEmlog(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Method mostSpecificMethod = ClassUtils.getMostSpecificMethod(method, joinPoint.getTarget().getClass());
        EmLog declaredAnnotation = mostSpecificMethod.getAnnotation(EmLog.class);
        return declaredAnnotation;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
