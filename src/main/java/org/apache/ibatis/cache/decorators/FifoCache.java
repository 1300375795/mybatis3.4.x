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
package org.apache.ibatis.cache.decorators;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * FIFO (first in, first out) cache decorator
 *
 * @author Clinton Begin
 */
public class FifoCache implements Cache {

    /**
     * 被装饰缓存
     */
    private final Cache delegate;

    /**
     * 缓存的key集合
     */
    private final Deque<Object> keyList;

    /**
     * 先进先出缓存大小
     */
    private int size;

    /**
     * 构造函数
     *
     * @param delegate
     */
    public FifoCache(Cache delegate) {
        this.delegate = delegate;
        this.keyList = new LinkedList<Object>();
        this.size = 1024;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    public void setSize(int size) {
        this.size = size;
    }

    /**
     * 放入缓存
     * 首先将key放入循环缓存key集合中 如果缓存已经达到最大的数量 那么就按先进先出的策略淘汰缓存 然后再存入新的缓存
     *
     * @param key   Can be any object but usually it is a {@link CacheKey}
     * @param value The result of a select.
     */
    @Override
    public void putObject(Object key, Object value) {
        cycleKeyList(key);
        delegate.putObject(key, value);
    }

    /**
     * 获取缓存
     *
     * @param key The key
     * @return
     */
    @Override
    public Object getObject(Object key) {
        return delegate.getObject(key);
    }

    /**
     * 删除缓存
     *
     * @param key The key
     * @return
     */
    @Override
    public Object removeObject(Object key) {
        return delegate.removeObject(key);
    }

    /**
     * 清除缓存
     */
    @Override
    public void clear() {
        delegate.clear();
        keyList.clear();
    }

    /**
     * 获取读写锁
     *
     * @return
     */
    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    /**
     * 先进先出策略淘汰缓存
     *
     * @param key
     */
    private void cycleKeyList(Object key) {
        keyList.addLast(key);
        if (keyList.size() > size) {
            Object oldestKey = keyList.removeFirst();
            delegate.removeObject(oldestKey);
        }
    }

}
