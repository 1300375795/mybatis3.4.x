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
package org.apache.ibatis.executor.resultset;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.type.UnknownTypeHandler;

/**
 * 结果集包装器
 *
 * @author Iwao AVE!
 */
public class ResultSetWrapper {

    /**
     * 源结果集
     */
    private final ResultSet resultSet;

    /**
     * 类型处理器注册器
     */
    private final TypeHandlerRegistry typeHandlerRegistry;

    /**
     * 数据库字段名称集合
     */
    private final List<String> columnNames = new ArrayList<String>();

    /**
     * 类集合名称
     */
    private final List<String> classNames = new ArrayList<String>();

    /**
     * jdbc类型集合
     */
    private final List<JdbcType> jdbcTypes = new ArrayList<JdbcType>();

    /**
     * 类型处理器map
     */
    private final Map<String, Map<Class<?>, TypeHandler<?>>> typeHandlerMap = new HashMap<String, Map<Class<?>, TypeHandler<?>>>();

    /**
     * 已映射的字段名称map
     */
    private final Map<String, List<String>> mappedColumnNamesMap = new HashMap<String, List<String>>();

    /**
     * 未映射的字段名称map
     */
    private final Map<String, List<String>> unMappedColumnNamesMap = new HashMap<String, List<String>>();

    /**
     * 构造函数
     *
     * @param rs
     * @param configuration
     * @throws SQLException
     */
    public ResultSetWrapper(ResultSet rs, Configuration configuration) throws SQLException {
        super();
        this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        this.resultSet = rs;
        final ResultSetMetaData metaData = rs.getMetaData();
        //字段数量
        final int columnCount = metaData.getColumnCount();
        //遍历字段
        for (int i = 1; i <= columnCount; i++) {
            //如果使用字段标签的话 就拿到元数据的字段标签否则拿到字段的名称 然后存到字段名称集合中
            columnNames.add(configuration.isUseColumnLabel() ? metaData.getColumnLabel(i) : metaData.getColumnName(i));
            //往jdbc类型中添加jdbc类型
            jdbcTypes.add(JdbcType.forCode(metaData.getColumnType(i)));
            //往类名称中添加
            classNames.add(metaData.getColumnClassName(i));
        }
    }

    /**
     * 返回结果集
     *
     * @return
     */
    public ResultSet getResultSet() {
        return resultSet;
    }

    /**
     * 获取字段名称集合
     *
     * @return
     */
    public List<String> getColumnNames() {
        return this.columnNames;
    }

    /**
     * 获取类名称集合
     *
     * @return
     */
    public List<String> getClassNames() {
        return Collections.unmodifiableList(classNames);
    }

    /**
     * 根据字段名称拿到这个字段的jdbc类型
     *
     * @param columnName
     * @return
     */
    public JdbcType getJdbcType(String columnName) {
        for (int i = 0; i < columnNames.size(); i++) {
            if (columnNames.get(i).equalsIgnoreCase(columnName)) {
                return jdbcTypes.get(i);
            }
        }
        return null;
    }

