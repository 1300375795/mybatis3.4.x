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
package org.apache.ibatis.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 调用--拦截器拦截的
 *
 * @author Clinton Begin
 */
public class Invocation {

    /**
     * 目标对象
     */
    private final Object target;

    /**
     * 方法
     */
    private final Method method;

    /**
     * 参数
     */
    private final Object[] args;

    /**
     * 构造函数
     *
     * @param target
     * @param method
     * @param args
     */
    public Invocation(Object target, Method method, Object[] args) {
        this.target = target;
        this.method = method;
        this.args = args;
    }

    /**
     * 获取目标对象
     *
     * @return
     */
    public Object getTarget() {
        return target;
    }

    /**
     * 获取方法
     *
     * @return
     */
    public Method getMethod() {
        return method;
    }

    /**
     * 获取参数
     *
     * @return
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * 执行方法
     *
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public Object proceed() throws InvocationTargetException, IllegalAccessException {
        return method.invoke(target, args);
    }

}
