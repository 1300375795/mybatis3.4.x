/**
 * Copyright 2009-2015 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.builder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 基础构建者
 * 能够通过别名获取JdbcType、ResultSetType、ParameterMode、这个别名对应的class对应的实例、TypeHandler
 *
 * @author Clinton Begin
 */
public abstract class BaseBuilder {

    /**
     * 全局配置
     */
    protected final Configuration configuration;

    /**
     * 类型别名注册器
     */
    protected final TypeAliasRegistry typeAliasRegistry;

    /**
     * 类型处理器注册器
     */
    protected final TypeHandlerRegistry typeHandlerRegistry;

    /**
     * 构造函数
     *
     * @param configuration
     */
    public BaseBuilder(Configuration configuration) {
        this.configuration = configuration;
        this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
        this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
    }

    /**
     * 获取全局配置
     *
     * @return
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * 获取正则表达式模式器
     *
     * @param regex
     * @param defaultValue
     * @return
     */
    protected Pattern parseExpression(String regex, String defaultValue) {
        return Pattern.compile(regex == null ? defaultValue : regex);
    }

    /**
     * 处理boolean
     *
     * @param value
     * @param defaultValue
     * @return
     */
    protected Boolean booleanValueOf(String value, Boolean defaultValue) {
        return value == null ? defaultValue : Boolean.valueOf(value);
    }

    /**
     * 处理integer
     *
     * @param value
     * @param defaultValue
     * @return
     */
    protected Integer integerValueOf(String value, Integer defaultValue) {
        return value == null ? defaultValue : Integer.valueOf(value);
    }

    /**
     * 处理,分割的string
     *
     * @param value
     * @param defaultValue
     * @return
     */
    protected Set<String> stringSetValueOf(String value, String defaultValue) {
        value = (value == null ? defaultValue : value);
        return new HashSet<String>(Arrays.asList(value.split(",")));
    }

    /**
     * 根据别名获取jdbcType
     *
     * @param alias
     * @return
     */
    protected JdbcType resolveJdbcType(String alias) {
        if (alias == null) {
            return null;
        }
        try {
            return JdbcType.valueOf(alias);
        } catch (IllegalArgumentException e) {
            throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
        }
    }

    /**
     * 根据别名获取ResultSetType
     *
     * @param alias
     * @return
     */
    protected ResultSetType resolveResultSetType(String alias) {
        if (alias == null) {
            return null;
        }
        try {
            return ResultSetType.valueOf(alias);
        } catch (IllegalArgumentException e) {
            throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
        }
    }

    /**
     * 根据别名获取参数模式
     *
     * @param alias
     * @return
     */
    protected ParameterMode resolveParameterMode(String alias) {
        if (alias == null) {
            return null;
        }
        try {
            return ParameterMode.valueOf(alias);
        } catch (IllegalArgumentException e) {
            throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
        }
    }

    /**
     * 根据别名创建对应的实例
     *
     * @param alias
     * @return
     */
    protected Object createInstance(String alias) {
        Class<?> clazz = resolveClass(alias);
        if (clazz == null) {
            return null;
        }
        try {
            return resolveClass(alias).newInstance();
        } catch (Exception e) {
            throw new BuilderException("Error creating instance. Cause: " + e, e);
        }
    }

    /**
     * 获取别名对应的class类
     *
     * @param alias
     * @return
     */
    protected Class<?> resolveClass(String alias) {
        if (alias == null) {
            return null;
        }
        try {
            return resolveAlias(alias);
        } catch (Exception e) {
            throw new BuilderException("Error resolving class. Cause: " + e, e);
        }
    }

    /**
     * 解析类型处理器
     *
     * @param javaType
     * @param typeHandlerAlias
     * @return
     */
    protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
        if (typeHandlerAlias == null) {
            return null;
        }
        //解析别名
        Class<?> type = resolveClass(typeHandlerAlias);
        //如果根据别名获取到相应的class对象 并且这个class不是TypeHandler的子类 抛出异常
        if (type != null && !TypeHandler.class.isAssignableFrom(type)) {
            throw new BuilderException("Type " + type.getName()
                    + " is not a valid TypeHandler because it does not implement TypeHandler interface");
        }
        @SuppressWarnings("unchecked") // already verified it is a TypeHandler
                Class<? extends TypeHandler<?>> typeHandlerType = (Class<? extends TypeHandler<?>>) type;
        return resolveTypeHandler(javaType, typeHandlerType);
    }

    /**
     * 解析类型处理器
     *
     * @param javaType
     * @param typeHandlerType
     * @return
     */
    protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, Class<? extends TypeHandler<?>> typeHandlerType) {
        if (typeHandlerType == null) {
            return null;
        }
        //从类型处理器注册器中获取这个类型处理器class对应的实例
        // javaType ignored for injected handlers see issue #746 for full detail
        TypeHandler<?> handler = typeHandlerRegistry.getMappingTypeHandler(typeHandlerType);
        //如果不存在这个类型处理器class对应的实例 那么创建一个
        if (handler == null) {
            // not in registry, create a new one
            handler = typeHandlerRegistry.getInstance(javaType, typeHandlerType);
        }
        return handler;
    }

    /**
     * 从类型别名注册器中获取这个别名对应的class
     *
     * @param alias
     * @return
     */
    protected Class<?> resolveAlias(String alias) {
        return typeAliasRegistry.resolveAlias(alias);
    }
}
