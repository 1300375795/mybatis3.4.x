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
package org.apache.ibatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.StatementType;

/**
 * @author Clinton Begin
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Options {

    /**
     * The options for the {@link Options#flushCache()}.
     * The default is {@link FlushCachePolicy#DEFAULT}
     * 刷新缓存策略
     */
    public enum FlushCachePolicy {

        /**
         * <code>false</code> for select statement; <code>true</code> for insert/update/delete statement.
         * 只有在新增、更新、删除的时候才会刷新缓存
         */
        DEFAULT,

        /**
         * Flushes cache regardless of the statement type.
         * 不管是什么sql语句都会刷新缓存
         */
        TRUE,

        /**
         * Does not flush cache regardless of the statement type.
         * 不管什么sql语句都不刷新缓存
         */
        FALSE
    }

    /**
     * 是否使用缓存
     *
     * @return
     */
    boolean useCache() default true;

    /**
     * 缓存刷新策略
     *
     * @return
     */
    FlushCachePolicy flushCache() default FlushCachePolicy.DEFAULT;

    /**
     * 结果集类型
     *
     * @return
     */
    ResultSetType resultSetType() default ResultSetType.FORWARD_ONLY;

    /**
     * 声明的类型
     *
     * @return
     */
    StatementType statementType() default StatementType.PREPARED;

    /**
     * @return
     */
    int fetchSize() default -1;

    /**
     * 超时时间
     *
     * @return
     */
    int timeout() default -1;

    /**
     * 是否使用自增键
     *
     * @return
     */
    boolean useGeneratedKeys() default false;

    /**
     * 主键属性
     *
     * @return
     */
    String keyProperty() default "id";

    /**
     * 主键字段
     *
     * @return
     */
    String keyColumn() default "";

    /**
     * 结果集
     *
     * @return
     */
    String resultSets() default "";
}
