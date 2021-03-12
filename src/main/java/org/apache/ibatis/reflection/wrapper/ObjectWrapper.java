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
package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 对象包包装器
 *
 * @author Clinton Begin
 */
public interface ObjectWrapper {

    /**
     * 获取某个属性的值
     *
     * @param prop
     * @return
     */
    Object get(PropertyTokenizer prop);

    /**
     * 设置某个属性的值
     *
     * @param prop
     * @param value
     */
    void set(PropertyTokenizer prop, Object value);

    /**
     * 获取属性
     *
     * @param name
     * @param useCamelCaseMapping 是否驼峰
     * @return
     */
    String findProperty(String name, boolean useCamelCaseMapping);

    /**
     * 获取get、is属性集合
     *
     * @return
     */
    String[] getGetterNames();

    /**
     * 获取set属性集合
     *
     * @return
     */
    String[] getSetterNames();

    /**
     * 获取属性对应的set类型class
     *
     * @param name
     * @return
     */
    Class<?> getSetterType(String name);

    /**
     * 获取属性对应的get属性class
     *
     * @param name
     * @return
     */
    Class<?> getGetterType(String name);

    /**
     * 是否有set属性
     *
     * @param name
     * @return
     */
    boolean hasSetter(String name);

    /**
     * 是否有get属性
     *
     * @param name
     * @return
     */
    boolean hasGetter(String name);

    MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

    /**
     * 是否集合
     *
     * @return
     */
    boolean isCollection();

    /**
     * 添加元素
     *
     * @param element
     */
    void add(Object element);

    /**
     * 添加元素集合
     *
     * @param element
     * @param <E>
     */
    <E> void addAll(List<E> element);

}