    /**
     * Gets the type handler to use when reading the result set.
     * Tries to get from the TypeHandlerRegistry by searching for the property type.
     * If not found it gets the column JDBC type and tries to get a handler for it.
     * 根据参数class类型以及字段名称获取对应的类型处理器
     *
     * @param propertyType
     * @param columnName
     * @return
     */
    public TypeHandler<?> getTypeHandler(Class<?> propertyType, String columnName) {
        TypeHandler<?> handler = null;
        Map<Class<?>, TypeHandler<?>> columnHandlers = typeHandlerMap.get(columnName);
        //如果这个字段对应的类型处理器不存在 那么直接创建一个新的 然后存到map中
        if (columnHandlers == null) {
            columnHandlers = new HashMap<Class<?>, TypeHandler<?>>();
            typeHandlerMap.put(columnName, columnHandlers);
        } else {
            //否则直接这个对应的字段类型的处理器
            handler = columnHandlers.get(propertyType);
        }
        //如果还是没有
        if (handler == null) {
            JdbcType jdbcType = getJdbcType(columnName);
            handler = typeHandlerRegistry.getTypeHandler(propertyType, jdbcType);
            // Replicate logic of UnknownTypeHandler#resolveTypeHandler
            // See issue #59 comment 10
            //如果没有找到类型处理器或者是未知的处理器
            if (handler == null || handler instanceof UnknownTypeHandler) {
                //找到这个字段的java类型然后根据java类型以及jdbc类型再次获取类型处理器
                final int index = columnNames.indexOf(columnName);
                final Class<?> javaType = resolveClass(classNames.get(index));
                if (javaType != null && jdbcType != null) {
                    handler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType);
                } else if (javaType != null) {
                    handler = typeHandlerRegistry.getTypeHandler(javaType);
                } else if (jdbcType != null) {
                    handler = typeHandlerRegistry.getTypeHandler(jdbcType);
                }
            }
            //如果还是没有找到 那么直接返回Object类型处理器
            if (handler == null || handler instanceof UnknownTypeHandler) {
                handler = new ObjectTypeHandler();
            }
            columnHandlers.put(propertyType, handler);
        }
        return handler;
    }

    /**
     * 根据类名称加载class
     *
     * @param className
     * @return
     */
    private Class<?> resolveClass(String className) {
        try {
            // #699 className could be null
            if (className != null) {
                return Resources.classForName(className);
            }
        } catch (ClassNotFoundException e) {
            // ignore
        }
        return null;
    }

    /**
     * 加载已映射的跟未映射的字段名称集合
     *
     * @param resultMap
     * @param columnPrefix
     * @throws SQLException
     */
    private void loadMappedAndUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
        List<String> mappedColumnNames = new ArrayList<String>();
        List<String> unmappedColumnNames = new ArrayList<String>();
        final String upperColumnPrefix = columnPrefix == null ? null : columnPrefix.toUpperCase(Locale.ENGLISH);
        final Set<String> mappedColumns = prependPrefixes(resultMap.getMappedColumns(), upperColumnPrefix);
        //判断字段名称集合是否存在 如果存在就加上已映射集合 没有的话 就加到未知字段集合
        for (String columnName : columnNames) {
            final String upperColumnName = columnName.toUpperCase(Locale.ENGLISH);
            if (mappedColumns.contains(upperColumnName)) {
                mappedColumnNames.add(upperColumnName);
            } else {
                unmappedColumnNames.add(columnName);
            }
        }
        mappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), mappedColumnNames);
        unMappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), unmappedColumnNames);
    }

    /**
     * 获取已映射的字段名称集合
     *
     * @param resultMap
     * @param columnPrefix
     * @return
     * @throws SQLException
     */
    public List<String> getMappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
        List<String> mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
        if (mappedColumnNames == null) {
            loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
            mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
        }
        return mappedColumnNames;
    }

    /**
     * 获取未映射的字段集合
     *
     * @param resultMap
     * @param columnPrefix
     * @return
     * @throws SQLException
     */
    public List<String> getUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
        List<String> unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
        if (unMappedColumnNames == null) {
            loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
            unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
        }
        return unMappedColumnNames;
    }

    /**
     * 获取结果map的id
     *
     * @param resultMap
     * @param columnPrefix
     * @return
     */
    private String getMapKey(ResultMap resultMap, String columnPrefix) {
        return resultMap.getId() + ":" + columnPrefix;
    }

    /**
     * 如果字段名称集合为null或者为空或者前缀为null或者长度为0
     * 那么直接返回名称集合
     * 否则给所有的名称加上前缀
     *
     * @param columnNames
     * @param prefix
     * @return
     */
    private Set<String> prependPrefixes(Set<String> columnNames, String prefix) {
        if (columnNames == null || columnNames.isEmpty() || prefix == null || prefix.length() == 0) {
            return columnNames;
        }
        final Set<String> prefixed = new HashSet<String>();
        for (String columnName : columnNames) {
            prefixed.add(prefix + columnName);
        }
        return prefixed;
    }

}
