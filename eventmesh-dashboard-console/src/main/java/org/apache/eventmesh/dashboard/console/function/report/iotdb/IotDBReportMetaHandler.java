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
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.CaseFormat;

public class IotDBReportMetaHandler extends AbstractReportMetaHandler {


    @Override
    public String insert() {
        StringBuilder sql = new StringBuilder("insert into ").append(this.reportMeta.getTableName()).append(" (");
        sql.append(this.fieldList.stream().map(this::column).collect(java.util.stream.Collectors.joining(",")));
        // 名称可以包含引号，数值也通过绑定保留零值、错误标记与 INT64 精度。
        sql.append(") values <foreach collection='_parameter' item='item' separator=','>(");
        sql.append(this.fieldList.stream().map(field -> "#{item." + field.getName() + "}")
            .collect(java.util.stream.Collectors.joining(",")));
        return sql.append(")</foreach>").toString();
    }

    @Override
    public String query(String type) {
        StringBuilder sql = new StringBuilder("select <choose><when test='interval != null'>")
            .append("date_bin(${interval}, time)</when><otherwise>time</otherwise></choose> as time");
        // 每个逗号由字段自身提供，所有 TAG 都参与分组，防止不同 Topic、消费组或分区被合并。
        for (Field field : this.fieldList) {
            if ("time".equals(field.getName())) {
                continue;
            }
            String name = column(field);
            sql.append(",");
            if (field.getName().startsWith("value")) {
                sql.append("<choose><when test='selectFun != null'>${selectFun}(").append(name)
                    .append(") as ").append(name).append("</when><otherwise>").append(name).append("</otherwise></choose>");
            } else if (isTag(field)) {
                sql.append(name);
            } else {
                sql.append("<choose><when test='selectFun != null'>last_by(").append(name)
                    .append(", time) as ").append(name).append("</when><otherwise>").append(name).append("</otherwise></choose>");
            }
        }
        sql.append(" from ").append(this.reportMeta.getTableName())
            .append(" where time &gt;= #{startTime} and time &lt; #{endTime}");
        for (Field field : this.fieldList) {
            String name = field.getName();
            if (name.endsWith("Id") || field.isAnnotationPresent(ReportTag.class) || "runtimeType".equals(name)) {
                sql.append("<if test='").append(name).append(" != null'> and ").append(column(field))
                    .append("=#{").append(name).append("}</if>");
            }
        }
        sql.append("<if test='selectFun != null'> group by ")
            .append("<choose><when test='interval != null'>date_bin(${interval}, time)</when><otherwise>time</otherwise></choose>");
        for (Field field : this.fieldList) {
            if (isTag(field)) {
                sql.append(",").append(column(field));
            }
        }
        return sql.append("</if>").toString();
    }

    private String column(Field field) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
    }

    private boolean isTag(Field field) {
        return field.getName().endsWith("Id") || field.isAnnotationPresent(ReportTag.class);
    }

    public boolean supportsFilter(String name) {
        return this.fieldList.stream().anyMatch(field -> field.getName().equals(name));
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
