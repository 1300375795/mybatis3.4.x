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
package org.apache.ibatis.executor.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Section;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

/**
 * 测试允许序列化的代理
 */
public abstract class SerializableProxyTest {

    protected Author author = new Author(999, "someone", "!@#@!#!@#", "someone@somewhere.com", "blah", Section.NEWS);

    /**
     * 代理工厂
     */
    protected ProxyFactory proxyFactory;

    /**
     * 测试一般类型
     *
     * @throws Exception
     */
    @Test
    public void shouldKeepGenericTypes() throws Exception {
        for (int i = 0; i < 10000; i++) {
            Author pc = new Author();
            Author proxy = (Author) proxyFactory
                    .createProxy(pc, new ResultLoaderMap(), new Configuration(), new DefaultObjectFactory(),
                            new ArrayList<Class<?>>(), new ArrayList<Object>());
            proxy.getBio();
        }
    }

    /**
     * 测试通过默认构造函数进行代理
     * 并将这个代理后的对象进行序列化反序列化
     * 这个时候两个的值要相同
     *
     * @throws Exception
     */
    @Test
    public void shouldSerializeAProxyForABeanWithDefaultConstructor() throws Exception {
        Object proxy = proxyFactory
                .createProxy(author, new ResultLoaderMap(), new Configuration(), new DefaultObjectFactory(),
                        new ArrayList<Class<?>>(), new ArrayList<Object>());
        Object proxy2 = deserialize(serialize((Serializable) proxy));
        assertEquals(author, proxy2);
    }

    /**
     * 测试基于带参数的构造函数进行代理
     * 然后代理对象进行序列化反序列化后
     * 两个的值要相同
     *
     * @throws Exception
     */
    @Test
    public void shouldSerializeAProxyForABeanWithoutDefaultConstructor() throws Exception {
        AuthorWithoutDefaultConstructor author = new AuthorWithoutDefaultConstructor(999, "someone", "!@#@!#!@#",
                "someone@somewhere.com", "blah", Section.NEWS);
        ArrayList<Class<?>> argTypes = new ArrayList<Class<?>>();
        argTypes.add(Integer.class);
        argTypes.add(String.class);
        argTypes.add(String.class);
        argTypes.add(String.class);
        argTypes.add(String.class);
        argTypes.add(Section.class);
        ArrayList<Object> argValues = new ArrayList<Object>();
        argValues.add(999);
        argValues.add("someone");
        argValues.add("!@#@!#!@#");
        argValues.add("someone@somewhere.com");
        argValues.add("blah");
        argValues.add(Section.NEWS);
        Object proxy = proxyFactory
                .createProxy(author, new ResultLoaderMap(), new Configuration(), new DefaultObjectFactory(), argTypes,
                        argValues);
        Object proxy2 = deserialize(serialize((Serializable) proxy));
        assertEquals(author, proxy2);
    }

    /**
     * 测试只有带参数构造函数
     * 进行增强后然后序列化反序列化
     * 两个的值应该相同
     *
     * @throws Exception
     */
    @Test
    public void shouldSerializeAProxyForABeanWithoutDefaultConstructorAndUnloadedProperties() throws Exception {
        AuthorWithoutDefaultConstructor author = new AuthorWithoutDefaultConstructor(999, "someone", "!@#@!#!@#",
                "someone@somewhere.com", "blah", Section.NEWS);
        ArrayList<Class<?>> argTypes = new ArrayList<Class<?>>();
        argTypes.add(Integer.class);
        argTypes.add(String.class);
        argTypes.add(String.class);
        argTypes.add(String.class);
        argTypes.add(String.class);
        argTypes.add(Section.class);
        ArrayList<Object> argValues = new ArrayList<Object>();
        argValues.add(999);
        argValues.add("someone");
        argValues.add("!@#@!#!@#");
        argValues.add("someone@somewhere.com");
        argValues.add("blah");
        argValues.add(Section.NEWS);
        ResultLoaderMap loader = new ResultLoaderMap();
        loader.addLoader("id", null, null);
        Object proxy = proxyFactory
                .createProxy(author, loader, new Configuration(), new DefaultObjectFactory(), argTypes, argValues);
        Object proxy2 = deserialize(serialize((Serializable) proxy));
        assertEquals(author, proxy2);
    }

    /**
     * 测试应该序列化全部的属性过去
     *
     * @throws Exception
     */
    @Test
    public void shouldSerizaliceAFullLoadedObjectToOriginalClass() throws Exception {
        Object proxy = proxyFactory
                .createProxy(author, new ResultLoaderMap(), new Configuration(), new DefaultObjectFactory(),
                        new ArrayList<Class<?>>(), new ArrayList<Object>());
        Object proxy2 = deserialize(serialize((Serializable) proxy));
        assertEquals(author.getClass(), proxy2.getClass());
    }

