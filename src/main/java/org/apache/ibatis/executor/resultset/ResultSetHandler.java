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
package org.apache.ibatis.executor.resultset;

import org.apache.ibatis.cursor.Cursor;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 结果集处理器
 *
 * @author Clinton Begin
 */
public interface ResultSetHandler {

    /**
     * 处理结果集
     *
     * @param stmt 已经替换完成参数的声明
     * @param <E>
     * @return
     * @throws SQLException
     */
    <E> List<E> handleResultSets(Statement stmt) throws SQLException;

    /**
     * 处理游标结果集
     *
     * @param stmt
     * @param <E>
     * @return
     * @throws SQLException
     */
    <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;

    /**
     * 处理回调声明
     *
     * @param cs
     * @throws SQLException
     */
    void handleOutputParameters(CallableStatement cs) throws SQLException;

}
