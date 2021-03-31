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
package org.apache.ibatis.executor.result;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

/**
 * 默认的结果处理器 list
 *
 * @author Clinton Begin
 */
public class DefaultResultHandler implements ResultHandler<Object> {

    /**
     * 结果list
     */
    private final List<Object> list;

    /**
     * 构造函数
     */
    public DefaultResultHandler() {
        list = new ArrayList<Object>();
    }

    /**
     * 构造函数
     *
     * @param objectFactory
     */
    @SuppressWarnings("unchecked")
    public DefaultResultHandler(ObjectFactory objectFactory) {
        list = objectFactory.create(List.class);
    }

    /**
     * 处理结果
     *
     * @param context
     */
    @Override
    public void handleResult(ResultContext<? extends Object> context) {
        list.add(context.getResultObject());
    }

    /**
     * 获取结果list
     *
     * @return
     */
    public List<Object> getResultList() {
        return list;
    }

}
