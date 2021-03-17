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
package org.apache.ibatis.mapping;

import javax.sql.DataSource;

import org.apache.ibatis.transaction.TransactionFactory;

/**
 * 环境配置
 *
 * @author Clinton Begin
 */
public final class Environment {

    /**
     * 环境id名称
     */
    private final String id;

    /**
     * 事务工厂
     */
    private final TransactionFactory transactionFactory;

    /**
     * 数据源
     */
    private final DataSource dataSource;

    /**
     * 构造函数
     *
     * @param id
     * @param transactionFactory
     * @param dataSource
     */
    public Environment(String id, TransactionFactory transactionFactory, DataSource dataSource) {
        if (id == null) {
            throw new IllegalArgumentException("Parameter 'id' must not be null");
        }
        if (transactionFactory == null) {
            throw new IllegalArgumentException("Parameter 'transactionFactory' must not be null");
        }
        this.id = id;
        if (dataSource == null) {
            throw new IllegalArgumentException("Parameter 'dataSource' must not be null");
        }
        this.transactionFactory = transactionFactory;
        this.dataSource = dataSource;
    }

    /**
     * 构造者
     */
    public static class Builder {

        /**
         * 环境id名称
         */
        private String id;

        /**
         * 事务工厂
         */
        private TransactionFactory transactionFactory;

        /**
         * 数据源
         */
        private DataSource dataSource;

        /**
         * 构造函数
         *
         * @param id
         */
        public Builder(String id) {
            this.id = id;
        }

        /**
         * 设置事务工厂
         *
         * @param transactionFactory
         * @return
         */
        public Builder transactionFactory(TransactionFactory transactionFactory) {
            this.transactionFactory = transactionFactory;
            return this;
        }

        /**
         * 设置数据源
         *
         * @param dataSource
         * @return
         */
        public Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        /**
         * 返回id
         *
         * @return
         */
        public String id() {
            return this.id;
        }

        /**
         * 构建环境对象
         *
         * @return
         */
        public Environment build() {
            return new Environment(this.id, this.transactionFactory, this.dataSource);
        }

    }

    /**
     * 获取环境id名称
     *
     * @return
     */
    public String getId() {
        return this.id;
    }

    /**
     * 获取事务工厂
     *
     * @return
     */
    public TransactionFactory getTransactionFactory() {
        return this.transactionFactory;
    }

    /**
     * 获取数据源
     *
     * @return
     */
    public DataSource getDataSource() {
        return this.dataSource;
    }

}
