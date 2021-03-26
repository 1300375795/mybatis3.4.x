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
package org.apache.ibatis.executor.resultset;

import java.util.HashMap;
import java.util.Map;

/**
 * 基本类型跟包装类型
 */
public class PrimitiveTypes {

    /**
     * 基本类型
     */
    private final Map<Class<?>, Class<?>> primitiveToWrappers;

    /**
     * 包装类型
     */
    private final Map<Class<?>, Class<?>> wrappersToPrimitives;

    /**
     * 构造函数
     */
    public PrimitiveTypes() {
        this.primitiveToWrappers = new HashMap<Class<?>, Class<?>>();
        this.wrappersToPrimitives = new HashMap<Class<?>, Class<?>>();

        add(boolean.class, Boolean.class);
        add(byte.class, Byte.class);
        add(char.class, Character.class);
        add(double.class, Double.class);
        add(float.class, Float.class);
        add(int.class, Integer.class);
        add(long.class, Long.class);
        add(short.class, Short.class);
        add(void.class, Void.class);
    }

    /**
     * 添加基本跟包装类型
     *
     * @param primitiveType
     * @param wrapperType
     */
    private void add(final Class<?> primitiveType, final Class<?> wrapperType) {
        primitiveToWrappers.put(primitiveType, wrapperType);
        wrappersToPrimitives.put(wrapperType, primitiveType);
    }

    /**
     * 根据基本类型拿到包装类型
     *
     * @param primitiveType
     * @return
     */
    public Class<?> getWrapper(final Class<?> primitiveType) {
        return primitiveToWrappers.get(primitiveType);
    }

    /**
     * 根据包装类型拿到基本类型
     *
     * @param wrapperType
     * @return
     */
    public Class<?> getPrimitive(final Class<?> wrapperType) {
        return wrappersToPrimitives.get(wrapperType);
    }
}
