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
package org.apache.ibatis.executor;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Array;
import java.util.List;

/**
 * 结果提取器
 *
 * @author Andrew Gustafson
 */
public class ResultExtractor {
    private final Configuration configuration;
    private final ObjectFactory objectFactory;

    /**
     * 构造函数
     *
     * @param configuration
     * @param objectFactory
     */
    public ResultExtractor(Configuration configuration, ObjectFactory objectFactory) {
        this.configuration = configuration;
        this.objectFactory = objectFactory;
    }

    /**
     * 从集合中提取对象
     *
     * @param list
     * @param targetType
     * @return
     */
    public Object extractObjectFromList(List<Object> list, Class<?> targetType) {
        Object value = null;
        //如果目标类型不为null并且是list的父类 那么直接返回
        if (targetType != null && targetType.isAssignableFrom(list.getClass())) {
            value = list;
        }
        //如果目标对象不为null 并且是集合 那么放入MetaObject
        else if (targetType != null && objectFactory.isCollection(targetType)) {
            value = objectFactory.create(targetType);
            MetaObject metaObject = configuration.newMetaObject(value);
            metaObject.addAll(list);
        }
        //如果不为null 并且是数组 那么转换成数组
        else if (targetType != null && targetType.isArray()) {
            Class<?> arrayComponentType = targetType.getComponentType();
            Object array = Array.newInstance(arrayComponentType, list.size());
            if (arrayComponentType.isPrimitive()) {
                for (int i = 0; i < list.size(); i++) {
                    Array.set(array, i, list.get(i));
                }
                value = array;
            } else {
                value = list.toArray((Object[]) array);
            }
        }
        //否则不是list类型的数据
        else {
            //如果不为null并且返回的结果记录大于1条 那么抛出异常
            if (list != null && list.size() > 1) {
                throw new ExecutorException(
                        "Statement returned more than one row, where no more than one was expected.");
            }
            //如果不为null并且长度为1 那么直接返回这条记录
            else if (list != null && list.size() == 1) {
                value = list.get(0);
            }
        }
        //其余情况直接返回null
        return value;
    }
}
