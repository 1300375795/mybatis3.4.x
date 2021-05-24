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

import static org.junit.Assert.*;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import org.junit.Ignore;
import org.junit.Test;

/**
 * 测试未进行池化的数据源
 */
public class UnpooledDataSourceTest {

    @Test
    public void shouldNotRegisterTheSameDriverMultipleTimes() throws Exception {
        // https://code.google.com/p/mybatis/issues/detail?id=430
        UnpooledDataSource dataSource = null;
        dataSource = new UnpooledDataSource("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:multipledrivers", "sa", "");
        dataSource.getConnection();
        int before = countRegisteredDrivers();
        dataSource = new UnpooledDataSource("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:multipledrivers", "sa", "");
        dataSource.getConnection();
        assertEquals(before, countRegisteredDrivers());
    }

    /**
     * 测试驱动数量
     * 由于我已经在依赖中加入了mysql驱动
     * 而之前没有做过任何变动的源码的时候是没有mysql的驱动的
     * 所以测试用例需要改成assertEquals(before, countRegisteredDrivers());
     * 而不是assertEquals(before+1, countRegisteredDrivers());
     *
     * @throws Exception
     */
    @Ignore("Requires MySQL server and a driver.")
    @Test
    public void shouldRegisterDynamicallyLoadedDriver() throws Exception {
        int before = countRegisteredDrivers();
        ClassLoader driverClassLoader = null;
        UnpooledDataSource dataSource = null;
        driverClassLoader = new URLClassLoader(
                new URL[] { new URL("jar:file:/PATH_TO/mysql-connector-java-5.1.25.jar!/") });
        dataSource = new UnpooledDataSource(driverClassLoader, "com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://127.0.0.1/mybatis", "root", "ydg@971810252");
        dataSource.getConnection();
        assertEquals(before, countRegisteredDrivers());
        driverClassLoader = new URLClassLoader(
                new URL[] { new URL("jar:file:/PATH_TO/mysql-connector-java-5.1.25.jar!/") });
        dataSource = new UnpooledDataSource(driverClassLoader, "com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://127.0.0.1/mybatis", "root", "ydg@971810252");
        dataSource.getConnection();
        assertEquals(before, countRegisteredDrivers());
    }

    protected int countRegisteredDrivers() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        int count = 0;
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            String s = driver.toString();
            System.out.println(s);
            count++;
        }
        return count;
    }

}
