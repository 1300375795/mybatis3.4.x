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
package org.apache.ibatis.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.cache.decorators.TransactionalCache;

/**
 * 事务缓存管理器
 *
 * @author Clinton Begin
 */
public class TransactionalCacheManager {

    /**
     * 缓存对应的装饰后的二级事务缓存
     */
    private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<Cache, TransactionalCache>();

    /**
     * 清除缓存
     *
     * @param cache
     */
    public void clear(Cache cache) {
        getTransactionalCache(cache).clear();
    }

    /**
     * 从缓存中获取对象
     *
     * @param cache
     * @param key
     * @return
     */
    public Object getObject(Cache cache, CacheKey key) {
        return getTransactionalCache(cache).getObject(key);
    }

    /**
     * 往缓存中存入对象
     *
     * @param cache
     * @param key
     * @param value
     */
    public void putObject(Cache cache, CacheKey key, Object value) {
        getTransactionalCache(cache).putObject(key, value);
    }

    /**
     * 提交
     */
    public void commit() {
        for (TransactionalCache txCache : transactionalCaches.values()) {
            txCache.commit();
        }
    }

    /**
     * 回滚
     */
    public void rollback() {
        for (TransactionalCache txCache : transactionalCaches.values()) {
            txCache.rollback();
        }
    }

    /**
     * 获取二级事务缓存
     *
     * @param cache
     * @return
     */
    private TransactionalCache getTransactionalCache(Cache cache) {
        //从事务缓存map中获取事务缓存对象
        TransactionalCache txCache = transactionalCaches.get(cache);
        //如果为空 那么装饰出来一个
        if (txCache == null) {
            txCache = new TransactionalCache(cache);
            transactionalCaches.put(cache, txCache);
        }
        return txCache;
    }

}
