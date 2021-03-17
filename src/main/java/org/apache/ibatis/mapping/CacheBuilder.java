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
package org.apache.ibatis.mapping;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.builder.InitializingObject;
import org.apache.ibatis.cache.decorators.BlockingCache;
import org.apache.ibatis.cache.decorators.LoggingCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.ScheduledCache;
import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.SynchronizedCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * 缓存构造器
 *
 * @author Clinton Begin
 */
public class CacheBuilder {

    /**
     * 缓存构造器的id  默认是nameSpace 命名空间
     */
    private final String id;

    /**
     * 缓存实现class
     */
    private Class<? extends Cache> implementation;

    /**
     * 缓存装饰类集合
     */
    private final List<Class<? extends Cache>> decorators;

    /**
     * 缓存大小
     */
    private Integer size;

    /**
     * 清理周期
     */
    private Long clearInterval;

    /**
     * 读写
     */
    private boolean readWrite;

    /**
     * 属性配置
     */
    private Properties properties;

    /**
     * 是否阻断
     */
    private boolean blocking;

    /**
     * 构造函数
     *
     * @param id
     */
    public CacheBuilder(String id) {
        this.id = id;
        this.decorators = new ArrayList<Class<? extends Cache>>();
    }

    /**
     * 设置缓存实现
     *
     * @param implementation
     * @return
     */
    public CacheBuilder implementation(Class<? extends Cache> implementation) {
        this.implementation = implementation;
        return this;
    }

    /**
     * 设置装饰者
     *
     * @param decorator
     * @return
     */
    public CacheBuilder addDecorator(Class<? extends Cache> decorator) {
        if (decorator != null) {
            this.decorators.add(decorator);
        }
        return this;
    }

    /**
     * 设置缓存大小
     *
     * @param size
     * @return
     */
    public CacheBuilder size(Integer size) {
        this.size = size;
        return this;
    }

    /**
     * 设置缓存清理周期
     *
     * @param clearInterval
     * @return
     */
    public CacheBuilder clearInterval(Long clearInterval) {
        this.clearInterval = clearInterval;
        return this;
    }

    /**
     * @param readWrite
     * @return
     */
    public CacheBuilder readWrite(boolean readWrite) {
        this.readWrite = readWrite;
        return this;
    }

    public CacheBuilder blocking(boolean blocking) {
        this.blocking = blocking;
        return this;
    }

    /**
     * 设置属性配置
     *
     * @param properties
     * @return
     */
    public CacheBuilder properties(Properties properties) {
        this.properties = properties;
        return this;
    }

    /**
     * 构建
     *
     * @return
     */
    public Cache build() {
        setDefaultImplementations();
        //构建基础的缓存实现类
        Cache cache = newBaseCacheInstance(implementation, id);
        //设置缓存属性配置
        setCacheProperties(cache);
        // issue #352, do not apply decorators to custom caches
        if (PerpetualCache.class.equals(cache.getClass())) {
            for (Class<? extends Cache> decorator : decorators) {
                cache = newCacheDecoratorInstance(decorator, cache);
                setCacheProperties(cache);
            }
            cache = setStandardDecorators(cache);
        } else if (!LoggingCache.class.isAssignableFrom(cache.getClass())) {
            cache = new LoggingCache(cache);
        }
        return cache;
    }

    /**
     * 设置缓存实现类 如果为null 那么默认为PerpetualCache
     * 如果装饰集合为空 那么设置LruCache为装饰器
     */
    private void setDefaultImplementations() {
        if (implementation == null) {
            implementation = PerpetualCache.class;
            if (decorators.isEmpty()) {
                decorators.add(LruCache.class);
            }
        }
    }

