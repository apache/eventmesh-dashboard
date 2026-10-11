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

package org.apache.eventmesh.dashboard.console.function.report.iotdb;

import org.apache.eventmesh.dashboard.console.function.report.annotation.AbstractReportMetaHandler;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportTag;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.StringJoiner;
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
            if (field.getName().endsWith("Id") || !(Number.class.isAssignableFrom(field.getType()))) {
                stringBuilder.append("'${item.").append(field.getName()).append("}'");
            } else {
                // 未采集到的指标不代表零值，写入 SQL NULL，避免生成空数值槽位。
                stringBuilder.append("<choose><when test='item.").append(field.getName()).append(" != null'>")
                    .append("${item.").append(field.getName()).append("}</when><otherwise>NULL</otherwise></choose>");
            }

            stringBuilder.append(",");
        });
        stringBuilder.deleteCharAt(stringBuilder.length() - 1).append(")\r\n").append("  </foreach>");
        return stringBuilder.toString();
    }

    @Override
    public String query(String type) {
        // 原查询在 ID/名称之间重复插入逗号，且对普通明细使用聚合；统一分隔符并区分明细和聚合。
        StringBuilder sql = new StringBuilder("select ");
        sql.append("<choose><when test='_parameter.interval != null'>")
            .append("date_bin(${_parameter.interval}, time) as time")
            .append("</when><otherwise>time</otherwise></choose>");
        StringJoiner tags = new StringJoiner(", ");
        for (Field field : this.fieldList) {
            String name = field.getName();
            if (Objects.equals(name, "time")) {
                continue;
            }
            String column = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
            sql.append(", ");
            if (name.endsWith("Id") || field.isAnnotationPresent(ReportTag.class)) {
                // Kafka 名称和分区号也是维度；聚合时保留全部标签，避免混合不同 Topic、Group 和分区。
                sql.append(column);
                tags.add(column);
            } else {
                sql.append("<choose><when test='_parameter.selectFun != null'>");
                if (name.startsWith("value")) {
                    sql.append("${_parameter.selectFun}(").append(column).append(")");
                } else {
                    sql.append("last_by(").append(column).append(", time)");
                }
                sql.append(" as ").append(column).append("</when><otherwise>")
                    .append(column).append("</otherwise></choose>");
            }
        }
        // 非聚合查询直接返回属性；聚合查询使用双参数 last_by，所有列只在一处添加分隔符。
        sql.append(" from ").append(this.reportMeta.getTableName())
            .append(" where time &gt; #{startTime} and time &lt; #{endTime}");
        for (Field field : this.fieldList) {
            String name = field.getName();
            if (name.endsWith("Id")) {
                String column = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
                sql.append(" <if test='_parameter.").append(name).append(" != null'> and ")
                    .append(column).append("=#{").append(name).append("}</if>");
            }
        }
        sql.append(" <if test='_parameter.selectFun != null'> group by ")
            .append("<choose><when test='_parameter.interval != null'>date_bin(${_parameter.interval}, time)")
            .append("</when><otherwise>time</otherwise></choose>");
        if (tags.length() > 0) {
            sql.append(", ").append(tags);
        }
        sql.append("</if>");
        return sql.toString();
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
            if (type == long.class
                || type == Long.class
                || type == int.class
                || type == Integer.class
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
            } else if (filedName.endsWith("Id") || field.isAnnotationPresent(ReportTag.class)) {
                stringBuilder.append(" tag");
            } else if (filedName.startsWith("value") || field.getDeclaringClass() == this.reportMeta.getClazz()) {
                stringBuilder.append(" field");

            } else {
                stringBuilder.append(" attribute");
            }
            if (num.decrementAndGet() != 0) {
                stringBuilder.append(",");
                stringBuilder.append(lineAndTabString);
            }
        });
        stringBuilder.append("\r\n)comment ' ").append(reportMeta.getComment()).append("'");
        return stringBuilder.toString();
    }


}
