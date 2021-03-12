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
package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Clinton Begin
 */
public class MethodInvoker implements Invoker {

    private final Class<?> type;
    private final Method method;

    public MethodInvoker(Method method) {
        this.method = method;
        //如果入参类型只有一个 那么type就是这个如参数类型
        if (method.getParameterTypes().length == 1) {
            type = method.getParameterTypes()[0];
        }
        //否则就是出参类型
        else {
            type = method.getReturnType();
        }
    }

    /**
     * 执行方法
     *
     * @param target
     * @param args
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @Override
    public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
        return method.invoke(target, args);
    }

    /**
     * 获取type
     *
     * @return
     */
    @Override
    public Class<?> getType() {
        return type;
    }
}
