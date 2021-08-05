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
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * 基础声明处理器
 *
 * @author Clinton Begin
 */
public abstract class BaseStatementHandler implements StatementHandler {

    /**
     * 全局配置
     */
    protected final Configuration configuration;

    /**
     * 对象工厂
     */
    protected final ObjectFactory objectFactory;

    /**
     * 类型处理器注册器
     */
    protected final TypeHandlerRegistry typeHandlerRegistry;

    /**
     * 结集果处理器
     */
    protected final ResultSetHandler resultSetHandler;

    /**
     * 参数处理器
     */
    protected final ParameterHandler parameterHandler;

    /**
     * 执行器
     */
    protected final Executor executor;

    /**
     * 已映射的声明
     */
    protected final MappedStatement mappedStatement;

    /**
     * 分页参数
     */
    protected final RowBounds rowBounds;

    /**
     * 绑定sql
     */
    protected BoundSql boundSql;

    /**
     * 构造函数
     *
     * @param executor
     * @param mappedStatement
     * @param parameterObject
     * @param rowBounds
     * @param resultHandler
     * @param boundSql
     */
    protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject,
            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        // TODO: 2021/7/6 CallYeDeGuo  创建声明处理器的时候会同时处理这些东西 还是很重要的一个构造函数
        this.configuration = mappedStatement.getConfiguration();
        this.executor = executor;
        this.mappedStatement = mappedStatement;
        this.rowBounds = rowBounds;

        this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        this.objectFactory = configuration.getObjectFactory();

        //如果绑定sql为null 那么生成主键并且获取绑定sql
        if (boundSql == null) { // issue #435, get the key before calculating the statement
            generateKeys(parameterObject);
            boundSql = mappedStatement.getBoundSql(parameterObject);
        }

        this.boundSql = boundSql;
        //插件还会包装这个
        this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
        this.resultSetHandler = configuration
                .newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
    }

    @Override
    public BoundSql getBoundSql() {
        return boundSql;
    }

    @Override
    public ParameterHandler getParameterHandler() {
        return parameterHandler;
    }

    @Override
    public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
        ErrorContext.instance().sql(boundSql.getSql());
        Statement statement = null;
        try {
            statement = instantiateStatement(connection);
            setStatementTimeout(statement, transactionTimeout);
            setFetchSize(statement);
            return statement;
        } catch (SQLException e) {
            closeStatement(statement);
            throw e;
        } catch (Exception e) {
            closeStatement(statement);
            throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
        }
    }

    protected abstract Statement instantiateStatement(Connection connection) throws SQLException;

    /**
     * 设置声明超时时间
     *
     * @param stmt
     * @param transactionTimeout
     * @throws SQLException
     */
    protected void setStatementTimeout(Statement stmt, Integer transactionTimeout) throws SQLException {
        Integer queryTimeout = null;
        //如果通过xml或者注解构建的MappedStatement设置了超时时间 那么就获取这个超时时间
        if (mappedStatement.getTimeout() != null) {
            queryTimeout = mappedStatement.getTimeout();
        }
        //如果全局配置中的超时时间不为空 那么就拿全局配置中的
        else if (configuration.getDefaultStatementTimeout() != null) {
            queryTimeout = configuration.getDefaultStatementTimeout();
        }
        //如果进行了配置 那么给声明设置超时时间
        if (queryTimeout != null) {
            stmt.setQueryTimeout(queryTimeout);
        }
        //根据事务超时时间以及设置的超时时间进行设置
        StatementUtil.applyTransactionTimeout(stmt, queryTimeout, transactionTimeout);
    }

    /**
     * 设置批量返回的值的大小
     *
     * @param stmt
     * @throws SQLException
     */
    protected void setFetchSize(Statement stmt) throws SQLException {
        ////如果通过xml或者注解构建的MappedStatement设置了批量返回的值 那么就获取这个批量返回的值
        Integer fetchSize = mappedStatement.getFetchSize();
        if (fetchSize != null) {
            stmt.setFetchSize(fetchSize);
            return;
        }
        Integer defaultFetchSize = configuration.getDefaultFetchSize();
        if (defaultFetchSize != null) {
            stmt.setFetchSize(defaultFetchSize);
        }
    }

    protected void closeStatement(Statement statement) {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            //ignore
        }
    }

    /**
     * 生成主键
     *
     * @param parameter
     */
    protected void generateKeys(Object parameter) {
        KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
        ErrorContext.instance().store();
        keyGenerator.processBefore(executor, mappedStatement, null, parameter);
        ErrorContext.instance().recall();
    }

}
