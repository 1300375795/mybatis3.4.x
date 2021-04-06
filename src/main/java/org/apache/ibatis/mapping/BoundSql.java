/**
 * Copyright 2009-2017 the original author or authors.
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
package org.apache.ibatis.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.session.Configuration;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * An actual SQL String got from an {@link SqlSource} after having processed any dynamic content.
 * The SQL may have SQL placeholders "?" and an list (ordered) of an parameter mappings
 * with the additional information for each parameter (at least the property name of the input object to read
 * the value from).
 * </br>
 * Can also have additional parameters that are created by the dynamic language (for loops, bind...).
 * 绑定的sql
 *
 * @author Clinton Begin
 */
public class BoundSql {

    /**
     * 实际要执行的sql 是替换了参数之后的 比兔#{id}会被替换成?
     */
    private final String sql;

    /**
     * 参数列表
     */
    private final List<ParameterMapping> parameterMappings;

    /**
     * 参数的值
     */
    private final Object parameterObject;

    /**
     * 额外的参数集合
     */
    private final Map<String, Object> additionalParameters;

    /**
     * 元参数对象
     */
    private final MetaObject metaParameters;

    /**
     * 构造函数
     *
     * @param configuration
     * @param sql
     * @param parameterMappings
     * @param parameterObject
     */
    public BoundSql(Configuration configuration, String sql, List<ParameterMapping> parameterMappings,
            Object parameterObject) {
        this.sql = sql;
        this.parameterMappings = parameterMappings;
        this.parameterObject = parameterObject;
        this.additionalParameters = new HashMap<String, Object>();
        this.metaParameters = configuration.newMetaObject(additionalParameters);
    }

    public String getSql() {
        return sql;
    }

    public List<ParameterMapping> getParameterMappings() {
        return parameterMappings;
    }

    public Object getParameterObject() {
        return parameterObject;
    }

    /**
     * 是否额外参数map中包含这个参数
     *
     * @param name
     * @return
     */
    public boolean hasAdditionalParameter(String name) {
        String paramName = new PropertyTokenizer(name).getName();
        return additionalParameters.containsKey(paramName);
    }

    /**
     * 设置额外的参数map
     *
     * @param name
     * @param value
     */
    public void setAdditionalParameter(String name, Object value) {
        metaParameters.setValue(name, value);
    }

    /**
     * 从额外的参数map中获取这个参数的值
     *
     * @param name
     * @return
     */
    public Object getAdditionalParameter(String name) {
        return metaParameters.getValue(name);
    }
}
