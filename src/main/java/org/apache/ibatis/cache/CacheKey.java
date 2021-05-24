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
package org.apache.ibatis.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.reflection.ArrayUtil;

/**
 * 缓存键
 *
 * @author Clinton Begin
 */
public class CacheKey implements Cloneable, Serializable {

    private static final long serialVersionUID = 1146682552656046210L;

    /**
     * 空对象缓存key
     */
    public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();

    /**
     * 默认的乘数
     */
    private static final int DEFAULT_MULTIPLYER = 37;

    /**
     * 默认的hashCode
     */
    private static final int DEFAULT_HASHCODE = 17;

    /**
     * 乘数
     */
    private final int multiplier;

    /**
     * 这个缓存键的hash
     */
    private int hashcode;

    /**
     * 基于baseHashCode加起来得到
     */
    private long checksum;

    /**
     * 总共计算了多少次hash
     */
    private int count;
    // 8/21/2017 - Sonarlint flags this as needing to be marked transient.  While true if content is not serializable, this is not always true and thus should not be marked transient.

    /**
     * 用来计算hash的对象的集合
     */
    private List<Object> updateList;

    /**
     * 构造函数
     */
    public CacheKey() {
        this.hashcode = DEFAULT_HASHCODE;
        this.multiplier = DEFAULT_MULTIPLYER;
        this.count = 0;
        this.updateList = new ArrayList<Object>();
    }

    /**
     * 构造函数
     *
     * @param objects
     */
    public CacheKey(Object[] objects) {
        this();
        updateAll(objects);
    }

    /**
     * 获取总共计算了多少个对象
     *
     * @return
     */
    public int getUpdateCount() {
        return updateList.size();
    }

    /**
     * 根据每次给出的内容计算hashCode
     * 同时将给出的object加到更新集合中
     * // TODO: 2021/5/24 CallYeDeGuo 这个算法有什么讲究吗
     *
     * @param object
     */
    public void update(Object object) {
        int baseHashCode = object == null ? 1 : ArrayUtil.hashCode(object);

        count++;
        checksum += baseHashCode;
        baseHashCode *= count;

        hashcode = multiplier * hashcode + baseHashCode;

        updateList.add(object);
    }

    /**
     * 根据给出的集合计算hash
     *
     * @param objects
     */
    public void updateAll(Object[] objects) {
        for (Object o : objects) {
            update(o);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CacheKey)) {
            return false;
        }

        final CacheKey cacheKey = (CacheKey) object;

        if (hashcode != cacheKey.hashcode) {
            return false;
        }
        if (checksum != cacheKey.checksum) {
            return false;
        }
        if (count != cacheKey.count) {
            return false;
        }

        for (int i = 0; i < updateList.size(); i++) {
            Object thisObject = updateList.get(i);
            Object thatObject = cacheKey.updateList.get(i);
            if (!ArrayUtil.equals(thisObject, thatObject)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String toString() {
        StringBuilder returnValue = new StringBuilder().append(hashcode).append(':').append(checksum);
        for (Object object : updateList) {
            returnValue.append(':').append(ArrayUtil.toString(object));
        }
        return returnValue.toString();
    }

    @Override
    public CacheKey clone() throws CloneNotSupportedException {
        CacheKey clonedCacheKey = (CacheKey) super.clone();
        clonedCacheKey.updateList = new ArrayList<Object>(updateList);
        return clonedCacheKey;
    }

}
