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

package org.apache.eventmesh.dashboard.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RowMapperUtil {


    public static <T> List<T> executeQuery(Connection connection, String sql, List<Object> argsList, Class<T> clazz) throws SQLException {
        connection.setAutoCommit(true);
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            for (int i = 0; i < argsList.size(); i++) {
                p.setObject(i + 1, argsList.get(i));
            }
            try (ResultSet rs = p.executeQuery()) {
                return mapList(rs, clazz);
            }
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public static <T> List<T> mapList(ResultSet rs, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        while (rs.next()) {
            T row = mapRow(rs, clazz);
            list.add(row);
        }
        return list;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> T mapRow(ResultSet rs, Class<T> clazz) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnLabel = metaData.getColumnLabel(i);
            String fieldName = toCamelCase(columnLabel);
            Object dbValue = rs.getObject(i);

            Field field = FieldUtils.getField(clazz, fieldName, true);
            if (field == null) {
                continue;
            }
            field.setAccessible(true);
            Class<?> fieldType = field.getType();

            if (dbValue == null) {
                field.set(instance, null);
                continue;
            }

            // 枚举处理
            if (Enum.class.isAssignableFrom(fieldType)) {
                Class<Enum> enumClass = (Class<Enum>) fieldType;
                Enum enumVal;
                if (dbValue instanceof String) {
                    enumVal = Enum.valueOf(enumClass, (String) dbValue);
                } else if (dbValue instanceof Number) {
                    enumVal = enumClass.getEnumConstants()[((Number) dbValue).intValue()];
                } else {
                    throw new IllegalArgumentException("无法转换枚举，字段：" + fieldName + "，值：" + dbValue);
                }
                field.set(instance, enumVal);
                continue;
            }

            // ====== 类型转换：解决 BigInteger → Long 等报错 ======
            Object converted = convertValue(dbValue, fieldType);
            field.set(instance, converted);
        }
        return instance;
    }

    private static String toCamelCase(String columnName) {
        if (StringUtils.isBlank(columnName)) {
            return columnName;
        }
        String[] parts = StringUtils.split(columnName, '_');
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(StringUtils.capitalize(parts[i]));
        }
        return sb.toString();
    }

    /**
     * 核心：把数据库读出的值转换为实体字段类型
     */
    private static Object convertValue(Object dbValue, Class<?> fieldType) {
        // 类型本来就匹配，直接用
        if (fieldType.isAssignableFrom(dbValue.getClass())) {
            return dbValue;
        }

        // ========== 数字类型互转 ==========
        if (Number.class.isAssignableFrom(fieldType) && dbValue instanceof Number) {
            return convertNumber((Number) dbValue, fieldType);
        }

        // ========== 字符串 → 数字 ==========
        if (Number.class.isAssignableFrom(fieldType) && dbValue instanceof String) {
            return convertNumber(new BigDecimal((String) dbValue), fieldType);
        }

        // ========== 日期类型 ==========
        if (fieldType == LocalDateTime.class && dbValue instanceof Timestamp) {
            return ((Timestamp) dbValue).toLocalDateTime();
        }
        if (fieldType == java.util.Date.class && dbValue instanceof Timestamp) {
            return new java.util.Date(((Timestamp) dbValue).getTime());
        }

        // 其他：原样返回，交给反射set（可能继续报错）
        return dbValue;
    }

    /**
     * 数字 → 指定数字类型的转换
     */
    private static Object convertNumber(Number number, Class<?> fieldType) {
        if (fieldType == Long.class || fieldType == long.class) {
            return number.longValue();
        }
        if (fieldType == BigInteger.class) {
            return BigInteger.valueOf(number.longValue());
        }
        if (fieldType == Integer.class || fieldType == int.class) {
            return number.intValue();
        }
        if (fieldType == Short.class || fieldType == short.class) {
            return number.shortValue();
        }
        if (fieldType == Byte.class || fieldType == byte.class) {
            return number.byteValue();
        }
        if (fieldType == BigDecimal.class) {
            return new BigDecimal(number.toString());
        }
        if (fieldType == Double.class || fieldType == double.class) {
            return number.doubleValue();
        }
        if (fieldType == Float.class || fieldType == float.class) {
            return number.floatValue();
        }
        return number;
    }
}
