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
package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

/**
 * sql源构建器
 *
 * @author Clinton Begin
 */
public class SqlSourceBuilder extends BaseBuilder {

    /**
     * 参数属性
     */
    private static final String parameterProperties = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

    /**
     * 构造函数
     *
     * @param configuration
     */
    public SqlSourceBuilder(Configuration configuration) {
        super(configuration);
    }

    /**
     * 解析原始sql
     *
     * @param originalSql
     * @param parameterType
     * @param additionalParameters
     * @return
     */
    public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
        ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType,
                additionalParameters);
        GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
        String sql = parser.parse(originalSql);
        return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
    }

    /**
     * 参数映射令牌处理器
     */
    private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {

        /**
         * 参数映射集合
         */
        private List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();

        /**
         * 参数类型
         */
        private Class<?> parameterType;

        /**
         * 元对象
         */
        private MetaObject metaParameters;

        /**
         * 构造函数
         *
         * @param configuration
         * @param parameterType
         * @param additionalParameters
         */
        public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType,
                Map<String, Object> additionalParameters) {
            super(configuration);
            this.parameterType = parameterType;
            this.metaParameters = configuration.newMetaObject(additionalParameters);
        }

        /**
         * 获取参数映射集合
         *
         * @return
         */
        public List<ParameterMapping> getParameterMappings() {
            return parameterMappings;
        }

        /**
         * 处理令牌
         * 这个就是将#{}的参数处理成? 并构建ParameterMapping存放到parameterMappings中
         *
         * @param content
         * @return
         */
        @Override
        public String handleToken(String content) {
            //参数映射集合中添加
            parameterMappings.add(buildParameterMapping(content));
            return "?";
        }

        /**
         * 构建参数映射
         *
         * @param content
         * @return
         */
        private ParameterMapping buildParameterMapping(String content) {
            //解析参数映射
            Map<String, String> propertiesMap = parseParameterMapping(content);
            //拿到属性配置
            String property = propertiesMap.get("property");
            Class<?> propertyType;
            //如果元对象中有这个属性
            if (metaParameters.hasGetter(property)) { // issue #448 get type from additional params
                propertyType = metaParameters.getGetterType(property);
            }
            //如果类型处理器中有这个参数类型
            else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
                propertyType = parameterType;
            }
            //如果jdbcType是游标
            else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
                propertyType = java.sql.ResultSet.class;
            }
            //如果参数是null 或者参数类型是map
            else if (property == null || Map.class.isAssignableFrom(parameterType)) {
                propertyType = Object.class;
            }
            //其他情况
            else {
                //转换元class
                MetaClass metaClass = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
                //如果元class中有这个属性 那么获取这个属性 否则置为object
                if (metaClass.hasGetter(property)) {
                    propertyType = metaClass.getGetterType(property);
                } else {
                    propertyType = Object.class;
                }
            }
            //构建参数映射
            ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
            Class<?> javaType = propertyType;
            String typeHandlerAlias = null;
            //处理各种类型
            for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                if ("javaType".equals(name)) {
                    javaType = resolveClass(value);
                    builder.javaType(javaType);
                } else if ("jdbcType".equals(name)) {
                    builder.jdbcType(resolveJdbcType(value));
                } else if ("mode".equals(name)) {
                    builder.mode(resolveParameterMode(value));
                } else if ("numericScale".equals(name)) {
                    builder.numericScale(Integer.valueOf(value));
                } else if ("resultMap".equals(name)) {
                    builder.resultMapId(value);
                } else if ("typeHandler".equals(name)) {
                    typeHandlerAlias = value;
                } else if ("jdbcTypeName".equals(name)) {
                    builder.jdbcTypeName(value);
                } else if ("property".equals(name)) {
                    // Do Nothing
                } else if ("expression".equals(name)) {
                    throw new BuilderException("Expression based parameters are not supported yet");
                } else {
                    throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content
                            + "}.  Valid properties are " + parameterProperties);
                }
            }
            if (typeHandlerAlias != null) {
                builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
            }
            return builder.build();
        }

        /**
         * 解析参数映射
         *
         * @param content
         * @return
         */
        private Map<String, String> parseParameterMapping(String content) {
            try {
                return new ParameterExpression(content);
            } catch (BuilderException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BuilderException("Parsing error was found in mapping #{" + content
                        + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
            }
        }
    }

}
