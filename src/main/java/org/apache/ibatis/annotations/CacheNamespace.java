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

import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

/**
 * 缓存命名空间
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CacheNamespace {

    /**
     * 缓存实现类  默认是PerpetualCache
     *
     * @return
     */
    Class<? extends org.apache.ibatis.cache.Cache> implementation() default PerpetualCache.class;

    /**
     * 回收缓存实现 默认是最新最少使用缓存
     * 作为implementation的装饰class使用
     *
     * @return
     */
    Class<? extends org.apache.ibatis.cache.Cache> eviction() default LruCache.class;

    /**
     * 清理周期
     *
     * @return
     */
    long flushInterval() default 0;

    /**
     * 缓存大小
     *
     * @return
     */
    int size() default 1024;

    /**
     * 读取
     *
     * @return
     */
    boolean readWrite() default true;

    boolean blocking() default false;

    /**
     * Property values for a implementation object.
     *
     * @since 3.4.2
     */
    Property[] properties() default {};

}
