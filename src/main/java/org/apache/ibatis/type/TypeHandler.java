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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public interface TypeHandler<T> {

    /**
     * 设置请求参数
     *
     * @param ps
     * @param i
     * @param parameter
     * @param jdbcType
     * @throws SQLException
     */
    void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

    /**
     * 根据列名称从结果集中获取相应的结果
     *
     * @param rs
     * @param columnName
     * @return
     * @throws SQLException
     */
    T getResult(ResultSet rs, String columnName) throws SQLException;

    /**
     * 根据列的序号从结果集中获取相应的结果
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException
     */
    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * 根据列序号从可调用声明中获取相应的结果
     *
     * @param cs
     * @param columnIndex
     * @return
     * @throws SQLException
     */
    T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}
