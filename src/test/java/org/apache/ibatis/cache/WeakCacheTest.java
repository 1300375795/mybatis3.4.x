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
package org.apache.ibatis.cache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.junit.Test;

/**
 * 弱引用测试类
 */
public class WeakCacheTest {

    /**
     * 测试弱引用 因为弱引用可能在任意时刻被回收
     * 如果进行了GC那么缓存的size肯定要小于实际之前加入到缓存的大小
     */
    @Test
    public void shouldDemonstrateObjectsBeingCollectedAsNeeded() {
        final int N = 3000000;
        WeakCache cache = new WeakCache(new PerpetualCache("default"));
        for (int i = 0; i < N; i++) {
            cache.putObject(i, i);
            if (cache.getSize() < i + 1) {
                //System.out.println("Cache exceeded with " + (i + 1) + " entries.");
                break;
            }
            if ((i + 1) % 100000 == 0) {
                // Try performing GC.
                System.gc();
            }
        }
        assertTrue(cache.getSize() < N);
    }

    /**
     * 测试基础的缓存加入、取出
     */
    @Test
    public void shouldDemonstrateCopiesAreEqual() {
        Cache cache = new WeakCache(new PerpetualCache("default"));
        cache = new SerializedCache(cache);
        for (int i = 0; i < 1000; i++) {
            cache.putObject(i, i);
            Object value = cache.getObject(i);
            assertTrue(value == null || value.equals(i));
        }
    }

    /**
     * 测试缓存的加入、取出、删除
     */
    @Test
    public void shouldRemoveItemOnDemand() {
        WeakCache cache = new WeakCache(new PerpetualCache("default"));
        cache.putObject(0, 0);
        assertNotNull(cache.getObject(0));
        cache.removeObject(0);
        assertNull(cache.getObject(0));
    }

    /**
     * 测试清除全部的缓存
     */
    @Test
    public void shouldFlushAllItemsOnDemand() {
        WeakCache cache = new WeakCache(new PerpetualCache("default"));
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
