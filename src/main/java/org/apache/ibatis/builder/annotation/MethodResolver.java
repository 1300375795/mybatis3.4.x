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
 * 方法解析器
 *
 * @author Eduardo Macarron
 */
public class MethodResolver {

    /**
     * mapper接口注解构造器
     */
    private final MapperAnnotationBuilder annotationBuilder;

    /**
     * mapper接口方法
     */
    private final Method method;

    /**
     * 构造函数
     *
     * @param annotationBuilder
     * @param method
     */
    public MethodResolver(MapperAnnotationBuilder annotationBuilder, Method method) {
        this.annotationBuilder = annotationBuilder;
        this.method = method;
    }

    /**
     * 解析声明
     * 一个select、update等对应一个MappedStatement
     */
    public void resolve() {
        annotationBuilder.parseStatement(method);
    }

}