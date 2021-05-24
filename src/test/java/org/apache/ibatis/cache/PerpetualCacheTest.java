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

import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.SynchronizedCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 测试基础的缓存类--永久缓存
 */
public class PerpetualCacheTest {

    /**
     * 测试放入的key跟value 跟根据这个key取出的value的值 是一样的
     * 并且放入多少个缓存对象 那么缓存的大小也要相同
     */
    @Test
    public void shouldDemonstrateHowAllObjectsAreKept() {
        Cache cache = new PerpetualCache("default");
        cache = new SynchronizedCache(cache);
        for (int i = 0; i < 100000; i++) {
            cache.putObject(i, i);
            assertEquals(i, cache.getObject(i));
        }
        assertEquals(100000, cache.getSize());
    }

    /**
     * 测试对永远缓存进行序列化装饰后 放入跟取出的缓存的内容要一致
     */
    @Test
    public void shouldDemonstrateCopiesAreEqual() {
        Cache cache = new PerpetualCache("default");
        cache = new SerializedCache(cache);
        for (int i = 0; i < 1000; i++) {
            cache.putObject(i, i);
            assertEquals(i, cache.getObject(i));
        }
    }

    /**
     * 测试基础的存入、获取、删除缓存操作能正常进行
     */
    @Test
    public void shouldRemoveItemOnDemand() {
        Cache cache = new PerpetualCache("default");
        cache = new SynchronizedCache(cache);
        cache.putObject(0, 0);
        assertNotNull(cache.getObject(0));
        cache.removeObject(0);
        assertNull(cache.getObject(0));
    }

    /**
     * 测试清空缓存操作
     */
    @Test
    public void shouldFlushAllItemsOnDemand() {
        Cache cache = new PerpetualCache("default");
        cache = new SynchronizedCache(cache);
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