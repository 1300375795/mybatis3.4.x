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
 * 选择sql 条件
 *
 * @author Clinton Begin
 */
public class ChooseSqlNode implements SqlNode {

    /**
     * 如果ifSqlNodes节点中没有任何符合的sql 节点
     * 那么最终会选中这个默认的sql节点
     */
    private final SqlNode defaultSqlNode;
    private final List<SqlNode> ifSqlNodes;

    /**
     * 构造函数
     *
     * @param ifSqlNodes
     * @param defaultSqlNode
     */
    public ChooseSqlNode(List<SqlNode> ifSqlNodes, SqlNode defaultSqlNode) {
        this.ifSqlNodes = ifSqlNodes;
        this.defaultSqlNode = defaultSqlNode;
    }

    @Override
    public boolean apply(DynamicContext context) {
        //执行条件sql节点 如果其中一个结果为true 那么返回
        for (SqlNode sqlNode : ifSqlNodes) {
            if (sqlNode.apply(context)) {
                return true;
            }
        }
        //如果条件sql节点没有为true的并且默认的sql节点不为空 那么返回默认
        if (defaultSqlNode != null) {
            defaultSqlNode.apply(context);
            return true;
        }
        return false;
    }
}
