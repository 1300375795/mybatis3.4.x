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
package org.apache.ibatis.datasource.unpooled;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;

/**
 * 没有池化的数据源
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class UnpooledDataSource implements DataSource {

    /**
     * 驱动的类加载器
     */
    private ClassLoader driverClassLoader;

    /**
     * 驱动的属性
     */
    private Properties driverProperties;

    /**
     * 已经注册的驱动
     */
    private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<String, Driver>();

    /**
     * 驱动名称
     */
    private String driver;

    /**
     * 数据库url
     */
    private String url;

    /**
     * 数据库名称
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * 是否自动提交
     */
    private Boolean autoCommit;

    /**
     * 默认的事务隔离级别
     */
    private Integer defaultTransactionIsolationLevel;

    /**
     * 静态代码块 初始化已注册的驱动map
     */
    static {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            registeredDrivers.put(driver.getClass().getName(), driver);
        }
    }

    /**
     * 构造函数
     */
    public UnpooledDataSource() {
    }

    /**
     * 构造函数
     *
     * @param driver
     * @param url
     * @param username
     * @param password
     */
    public UnpooledDataSource(String driver, String url, String username, String password) {
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /**
     * 构造函数
     *
     * @param driver
     * @param url
     * @param driverProperties
     */
    public UnpooledDataSource(String driver, String url, Properties driverProperties) {
        this.driver = driver;
        this.url = url;
        this.driverProperties = driverProperties;
    }

    /**
     * 构造函数
     *
     * @param driverClassLoader
     * @param driver
     * @param url
     * @param username
     * @param password
     */
    public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username,
            String password) {
        this.driverClassLoader = driverClassLoader;
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /**
     * 构造函数
     *
     * @param driverClassLoader
     * @param driver
     * @param url
     * @param driverProperties
     */
    public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
        this.driverClassLoader = driverClassLoader;
        this.driver = driver;
        this.url = url;
        this.driverProperties = driverProperties;
    }

    /**
     * 从数据源中获取连接
     *
     * @return
     * @throws SQLException
     */
    @Override
    public Connection getConnection() throws SQLException {
        return doGetConnection(username, password);
    }

    /**
     * 从数据源中获取连接
     *
     * @param username
     * @param password
     * @return
     * @throws SQLException
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return doGetConnection(username, password);
    }

    /**
     * 设置注册超时时间
     *
     * @param loginTimeout
     * @throws SQLException
     */
    @Override
    public void setLoginTimeout(int loginTimeout) throws SQLException {
        DriverManager.setLoginTimeout(loginTimeout);
    }

    /**
     * 获取注册超时时间
     *
     * @return
     * @throws SQLException
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    /**
     * 设置日志输出对象
     *
     * @param logWriter
     * @throws SQLException
     */
    @Override
    public void setLogWriter(PrintWriter logWriter) throws SQLException {
        DriverManager.setLogWriter(logWriter);
    }

    /**
     * 获取日志输出对象
     *
     * @return
     * @throws SQLException
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    /**
     * 获取驱动的类加载器
     *
     * @return
     */
    public ClassLoader getDriverClassLoader() {
        return driverClassLoader;
    }

    /**
     * 设置驱动的类加载器
     *
     * @param driverClassLoader
     */
    public void setDriverClassLoader(ClassLoader driverClassLoader) {
        this.driverClassLoader = driverClassLoader;
    }

    /**
     * 获取驱动的属性
     *
     * @return
     */
    public Properties getDriverProperties() {
        return driverProperties;
    }

    /**
     * 设置驱动的属性
     *
     * @param driverProperties
     */
    public void setDriverProperties(Properties driverProperties) {
        this.driverProperties = driverProperties;
    }

    /**
     * 获取驱动名称
     *
     * @return
     */
    public String getDriver() {
        return driver;
    }

    /**
     * 设置驱动名称
     *
     * @param driver
     */
    public synchronized void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * 获取url
     *
     * @return
     */
    public String getUrl() {
        return url;
    }

    /**
     * 设置url
     *
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 获取用户名称
     *
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名称
     *
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取密码
     *
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码
     *
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 是否自动提交
     *
     * @return
     */
    public Boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * 设置是否自动提交
     *
     * @param autoCommit
     */
    public void setAutoCommit(Boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    /**
     * 获取默认的事务隔离级别
     *
     * @return
     */
    public Integer getDefaultTransactionIsolationLevel() {
        return defaultTransactionIsolationLevel;
    }

    /**
     * 设置默认的事务隔离级别
     *
     * @param defaultTransactionIsolationLevel
     */
    public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
        this.defaultTransactionIsolationLevel = defaultTransactionIsolationLevel;
    }

    /**
     * 获取连接
     *
     * @param username
     * @param password
     * @return
     * @throws SQLException
     */
    private Connection doGetConnection(String username, String password) throws SQLException {
        Properties props = new Properties();
        if (driverProperties != null) {
            props.putAll(driverProperties);
        }
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        return doGetConnection(props);
    }

    /**
     * 从属性变量中获取连接
     *
     * @param properties
     * @return
     * @throws SQLException
     */
    private Connection doGetConnection(Properties properties) throws SQLException {
        initializeDriver();
        //获取连接
        Connection connection = DriverManager.getConnection(url, properties);
        configureConnection(connection);
        return connection;
    }

    /**
     * 初始化驱动
     * 根据driver注册驱动代理到registeredDrivers中
     *
     * @throws SQLException
     */
    private synchronized void initializeDriver() throws SQLException {
        if (!registeredDrivers.containsKey(driver)) {
            Class<?> driverType;
            try {
                if (driverClassLoader != null) {
                    driverType = Class.forName(driver, true, driverClassLoader);
                } else {
                    driverType = Resources.classForName(driver);
                }
                // DriverManager requires the driver to be loaded via the system ClassLoader.
                // http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
                Driver driverInstance = (Driver) driverType.newInstance();
                DriverManager.registerDriver(new DriverProxy(driverInstance));
                registeredDrivers.put(driver, driverInstance);
            } catch (Exception e) {
                throw new SQLException("Error setting driver on UnpooledDataSource. Cause: " + e);
            }
        }
    }

    /**
     * 配置连接
     *
     * @param conn
     * @throws SQLException
     */
    private void configureConnection(Connection conn) throws SQLException {
        //如果本数据源中的是否自动提交不为null并且跟连接中的是否自动提交不一样 那么设置连接的是否自动提交为本数据源的是否自动提交
        if (autoCommit != null && autoCommit != conn.getAutoCommit()) {
            conn.setAutoCommit(autoCommit);
        }
        //如果本数据源中的默认事务隔离级别不为null 那么设置连接中的默认事务隔离级别为本数据源中的
        if (defaultTransactionIsolationLevel != null) {
            conn.setTransactionIsolation(defaultTransactionIsolationLevel);
        }
    }

    /**
     * 驱动代理
     */
    private static class DriverProxy implements Driver {

        /**
         * 被代理驱动
         */
        private Driver driver;

        /**
         * 构造函数
         *
         * @param d
         */
        DriverProxy(Driver d) {
            this.driver = d;
        }

        @Override
        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        @Override
        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        @Override
        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        @Override
        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }

        // @Override only valid jdk7+
        public Logger getParentLogger() {
            return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException(getClass().getName() + " is not a wrapper.");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    // @Override only valid jdk7+
    public Logger getParentLogger() {
        // requires JDK version 1.6
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

}
