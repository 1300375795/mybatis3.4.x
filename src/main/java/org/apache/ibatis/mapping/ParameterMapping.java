/**
 * Copyright 2009-2016 the original author or authors.
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

import java.sql.ResultSet;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * 参数映射
 * 对应xml中parameterMap各种子属性
 *
 * @author Clinton Begin
 */
public class ParameterMapping {

    /**
     * 全局配置
     */
    private Configuration configuration;

    /**
     * 属性名称
     */
    private String property;

    /**
     * 属性模式
     */
    private ParameterMode mode;

    /**
     * java类型
     */
    private Class<?> javaType = Object.class;

    /**
     * jdbc类型
     */
    private JdbcType jdbcType;

    /**
     * 数值范围
     */
    private Integer numericScale;

    /**
     * 类型处理器
     */
    private TypeHandler<?> typeHandler;

    /**
     * resultMap的id
     */
    private String resultMapId;

    /**
     * jdbc类型的名称
     */
    private String jdbcTypeName;

    /**
     * 表达式
     */
    private String expression;

    /**
     * 构造函数
     */
    private ParameterMapping() {
    }

    /**
     * 构造者模式
     */
    public static class Builder {

        private ParameterMapping parameterMapping = new ParameterMapping();

        /**
         * 构造函数
         *
         * @param configuration
         * @param property
         * @param typeHandler
         */
        public Builder(Configuration configuration, String property, TypeHandler<?> typeHandler) {
            parameterMapping.configuration = configuration;
            parameterMapping.property = property;
            parameterMapping.typeHandler = typeHandler;
            //默认输入参数
            parameterMapping.mode = ParameterMode.IN;
        }

        /**
         * 构造函数
         *
         * @param configuration
         * @param property
         * @param javaType
         */
        public Builder(Configuration configuration, String property, Class<?> javaType) {
            parameterMapping.configuration = configuration;
            parameterMapping.property = property;
            parameterMapping.javaType = javaType;
            parameterMapping.mode = ParameterMode.IN;
        }

        /**
         * 设置参数模式
         *
         * @param mode
         * @return
         */
        public Builder mode(ParameterMode mode) {
            parameterMapping.mode = mode;
            return this;
        }

        /**
         * 设置java类型
         *
         * @param javaType
         * @return
         */
        public Builder javaType(Class<?> javaType) {
            parameterMapping.javaType = javaType;
            return this;
        }

        /**
         * 设置jdbc类型
         *
         * @param jdbcType
         * @return
         */
        public Builder jdbcType(JdbcType jdbcType) {
            parameterMapping.jdbcType = jdbcType;
            return this;
        }

        /**
         * 设置数据范围
         *
         * @param numericScale
         * @return
         */
        public Builder numericScale(Integer numericScale) {
            parameterMapping.numericScale = numericScale;
            return this;
        }

        /**
         * 设置resultMap的id
         *
         * @param resultMapId
         * @return
         */
        public Builder resultMapId(String resultMapId) {
            parameterMapping.resultMapId = resultMapId;
            return this;
        }

        /**
         * 设置类型处理器
         *
         * @param typeHandler
         * @return
         */
        public Builder typeHandler(TypeHandler<?> typeHandler) {
            parameterMapping.typeHandler = typeHandler;
            return this;
        }

        /**
         * 设置jdbc类型名称
         *
         * @param jdbcTypeName
         * @return
         */
        public Builder jdbcTypeName(String jdbcTypeName) {
            parameterMapping.jdbcTypeName = jdbcTypeName;
            return this;
        }

        /**
         * 设置表达式
         *
         * @param expression
         * @return
         */
        public Builder expression(String expression) {
            parameterMapping.expression = expression;
            return this;
        }

        /**
         * 构建参数映射对象
         *
         * @return
         */
        public ParameterMapping build() {
            resolveTypeHandler();
            validate();
            return parameterMapping;
        }

        /**
         * 校验一些逻辑
         * 如果返回类型是ResultSet 那么需要会给出resultMap
         * 不是的话 那么需要给出类型处理器
         */
        private void validate() {
            if (ResultSet.class.equals(parameterMapping.javaType)) {
                if (parameterMapping.resultMapId == null) {
                    throw new IllegalStateException(
                            "Missing resultmap in property '" + parameterMapping.property + "'.  "
                                    + "Parameters of type java.sql.ResultSet require a resultmap.");
                }
            } else {
                if (parameterMapping.typeHandler == null) {
                    throw new IllegalStateException(
                            "Type handler was null on parameter mapping for property '" + parameterMapping.property
                                    + "'. It was either not specified and/or could not be found for the javaType ("
                                    + parameterMapping.javaType.getName() + ") : jdbcType (" + parameterMapping.jdbcType
                                    + ") combination.");
                }
            }
        }

        /**
         * 如果类型处理器为空并且java类型不为空 那么通过java类型解析获取类型处理器对象
         */
        private void resolveTypeHandler() {
            if (parameterMapping.typeHandler == null && parameterMapping.javaType != null) {
                Configuration configuration = parameterMapping.configuration;
                TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
                parameterMapping.typeHandler = typeHandlerRegistry
                        .getTypeHandler(parameterMapping.javaType, parameterMapping.jdbcType);
            }
        }

    }

    /**
     * 获取属性名称
     *
     * @return
     */
    public String getProperty() {
        return property;
    }

    /**
     * Used for handling output of callable statements
     * 获取参数类型
     *
     * @return
     */
    public ParameterMode getMode() {
        return mode;
    }

    /**
     * Used for handling output of callable statements
     * 获取java类型
     *
     * @return
     */
    public Class<?> getJavaType() {
        return javaType;
    }

    /**
     * Used in the UnknownTypeHandler in case there is no handler for the property type
     * 获取jdbc类型
     *
     * @return
     */
    public JdbcType getJdbcType() {
        return jdbcType;
    }

    /**
     * Used for handling output of callable statements
     * 获取数据范围
     *
     * @return
     */
    public Integer getNumericScale() {
        return numericScale;
    }

    /**
     * Used when setting parameters to the PreparedStatement
     * 获取类型处理器
     *
     * @return
     */
    public TypeHandler<?> getTypeHandler() {
        return typeHandler;
    }

    /**
     * Used for handling output of callable statements
     * 获取resultMapId
     *
     * @return
     */
    public String getResultMapId() {
        return resultMapId;
    }

    /**
     * Used for handling output of callable statements
     * 获取jdbc名称
     *
     * @return
     */
    public String getJdbcTypeName() {
        return jdbcTypeName;
    }

    /**
     * Not used
     * 获取表达式
     *
     * @return
     */
    public String getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ParameterMapping{");
        //sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
        sb.append("property='").append(property).append('\'');
        sb.append(", mode=").append(mode);
        sb.append(", javaType=").append(javaType);
        sb.append(", jdbcType=").append(jdbcType);
        sb.append(", numericScale=").append(numericScale);
        //sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
        sb.append(", resultMapId='").append(resultMapId).append('\'');
        sb.append(", jdbcTypeName='").append(jdbcTypeName).append('\'');
        sb.append(", expression='").append(expression).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
