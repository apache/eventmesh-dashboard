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

package org.apache.eventmesh.dashboard.console.function.report;

import org.apache.eventmesh.dashboard.console.function.report.ReportConfig.ReportEngineConfig;
import org.apache.eventmesh.dashboard.console.function.report.annotation.AbstractReportMetaHandler;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

@Setter
public abstract class AbstractReportEngine implements ReportEngine {


    protected ReportEngineConfig reportEngineConfig;
    protected XMLLanguageDriver xmlLanguageDriver = new XMLLanguageDriver();
    private VelocityEngine velocityEngine;
    private Configuration configuration = new Configuration();
    private Map<String, Map<String, SqlSource>> stringSqlSourceMap = new HashMap<>();
    private Class<?> reportClass;

    private StringResourceRepository resourceLoader = new StringResourceRepositoryImpl();

    @Getter
    private Map<String, AbstractReportMetaHandler> reportMetaHandlerMap = new HashMap<>();

    private Map<String, Map<String, String>> reportSQL = new ConcurrentHashMap<>();

    public void init() {
        this.createVelocity();

        this.doInit();
    }

    protected abstract void doInit();

    @Deprecated
    private void createVelocity() {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "report");
        velocityEngine.setProperty("resource.loader.report.class", StringResourceLoader.class.getName());
        velocityEngine.setProperty("resource.loader.report.repository.name", "report");
        velocityEngine.setProperty("resource.loader.report.repository.static", false);
        velocityEngine.setApplicationAttribute("report", resourceLoader);
        velocityEngine.init();
        this.velocityEngine = velocityEngine;
    }


    protected String querySentence(SingleGeneralReportDO singleGeneralReportDO) {
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("data", singleGeneralReportDO);
        StringWriter stringWriter = new StringWriter();
        this.velocityEngine.getTemplate("").merge(velocityContext, stringWriter);
        return stringWriter.toString();
    }

    @Override
    public void createReportHandler(ReportMetaData reportMetaData, List<Field> fieldList) {
        AbstractReportMetaHandler abstractReportMetaHandler = this.doCreateReportHandler(reportMetaData, fieldList);
        abstractReportMetaHandler.setReportMeta(reportMetaData);
        abstractReportMetaHandler.setFieldList(fieldList);
        this.reportMetaHandlerMap.put(reportMetaData.getReportName(), abstractReportMetaHandler);
    }

    protected abstract AbstractReportMetaHandler doCreateReportHandler(ReportMetaData reportMetaData, List<Field> fieldList);

    protected String buildSql(String reportName, String type, Object value) {
        SqlSource sqlSource = this.buildSqlSource(reportName, type);
        BoundSql boundSql = sqlSource.getBoundSql(value);
        return boundSql.getSql();
    }

    protected SqlSource buildSqlSource(String reportName, String type) {
        Map<String, SqlSource> sourceMap = this.stringSqlSourceMap.computeIfAbsent(reportName, k -> new ConcurrentHashMap<>());
        return sourceMap.computeIfAbsent(type, (k) -> {
            String sql = this.createSql(reportName, type);
            String stringBuilder = "<script>\r\n  " + sql + "\r\n</script>";
            return this.xmlLanguageDriver.createSqlSource(configuration, stringBuilder, null);
        });
    }

    protected String createSql(String reportName, String type) {
        AbstractReportMetaHandler handler = this.reportMetaHandlerMap.get(reportName);

        String sql;
        if (Objects.equals("insert", type)) {
            sql = handler.insert();
        } else if (Objects.equals(ReportViewType.CREATE_TABLE.getName(), type)) {
            sql = handler.createTable();
        } else {
            sql = handler.query(type);
        }
        return sql;
    }
}
