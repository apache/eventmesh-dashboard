/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.function.report.iotdb;

import org.apache.eventmesh.dashboard.console.function.report.annotation.AbstractReportMetaHandler;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.CaseFormat;

public class IotDBReportMetaHandler extends AbstractReportMetaHandler {


    @Override
    public String insert() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insert into ");
        stringBuilder.append(this.reportMeta.getTableName());
        stringBuilder.append(" (");
        this.fieldList.forEach(field -> {
            String key = CaseFormat.LOWER_CAMEL.to(com.google.common.base.CaseFormat.LOWER_UNDERSCORE, field.getName());
            stringBuilder.append(key).append(",");
        });
        stringBuilder.deleteCharAt(stringBuilder.length() - 1).append(" ) values \r\n");
        stringBuilder.append("  <foreach  collection='_parameter' item='item' index='index'  separator=','> \r\n    (");
        this.fieldList.forEach(field -> {
            if(field.getName().endsWith("Id") || !(Number.class.isAssignableFrom(field.getType()))){
                stringBuilder.append("'${item.").append(field.getName()).append("}'");
            }else {
                stringBuilder.append("${item.").append(field.getName()).append("}");
            }

            stringBuilder.append(",");
        });
        stringBuilder.deleteCharAt(stringBuilder.length() - 1).append(")\r\n").append("  </foreach>");
        return stringBuilder.toString();
    }

    @Override
    public String query(String type) {
        Map<String, Field> map = new HashMap<>();
        this.fieldList.forEach(field -> {
           map.put(field.getName(), field);
        });
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select ")
            .append("\r\n  <if test='_parameter.interval != null'> date_bin(${_parameter.interval}, time) as time , </if>")
            .append("\r\n  <if test='_parameter.interval == null'> time ,</if> ");
        stringBuilder.append("\r\n  <trim prefix=' ' prefixOverrides=','>");
        this.fieldList.forEach(field -> {
            String filedName = field.getName();
            if(Objects.equals(filedName,"time")){
                return;
            }
            String key = CaseFormat.LOWER_CAMEL.to(com.google.common.base.CaseFormat.LOWER_UNDERSCORE, filedName);
            if (filedName.startsWith("value")) {
                stringBuilder.append("\r\n    <if test='_parameter.selectFun == null'>,").append(key).append("</if>")
                    .append("\r\n    <if test='_parameter.selectFun != null'> ,${_parameter.selectFun}(").append(key).append(")</if>");
            } else if (key.endsWith("_id")) {
                stringBuilder.append("\r\n    ");
                stringBuilder.append("<if test='_parameter.").append(filedName).append("!=null'>  ,")
                    .append(key).append(',');
                String typeField = filedName.replace("Id","Type");
                if(map.containsKey(typeField)){
                    stringBuilder.append(",").append(key.replace("id", "type"));
                }
                String nameField = filedName.replace("Id","Name");
                if(map.containsKey(nameField)){
                    //  last_by 函数可以解决 attribute类型字段 必须 与 group by 保持一致的问题
                    stringBuilder.append(",")
                        .append("last_by(").append(key.replace("id", "name"))
                        .append(")").append(",");
                }
                stringBuilder.append("  </if>");
            }
        });
        stringBuilder.append("\r\n  </trim>");
        stringBuilder.append("\r\nfrom ").append(this.reportMeta.getTableName()).append("\r\n where \r\n")
            .append(" time &gt; ${startTime} and time &lt; ${endTime}  \r\n");
        this.fieldList.forEach(field -> {
            String filedName = field.getName();
            if (filedName.endsWith("Id")) {
                String key = CaseFormat.LOWER_CAMEL.to(com.google.common.base.CaseFormat.LOWER_UNDERSCORE, filedName);
                stringBuilder.append("\r\n    <if test='_parameter.").append(field.getName()).append("!=null'>").append("   and  ").
                    append(key).append("='${_parameter.").append(field.getName()).append("}'   </if>");
            }
        });
        stringBuilder.append("\r\n  <if test='_parameter.selectFun != null'> ");
        stringBuilder.append("\r\n group by time, \r\n  <trim prefix=' ' prefixOverrides=','>");
        this.fieldList.forEach(field -> {
            String filedName = field.getName();
            String key = CaseFormat.LOWER_CAMEL.to(com.google.common.base.CaseFormat.LOWER_UNDERSCORE, filedName);
            if (filedName.endsWith("Id")) {
                stringBuilder.append(AbstractReportMetaHandler.lineAndTabString);
                stringBuilder.append("<if test='_parameter.").append(filedName).append("!=null'>,")
                    .append(key).append(" , ");
//                stringBuilder.append(key.replace("id", "name"));
//                String typeField = filedName.replace("Id","Type");
//                if(map.containsKey(typeField)){
//                    stringBuilder.append(",").append(key.replace("id", "type"));
//                }
                stringBuilder.append("  </if>");
            }
        });
        stringBuilder.append("\r\n  </trim>").append("\r\n  </if>");
        return stringBuilder.toString();
    }

    @Override
    public String createTable() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("create table if not exists ");
        stringBuilder.append(this.reportMeta.getTableName());
        stringBuilder.append(" (\r\n    ");
        AtomicInteger num = new AtomicInteger(this.fieldList.size());
        this.fieldList.forEach(field -> {
            String filedName = field.getName();
            String key = CaseFormat.LOWER_CAMEL.to(com.google.common.base.CaseFormat.LOWER_UNDERSCORE, filedName);
            stringBuilder.append(key);
            Class<?> type = field.getType();
            if (type == long.class || type == Long.class ||
                type == int.class || type == Integer.class
            ) {
                stringBuilder.append(key.endsWith("_id") ? " string " : " int64 ");
            } else if (type == float.class || type == Float.class) {
                stringBuilder.append(" float ");
            } else if (type == String.class) {
                stringBuilder.append(" string ");
            } else if (type == LocalDateTime.class) {
                stringBuilder.append(" timestamp ");
            }

            if (Objects.equals(filedName, "time")) {
                stringBuilder.append(" time");
            } else if (filedName.endsWith("Id")) {
                stringBuilder.append(" tag");
            } else if (filedName.startsWith("value") || field.getDeclaringClass() == this.reportMeta.getClazz()) {
                stringBuilder.append(" field");

            } else {
                stringBuilder.append(" attribute");
            }
            if(num.decrementAndGet() != 0){
                stringBuilder.append(",");
                stringBuilder.append(lineAndTabString);
            }
        });
        stringBuilder.append("\r\n)comment ' ").append(reportMeta.getComment()).append("'");
        return stringBuilder.toString();
    }


}
