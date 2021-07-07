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
package org.apache.ibatis.executor.keygen;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * jdbc类型的主键生成器
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class Jdbc3KeyGenerator implements KeyGenerator {

    /**
     * 单例
     * A shared instance.
     *
     * @since 3.4.3
     */
    public static final Jdbc3KeyGenerator INSTANCE = new Jdbc3KeyGenerator();

    /**
     * 在update之前执行
     *
     * @param executor
     * @param ms
     * @param stmt
     * @param parameter
     */
    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        // do nothing
    }

    /**
     * 在update之后执行
     *
     * @param executor
     * @param ms
     * @param stmt
     * @param parameter
     */
    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        processBatch(ms, stmt, getParameters(parameter));
    }

    /**
     * 批量执行
     *
     * @param ms
     * @param stmt
     * @param parameters
     */
    public void processBatch(MappedStatement ms, Statement stmt, Collection<Object> parameters) {
        ResultSet rs = null;
        try {
            rs = stmt.getGeneratedKeys();
            final Configuration configuration = ms.getConfiguration();
            final TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            final String[] keyProperties = ms.getKeyProperties();
            final ResultSetMetaData rsmd = rs.getMetaData();
            TypeHandler<?>[] typeHandlers = null;
            if (keyProperties != null && rsmd.getColumnCount() >= keyProperties.length) {
                for (Object parameter : parameters) {
                    // there should be one row for each statement (also one for each parameter)
                    if (!rs.next()) {
                        break;
                    }
                    final MetaObject metaParam = configuration.newMetaObject(parameter);
                    if (typeHandlers == null) {
                        typeHandlers = getTypeHandlers(typeHandlerRegistry, metaParam, keyProperties, rsmd);
                    }
                    populateKeys(rs, metaParam, keyProperties, typeHandlers);
                }
            }
        } catch (Exception e) {
            throw new ExecutorException(
                    "Error getting generated key or setting result to parameter object. Cause: " + e, e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 获取参数
     *
     * @param parameter
     * @return
     */
    private Collection<Object> getParameters(Object parameter) {
        Collection<Object> parameters = null;
        if (parameter instanceof Collection) {
            parameters = (Collection) parameter;
        } else if (parameter instanceof Map) {
            Map parameterMap = (Map) parameter;
            if (parameterMap.containsKey("collection")) {
                parameters = (Collection) parameterMap.get("collection");
            } else if (parameterMap.containsKey("list")) {
                parameters = (List) parameterMap.get("list");
            } else if (parameterMap.containsKey("array")) {
                parameters = Arrays.asList((Object[]) parameterMap.get("array"));
            }
        }
        if (parameters == null) {
            parameters = new ArrayList<Object>();
            parameters.add(parameter);
        }
        return parameters;
    }

    /**
     * 获取类型处理器
     *
     * @param typeHandlerRegistry
     * @param metaParam
     * @param keyProperties
     * @param rsmd
     * @return
     * @throws SQLException
     */
    private TypeHandler<?>[] getTypeHandlers(TypeHandlerRegistry typeHandlerRegistry, MetaObject metaParam,
            String[] keyProperties, ResultSetMetaData rsmd) throws SQLException {
        TypeHandler<?>[] typeHandlers = new TypeHandler<?>[keyProperties.length];
        for (int i = 0; i < keyProperties.length; i++) {
            if (metaParam.hasSetter(keyProperties[i])) {
                TypeHandler<?> th;
                try {
                    Class<?> keyPropertyType = metaParam.getSetterType(keyProperties[i]);
                    th = typeHandlerRegistry
                            .getTypeHandler(keyPropertyType, JdbcType.forCode(rsmd.getColumnType(i + 1)));
                } catch (BindingException e) {
                    th = null;
                }
                typeHandlers[i] = th;
            }
        }
        return typeHandlers;
    }

    /**
     * 迁移keys
     *
     * @param rs
     * @param metaParam
     * @param keyProperties
     * @param typeHandlers
     * @throws SQLException
     */
    private void populateKeys(ResultSet rs, MetaObject metaParam, String[] keyProperties, TypeHandler<?>[] typeHandlers)
            throws SQLException {
        for (int i = 0; i < keyProperties.length; i++) {
            String property = keyProperties[i];
            TypeHandler<?> th = typeHandlers[i];
            if (th != null) {
                Object value = th.getResult(rs, i + 1);
                metaParam.setValue(property, value);
            }
        }
    }

}
