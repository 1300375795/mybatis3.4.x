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
package org.apache.ibatis.transaction.managed;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;

/**
 * {@link Transaction} that lets the container manage the full lifecycle of the transaction.
 * Delays connection retrieval until getConnection() is called.
 * Ignores all commit or rollback requests.
 * By default, it closes the connection but can be configured not to do it.
 * 被管理的事务
 *
 * @author Clinton Begin
 * @see ManagedTransactionFactory
 */
public class ManagedTransaction implements Transaction {

    private static final Log log = LogFactory.getLog(ManagedTransaction.class);

    /**
     * 数据源
     */
    private DataSource dataSource;

    /**
     * 事务隔离级别
     */
    private TransactionIsolationLevel level;

    /**
     * 连接
     */
    private Connection connection;

    /**
     * 是否关闭连接
     */
    private final boolean closeConnection;

    /**
     * 构造函数
     *
     * @param connection      连接
     * @param closeConnection 是否关闭连接
     */
    public ManagedTransaction(Connection connection, boolean closeConnection) {
        this.connection = connection;
        this.closeConnection = closeConnection;
    }

    /**
     * 构造函数
     *
     * @param ds              数据源
     * @param level           事务隔离级别
     * @param closeConnection 是够关闭连接
     */
    public ManagedTransaction(DataSource ds, TransactionIsolationLevel level, boolean closeConnection) {
        this.dataSource = ds;
        this.level = level;
        this.closeConnection = closeConnection;
    }

    /**
     * 获取连接
     *
     * @return
     * @throws SQLException
     */
    @Override
    public Connection getConnection() throws SQLException {
        //如果连接为null 那么通过数据源获取连接
        if (this.connection == null) {
            openConnection();
        }
        return this.connection;
    }

    /**
     * 提交
     * 不做任何事情
     *
     * @throws SQLException
     */
    @Override
    public void commit() throws SQLException {
        // Does nothing
    }

    /**
     * 回滚
     * 不做任何事情
     *
     * @throws SQLException
     */
    @Override
    public void rollback() throws SQLException {
        // Does nothing
    }

    /**
     * 如果要关闭连接并且连接不为null
     * 那么关闭连接
     *
     * @throws SQLException
     */
    @Override
    public void close() throws SQLException {
        if (this.closeConnection && this.connection != null) {
            if (log.isDebugEnabled()) {
                log.debug("Closing JDBC Connection [" + this.connection + "]");
            }
            this.connection.close();
        }
    }

    /**
     * 根据数据源获取连接
     *
     * @throws SQLException
     */
    protected void openConnection() throws SQLException {
        if (log.isDebugEnabled()) {
            log.debug("Opening JDBC Connection");
        }
        //从数据源中获取连接
        this.connection = this.dataSource.getConnection();
        //如果事务隔离级别不为空 那么设置连接的事务隔离级别
        if (this.level != null) {
            this.connection.setTransactionIsolation(this.level.getLevel());
        }
    }

    @Override
    public Integer getTimeout() throws SQLException {
        return null;
    }

}
