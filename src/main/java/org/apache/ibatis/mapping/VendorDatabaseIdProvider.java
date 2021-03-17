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
package org.apache.ibatis.mapping;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * Vendor DatabaseId provider
 * <p>
 * It returns database product name as a databaseId
 * If the user provides a properties it uses it to translate database product name
 * key="Microsoft SQL Server", value="ms" will return "ms"
 * It can return null, if no database product name or
 * a properties was specified and no translation was found
 * 供应商DatabaseId提供者
 * 它以databaseId的形式返回数据库产品名
 * 如果用户提供了一个属性，它将使用它来翻译数据库产品名
 * key="Microsoft SQL Server"， value="ms"将返回"ms"
 * 如果没有数据库产品名称或提供的属性没有匹配到 它会返回null
 *
 * @author Eduardo Macarron
 */
public class VendorDatabaseIdProvider implements DatabaseIdProvider {

    private static final Log log = LogFactory.getLog(VendorDatabaseIdProvider.class);

    private Properties properties;

    @Override
    public String getDatabaseId(DataSource dataSource) {
        if (dataSource == null) {
            throw new NullPointerException("dataSource cannot be null");
        }
        try {
            return getDatabaseName(dataSource);
        } catch (Exception e) {
            log.error("Could not get a databaseId from dataSource", e);
        }
        return null;
    }

    @Override
    public void setProperties(Properties p) {
        this.properties = p;
    }

    /**
     * 根据数据源获取数据库id
     *
     * @param dataSource
     * @return
     * @throws SQLException
     */
    private String getDatabaseName(DataSource dataSource) throws SQLException {
        //获取这个数据源的产品名称 例如MySQL
        String productName = getDatabaseProductName(dataSource);
        //如果属性不为空 并且产品名称中包含属性给出的内容 那么匹配得上 返回配置的属性的value
        if (this.properties != null) {
            for (Map.Entry<Object, Object> property : properties.entrySet()) {
                if (productName.contains((String) property.getKey())) {
                    return (String) property.getValue();
                }
            }
            // no match, return null
            //如果没有匹配得到 返回null
            return null;
        }
        //如果没有属性配置 直接返回数据库产品名称
        return productName;
    }

    /**
     * 获取这个数据源对应的数据库产品名称
     *
     * @param dataSource
     * @return
     * @throws SQLException
     */
    private String getDatabaseProductName(DataSource dataSource) throws SQLException {
        Connection con = null;
        try {
            con = dataSource.getConnection();
            DatabaseMetaData metaData = con.getMetaData();
            return metaData.getDatabaseProductName();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // ignored
                }
            }
        }
    }

}
