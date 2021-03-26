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
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;

/**
 * 声明处理器
 *
 * @author Clinton Begin
 */
public interface StatementHandler {

    /**
     * 准备声明
     *
     * @param connection
     * @param transactionTimeout
     * @return
     * @throws SQLException
     */
    Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException;

    /**
     * 参数化
     *
     * @param statement
     * @throws SQLException
     */
    void parameterize(Statement statement) throws SQLException;

    /**
     * 批处理
     *
     * @param statement
     * @throws SQLException
     */
    void batch(Statement statement) throws SQLException;

    /**
     * 更新
     *
     * @param statement
     * @return
     * @throws SQLException
     */
    int update(Statement statement) throws SQLException;

    /**
     * 查询
     *
     * @param statement
     * @param resultHandler
     * @param <E>
     * @return
     * @throws SQLException
     */
    <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException;

    /**
     * 通过游标查询
     *
     * @param statement
     * @param <E>
     * @return
     * @throws SQLException
     */
    <E> Cursor<E> queryCursor(Statement statement) throws SQLException;

    /**
     * 获取绑定的sql
     *
     * @return
     */
    BoundSql getBoundSql();

    /**
     * 参数处理器
     *
     * @return
     */
    ParameterHandler getParameterHandler();

}
