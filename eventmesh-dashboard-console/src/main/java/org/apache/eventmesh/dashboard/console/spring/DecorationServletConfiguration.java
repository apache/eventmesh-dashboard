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

package org.apache.eventmesh.dashboard.console.spring;

import org.apache.eventmesh.dashboard.console.controller.ReportController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.github.pagehelper.PageHelper;
import com.lamp.decoration.core.DecorationContext;
import com.lamp.decoration.core.databases.queryClauseInte.AbstractQueryClauseInterceptor;
import com.lamp.decoration.core.exception.AbstractDecorationExceptionHandler;
import com.lamp.decoration.core.exception.CustomExceptionResult;
import com.lamp.decoration.core.exception.DecorationCustomExceptionResult;
import com.lamp.decoration.core.exception.ExceptionConfig;
import com.lamp.decoration.core.exception.ExceptionResult;
import com.lamp.decoration.core.result.ResultAction;
import com.lamp.decoration.core.result.ResultConfig;
import com.lamp.decoration.core.spring.DecorationAutoConfiguration;
import com.lamp.decoration.core.spring.DecorationProperties;
import com.lamp.decoration.core.spring.OperationSpringMVCBehavior;
import com.lamp.decoration.core.spring.plugs.DecorationCorsRegistry;
import com.lamp.decoration.core.spring.plugs.FastJsonMessageConverters;

/** 仅连接项目实际使用的核心扩展，Servlet 接口和视图由 Spring 5 提供。 */
@Configuration
@ConditionalOnProperty(prefix = "decoration", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(DecorationProperties.class)
public class DecorationServletConfiguration {

    private final DecorationAutoConfiguration core = new DecorationAutoConfiguration();

    @Bean
    public OperationSpringMVCBehavior operationSpringMvcBehavior(DecorationProperties properties)
        throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        // 空集合配置表示没有规则，避免核心库在正常请求和异常处理中读取 null 集合。
        ResultConfig result = properties.getResultConfig();
        if (result.getPrintUrl() == null) {
            result.setPrintUrl(Set.of());
        }
        if (result.getPrintClasses() == null) {
            result.setPrintClasses(Set.of());
        }
        if (result.getExcludeUrl() == null) {
            result.setExcludeUrl(Set.of());
        }
        if (result.getExcludeClasses() == null) {
            result.setExcludeClasses(Set.of());
        }
        ExceptionConfig error = properties.getErrorConfig();
        if (error.getExceptionNotPrint() == null) {
            error.setExceptionNotPrint(Set.of());
        }
        if (error.getExceptionPrint() == null) {
            error.setExceptionPrint(Set.of());
        }
        return core.operationSpringMvcBehavior(properties);
    }

    @Bean
    @DependsOn("operationSpringMvcBehavior")
    public HandlerExceptionResolver decorationExceptionHandler(DecorationProperties properties) {
        ResultConfig config = properties.getResultConfig();
        ResultAction<Object> action = Objects.requireNonNull(config.getInjection(), "Decoration result action is required");
        CustomExceptionResult defaults = config.getDefaultExceptionResult() == null ? new DecorationCustomExceptionResult()
            : createExceptionResult(config.getDefaultExceptionResult());
        action.setDefaultExceptionResult(defaults.getDefaultExceptionResult());
        List<ExceptionResult> exceptions = new ArrayList<>();
        addExceptions(exceptions, defaults);
        if (config.getExceptionResult() != null) {
            config.getExceptionResult().forEach(name -> addExceptions(exceptions, createExceptionResult(name)));
        }
        action.setExceptionResultList(exceptions);
        ServletExceptionResolver resolver = new ServletExceptionResolver(action);
        resolver.setExceptionConfig(properties.getErrorConfig());
        resolver.setResultAction(action);
        return resolver;
    }

    @Bean
    @ConditionalOnProperty(prefix = "decoration", name = "corsConfigurationList")
    public DecorationCorsRegistry createDecorationCorsRegistry(DecorationProperties properties) {
        return core.createDecorationCorsRegistry(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "decoration", name = "corsEnable", havingValue = "true")
    public DecorationCorsRegistry createAllDecorationCorsRegistry(DecorationProperties properties) {
        // Bean 已由公开的 corsEnable=true 开关启用，不能再依赖未配置的内部默认 false。
        return new DecorationCorsRegistry(true, List.of());
    }

    @Bean
    public FastJsonMessageConverters createFastJsonMessageConverters() {
        return core.createFastJsonMessageConverters();
    }

    @Bean
    public WebMvcConfigurer jsonMediaTypes() {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                for (HttpMessageConverter<?> converter : converters) {
                    if (converter instanceof com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter json) {
                        // 既有转换器默认写入 */*，Spring 禁止将通配类型用于响应 Content-Type。
                        json.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
                    }
                }
            }
        };
    }

    @Bean
    public WebMvcConfigurer queryClauselnteWebMvcConfigurer(DecorationProperties properties) {
        ServletQueryInterceptor query = new ServletQueryInterceptor(properties);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(query);
            }
        };
    }

    private CustomExceptionResult createExceptionResult(String name) {
        Class<?> type = ClassUtils.resolveClassName(name, getClass().getClassLoader());
        return BeanUtils.instantiateClass(type.asSubclass(CustomExceptionResult.class));
    }

    private void addExceptions(List<ExceptionResult> target, CustomExceptionResult source) {
        if (source.getExceptionResultList() != null) {
            target.addAll(source.getExceptionResultList());
        }
    }

    private static void clearRequestContext() {
        PageHelper.clearPage();
        DecorationContext.get().close();
    }

    private static final class ServletQueryInterceptor extends AbstractQueryClauseInterceptor implements HandlerInterceptor {
        private ServletQueryInterceptor(DecorationProperties properties) {
            setConstantConfig(properties.getConstantConfig());
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            try {
                // 时序报表在 IoTDB 中过滤和聚合，不进入 MyBatis 的隐式分页，但仍统一归还上下文。
                if (handler instanceof HandlerMethod method && ReportController.class.isAssignableFrom(method.getBeanType())) {
                    return true;
                }
                String key = constantConfig.getDiscern();
                String clause = request.getHeader(key);
                if (clause == null) {
                    clause = request.getParameter(key);
                }
                boolean accepted = handler(getQueryLimitData(handler), clause, null);
                if (!accepted) {
                    clearRequestContext();
                }
                return accepted;
            } catch (Exception | Error e) {
                // 当前拦截器尚未成功返回时，Spring 不会为它调用 afterCompletion。
                clearRequestContext();
                throw e;
            }
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            clearRequestContext();
        }
    }

    private static final class ServletExceptionResolver extends AbstractDecorationExceptionHandler implements HandlerExceptionResolver, Ordered {
        private final ResultAction<Object> action;

        private ServletExceptionResolver(ResultAction<Object> action) {
            this.action = action;
        }

        @Override
        public int getOrder() {
            // 在默认的纯状态码处理器之前复用现有错误 JSON 契约，包括 javax 参数校验错误。
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            return handlerException(ex);
        }

        @Override
        protected ModelAndView buildModelAndView(Throwable error) {
            // 使用 Spring 5 提供的视图，不依赖外部模块中编译为 Jakarta Servlet 的视图子类。
            MappingJackson2JsonView view = new MappingJackson2JsonView();
            view.setExtractValueFromSingleKeyModel(true);
            return new ModelAndView(view, Map.of("result", action.throwableResult(error)));
        }
    }
}