    private Cache setStandardDecorators(Cache cache) {
        try {
            MetaObject metaCache = SystemMetaObject.forObject(cache);
            if (size != null && metaCache.hasSetter("size")) {
                metaCache.setValue("size", size);
            }
            if (clearInterval != null) {
                cache = new ScheduledCache(cache);
                ((ScheduledCache) cache).setClearInterval(clearInterval);
            }
            if (readWrite) {
                cache = new SerializedCache(cache);
            }
            cache = new LoggingCache(cache);
            cache = new SynchronizedCache(cache);
            if (blocking) {
                cache = new BlockingCache(cache);
            }
            return cache;
        } catch (Exception e) {
            throw new CacheException("Error building standard cache decorators.  Cause: " + e, e);
        }
    }

    /**
     * 给基础的缓存实现类设置属性配置
     *
     * @param cache
     */
    private void setCacheProperties(Cache cache) {
        //如果属性不为空 进行设置
        if (properties != null) {
            MetaObject metaCache = SystemMetaObject.forObject(cache);
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String name = (String) entry.getKey();
                String value = (String) entry.getValue();
                if (metaCache.hasSetter(name)) {
                    Class<?> type = metaCache.getSetterType(name);
                    if (String.class == type) {
                        metaCache.setValue(name, value);
                    } else if (int.class == type || Integer.class == type) {
                        metaCache.setValue(name, Integer.valueOf(value));
                    } else if (long.class == type || Long.class == type) {
                        metaCache.setValue(name, Long.valueOf(value));
                    } else if (short.class == type || Short.class == type) {
                        metaCache.setValue(name, Short.valueOf(value));
                    } else if (byte.class == type || Byte.class == type) {
                        metaCache.setValue(name, Byte.valueOf(value));
                    } else if (float.class == type || Float.class == type) {
                        metaCache.setValue(name, Float.valueOf(value));
                    } else if (boolean.class == type || Boolean.class == type) {
                        metaCache.setValue(name, Boolean.valueOf(value));
                    } else if (double.class == type || Double.class == type) {
                        metaCache.setValue(name, Double.valueOf(value));
                    } else {
                        throw new CacheException("Unsupported property type for cache: '" + name + "' of type " + type);
                    }
                }
            }
        }
        if (InitializingObject.class.isAssignableFrom(cache.getClass())) {
            try {
                ((InitializingObject) cache).initialize();
            } catch (Exception e) {
                throw new CacheException(
                        "Failed cache initialization for '" + cache.getId() + "' on '" + cache.getClass().getName()
                                + "'", e);
            }
        }
    }

    /**
     * 创建缓存实现类实例
     *
     * @param cacheClass
     * @param id
     * @return
     */
    private Cache newBaseCacheInstance(Class<? extends Cache> cacheClass, String id) {
        Constructor<? extends Cache> cacheConstructor = getBaseCacheConstructor(cacheClass);
        try {
            return cacheConstructor.newInstance(id);
        } catch (Exception e) {
            throw new CacheException("Could not instantiate cache implementation (" + cacheClass + "). Cause: " + e, e);
        }
    }

    private Constructor<? extends Cache> getBaseCacheConstructor(Class<? extends Cache> cacheClass) {
        try {
            return cacheClass.getConstructor(String.class);
        } catch (Exception e) {
            throw new CacheException("Invalid base cache implementation (" + cacheClass + ").  "
                    + "Base cache implementations must have a constructor that takes a String id as a parameter.  Cause: "
                    + e, e);
        }
    }

    private Cache newCacheDecoratorInstance(Class<? extends Cache> cacheClass, Cache base) {
        Constructor<? extends Cache> cacheConstructor = getCacheDecoratorConstructor(cacheClass);
        try {
            return cacheConstructor.newInstance(base);
        } catch (Exception e) {
            throw new CacheException("Could not instantiate cache decorator (" + cacheClass + "). Cause: " + e, e);
        }
    }

    private Constructor<? extends Cache> getCacheDecoratorConstructor(Class<? extends Cache> cacheClass) {
        try {
            return cacheClass.getConstructor(Cache.class);
        } catch (Exception e) {
            throw new CacheException("Invalid cache decorator (" + cacheClass + ").  "
                    + "Cache decorators must have a constructor that takes a Cache instance as a parameter.  Cause: "
                    + e, e);
        }
    }
}
