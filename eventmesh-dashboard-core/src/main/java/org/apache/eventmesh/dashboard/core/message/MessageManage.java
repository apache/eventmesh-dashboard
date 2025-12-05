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


package org.apache.eventmesh.dashboard.core.message;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;
import org.apache.eventmesh.dashboard.core.message.model.AbstractMessageDTO;
import org.apache.eventmesh.dashboard.core.message.model.ConsumerDTO;
import org.apache.eventmesh.dashboard.core.message.model.ProducerDTO;
import org.apache.eventmesh.dashboard.core.message.operate.AbstractConsumerOperate;
import org.apache.eventmesh.dashboard.core.message.operate.AbstractMessageOperate;
import org.apache.eventmesh.dashboard.core.message.operate.ConsumerOperate;
import org.apache.eventmesh.dashboard.core.message.operate.ProducerOperate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MessageManage {

    private static final Map<ClusterType, Map<Class<?>, Class<?>>> MESSAGE_CLASSES = new HashMap<>();

    static {
        Set<Class<?>> interfaceSet = new HashSet<>();
        interfaceSet.add(ProducerOperate.class);
        interfaceSet.add(ConsumerOperate.class);
        ClasspathScanner classpathScanner = ClasspathScanner.builder().base(SDKManage.class).subPath("/operation").interfaceSet(interfaceSet).build();
        try {
            List<Class<?>> classList = classpathScanner.getClazz();
            classList.forEach(MessageManage::createMessageOperate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Class<?>, Map<String, AbstractMessageOperate>> classMapConcurrentHashMap = new ConcurrentHashMap<>();

    {
        classMapConcurrentHashMap.put(ProducerOperate.class, new ConcurrentHashMap<>());
        classMapConcurrentHashMap.put(ConsumerOperate.class, new ConcurrentHashMap<>());
    }

    static void createMessageOperate(Class<?> clazz) {

    }

    public ProducerOperate createProducerOperate(ProducerDTO producerDTO) throws Exception {
        return this.createOperate(producerDTO, ProducerOperate.class, SDKTypeEnum.PRODUCER);
    }

    public ConsumerOperate createConsumerOperate(ConsumerDTO consumerDTO) throws Exception {
        return this.createOperate(consumerDTO, ConsumerOperate.class, SDKTypeEnum.CONSUMER);
    }

    private <T> T createOperate(AbstractMessageDTO abstractMessageDTO, Class<?> operateClazz, SDKTypeEnum sdkTypeEnum) throws Exception {
        Class<?> clazz = MESSAGE_CLASSES.get(abstractMessageDTO.getClusterType()).get(operateClazz);

        AbstractConsumerOperate abstractConsumerOperate = (AbstractConsumerOperate) clazz.newInstance();
        AbstractMultiCreateSDKConfig abstractMultiCreateSDKConfig =
            ConfigManage.getInstance().getMultiCreateSDKConfig(abstractMessageDTO.getClusterType(), sdkTypeEnum);

        CreateSDKConfig consumerConfig = abstractConsumerOperate.createSDKConfig(abstractMultiCreateSDKConfig, null);
        // RocketMQ，模式如何处理？
        Object client = null;
        //SDKManage.getInstance().createClient(sdkTypeEnum, consumerConfig, abstractMessageDTO.getClusterType());
        abstractConsumerOperate.setClient(client);
        abstractConsumerOperate.setAbstractMessageDTO(abstractMessageDTO);

        abstractConsumerOperate.start();
        String uuId = java.util.UUID.randomUUID().toString();
        classMapConcurrentHashMap.get(operateClazz).put(uuId, abstractConsumerOperate);
        return (T) abstractConsumerOperate;
    }


    public List<Object> pull(String uuId) {
        ConsumerOperate consumerOperate = (ConsumerOperate) classMapConcurrentHashMap.get(ConsumerOperate.class).get(uuId);

        return consumerOperate.pull();
    }

    public void send(String uuId) {
        ProducerOperate producerOperate = (ProducerOperate) classMapConcurrentHashMap.get(ProducerOperate.class).get(uuId);
    }
}

