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
package org.apache.ibatis.executor.loader.cglib;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.ibatis.executor.loader.AbstractEnhancedDeserializationProxy;
import org.apache.ibatis.executor.loader.AbstractSerialStateHolder;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.loader.ResultLoaderMap;
import org.apache.ibatis.executor.loader.WriteReplaceInterface;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyCopier;
import org.apache.ibatis.reflection.property.PropertyNamer;
import org.apache.ibatis.session.Configuration;

/**
 * cglib代理工厂
 *
 * @author Clinton Begin
 */
public class CglibProxyFactory implements ProxyFactory {

    /**
     * 日志
     */
    private static final Log log = LogFactory.getLog(CglibProxyFactory.class);

    /**
     * finalize方法
     */
    private static final String FINALIZE_METHOD = "finalize";

    /**
     * writeReplace方法
     */
    private static final String WRITE_REPLACE_METHOD = "writeReplace";

    /**
     * 构造函数
     */
    public CglibProxyFactory() {
        try {
            Resources.classForName("net.sf.cglib.proxy.Enhancer");
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Cannot enable lazy loading because CGLIB is not available. Add CGLIB to your classpath.", e);
        }
    }

    @Override
    public Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration,
            ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        return EnhancedResultObjectProxyImpl
                .createProxy(target, lazyLoader, configuration, objectFactory, constructorArgTypes, constructorArgs);
    }

    public Object createDeserializationProxy(Object target, Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
            ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        return EnhancedDeserializationProxyImpl
                .createProxy(target, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
    }

    @Override
    public void setProperties(Properties properties) {
        // Not Implemented
    }

    /**
     * 创建代理对象
     *
     * @param type                目标对象class类
     * @param callback            EnhancedResultObjectProxyImpl对象
     * @param constructorArgTypes 目标对象构造参数类型集合
     * @param constructorArgs     目标对象构造参数集合
     * @return
     */
    static Object crateProxy(Class<?> type, Callback callback, List<Class<?>> constructorArgTypes,
            List<Object> constructorArgs) {
        //创建增强
        Enhancer enhancer = new Enhancer();
        //设置回调
        enhancer.setCallback(callback);
        //设置父类class
        enhancer.setSuperclass(type);
        try {
            //设置读写方法
            type.getDeclaredMethod(WRITE_REPLACE_METHOD);
            // ObjectOutputStream will call writeReplace of objects returned by writeReplace
            if (log.isDebugEnabled()) {
                log.debug(WRITE_REPLACE_METHOD + " method was found on bean " + type + ", make sure it returns this");
            }
        } catch (NoSuchMethodException e) {
            //如果没有这个方法 那么就加上这个接口
            enhancer.setInterfaces(new Class[] { WriteReplaceInterface.class });
        } catch (SecurityException e) {
            // nothing to do here
        }
        Object enhanced;
        //如果构造参数类型为空 那么直接创建增强
        if (constructorArgTypes.isEmpty()) {
            enhanced = enhancer.create();
        }
        //否则将构造参数类型、构造参数转换成数组 然后创建
        else {
            Class<?>[] typesArray = constructorArgTypes.toArray(new Class[constructorArgTypes.size()]);
            Object[] valuesArray = constructorArgs.toArray(new Object[constructorArgs.size()]);
            enhanced = enhancer.create(typesArray, valuesArray);
        }
        return enhanced;
    }

    /**
     * 增强结果对象代理实现类
     * 内部类实现了cglib的方法拦截器
     */
    private static class EnhancedResultObjectProxyImpl implements MethodInterceptor {

        /**
         * 目标代理对象
         */
        private final Class<?> type;
        private final ResultLoaderMap lazyLoader;

        /**
         * 是否按需加载
         */
        private final boolean aggressive;

        /**
         * 懒加载跳过的方法集合
         */
        private final Set<String> lazyLoadTriggerMethods;

        /**
         * 对象工厂 创建对象用
         */
        private final ObjectFactory objectFactory;

        /**
         * 构造参数类型集合
         */
        private final List<Class<?>> constructorArgTypes;

        /**
         * 构造参数集合
         */
        private final List<Object> constructorArgs;

        /**
         * 构造函数
         *
         * @param type
         * @param lazyLoader
         * @param configuration
         * @param objectFactory
         * @param constructorArgTypes
         * @param constructorArgs
         */
        private EnhancedResultObjectProxyImpl(Class<?> type, ResultLoaderMap lazyLoader, Configuration configuration,
                ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
            this.type = type;
            this.lazyLoader = lazyLoader;
            this.aggressive = configuration.isAggressiveLazyLoading();
            this.lazyLoadTriggerMethods = configuration.getLazyLoadTriggerMethods();
            this.objectFactory = objectFactory;
            this.constructorArgTypes = constructorArgTypes;
            this.constructorArgs = constructorArgs;
        }

        /**
         * 创建代理对象
         *
         * @param target
         * @param lazyLoader
         * @param configuration
         * @param objectFactory
         * @param constructorArgTypes
         * @param constructorArgs
         * @return
         */
        public static Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration,
                ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
            final Class<?> type = target.getClass();
            EnhancedResultObjectProxyImpl callback = new EnhancedResultObjectProxyImpl(type, lazyLoader, configuration,
                    objectFactory, constructorArgTypes, constructorArgs);
            //创建代理对象
            Object enhanced = crateProxy(type, callback, constructorArgTypes, constructorArgs);
            //拷贝源对象的属性到增强对象中
            PropertyCopier.copyBeanProperties(type, target, enhanced);
            return enhanced;
        }

        @Override
        public Object intercept(Object enhanced, Method method, Object[] args, MethodProxy methodProxy)
                throws Throwable {
            final String methodName = method.getName();
            try {
                synchronized (lazyLoader) {
                    if (WRITE_REPLACE_METHOD.equals(methodName)) {
                        Object original;
                        if (constructorArgTypes.isEmpty()) {
                            original = objectFactory.create(type);
                        } else {
                            original = objectFactory.create(type, constructorArgTypes, constructorArgs);
                        }
                        PropertyCopier.copyBeanProperties(type, enhanced, original);
                        if (lazyLoader.size() > 0) {
                            return new CglibSerialStateHolder(original, lazyLoader.getProperties(), objectFactory,
                                    constructorArgTypes, constructorArgs);
                        } else {
                            return original;
                        }
                    } else {
                        if (lazyLoader.size() > 0 && !FINALIZE_METHOD.equals(methodName)) {
                            if (aggressive || lazyLoadTriggerMethods.contains(methodName)) {
                                lazyLoader.loadAll();
                            } else if (PropertyNamer.isSetter(methodName)) {
                                final String property = PropertyNamer.methodToProperty(methodName);
                                lazyLoader.remove(property);
                            } else if (PropertyNamer.isGetter(methodName)) {
                                final String property = PropertyNamer.methodToProperty(methodName);
                                if (lazyLoader.hasLoader(property)) {
                                    lazyLoader.load(property);
                                }
                            }
                        }
                    }
                }
                return methodProxy.invokeSuper(enhanced, args);
            } catch (Throwable t) {
                throw ExceptionUtil.unwrapThrowable(t);
            }
        }
    }

    /**
     * 增强的反序列化代理实现类
     */
    private static class EnhancedDeserializationProxyImpl extends AbstractEnhancedDeserializationProxy
            implements MethodInterceptor {

        private EnhancedDeserializationProxyImpl(Class<?> type,
                Map<String, ResultLoaderMap.LoadPair> unloadedProperties, ObjectFactory objectFactory,
                List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
            super(type, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
        }

        public static Object createProxy(Object target, Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
                ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
            final Class<?> type = target.getClass();
            EnhancedDeserializationProxyImpl callback = new EnhancedDeserializationProxyImpl(type, unloadedProperties,
                    objectFactory, constructorArgTypes, constructorArgs);
            Object enhanced = crateProxy(type, callback, constructorArgTypes, constructorArgs);
            PropertyCopier.copyBeanProperties(type, target, enhanced);
            return enhanced;
        }

        @Override
        public Object intercept(Object enhanced, Method method, Object[] args, MethodProxy methodProxy)
                throws Throwable {
            final Object o = super.invoke(enhanced, method, args);
            return o instanceof AbstractSerialStateHolder ? o : methodProxy.invokeSuper(o, args);
        }

        @Override
        protected AbstractSerialStateHolder newSerialStateHolder(Object userBean,
                Map<String, ResultLoaderMap.LoadPair> unloadedProperties, ObjectFactory objectFactory,
                List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
            return new CglibSerialStateHolder(userBean, unloadedProperties, objectFactory, constructorArgTypes,
                    constructorArgs);
        }
    }
}
