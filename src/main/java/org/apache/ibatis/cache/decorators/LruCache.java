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
package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * Lru (least recently used) cache decorator
 * 最最近最少使用缓存
 *
 * @author Clinton Begin
 */
public class LruCache implements Cache {

    private final Cache delegate;

    /**
     * 缓存key map
     */
    private Map<Object, Object> keyMap;

    /**
     * 最久的key
     */
    private Object eldestKey;

    /**
     * 构造函数
     *
     * @param delegate
     */
    public LruCache(Cache delegate) {
        this.delegate = delegate;
        setSize(1024);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    /**
     * 设置大小
     * linkedHashMap在执行get、put等操作的时候 会更新map中的键值对的顺序
     * 注意最久的缓存key是在map的最前面 最近最长使用的key是在map的最后面
     *
     * @param size
     */
    public void setSize(final int size) {
        keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
            private static final long serialVersionUID = 4267176411845948333L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
                boolean tooBig = size() > size;
                if (tooBig) {
                    eldestKey = eldest.getKey();
                }
                return tooBig;
            }
        };
    }

    /**
     * 存放缓存
     *
     * @param key   Can be any object but usually it is a {@link CacheKey}
     * @param value The result of a select.
     */
    @Override
    public void putObject(Object key, Object value) {
        delegate.putObject(key, value);
        cycleKeyList(key);
    }

    /**
     * 获取缓存
     *
     * @param key The key
     * @return
     */
    @Override
    public Object getObject(Object key) {
        keyMap.get(key); //touch
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
        keyMap.clear();
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
     * 缓存缓存key集合
     *
     * @param key
     */
    private void cycleKeyList(Object key) {
        //往缓存key 中存入值  如果最久的缓存key 不能null 那么删除这个最久的缓存
        keyMap.put(key, key);
        if (eldestKey != null) {
            delegate.removeObject(eldestKey);
            eldestKey = null;
        }
    }

}
