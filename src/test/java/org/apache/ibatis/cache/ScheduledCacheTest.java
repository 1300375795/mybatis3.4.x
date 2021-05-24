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
package org.apache.ibatis.cache;

import org.apache.ibatis.cache.decorators.LoggingCache;
import org.apache.ibatis.cache.decorators.ScheduledCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 测试定时clear缓存
 */
public class ScheduledCacheTest {

    /**
     * 测试存入缓存、取出缓存正常
     * 并且定时清理能正常清理
     *
     * @throws Exception
     */
    @Test
    public void shouldDemonstrateHowAllObjectsAreFlushedAfterBasedOnTime() throws Exception {
        Cache cache = new PerpetualCache("DefaultCache");
        cache = new ScheduledCache(cache);
        ((ScheduledCache) cache).setClearInterval(2500);
        cache = new LoggingCache(cache);
        for (int i = 0; i < 100; i++) {
            cache.putObject(i, i);
            assertEquals(i, cache.getObject(i));
        }
        Thread.sleep(5000);
        assertEquals(0, cache.getSize());
    }

    /**
     * 测试基础的存入、取出、删除缓存操作能正常运行
     */
    @Test
    public void shouldRemoveItemOnDemand() {
        Cache cache = new PerpetualCache("DefaultCache");
        cache = new ScheduledCache(cache);
        ((ScheduledCache) cache).setClearInterval(60000);
        cache = new LoggingCache(cache);
        cache.putObject(0, 0);
        assertNotNull(cache.getObject(0));
        cache.removeObject(0);
        assertNull(cache.getObject(0));
    }

    /**
     * 测试主动进行clear操作 能正常运行
     */
    @Test
    public void shouldFlushAllItemsOnDemand() {
        Cache cache = new PerpetualCache("DefaultCache");
        cache = new ScheduledCache(cache);
        ((ScheduledCache) cache).setClearInterval(60000);
        cache = new LoggingCache(cache);
        for (int i = 0; i < 5; i++) {
            cache.putObject(i, i);
        }
        assertNotNull(cache.getObject(0));
        assertNotNull(cache.getObject(4));
        cache.clear();
        assertNull(cache.getObject(0));
        assertNull(cache.getObject(4));
    }

}