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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.ibatis.BaseDataTest;
import org.junit.Test;

/**
 * 测试资源加载器
 */
public class ResourcesTest extends BaseDataTest {

    private static final ClassLoader CLASS_LOADER = ResourcesTest.class.getClassLoader();

    /**
     * 测试根据url加载资源  并且URL对象的名称结尾跟给出的资源名称一样
     *
     * @throws Exception
     */
    @Test
    public void shouldGetUrlForResource() throws Exception {
        URL url = Resources.getResourceURL(JPETSTORE_PROPERTIES);
        assertTrue(url.toString().endsWith("jpetstore/jpetstore-hsqldb.properties"));
    }

    /**
     * 测试给出默认类加载器加载资源转换成properties
     *
     * @throws Exception
     */
    @Test
    public void shouldGetUrlAsProperties() throws Exception {
        URL url = Resources.getResourceURL(CLASS_LOADER, JPETSTORE_PROPERTIES);
        Properties props = Resources.getUrlAsProperties(url.toString());
        assertNotNull(props.getProperty("driver"));
    }

    /**
     * 测试给出默认的类加载器加载资源
     * 转换成properties 并从中获取某个key
     *
     * @throws Exception
     */
    @Test
    public void shouldGetResourceAsProperties() throws Exception {
        Properties props = Resources.getResourceAsProperties(CLASS_LOADER, JPETSTORE_PROPERTIES);
        assertNotNull(props.getProperty("driver"));
    }

    /**
     * 测试基于url加载资源
     * 基于url资源加载成inputStream
     *
     * @throws Exception
     */
    @Test
    public void shouldGetUrlAsStream() throws Exception {
        URL url = Resources.getResourceURL(CLASS_LOADER, JPETSTORE_PROPERTIES);
        InputStream in = Resources.getUrlAsStream(url.toString());
        assertNotNull(in);
        in.close();
    }

    /**
     * 测试基于url加载资源
     * 基于url资源加载成Reader
     *
     * @throws Exception
     */
    @Test
    public void shouldGetUrlAsReader() throws Exception {
        URL url = Resources.getResourceURL(CLASS_LOADER, JPETSTORE_PROPERTIES);
        Reader in = Resources.getUrlAsReader(url.toString());
        assertNotNull(in);
        in.close();
    }

    /**
     * 测试加载资源为inputStream
     *
     * @throws Exception
     */
    @Test
    public void shouldGetResourceAsStream() throws Exception {
        InputStream in = Resources.getResourceAsStream(CLASS_LOADER, JPETSTORE_PROPERTIES);
        assertNotNull(in);
        in.close();
    }

    /**
     * 测试加载资源为reader
     *
     * @throws Exception
     */
    @Test
    public void shouldGetResourceAsReader() throws Exception {
        Reader in = Resources.getResourceAsReader(CLASS_LOADER, JPETSTORE_PROPERTIES);
        assertNotNull(in);
        in.close();
    }

    /**
     * 测试加载为file
     *
     * @throws Exception
     */
    @Test
    public void shouldGetResourceAsFile() throws Exception {
        File file = Resources.getResourceAsFile(JPETSTORE_PROPERTIES);
        assertTrue(file.getAbsolutePath().replace('\\', '/').endsWith("jpetstore/jpetstore-hsqldb.properties"));
    }

    /**
     * 测试加载资源为file  并且file名称跟资源名称处理后相同
     *
     * @throws Exception
     */
    @Test
    public void shouldGetResourceAsFileWithClassloader() throws Exception {
        File file = Resources.getResourceAsFile(CLASS_LOADER, JPETSTORE_PROPERTIES);
        assertTrue(file.getAbsolutePath().replace('\\', '/').endsWith("jpetstore/jpetstore-hsqldb.properties"));
    }

    /**
     * 测试加载成properties
     *
     * @throws Exception
     */
    @Test
    public void shouldGetResourceAsPropertiesWithOutClassloader() throws Exception {
        Properties file = Resources.getResourceAsProperties(JPETSTORE_PROPERTIES);
        assertNotNull(file);
    }

    /**
     * 测试给出类加载器加载成properties
     *
     * @throws Exception
     */
    @Test
    public void shouldGetResourceAsPropertiesWithClassloader() throws Exception {
        Properties file = Resources.getResourceAsProperties(CLASS_LOADER, JPETSTORE_PROPERTIES);
        assertNotNull(file);
    }

    /**
     * 测试前后获取的类加载器是否相同
     */
    @Test
    public void shouldAllowDefaultClassLoaderToBeSet() {
        Resources.setDefaultClassLoader(this.getClass().getClassLoader());
        assertEquals(this.getClass().getClassLoader(), Resources.getDefaultClassLoader());
    }

    /**
     * 测试前后设置字符集是否相同
     */
    @Test
    public void shouldAllowDefaultCharsetToBeSet() {
        Resources.setCharset(Charset.defaultCharset());
        assertEquals(Charset.defaultCharset(), Resources.getCharset());
    }

    /**
     * 测试基于class名称进行加载 能加载到class对象
     *
     * @throws Exception
     */
    @Test
    public void shouldGetClassForName() throws Exception {
        Class<?> clazz = Resources.classForName(ResourcesTest.class.getName());
        assertNotNull(clazz);
    }

    /**
     * 测试加载不存在的文件抛出异常
     *
     * @throws ClassNotFoundException
     */
    @Test(expected = ClassNotFoundException.class)
    public void shouldNotFindThisClass() throws ClassNotFoundException {
        Resources.classForName("some.random.class.that.does.not.Exist");
    }

    /**
     * 测试给出字符集加载跟没有给出字符集加载
     *
     * @throws IOException
     */
    @Test
    public void shouldGetReader() throws IOException {

        // save the value
        Charset charset = Resources.getCharset();

        // charset
        Resources.setCharset(Charset.forName("US-ASCII"));
        assertNotNull(Resources.getResourceAsReader(JPETSTORE_PROPERTIES));

        // no charset
        Resources.setCharset(null);
        assertNotNull(Resources.getResourceAsReader(JPETSTORE_PROPERTIES));

        // clean up
        Resources.setCharset(charset);

    }

    /**
     * 测试给出字符集根跟没有给出字符集并设置了默认类加载器加载
     *
     * @throws IOException
     */
    @Test
    public void shouldGetReaderWithClassLoader() throws IOException {

        // save the value
        Charset charset = Resources.getCharset();

        // charset
        Resources.setCharset(Charset.forName("US-ASCII"));
        assertNotNull(Resources.getResourceAsReader(getClass().getClassLoader(), JPETSTORE_PROPERTIES));

        // no charset
        Resources.setCharset(null);
        assertNotNull(Resources.getResourceAsReader(getClass().getClassLoader(), JPETSTORE_PROPERTIES));

        // clean up
        Resources.setCharset(charset);

    }

    /**
     * 测试not null
     */
    @Test
    public void stupidJustForCoverage() {
        assertNotNull(new Resources());
    }
}
