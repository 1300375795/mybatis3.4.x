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
package org.apache.ibatis.scripting;

import java.util.HashMap;
import java.util.Map;

/**
 * 语言驱动注册器
 *
 * @author Frank D. Martinez [mnesarco]
 */
public class LanguageDriverRegistry {

    /**
     * 语言驱动class跟实现类map
     */
    private final Map<Class<?>, LanguageDriver> LANGUAGE_DRIVER_MAP = new HashMap<Class<?>, LanguageDriver>();

    /**
     * 默认的语言驱动class
     */
    private Class<?> defaultDriverClass;

    /**
     * 根据语言驱动类的class进行注册
     *
     * @param cls
     */
    public void register(Class<?> cls) {
        if (cls == null) {
            throw new IllegalArgumentException("null is not a valid Language Driver");
        }
        //如果在map中不存在 才会添加到map中 存在了 就忽略
        if (!LANGUAGE_DRIVER_MAP.containsKey(cls)) {
            try {
                LANGUAGE_DRIVER_MAP.put(cls, (LanguageDriver) cls.newInstance());
            } catch (Exception ex) {
                throw new ScriptingException("Failed to load language driver for " + cls.getName(), ex);
            }
        }
    }

    /**
     * 根据语言驱动类的实现类进行注册
     *
     * @param instance
     */
    public void register(LanguageDriver instance) {
        if (instance == null) {
            throw new IllegalArgumentException("null is not a valid Language Driver");
        }
        //拿到这个实现类的class 如果这个class不在map中 存到map中
        Class<?> cls = instance.getClass();
        if (!LANGUAGE_DRIVER_MAP.containsKey(cls)) {
            LANGUAGE_DRIVER_MAP.put(cls, instance);
        }
    }

    /**
     * 拿到这个语言驱动class的实现类
     *
     * @param cls
     * @return
     */
    public LanguageDriver getDriver(Class<?> cls) {
        return LANGUAGE_DRIVER_MAP.get(cls);
    }

    /**
     * 获取默认的语言驱动实现类
     *
     * @return
     */
    public LanguageDriver getDefaultDriver() {
        return getDriver(getDefaultDriverClass());
    }

    /**
     * 获取默认的语言驱动class
     *
     * @return
     */
    public Class<?> getDefaultDriverClass() {
        return defaultDriverClass;
    }

    /**
     * 设置默认的语言驱动class
     * 默认就是XMLLanguageDriver 在全局配置中初始化的时候设置的
     *
     * @param defaultDriverClass
     */
    public void setDefaultDriverClass(Class<?> defaultDriverClass) {
        register(defaultDriverClass);
        this.defaultDriverClass = defaultDriverClass;
    }

}
