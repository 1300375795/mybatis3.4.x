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

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 定时clear缓存装饰类
 *
 * @author Clinton Begin
 */
public class ScheduledCache implements Cache {

    /**
     * 被装饰对象
     */
    private final Cache delegate;

    /**
     * 清理缓存周期
     */
    protected long clearInterval;

    /**
     * 最近一次清理缓存的时间
     */
    protected long lastClear;

    /**
     * 构造函数 默认1小时
     *
     * @param delegate
     */
    public ScheduledCache(Cache delegate) {
        this.delegate = delegate;
        this.clearInterval = 60 * 60 * 1000; // 1 hour
        this.lastClear = System.currentTimeMillis();
    }

    /**
     * 设置清理周期
     *
     * @param clearInterval
     */
    public void setClearInterval(long clearInterval) {
        this.clearInterval = clearInterval;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    /**
     * 获取缓存大小
     *
     * @return
     */
    @Override
    public int getSize() {
        clearWhenStale();
        return delegate.getSize();
    }

    /**
     * 存入缓存
     *
     * @param key    Can be any object but usually it is a {@link CacheKey}
     * @param object
     */
    @Override
    public void putObject(Object key, Object object) {
        clearWhenStale();
        delegate.putObject(key, object);
    }

    /**
     * 获取缓存
     *
     * @param key The key
     * @return
     */
    @Override
    public Object getObject(Object key) {
        return clearWhenStale() ? null : delegate.getObject(key);
    }

    /**
     * 删除缓存
     *
     * @param key The key
     * @return
     */
    @Override
    public Object removeObject(Object key) {
        clearWhenStale();
        return delegate.removeObject(key);
    }

    /**
     * 清理缓存
     */
    @Override
    public void clear() {
        lastClear = System.currentTimeMillis();
        delegate.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    /**
     * 当缓存超过了过期时间 那么需要进行清理
     *
     * @return
     */
    private boolean clearWhenStale() {
        //如果当前时间-上一次清理的时间大于清理周期 那么进行clear操作
        if (System.currentTimeMillis() - lastClear > clearInterval) {
            clear();
            return true;
        }
        return false;
    }

}
