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
package org.apache.ibatis.binding;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.ibatis.lang.UsesJava7;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

/**
 * 映射代理
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

    private static final long serialVersionUID = -6424540398559729838L;

    /**
     * sql会话
     */
    private final SqlSession sqlSession;

    /**
     * 源映射接口
     */
    private final Class<T> mapperInterface;

    /**
     * 方法跟映射方法的map
     */
    private final Map<Method, MapperMethod> methodCache;

    /**
     * 构造函数
     *
     * @param sqlSession
     * @param mapperInterface
     * @param methodCache
     */
    public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
    }

    /**
     * 被代理接口执行方法时实际会执行这个方法
     *
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            // TODO: 2021/3/25 CallYeDeGuo 在调用映射方法的时候会执行这个代理
            //如果这个方法是Object的方法 直接执行
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);
            }
            //如果是默认的方法
            else if (isDefaultMethod(method)) {
                //执行默认的方法
                return invokeDefaultMethod(proxy, method, args);
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
        //其他情况 在执行的时候缓存相关的映射方法
        final MapperMethod mapperMethod = cachedMapperMethod(method);
        //执行这个方法
        return mapperMethod.execute(sqlSession, args);
    }

    /**
     * 缓存mapper对应的方法
     * 如果这个method不存在在缓存中
     * 那么创建这个method对应的mapperMethod
     * 并返回这个mapperMethod
     *
     * @param method
     * @return
     */
    private MapperMethod cachedMapperMethod(Method method) {
        MapperMethod mapperMethod = methodCache.get(method);
        if (mapperMethod == null) {
            mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
            methodCache.put(method, mapperMethod);
        }
        return mapperMethod;
    }

    /**
     * 执行默认的方法
     *
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @UsesJava7
    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                .getDeclaredConstructor(Class.class, int.class);
        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        final Class<?> declaringClass = method.getDeclaringClass();
        return constructor.newInstance(declaringClass,
                MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED | MethodHandles.Lookup.PACKAGE
                        | MethodHandles.Lookup.PUBLIC).unreflectSpecial(method, declaringClass).bindTo(proxy)
                .invokeWithArguments(args);
    }

    /**
     * Backport of java.lang.reflect.Method#isDefault()
     * 按位与运算符（&）
     * 按位或运算符（|）
     * 取反运算符（~）
     * 异或运算符（^）
     * 是否默认方法
     */
    private boolean isDefaultMethod(Method method) {
        return (method.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC
                && method.getDeclaringClass().isInterface();
    }
}
