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
import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 弱引用缓存装饰者
 * Weak Reference cache decorator.
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 *
 * @author Clinton Begin
 */
public class WeakCache implements Cache {

    /**
     * 强链接避免被垃圾回收的队列
     * 能做到避免被回收是因为将软引用对应的value加到了这个集合中
     * 这样这些value就被强引用关联了
     */
    private final Deque<Object> hardLinksToAvoidGarbageCollection;

    /**
     * 垃圾回收键值对队列--里面的object继承了弱应用
     * 如果该弱引用的value被回收了 那么会被放在这个队列里面
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

    public WeakCache(Cache delegate) {
        // TODO: 2021/5/24 CallYeDeGuo 跟SoftCache类似
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

    @Override
    public void putObject(Object key, Object value) {
        //先进行删除掉已经被jvm回收掉的缓存
        removeGarbageCollectedItems();
        //放入包装后的对象
        delegate.putObject(key, new WeakEntry(key, value, queueOfGarbageCollectedEntries));
    }

    @Override
    public Object getObject(Object key) {
        Object result = null;
        @SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
        WeakReference<Object> weakReference = (WeakReference<Object>) delegate.getObject(key);
        //如果缓存中存在该弱引用 那么从弱引中获取具体的对象
        if (weakReference != null) {
            //如果为null 表示被回收了 那么这个时候也从缓存中删除 否则将这个结果放入强引用队列中 避免被回收
            result = weakReference.get();
            if (result == null) {
                delegate.removeObject(key);
            } else {
                hardLinksToAvoidGarbageCollection.addFirst(result);
                if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
                    hardLinksToAvoidGarbageCollection.removeLast();
                }
            }
        }
        return result;
    }

    @Override
    public Object removeObject(Object key) {
        removeGarbageCollectedItems();
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        hardLinksToAvoidGarbageCollection.clear();
        removeGarbageCollectedItems();
        delegate.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    /**
     * 从弱引用队列中获取被jvm回收的对象 并删除缓存中的该对象
     */
    private void removeGarbageCollectedItems() {
        WeakEntry sv;
        while ((sv = (WeakEntry) queueOfGarbageCollectedEntries.poll()) != null) {
            delegate.removeObject(sv.key);
        }
    }

    /**
     * 弱引用键值对
     */
    private static class WeakEntry extends WeakReference<Object> {
        private final Object key;

        private WeakEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
            super(value, garbageCollectionQueue);
            this.key = key;
        }
    }

}
