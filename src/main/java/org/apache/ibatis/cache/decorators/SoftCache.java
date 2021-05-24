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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 软引用缓存装饰类
 * Soft Reference cache decorator
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 *
 * @author Clinton Begin
 */
public class SoftCache implements Cache {

    /**
     * 强链接避免被垃圾回收的队列
     * 能做到避免被回收是因为将软引用对应的value加到了这个集合中
     * 这样这些value就被强引用关联了
     */
    private final Deque<Object> hardLinksToAvoidGarbageCollection;

    /**
     * 垃圾回收键值对队列--里面的object继承了软应用
     * 如果该软引用的value被回收了 那么会被放在这个队列里面
     */
    private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;

    /**
     * 被装饰对象
     */
    private final Cache delegate;

    /**
     * 强链接数量
     */
    private int numberOfHardLinks;

    /**
     * 构造函数
     *
     * @param delegate
     */
    public SoftCache(Cache delegate) {
        this.delegate = delegate;
        this.numberOfHardLinks = 256;
        this.hardLinksToAvoidGarbageCollection = new LinkedList<Object>();
        this.queueOfGarbageCollectedEntries = new ReferenceQueue<Object>();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        removeGarbageCollectedItems();
        return delegate.getSize();
    }

    public void setSize(int size) {
        this.numberOfHardLinks = size;
    }

    /**
     * 放入缓存
     * 将给出的key跟value包装成软应用对象 然后删除
     *
     * @param key   Can be any object but usually it is a {@link CacheKey}
     * @param value The result of a select.
     */
    @Override
    public void putObject(Object key, Object value) {
        removeGarbageCollectedItems();
        delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
    }

    /**
     * 获取缓存key
     *
     * @param key The key
     * @return
     */
    @Override
    public Object getObject(Object key) {
        Object result = null;
        // 假设委托缓存完全由这个缓存管理
        // assumed delegate cache is totally managed by this cache
        @SuppressWarnings("unchecked")
        //获取缓存
        SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
        //如果不为空
        if (softReference != null) {
            //从软应用中获取被保存的对象
            result = softReference.get();
            //如果是null的话 表示这个软应用的value被回收了 那么需要从缓存中删除掉这个key对应的value
            //可能存在内存不够的时候 软应用被回收了 但是没有执行这个类里面的其他的方法中的removeGarbageCollectedItems
            //那么这个时候就会到这个case 这个时候需要将缓存中的值删除掉
            if (result == null) {
                delegate.removeObject(key);
            }
            //如果还有值 那么表示这个软应用没有被垃圾回收 这个时候需要将这个软应用加到强链接队列中 这样避免这个软引用被删除
            //同时这个强链接集合的大小是有限制的 如果达到最大值了 那么会按照FIFO进行处理  即删除掉队列最尾的数据 然后将新数据加到队头
            else {
                // See #586 (and #335) modifications need more than a read lock
                synchronized (hardLinksToAvoidGarbageCollection) {
                    hardLinksToAvoidGarbageCollection.addFirst(result);
                    if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
                        hardLinksToAvoidGarbageCollection.removeLast();
                    }
                }
            }
        }
        return result;
    }

    /**
     * 删除指定的缓存key
     *
     * @param key The key
     * @return
     */
    @Override
    public Object removeObject(Object key) {
        removeGarbageCollectedItems();
        return delegate.removeObject(key);
    }

    /**
     * 清理缓存
     * 会清除掉强链接集合、软应用垃圾回收队列以及实际的缓存
     */
    @Override
    public void clear() {
        synchronized (hardLinksToAvoidGarbageCollection) {
            hardLinksToAvoidGarbageCollection.clear();
        }
        removeGarbageCollectedItems();
        delegate.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    /**
     * 当软应用被回收的对象队列不为空的时候 那么表示存在被垃圾回收的缓存
     * 那么这个时候需要同时删除掉实际的缓存
     */
    private void removeGarbageCollectedItems() {
        SoftEntry sv;
        while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
            delegate.removeObject(sv.key);
        }
    }

    /**
     * 软应用键值对
     */
    private static class SoftEntry extends SoftReference<Object> {
        private final Object key;

        /**
         * 构造函数
         *
         * @param key                    键
         * @param value                  值
         * @param garbageCollectionQueue 软应用垃圾回收队列 如果软应用对应的value被回收 那么这个软应用会被放入这个队列
         */
        SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
            super(value, garbageCollectionQueue);
            this.key = key;
        }
    }

}