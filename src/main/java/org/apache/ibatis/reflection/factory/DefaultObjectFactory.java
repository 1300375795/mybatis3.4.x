/**
 * Copyright 2009-2018 the original author or authors.
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
package org.apache.ibatis.reflection.factory;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * 默认的对象工厂
 *
 * @author Clinton Begin
 */
public class DefaultObjectFactory implements ObjectFactory, Serializable {

    private static final long serialVersionUID = -8855120656740914948L;

    /**
     * 根据class实例化对象
     *
     * @param type Object type
     * @param <T>
     * @return
     */
    @Override
    public <T> T create(Class<T> type) {
        return create(type, null, null);
    }

    /**
     * 根据给出的参数实例化对象
     *
     * @param type                Object type
     * @param constructorArgTypes Constructor argument types
     * @param constructorArgs     Constructor argument values
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        Class<?> classToCreate = resolveInterface(type);
        // we know types are assignable
        return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
    }

    /**
     * 设置属性配置
     *
     * @param properties configuration properties
     */
    @Override
    public void setProperties(Properties properties) {
        // no props for default
    }

    /**
     * 根据给出的class跟指定的参数类型、参数创建实体化对象
     * 如果创建失败抛出相应的异常
     *
     * @param type
     * @param constructorArgTypes
     * @param constructorArgs
     * @param <T>
     * @return
     */
    private <T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        try {
            Constructor<T> constructor;
            if (constructorArgTypes == null || constructorArgs == null) {
                constructor = type.getDeclaredConstructor();
                if (!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }
                return constructor.newInstance();
            }
            constructor = type
                    .getDeclaredConstructor(constructorArgTypes.toArray(new Class[constructorArgTypes.size()]));
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
        } catch (Exception e) {
            StringBuilder argTypes = new StringBuilder();
            if (constructorArgTypes != null && !constructorArgTypes.isEmpty()) {
                for (Class<?> argType : constructorArgTypes) {
                    argTypes.append(argType.getSimpleName());
                    argTypes.append(",");
                }
                argTypes.deleteCharAt(argTypes.length() - 1); // remove trailing ,
            }
            StringBuilder argValues = new StringBuilder();
            if (constructorArgs != null && !constructorArgs.isEmpty()) {
                for (Object argValue : constructorArgs) {
                    argValues.append(String.valueOf(argValue));
                    argValues.append(",");
                }
                argValues.deleteCharAt(argValues.length() - 1); // remove trailing ,
            }
            throw new ReflectionException(
                    "Error instantiating " + type + " with invalid types (" + argTypes + ") or values (" + argValues
                            + "). Cause: " + e, e);
        }
    }

    /**
     * 根据给出的class的类型转换成相应的class
     * 如给出了List、Collection、Iterable的时候转换成ArrayList
     *
     * @param type
     * @return
     */
    protected Class<?> resolveInterface(Class<?> type) {
        Class<?> classToCreate;
        if (type == List.class || type == Collection.class || type == Iterable.class) {
            classToCreate = ArrayList.class;
        } else if (type == Map.class) {
            classToCreate = HashMap.class;
        } else if (type == SortedSet.class) { // issue #510 Collections Support
            classToCreate = TreeSet.class;
        } else if (type == Set.class) {
            classToCreate = HashSet.class;
        } else {
            classToCreate = type;
        }
        return classToCreate;
    }

    /**
     * 判断是否是集合
     *
     * @param type Object type
     * @param <T>
     * @return
     */
    @Override
    public <T> boolean isCollection(Class<T> type) {
        return Collection.class.isAssignableFrom(type);
    }

}