    /**
     * 测试一开始没有writeReplace方法 后面加上了接口后会有这个方法
     *
     * @throws Exception
     */
    @Test
    public void shouldGenerateWriteReplace() throws Exception {
        try {
            author.getClass().getDeclaredMethod("writeReplace");
            fail("Author should not have a writeReplace method");
        } catch (NoSuchMethodException e) {
            System.out.println("一开始是没有这个方法的");
            // ok
        }
        Object proxy = proxyFactory
                .createProxy(author, new ResultLoaderMap(), new Configuration(), new DefaultObjectFactory(),
                        new ArrayList<Class<?>>(), new ArrayList<Object>());
        Method m = proxy.getClass().getDeclaredMethod("writeReplace");
    }

    /**
     * 测试存在writeReplace方法的对象 不会在interfaces里面额外加上WriteReplaceInterface接口
     *
     * @throws Exception
     */
    @Test
    public void shouldNotGenerateWriteReplaceItThereIsAlreadyOne() throws Exception {
        AuthorWithWriteReplaceMethod beanWithWriteReplace = new AuthorWithWriteReplaceMethod(999, "someone",
                "!@#@!#!@#", "someone@somewhere.com", "blah", Section.NEWS);
        try {
            beanWithWriteReplace.getClass().getDeclaredMethod("writeReplace");
        } catch (NoSuchMethodException e) {
            fail("Bean should declare a writeReplace method");
        }
        Object proxy = proxyFactory.createProxy(beanWithWriteReplace, new ResultLoaderMap(), new Configuration(),
                new DefaultObjectFactory(), new ArrayList<Class<?>>(), new ArrayList<Object>());
        Class<?>[] interfaces = proxy.getClass().getInterfaces();
        boolean ownInterfaceFound = false;
        for (Class<?> i : interfaces) {
            if (i.equals(WriteReplaceInterface.class)) {
                ownInterfaceFound = true;
                break;
            }
        }
        assertFalse(ownInterfaceFound);
    }

    /**
     * 测试不会为全量加载的bean创建代理
     *
     * @throws Exception
     */
    @Test
    public void shouldNotCreateAProxyForAFullyLoadedBean() throws Exception {
        Object proxy = proxyFactory
                .createProxy(author, new ResultLoaderMap(), new Configuration(), new DefaultObjectFactory(),
                        new ArrayList<Class<?>>(), new ArrayList<Object>());
        Class<?> aClass = proxy.getClass();
        System.out.println(aClass);
        Author author2 = (Author) deserialize(serialize((Serializable) proxy));
        assertEquals(author.getClass(), author2.getClass());
    }

    @Test(expected = ExecutorException.class)
    public void shouldNotLetReadUnloadedPropertyAfterSerialization() throws Exception {
        ResultLoaderMap loader = new ResultLoaderMap();
        loader.addLoader("id", null, null);
        Object proxy = proxyFactory
                .createProxy(author, loader, new Configuration(), new DefaultObjectFactory(), new ArrayList<Class<?>>(),
                        new ArrayList<Object>());
        Author author2 = (Author) deserialize(serialize((Serializable) proxy));
        author2.getId();
    }

    @Test(expected = ExecutorException.class)
    public void shouldNotLetReadUnloadedPropertyAfterTwoSerializations() throws Exception {
        ResultLoaderMap loader = new ResultLoaderMap();
        loader.addLoader("id", null, null);
        Object proxy = proxyFactory
                .createProxy(author, loader, new Configuration(), new DefaultObjectFactory(), new ArrayList<Class<?>>(),
                        new ArrayList<Object>());
        Author author2 = (Author) deserialize(serialize(deserialize(serialize((Serializable) proxy))));
        author2.getId();
    }

    @Test
    public void shouldLetReadALoadedPropertyAfterSerialization() throws Exception {
        Object proxy = proxyFactory
                .createProxy(author, new ResultLoaderMap(), new Configuration(), new DefaultObjectFactory(),
                        new ArrayList<Class<?>>(), new ArrayList<Object>());
        byte[] ser = serialize((Serializable) proxy);
        Author author2 = (Author) deserialize(ser);
        assertEquals(999, author2.getId());
    }

    /**
     * 序列化
     *
     * @param value
     * @return
     * @throws Exception
     */
    protected byte[] serialize(Serializable value) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(value);
        oos.flush();
        oos.close();
        return bos.toByteArray();
    }

    /**
     * 反序列化
     *
     * @param value
     * @return
     * @throws Exception
     */
    protected Serializable deserialize(byte[] value) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(value);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Serializable result = (Serializable) ois.readObject();
        ois.close();
        return result;
    }

    /**
     * 同时存在默认构造函数以及带参数的构造函数
     * 并且有writeReplace方法
     */
    public static class AuthorWithWriteReplaceMethod extends Author {

        public AuthorWithWriteReplaceMethod() {
        }

        public AuthorWithWriteReplaceMethod(Integer id, String username, String password, String email, String bio,
                Section section) {
            super(id, username, password, email, bio, section);
        }

        protected Object writeReplace() throws ObjectStreamException {
            return this;
        }
    }

    /**
     * 不通过默认构造函数的Author类 并且有writeReplace方法
     */
    public static class AuthorWithoutDefaultConstructor extends Author {

        public AuthorWithoutDefaultConstructor(Integer id, String username, String password, String email, String bio,
                Section section) {
            super(id, username, password, email, bio, section);
        }

        protected Object writeReplace() throws ObjectStreamException {
            return this;
        }
    }

}
