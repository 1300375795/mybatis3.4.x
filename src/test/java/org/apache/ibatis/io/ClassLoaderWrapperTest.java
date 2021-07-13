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
package org.apache.ibatis.io;

import org.apache.ibatis.BaseDataTest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

/**
 * 测试类加载器加载资源
 */
public class ClassLoaderWrapperTest extends BaseDataTest {

    ClassLoaderWrapper wrapper;
    ClassLoader loader;
    private final String RESOURCE_NOT_FOUND = "some_resource_that_does_not_exist.properties";
    private final String CLASS_NOT_FOUND = "some.random.class.that.does.not.Exist";
    private final String CLASS_FOUND = "java.lang.Object";

    @Before
    public void beforeClassLoaderWrapperTest() {
        wrapper = new ClassLoaderWrapper();
        loader = getClass().getClassLoader();
    }

    /**
     * 测试根据类名进行加载
     *
     * @throws ClassNotFoundException
     */
    @Test
    public void classForName() throws ClassNotFoundException {
        assertNotNull(wrapper.classForName(CLASS_FOUND));
    }

    /**
     * 测试给出一个没有的类 需要抛出异常
     *
     * @throws ClassNotFoundException
     */
    @Test(expected = ClassNotFoundException.class)
    public void classForNameNotFound() throws ClassNotFoundException {
        assertNotNull(wrapper.classForName(CLASS_NOT_FOUND));
    }

    /**
     * 测试给出了默认的类加载器加载一个存在的类
     *
     * @throws ClassNotFoundException
     */
    @Test
    public void classForNameWithClassLoader() throws ClassNotFoundException {
        assertNotNull(wrapper.classForName(CLASS_FOUND, loader));
    }

    /**
     * 测试加载以url方式加载资源
     */
    @Test
    public void getResourceAsURL() {
        assertNotNull(wrapper.getResourceAsURL(JPETSTORE_PROPERTIES));
    }

    /**
     * 测试以url方式加载资源 没有加载到的话 返回null
     */
    @Test
    public void getResourceAsURLNotFound() {
        assertNull(wrapper.getResourceAsURL(RESOURCE_NOT_FOUND));
    }

    /**
     * 测试给出指定的类加载器加载url资源
     */
    @Test
    public void getResourceAsURLWithClassLoader() {
        assertNotNull(wrapper.getResourceAsURL(JPETSTORE_PROPERTIES, loader));
    }

    /**
     * 测试以资源流的方式加载资源
     */
    @Test
    public void getResourceAsStream() {
        assertNotNull(wrapper.getResourceAsStream(JPETSTORE_PROPERTIES));
    }

    /**
     * 测试以资源流的方式加载不存在的资源 返回null
     */
    @Test
    public void getResourceAsStreamNotFound() {
        assertNull(wrapper.getResourceAsStream(RESOURCE_NOT_FOUND));
    }

    /**
     * 测试给出自定的类加载器加载资源流
     */
    @Test
    public void getResourceAsStreamWithClassLoader() {
        assertNotNull(wrapper.getResourceAsStream(JPETSTORE_PROPERTIES, loader));
    }

}
