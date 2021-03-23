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
package org.apache.ibatis.builder.annotation;

import java.lang.reflect.Method;

/**
 * The context object for sql provider method.
 * sql提供者注解的上下文对象
 *
 * @author Kazuki Shimizu
 * @since 3.4.5
 */
public final class ProviderContext {

    /**
     * mapper接口class
     */
    private final Class<?> mapperType;

    /**
     * 被XxProvider注解的方法
     */
    private final Method mapperMethod;

    /**
     * Constructor.
     * 构造函数
     *
     * @param mapperType   A mapper interface type that specified provider
     * @param mapperMethod A mapper method that specified provider
     */
    ProviderContext(Class<?> mapperType, Method mapperMethod) {
        this.mapperType = mapperType;
        this.mapperMethod = mapperMethod;
    }

    /**
     * Get a mapper interface type that specified provider.
     * 获取mapper接口class
     *
     * @return A mapper interface type that specified provider
     */
    public Class<?> getMapperType() {
        return mapperType;
    }

    /**
     * Get a mapper method that specified provider.
     * 获取被@XxProvider注解的方法
     *
     * @return A mapper method that specified provider
     */
    public Method getMapperMethod() {
        return mapperMethod;
    }

}
