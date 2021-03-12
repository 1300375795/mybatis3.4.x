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
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * 属性分词器
 *
 * @author Clinton Begin
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {

    /**
     * 属性名称
     */
    private String name;

    /**
     * 上一级节点
     */
    private final String indexedName;
    private String index;
    private final String children;

    /**
     * 构造函数
     *
     * @param fullname
     */
    public PropertyTokenizer(String fullname) {
        int delim = fullname.indexOf('.');
        //如果给出的名称中包含.
        if (delim > -1) {
            //截取.之前的
            name = fullname.substring(0, delim);
            //.之后的作为子节点
            children = fullname.substring(delim + 1);
        } else {
            //如果不包含.的话 那么直接就将给出的名称作为属性名称 子节点为null
            name = fullname;
            children = null;
        }
        //设置上一级节点
        indexedName = name;
        delim = name.indexOf('[');
        //如果名称中包含[ 那么index设置为
        if (delim > -1) {
            index = name.substring(delim + 1, name.length() - 1);
            name = name.substring(0, delim);
        }
    }

    /**
     * 获取属性名称
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public String getIndex() {
        return index;
    }

    /**
     * 获取上级节点名称
     *
     * @return
     */
    public String getIndexedName() {
        return indexedName;
    }

    /**
     * 获取子节点
     *
     * @return
     */
    public String getChildren() {
        return children;
    }

    /**
     * 判断是否有下一个节点
     *
     * @return
     */
    @Override
    public boolean hasNext() {
        return children != null;
    }

    /**
     * 获取下一个节点
     *
     * @return
     */
    @Override
    public PropertyTokenizer next() {
        return new PropertyTokenizer(children);
    }

    /**
     * 删除节点 不支持这个操作
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "Remove is not supported, as it has no meaning in the context of properties.");
    }
}
