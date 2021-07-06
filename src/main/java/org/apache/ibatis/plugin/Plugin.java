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
package org.apache.ibatis.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * 插件实现了执行处理器
 *
 * @author Clinton Begin
 */
public class Plugin implements InvocationHandler {

    /**
     * 包装的目标对象 最低层的包装是statementHandler 然后后面那一层都是包装包的插件对象
     */
    private final Object target;

    /**
     * 被包装的插件
     */
    private final Interceptor interceptor;

    /**
     * 这个插件实现类对应的插件作用签名对象map
     */
    private final Map<Class<?>, Set<Method>> signatureMap;

    /**
     * 私有构造函数
     *
     * @param target
     * @param interceptor
     * @param signatureMap
     */
    private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
        this.target = target;
        this.interceptor = interceptor;
        this.signatureMap = signatureMap;
    }

    /**
     * 将插件进行包装
     *
     * @param target      被包装的目标对象
     * @param interceptor 当前插件
     * @return
     */
    public static Object wrap(Object target, Interceptor interceptor) {
        //获取这个插件作用的签名map
        Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
        Class<?> type = target.getClass();
        Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
        //如果target有接口 那么进行代理
        if (interfaces.length > 0) {
            return Proxy
                    .newProxyInstance(type.getClassLoader(), interfaces, new Plugin(target, interceptor, signatureMap));
        }
        return target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            Set<Method> methods = signatureMap.get(method.getDeclaringClass());
            if (methods != null && methods.contains(method)) {
                return interceptor.intercept(new Invocation(target, method, args));
            }
            return method.invoke(target, args);
        } catch (Exception e) {
            throw ExceptionUtil.unwrapThrowable(e);
        }
    }

    /**
     * 获取签名map
     *
     * @param interceptor
     * @return
     */
    private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
        //获取这个插件的作用目标
        Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
        // issue #251 一个插件对象不能没有作用目标注解
        if (interceptsAnnotation == null) {
            throw new PluginException(
                    "No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
        }
        //获取插件的所有目标签名数组
        Signature[] sigs = interceptsAnnotation.value();
        //处理所有的目标签名数组 key是类对象class  value为这个类里面的对应的方法
        Map<Class<?>, Set<Method>> signatureMap = new HashMap<Class<?>, Set<Method>>();
        for (Signature sig : sigs) {
            Set<Method> methods = signatureMap.get(sig.type());
            if (methods == null) {
                methods = new HashSet<Method>();
                signatureMap.put(sig.type(), methods);
            }
            try {
                Method method = sig.type().getMethod(sig.method(), sig.args());
                methods.add(method);
            } catch (NoSuchMethodException e) {
                throw new PluginException(
                        "Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
            }
        }
        return signatureMap;
    }

    /**
     * 获取这个target的所有的接口
     *
     * @param type
     * @param signatureMap
     * @return
     */
    private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        while (type != null) {
            for (Class<?> c : type.getInterfaces()) {
                if (signatureMap.containsKey(c)) {
                    interfaces.add(c);
                }
            }
            type = type.getSuperclass();
        }
        return interfaces.toArray(new Class<?>[interfaces.size()]);
    }

}
