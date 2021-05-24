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
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 测试软引用缓存
 */
public class SoftCacheTest {

    /**
     * 测试快出现OOM的时候 会回收掉软引用从而释放出相应的内存
     *
     * @throws Exception
     */
    @Test
    public void shouldDemonstrateObjectsBeingCollectedAsNeeded() throws Exception {
        final int N = 3000000;
        SoftCache cache = new SoftCache(new PerpetualCache("default"));
        for (int i = 0; i < N; i++) {
            byte[] array = new byte[5001]; //waste a bunch of memory
            array[5000] = 1;
            cache.putObject(i, array);
            Object value = cache.getObject(i);
            if (cache.getSize() < i + 1) {
                System.out.println("i的大小是:" + i);
                //System.out.println("Cache exceeded with " + (i + 1) + " entries.");
                break;
            }
        }
        int size = cache.getSize();
        System.out.println("size的大小是:" + size);
        assertTrue(size < N);
    }

    /**
     * 测试基础的缓存的存入、取出
     * 由于是软引用 如果这个软引用被回收了 那么存入的value就是null
     */
    @Test
    public void shouldDemonstrateCopiesAreEqual() {
        Cache cache = new SoftCache(new PerpetualCache("default"));
        cache = new SerializedCache(cache);
        for (int i = 0; i < 1000; i++) {
            cache.putObject(i, i);
            Object value = cache.getObject(i);
            assertTrue(value == null || value.equals(i));
        }
    }

    /**
     * 测试基础的存入、取出以及删除缓存
     */
    @Test
    public void shouldRemoveItemOnDemand() {
        Cache cache = new SoftCache(new PerpetualCache("default"));
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
        Cache cache = new SoftCache(new PerpetualCache("default"));
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