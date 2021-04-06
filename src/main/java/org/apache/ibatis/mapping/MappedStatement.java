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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * 已映射声明
 *
 * @author Clinton Begin
 */
public final class MappedStatement {

    /**
     * 来源 一般是xml文件地址或者映射接口路径
     */
    private String resource;

    /**
     * 全局配置
     */
    private Configuration configuration;

    /**
     * id 一般就是映射接口+方法名称
     */
    private String id;

    private Integer fetchSize;
    private Integer timeout;

    /**
     * 声明类型
     */
    private StatementType statementType;

    /**
     * 结果集类型
     */
    private ResultSetType resultSetType;

    /**
     * sql源
     */
    private SqlSource sqlSource;

    /**
     * 缓存
     */
    private Cache cache;

    /**
     * 参数map
     */
    private ParameterMap parameterMap;

    /**
     * 结果map集合
     */
    private List<ResultMap> resultMaps;

    /**
     * 是否必须刷新缓存
     */
    private boolean flushCacheRequired;

    /**
     * 是否使用缓存
     */
    private boolean useCache;

    /**
     * 是否结果排序
     */
    private boolean resultOrdered;

    /**
     * sql命令类型
     */
    private SqlCommandType sqlCommandType;

    /**
     * 主键生成器
     */
    private KeyGenerator keyGenerator;

    private String[] keyProperties;
    private String[] keyColumns;

    /**
     * 是否有嵌套结果map
     */
    private boolean hasNestedResultMaps;

    /**
     * 数据库id
     */
    private String databaseId;

    /**
     * 声明日志
     */
    private Log statementLog;

    /**
     * 语言驱动
     */
    private LanguageDriver lang;

    /**
     * 结果集数组
     */
    private String[] resultSets;

    /**
     * 构造函数
     */
    MappedStatement() {
        // constructor disabled
    }

    /**
     * 构建者模式
     */
    public static class Builder {

        /**
         * 已映射的声明
         */
        private MappedStatement mappedStatement = new MappedStatement();

        /**
         * 构造函数
         *
         * @param configuration
         * @param id
         * @param sqlSource
         * @param sqlCommandType
         */
        public Builder(Configuration configuration, String id, SqlSource sqlSource, SqlCommandType sqlCommandType) {
            mappedStatement.configuration = configuration;
            mappedStatement.id = id;
            mappedStatement.sqlSource = sqlSource;
            mappedStatement.statementType = StatementType.PREPARED;
            mappedStatement.parameterMap = new ParameterMap.Builder(configuration, "defaultParameterMap", null,
                    new ArrayList<ParameterMapping>()).build();
            mappedStatement.resultMaps = new ArrayList<ResultMap>();
            mappedStatement.sqlCommandType = sqlCommandType;
            //主键生产器 如果允许使用主键生成器并且是insert操作 那么设置主键生成器为Jdbc3KeyGenerator否则就是默认的不做任何事情的主键生成器
            mappedStatement.keyGenerator =
                    configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType) ?
                            Jdbc3KeyGenerator.INSTANCE :
                            NoKeyGenerator.INSTANCE;
            String logId = id;
            if (configuration.getLogPrefix() != null) {
                logId = configuration.getLogPrefix() + id;
            }
            mappedStatement.statementLog = LogFactory.getLog(logId);
            mappedStatement.lang = configuration.getDefaultScriptingLanguageInstance();
        }

        /**
         * 设置来源
         *
         * @param resource
         * @return
         */
        public Builder resource(String resource) {
            mappedStatement.resource = resource;
            return this;
        }

        /**
         * 返回id
         *
         * @return
         */
        public String id() {
            return mappedStatement.id;
        }

        /**
         * 设置参数map
         *
         * @param parameterMap
         * @return
         */
        public Builder parameterMap(ParameterMap parameterMap) {
            mappedStatement.parameterMap = parameterMap;
            return this;
        }

        /**
         * 设置结果map集合
         *
         * @param resultMaps
         * @return
         */
        public Builder resultMaps(List<ResultMap> resultMaps) {
            mappedStatement.resultMaps = resultMaps;
            for (ResultMap resultMap : resultMaps) {
                mappedStatement.hasNestedResultMaps =
                        mappedStatement.hasNestedResultMaps || resultMap.hasNestedResultMaps();
            }
            return this;
        }

        public Builder fetchSize(Integer fetchSize) {
            mappedStatement.fetchSize = fetchSize;
            return this;
        }

        /**
         * 设置声明超时时间
         *
         * @param timeout
         * @return
         */
        public Builder timeout(Integer timeout) {
            mappedStatement.timeout = timeout;
            return this;
        }

        /**
         * 设置声明类型
         *
         * @param statementType
         * @return
         */
        public Builder statementType(StatementType statementType) {
            mappedStatement.statementType = statementType;
            return this;
        }

        /**
         * 设置结果集类型
         *
         * @param resultSetType
         * @return
         */
        public Builder resultSetType(ResultSetType resultSetType) {
            mappedStatement.resultSetType = resultSetType;
            return this;
        }

        /**
         * 设置缓存
         *
         * @param cache
         * @return
         */
        public Builder cache(Cache cache) {
            mappedStatement.cache = cache;
            return this;
        }

        /**
         * 设置是否必须刷新缓存
         *
         * @param flushCacheRequired
         * @return
         */
        public Builder flushCacheRequired(boolean flushCacheRequired) {
            mappedStatement.flushCacheRequired = flushCacheRequired;
            return this;
        }

        /**
         * 设置是否使用缓存
         *
         * @param useCache
         * @return
         */
        public Builder useCache(boolean useCache) {
            mappedStatement.useCache = useCache;
            return this;
        }

