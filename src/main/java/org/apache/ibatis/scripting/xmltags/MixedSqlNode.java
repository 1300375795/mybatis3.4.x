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
package org.apache.ibatis.scripting.xmltags;

import java.util.List;

/**
 * 混合sql节点
 * 会将各处的构造函数的sql节点保存起来
 * 在执行apply的时候 循环调用每一个节点的apply
 *
 * @author Clinton Begin
 */
public class MixedSqlNode implements SqlNode {
    private final List<SqlNode> contents;

    /**
     * 混合sql节点
     *
     * @param contents
     */
    public MixedSqlNode(List<SqlNode> contents) {
        this.contents = contents;
    }

    /**
     * 循环处理混合节点中的每一个节点
     *
     * @param context
     * @return
     */
    @Override
    public boolean apply(DynamicContext context) {
        for (SqlNode sqlNode : contents) {
            sqlNode.apply(context);
        }
        return true;
    }
}
