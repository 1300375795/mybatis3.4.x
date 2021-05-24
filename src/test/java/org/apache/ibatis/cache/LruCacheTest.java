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

import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 测试最近最常使用策略的缓存装饰类
 */
public class LruCacheTest {

    /**
     * 测试5个元素最近最常使用缓存的时候
     * 如果get、put操作了 那么这个缓存key会被放在缓存的最后面
     * 如果超过了缓存的大小 那么缓存中最前面的那个缓存会被删除
     */
    @Test
    public void shouldRemoveLeastRecentlyUsedItemInBeyondFiveEntries() {
        LruCache cache = new LruCache(new PerpetualCache("default"));
        cache.setSize(5);
        for (int i = 0; i < 5; i++) {
            cache.putObject(i, i);
        }
        assertEquals(0, cache.getObject(0));
        cache.putObject(5, 5);
        assertNull(cache.getObject(1));
        assertEquals(5, cache.getSize());
    }

    /**
     * 测试存入以及取出缓存能正常通过
     */
    @Test
    public void shouldRemoveItemOnDemand() {
        Cache cache = new LruCache(new PerpetualCache("default"));
        cache.putObject(0, 0);
        assertNotNull(cache.getObject(0));
        cache.removeObject(0);
        assertNull(cache.getObject(0));
    }

    /**
     * 测试情况缓存之后 获取被情况的缓存的结果正常
     */
    @Test
    public void shouldFlushAllItemsOnDemand() {
        Cache cache = new LruCache(new PerpetualCache("default"));
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