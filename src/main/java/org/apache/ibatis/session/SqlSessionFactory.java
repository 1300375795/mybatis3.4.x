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
package org.apache.ibatis.session;

import java.sql.Connection;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * Creates an {@link SqlSession} out of a connection or a DataSource
 * sqlSession工厂
 *
 * @author Clinton Begin
 */
public interface SqlSessionFactory {

    /**
     * 打开session
     *
     * @return
     */
    SqlSession openSession();

    /**
     * 打开session
     *
     * @param autoCommit
     * @return
     */
    SqlSession openSession(boolean autoCommit);

    /**
     * 打开session
     *
     * @param connection
     * @return
     */
    SqlSession openSession(Connection connection);

    /**
     * 打开session
     *
     * @param level
     * @return
     */
    SqlSession openSession(TransactionIsolationLevel level);

    /**
     * 打开session
     *
     * @param execType
     * @return
     */
    SqlSession openSession(ExecutorType execType);

    /**
     * 打开session
     *
     * @param execType
     * @param autoCommit
     * @return
     */
    SqlSession openSession(ExecutorType execType, boolean autoCommit);

    /**
     * 打开session
     *
     * @param execType
     * @param level
     * @return
     */
    SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);

    /**
     * 打开session
     *
     * @param execType
     * @param connection
     * @return
     */
    SqlSession openSession(ExecutorType execType, Connection connection);

    /**
     * 获取全局配置
     *
     * @return
     */
    Configuration getConfiguration();

}
