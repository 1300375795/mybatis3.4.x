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

import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 测试先进先出策略的缓存的缓存装饰类
 */
public class FifoCacheTest {

    /**
     * 测试指定了指定大小的先进先出缓存后 缓存的大小不能超过设置的值
     * 并且被淘汰的缓存不能再次查到
     */
    @Test
    public void shouldRemoveFirstItemInBeyondFiveEntries() {
        FifoCache cache = new FifoCache(new PerpetualCache("default"));
        cache.setSize(5);
        for (int i = 0; i < 5; i++) {
            cache.putObject(i, i);
        }
        assertEquals(0, cache.getObject(0));
        cache.putObject(5, 5);
        assertNull(cache.getObject(0));
        assertEquals(5, cache.getSize());
    }

    /**
     * 测试删除了指定缓存后 不能再次查到
     */
    @Test
    public void shouldRemoveItemOnDemand() {
        FifoCache cache = new FifoCache(new PerpetualCache("default"));
        cache.putObject(0, 0);
        assertNotNull(cache.getObject(0));
        cache.removeObject(0);
        assertNull(cache.getObject(0));
    }

    /**
     * 测试进行clear操作后 所有的缓存都不能查到
     */
    @Test
    public void shouldFlushAllItemsOnDemand() {
        FifoCache cache = new FifoCache(new PerpetualCache("default"));
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
