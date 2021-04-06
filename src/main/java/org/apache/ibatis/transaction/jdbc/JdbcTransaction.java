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
package org.apache.ibatis.transaction.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionException;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * {@link Transaction} that makes use of the JDBC commit and rollback facilities directly.
 * It relies on the connection retrieved from the dataSource to manage the scope of the transaction.
 * Delays connection retrieval until getConnection() is called.
 * Ignores commit or rollback requests when autocommit is on.
 *
 * @author Clinton Begin
 * @see JdbcTransactionFactory
 */
public class JdbcTransaction implements Transaction {

    private static final Log log = LogFactory.getLog(JdbcTransaction.class);

    /**
     * 连接
     */
    protected Connection connection;

    /**
     * 数据源
     */
    protected DataSource dataSource;

    /**
     * 事务隔离级别
     */
    protected TransactionIsolationLevel level;
    // MEMO: We are aware of the typo. See #941

    /**
     * 期望的自动提交设置 如果没有通过构造器主动设置 那么boolean初始化值为false
     */
    protected boolean autoCommmit;

    /**
     * 构造函数
     *
     * @param ds                数据源
     * @param desiredLevel      事务隔离级别
     * @param desiredAutoCommit 期望的自动提交设置
     */
    public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
        dataSource = ds;
        level = desiredLevel;
        autoCommmit = desiredAutoCommit;
    }

    /**
     * 构造函数
     *
     * @param connection 连接
     */
    public JdbcTransaction(Connection connection) {
        this.connection = connection;
    }

    /**
     * 获取连接 如果连接为空 那么通过数据源获取连接
     *
     * @return
     * @throws SQLException
     */
    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null) {
            openConnection();
        }
        return connection;
    }

    /**
     * 如果连接不为空并且不是自动提交 那么进行提交
     *
     * @throws SQLException
     */
    @Override
    public void commit() throws SQLException {
        if (connection != null && !connection.getAutoCommit()) {
            if (log.isDebugEnabled()) {
                log.debug("Committing JDBC Connection [" + connection + "]");
            }
            connection.commit();
        }
    }

    /**
     * 回滚
     *
     * @throws SQLException
     */
    @Override
    public void rollback() throws SQLException {
        //如果连接不为空并且不是自定提交 进行回滚操作
        if (connection != null && !connection.getAutoCommit()) {
            if (log.isDebugEnabled()) {
                log.debug("Rolling back JDBC Connection [" + connection + "]");
            }
            connection.rollback();
        }
    }

    /**
     * 关闭连接
     *
     * @throws SQLException
     */
    @Override
    public void close() throws SQLException {
        //如果连接不为空 那么重置自动提交 并且关闭连接
        if (connection != null) {
            resetAutoCommit();
            if (log.isDebugEnabled()) {
                log.debug("Closing JDBC Connection [" + connection + "]");
            }
            connection.close();
        }
    }

    /**
     * 设置期望的自动提交
     *
     * @param desiredAutoCommit
     */
    protected void setDesiredAutoCommit(boolean desiredAutoCommit) {
        try {
            //如果连接中的自动提交跟给出的自动提交不一样 那么将连接中的自动提交设置为期望的自动提交
            if (connection.getAutoCommit() != desiredAutoCommit) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
                }
                connection.setAutoCommit(desiredAutoCommit);
            }
        } catch (SQLException e) {
            // Only a very poorly implemented driver would fail here,
            // and there's not much we can do about that.
            throw new TransactionException("Error configuring AutoCommit.  "
                    + "Your driver may not support getAutoCommit() or setAutoCommit(). " + "Requested setting: "
                    + desiredAutoCommit + ".  Cause: " + e, e);
        }
    }

    /**
     * 重置自动提交
     */
    protected void resetAutoCommit() {
        try {
            //如果不是自动提交的 那么设置成自动提交
            if (!connection.getAutoCommit()) {
                // MyBatis does not call commit/rollback on a connection if just selects were performed.
                // Some databases start transactions with select statements
                // and they mandate a commit/rollback before closing the connection.
                // A workaround is setting the autocommit to true before closing the connection.
                // Sybase throws an exception here.
                if (log.isDebugEnabled()) {
                    log.debug("Resetting autocommit to true on JDBC Connection [" + connection + "]");
                }
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error resetting autocommit to true " + "before closing the connection.  Cause: " + e);
            }
        }
    }

    /**
     * 打开连接
     *
     * @throws SQLException
     */
    protected void openConnection() throws SQLException {
        if (log.isDebugEnabled()) {
            log.debug("Opening JDBC Connection");
        }
        //从数据源中获取连接
        connection = dataSource.getConnection();
        //如果事务隔离级别不为空那么设置到连接中
        if (level != null) {
            connection.setTransactionIsolation(level.getLevel());
        }
        //设置是否自动提交
        setDesiredAutoCommit(autoCommmit);
    }

    @Override
    public Integer getTimeout() throws SQLException {
        return null;
    }

}
