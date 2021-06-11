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

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BaseStatementHandlerTest {

    @Spy
    Configuration configuration;

    @Mock
    Statement statement;

    MappedStatement.Builder mappedStatementBuilder;

    @Before
    public void setupMappedStatement() {
        this.mappedStatementBuilder = new MappedStatement.Builder(configuration, "id",
                new StaticSqlSource(configuration, "sql"), null);
    }

    @After
    public void resetMocks() {
        reset(configuration, statement);
    }

    /**
     * 测试没有指定超时时间
     *
     * @throws SQLException
     */
    @Test
    public void notSpecifyTimeout() throws SQLException {
        BaseStatementHandler handler = new SimpleStatementHandler(null, mappedStatementBuilder.build(), null, null,
                null, null);
        handler.setStatementTimeout(statement, null);

        verifyZeroInteractions(statement); // not apply anything
    }

    /**
     * 测试只指定了声明超时时间
     *
     * @throws SQLException
     */
    @Test
    public void specifyMappedStatementTimeoutOnly() throws SQLException {
        mappedStatementBuilder.timeout(10);

        BaseStatementHandler handler = new SimpleStatementHandler(null, mappedStatementBuilder.build(), null, null,
                null, null);
        handler.setStatementTimeout(statement, null);

        verify(statement).setQueryTimeout(10); // apply a mapped statement timeout
    }

    /**
     * 测试只指定了默认的全局配置中的超时时间
     *
     * @throws SQLException
     */
    @Test
    public void specifyDefaultTimeoutOnly() throws SQLException {
        doReturn(20).when(configuration).getDefaultStatementTimeout();

        BaseStatementHandler handler = new SimpleStatementHandler(null, mappedStatementBuilder.build(), null, null,
                null, null);
        handler.setStatementTimeout(statement, null);

        verify(statement).setQueryTimeout(20); // apply a default timeout
    }

    /**
     * 测试指定了事务超时时间
     *
     * @throws SQLException
     */
    @Test
    public void specifyTransactionTimeout() throws SQLException {
        BaseStatementHandler handler = new SimpleStatementHandler(null, mappedStatementBuilder.build(), null, null,
                null, null);
        handler.setStatementTimeout(statement, 5);

        verify(statement).setQueryTimeout(5); // apply a transaction timeout
    }

    /**
     * 测试指定了默认的全局配置中的超时时间以及事务的超时时间
     *
     * @throws SQLException
     */
    @Test
    public void specifyQueryTimeoutZeroAndTransactionTimeout() throws SQLException {
        doReturn(0).when(configuration).getDefaultStatementTimeout();

        BaseStatementHandler handler = new SimpleStatementHandler(null, mappedStatementBuilder.build(), null, null,
                null, null);
        handler.setStatementTimeout(statement, 5);

        verify(statement).setQueryTimeout(5); // apply a transaction timeout
    }

    /**
     * 测试指定了全局超时时间、mappedStatement超时时间
     *
     * @throws SQLException
     */
    @Test
    public void specifyMappedStatementTimeoutAndDefaultTimeout() throws SQLException {
        doReturn(20).when(configuration).getDefaultStatementTimeout();
        mappedStatementBuilder.timeout(30);

        BaseStatementHandler handler = new SimpleStatementHandler(null, mappedStatementBuilder.build(), null, null,
                null, null);
        handler.setStatementTimeout(statement, null);

        verify(statement).setQueryTimeout(30); // apply a mapped statement timeout
        verify(configuration, never()).getDefaultStatementTimeout();
    }

    @Test
    public void specifyQueryTimeoutAndTransactionTimeoutMinIsQueryTimeout() throws SQLException {
        doReturn(10).when(configuration).getDefaultStatementTimeout();

        BaseStatementHandler handler = new SimpleStatementHandler(null, mappedStatementBuilder.build(), null, null,
                null, null);
        handler.setStatementTimeout(statement, 20);

        verify(statement).setQueryTimeout(10); // apply a query timeout
    }

    @Test
    public void specifyQueryTimeoutAndTransactionTimeoutMinIsTransactionTimeout() throws SQLException {
        doReturn(10).when(configuration).getDefaultStatementTimeout();

        BaseStatementHandler handler = new SimpleStatementHandler(null, mappedStatementBuilder.build(), null, null,
                null, null);
        handler.setStatementTimeout(statement, 5);

        verify(statement).setQueryTimeout(10);
        verify(statement).setQueryTimeout(5); // apply a transaction timeout
    }

    @Test
    public void specifyQueryTimeoutAndTransactionTimeoutWithSameValue() throws SQLException {
        doReturn(10).when(configuration).getDefaultStatementTimeout();

        BaseStatementHandler handler = new SimpleStatementHandler(null, mappedStatementBuilder.build(), null, null,
                null, null);
        handler.setStatementTimeout(statement, 10);

        verify(statement).setQueryTimeout(10);
    }

}
