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
package org.apache.ibatis.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSession;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * 映射代理工厂
 *
 * @author Lasse Voss
 */
public class MapperProxyFactory<T> {

    /**
     * 映射接口class
     */
    private final Class<T> mapperInterface;

    /**
     * 方法跟方法对应的映射方法map
     */
    private final Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<Method, MapperMethod>();

    /**
     * 构造函数
     *
     * @param mapperInterface
     */
    public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    /**
     * 拿到源mapper接口
     *
     * @return
     */
    public Class<T> getMapperInterface() {
        return mapperInterface;
    }

    /**
     * 拿到方法的缓存map
     *
     * @return
     */
    public Map<Method, MapperMethod> getMethodCache() {
        return methodCache;
    }

    /**
     * 创建映射代理实例
     *
     * @param mapperProxy
     * @return
     */
    @SuppressWarnings("unchecked")
    protected T newInstance(MapperProxy<T> mapperProxy) {
        return (T) Proxy
                .newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
    }

    /**
     * 根据sqlSession创建映射代理实例
     *
     * @param sqlSession
     * @return
     */
    public T newInstance(SqlSession sqlSession) {
        /**
         * 创建代理
         */
        final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
        return newInstance(mapperProxy);
    }

}