        /**
         * 设置是否结果排序
         *
         * @param resultOrdered
         * @return
         */
        public Builder resultOrdered(boolean resultOrdered) {
            mappedStatement.resultOrdered = resultOrdered;
            return this;
        }

        /**
         * 设置主键生成器
         *
         * @param keyGenerator
         * @return
         */
        public Builder keyGenerator(KeyGenerator keyGenerator) {
            mappedStatement.keyGenerator = keyGenerator;
            return this;
        }

        public Builder keyProperty(String keyProperty) {
            mappedStatement.keyProperties = delimitedStringToArray(keyProperty);
            return this;
        }

        public Builder keyColumn(String keyColumn) {
            mappedStatement.keyColumns = delimitedStringToArray(keyColumn);
            return this;
        }

        /**
         * 设置数据库id
         *
         * @param databaseId
         * @return
         */
        public Builder databaseId(String databaseId) {
            mappedStatement.databaseId = databaseId;
            return this;
        }

        /**
         * 设置语言驱动
         *
         * @param driver
         * @return
         */
        public Builder lang(LanguageDriver driver) {
            mappedStatement.lang = driver;
            return this;
        }

        /**
         * 设置结果集
         *
         * @param resultSet
         * @return
         */
        public Builder resultSets(String resultSet) {
            mappedStatement.resultSets = delimitedStringToArray(resultSet);
            return this;
        }

        /**
         * @deprecated Use {@link #resultSets}
         */
        @Deprecated
        public Builder resulSets(String resultSet) {
            mappedStatement.resultSets = delimitedStringToArray(resultSet);
            return this;
        }

        /**
         * 构建对象
         *
         * @return
         */
        public MappedStatement build() {
            assert mappedStatement.configuration != null;
            assert mappedStatement.id != null;
            assert mappedStatement.sqlSource != null;
            assert mappedStatement.lang != null;
            mappedStatement.resultMaps = Collections.unmodifiableList(mappedStatement.resultMaps);
            return mappedStatement;
        }
    }

    /**
     * 获取主键生成器
     *
     * @return
     */
    public KeyGenerator getKeyGenerator() {
        return keyGenerator;
    }

    /**
     * 获取sql命令类型
     *
     * @return
     */
    public SqlCommandType getSqlCommandType() {
        return sqlCommandType;
    }

    /**
     * 获取来源
     *
     * @return
     */
    public String getResource() {
        return resource;
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
     * 获取id
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * 获取是否有嵌套结果map
     *
     * @return
     */
    public boolean hasNestedResultMaps() {
        return hasNestedResultMaps;
    }

    public Integer getFetchSize() {
        return fetchSize;
    }

    /**
     * 获取超时
     *
     * @return
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * 获取声明类型
     *
     * @return
     */
    public StatementType getStatementType() {
        return statementType;
    }

    /**
     * 获取结果集类型
     *
     * @return
     */
    public ResultSetType getResultSetType() {
        return resultSetType;
    }

    /**
     * 获取sql源
     *
     * @return
     */
    public SqlSource getSqlSource() {
        return sqlSource;
    }

    /**
     * 获取参数map
     *
     * @return
     */
    public ParameterMap getParameterMap() {
        return parameterMap;
    }

    /**
     * 获取结果map集
     *
     * @return
     */
    public List<ResultMap> getResultMaps() {
        return resultMaps;
    }

    /**
     * 获取缓存
     *
     * @return
     */
    public Cache getCache() {
        return cache;
    }

    /**
     * 获取是否必须刷新
     *
     * @return
     */
    public boolean isFlushCacheRequired() {
        return flushCacheRequired;
    }

    /**
     * 获取是否使用缓存
     *
     * @return
     */
    public boolean isUseCache() {
        return useCache;
    }

    /**
     * 获取是否结果排序
     *
     * @return
     */
    public boolean isResultOrdered() {
        return resultOrdered;
    }

    /**
     * 获取数据库id
     *
     * @return
     */
    public String getDatabaseId() {
        return databaseId;
    }

    public String[] getKeyProperties() {
        return keyProperties;
    }

    public String[] getKeyColumns() {
        return keyColumns;
    }

    /**
     * 获取声明日志
     *
     * @return
     */
    public Log getStatementLog() {
        return statementLog;
    }

    /**
     * 获取语言驱动
     *
     * @return
     */
    public LanguageDriver getLang() {
        return lang;
    }

    /**
     * 获取结果集
     *
     * @return
     */
    public String[] getResultSets() {
        return resultSets;
    }

    /**
     * @deprecated Use {@link #getResultSets()}
     */
    @Deprecated
    public String[] getResulSets() {
        return resultSets;
    }

    /**
     * 绑定sql
     *
     * @param parameterObject
     * @return
     */
    public BoundSql getBoundSql(Object parameterObject) {
        BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings == null || parameterMappings.isEmpty()) {
            boundSql = new BoundSql(configuration, boundSql.getSql(), parameterMap.getParameterMappings(),
                    parameterObject);
        }

        // check for nested result maps in parameter mappings (issue #30)
        for (ParameterMapping pm : boundSql.getParameterMappings()) {
            String rmId = pm.getResultMapId();
            if (rmId != null) {
                ResultMap rm = configuration.getResultMap(rmId);
                if (rm != null) {
                    hasNestedResultMaps |= rm.hasNestedResultMaps();
                }
            }
        }

        return boundSql;
    }

    /**
     * 将输入的.分割的字符串转换成数组
     *
     * @param in
     * @return
     */
    private static String[] delimitedStringToArray(String in) {
        if (in == null || in.trim().length() == 0) {
            return null;
        } else {
            return in.split(",");
        }
    }

}
