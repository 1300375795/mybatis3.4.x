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
package org.apache.ibatis.builder.xml.dynamic;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.scripting.xmltags.ChooseSqlNode;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.apache.ibatis.scripting.xmltags.IfSqlNode;
import org.apache.ibatis.scripting.xmltags.MixedSqlNode;
import org.apache.ibatis.scripting.xmltags.SetSqlNode;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.scripting.xmltags.TextSqlNode;
import org.apache.ibatis.scripting.xmltags.WhereSqlNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * 动态sql测试
 */
public class DynamicSqlSourceTest extends BaseDataTest {

    /**
     * 测试单个普通文本sql节点的拼接
     *
     * @throws Exception
     */
    @Test
    public void shouldDemonstrateSimpleExpectedTextWithNoLoopsOrConditionals() throws Exception {
        final String expected = "SELECT * FROM BLOG";
        final MixedSqlNode sqlNode = mixedContents(new TextSqlNode(expected));
        DynamicSqlSource source = createDynamicSqlSource(sqlNode);
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试多个普通文本sql节点的拼接
     *
     * @throws Exception
     */
    @Test
    public void shouldDemonstrateMultipartExpectedTextWithNoLoopsOrConditionals() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE ID = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new TextSqlNode("WHERE ID = ?"));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试文本sql节点跟if sql节点 混合
     * 其中if sql节点 的条件结果为true
     *
     * @throws Exception
     */
    @Test
    public void shouldConditionallyIncludeWhere() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE ID = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new IfSqlNode(mixedContents(new TextSqlNode("WHERE ID = ?")), "true"));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试文本sql节点跟if sql节点混合
     * 其中if sql节点 的条件结果为false
     *
     * @throws Exception
     */
    @Test
    public void shouldConditionallyExcludeWhere() throws Exception {
        final String expected = "SELECT * FROM BLOG";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new IfSqlNode(mixedContents(new TextSqlNode("WHERE ID = ?")), "false"));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试choose sql节点
     * 如果choose sql节点中的条件都不为true  那么就是default
     *
     * @throws Exception
     */
    @Test
    public void shouldConditionallyDefault() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE CATEGORY = 'DEFAULT'";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new ChooseSqlNode(new ArrayList<SqlNode>() {{
                    add(new IfSqlNode(mixedContents(new TextSqlNode("WHERE CATEGORY = ?")), "false"));
                    add(new IfSqlNode(mixedContents(new TextSqlNode("WHERE CATEGORY = 'NONE'")), "false"));
                }}, mixedContents(new TextSqlNode("WHERE CATEGORY = 'DEFAULT'"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试 choose sql节点
     * 如果choose 节点中的第一个节点结果是true
     * 那么就选择第一个节点 而不是default
     *
     * @throws Exception
     */
    @Test
    public void shouldConditionallyChooseFirst() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE CATEGORY = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new ChooseSqlNode(new ArrayList<SqlNode>() {{
                    add(new IfSqlNode(mixedContents(new TextSqlNode("WHERE CATEGORY = ?")), "true"));
                    add(new IfSqlNode(mixedContents(new TextSqlNode("WHERE CATEGORY = 'NONE'")), "false"));
                }}, mixedContents(new TextSqlNode("WHERE CATEGORY = 'DEFAULT'"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试choose 节点
     * 如果choose sql节点的第一个节点结果不为true  那么顺序选择下一个为true的节点
     * 这个测试用例里面就是第二个节点 而不是default
     *
     * @throws Exception
     */
    @Test
    public void shouldConditionallyChooseSecond() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE CATEGORY = 'NONE'";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new ChooseSqlNode(new ArrayList<SqlNode>() {{
                    add(new IfSqlNode(mixedContents(new TextSqlNode("WHERE CATEGORY = ?")), "false"));
                    add(new IfSqlNode(mixedContents(new TextSqlNode("WHERE CATEGORY = 'NONE'")), "true"));
                }}, mixedContents(new TextSqlNode("WHERE CATEGORY = 'DEFAULT'"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试where sql节点
     * 测试第一个where sql节点中要去掉and 这种操作符
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimWHEREInsteadOfANDForFirstCondition() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE  ID = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new WhereSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   and ID = ?  ")), "true"),
                                new IfSqlNode(mixedContents(new TextSqlNode("   or NAME = ?  ")), "false"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试where节点 中第一个节点是 and\n 前缀
     * 这个也要去掉
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimWHEREANDWithLFForFirstCondition() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE \n ID = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new WhereSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   and\n ID = ?  ")), "true"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试where节点中第一个节点的前置是and\r\n 前缀
     * 这个时候只去掉and
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimWHEREANDWithCRLFForFirstCondition() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE \r\n ID = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new WhereSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   and\r\n ID = ?  ")), "true"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试where 节点 and\t
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimWHEREANDWithTABForFirstCondition() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE \t ID = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new WhereSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   and\t ID = ?  ")), "true"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试where节点第一个节点的or\n
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimWHEREORWithLFForFirstCondition() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE \n ID = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new WhereSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   or\n ID = ?  ")), "true"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试where节点第一个节点的or\r\n
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimWHEREORWithCRLFForFirstCondition() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE \r\n ID = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new WhereSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   or\r\n ID = ?  ")), "true"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试where节点第一个节点的or\t
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimWHEREORWithTABForFirstCondition() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE \t ID = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new WhereSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   or\t ID = ?  ")), "true"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试where节点第一个节点的or
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimWHEREInsteadOfORForSecondCondition() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE  NAME = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new WhereSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   and ID = ?  ")), "false"),
                                new IfSqlNode(mixedContents(new TextSqlNode("   or NAME = ?  ")), "true"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试where节点 符合条件的多个节点中第一个节点要去掉and(or)  下面的节点则保留
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimWHEREInsteadOfANDForBothConditions() throws Exception {
        final String expected = "SELECT * FROM BLOG WHERE  ID = ?   OR NAME = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new WhereSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   and ID = ?   ")), "true"),
                                new IfSqlNode(mixedContents(new TextSqlNode("OR NAME = ?  ")), "true"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试where节点 如果没有符合的节点 那么不拼接
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimNoWhereClause() throws Exception {
        final String expected = "SELECT * FROM BLOG";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new WhereSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   and ID = ?   ")), "false"),
                                new IfSqlNode(mixedContents(new TextSqlNode("OR NAME = ?  ")), "false"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试set 节点 如果多个节点符合 那么去掉最后一个节点中的,
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimSETInsteadOfCOMMAForBothConditions() throws Exception {
        final String expected = "UPDATE BLOG SET ID = ?,  NAME = ?";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("UPDATE BLOG"),
                new SetSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode(" ID = ?, ")), "true"),
                                new IfSqlNode(mixedContents(new TextSqlNode(" NAME = ?, ")), "true"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试set节点  瑞如果没有符合条件的节点 那么都不拼接
     *
     * @throws Exception
     */
    @Test
    public void shouldTrimNoSetClause() throws Exception {
        final String expected = "UPDATE BLOG";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("UPDATE BLOG"),
                new SetSqlNode(new Configuration(),
                        mixedContents(new IfSqlNode(mixedContents(new TextSqlNode("   , ID = ?   ")), "false"),
                                new IfSqlNode(mixedContents(new TextSqlNode(", NAME = ?  ")), "false"))));
        BoundSql boundSql = source.getBoundSql(null);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试for each 节点
     *
     * @throws Exception
     */
    @Test
    public void shouldIterateOnceForEachItemInCollection() throws Exception {
        final HashMap<String, String[]> parameterObject = new HashMap<String, String[]>() {{
            put("array", new String[] { "one", "two", "three" });
        }};
        final String expected = "SELECT * FROM BLOG WHERE ID in (  one = ? AND two = ? AND three = ? )";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG WHERE ID in"),
                new ForEachSqlNode(new Configuration(), mixedContents(new TextSqlNode("${item} = #{item}")), "array",
                        "index", "item", "(", ")", "AND"));
        BoundSql boundSql = source.getBoundSql(parameterObject);
        assertEquals(expected, boundSql.getSql());
        assertEquals(3, boundSql.getParameterMappings().size());
        assertEquals("__frch_item_0", boundSql.getParameterMappings().get(0).getProperty());
        assertEquals("__frch_item_1", boundSql.getParameterMappings().get(1).getProperty());
        assertEquals("__frch_item_2", boundSql.getParameterMappings().get(2).getProperty());
    }

    /**
     * 测试处理ognl 表达式
     *
     * @throws Exception
     */
    @Test
    public void shouldHandleOgnlExpression() throws Exception {
        final HashMap<String, String> parameterObject = new HashMap<String, String>() {{
            put("name", "Steve");
        }};
        final String expected = "Expression test: 3 / yes.";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode(
                "Expression test: ${name.indexOf('v')} / ${name in {'Bob', 'Steve'\\} ? 'yes' : 'no'}."));
        BoundSql boundSql = source.getBoundSql(parameterObject);
        assertEquals(expected, boundSql.getSql());
    }

    /**
     * 测试for each节点 其中空数组的话 要跳过这个拼接
     *
     * @throws Exception
     */
    @Test
    public void shouldSkipForEachWhenCollectionIsEmpty() throws Exception {
        final HashMap<String, Integer[]> parameterObject = new HashMap<String, Integer[]>() {{
            put("array", new Integer[] {});
        }};
        final String expected = "SELECT * FROM BLOG";
        DynamicSqlSource source = createDynamicSqlSource(new TextSqlNode("SELECT * FROM BLOG"),
                new ForEachSqlNode(new Configuration(), mixedContents(new TextSqlNode("#{item}")), "array", null,
                        "item", "WHERE id in (", ")", ","));
        BoundSql boundSql = source.getBoundSql(parameterObject);
        assertEquals(expected, boundSql.getSql());
        assertEquals(0, boundSql.getParameterMappings().size());
    }

    /**
     * 测试for each节点 中的嵌套查询
     *
     * @throws Exception
     */
    @Test
    public void shouldPerformStrictMatchOnForEachVariableSubstitution() throws Exception {
        final Map<String, Object> param = new HashMap<String, Object>();
        final Map<String, String> uuu = new HashMap<String, String>();
        uuu.put("u", "xyz");
        List<Bean> uuuu = new ArrayList<Bean>();
        uuuu.add(new Bean("bean id"));
        param.put("uuu", uuu);
        param.put("uuuu", uuuu);
        DynamicSqlSource source = createDynamicSqlSource(
                new TextSqlNode("INSERT INTO BLOG (ID, NAME, NOTE, COMMENT) VALUES"),
                new ForEachSqlNode(new Configuration(), mixedContents(new TextSqlNode(
                        "#{uuu.u}, #{u.id}, #{ u,typeHandler=org.apache.ibatis.type.StringTypeHandler},"
                                + " #{u:VARCHAR,typeHandler=org.apache.ibatis.type.StringTypeHandler}")), "uuuu", "uu",
                        "u", "(", ")", ","));
        BoundSql boundSql = source.getBoundSql(param);
        assertEquals(4, boundSql.getParameterMappings().size());
        assertEquals("uuu.u", boundSql.getParameterMappings().get(0).getProperty());
        assertEquals("__frch_u_0.id", boundSql.getParameterMappings().get(1).getProperty());
        assertEquals("__frch_u_0", boundSql.getParameterMappings().get(2).getProperty());
        assertEquals("__frch_u_0", boundSql.getParameterMappings().get(3).getProperty());
    }

    /**
     * 创建动态sql 配合测试用
     *
     * @param contents
     * @return
     * @throws IOException
     * @throws SQLException
     */
    private DynamicSqlSource createDynamicSqlSource(SqlNode... contents) throws IOException, SQLException {
        createBlogDataSource();
        final String resource = "org/apache/ibatis/builder/MapperConfig.xml";
        final Reader reader = Resources.getResourceAsReader(resource);
        SqlSessionFactory sqlMapper = new SqlSessionFactoryBuilder().build(reader);
        Configuration configuration = sqlMapper.getConfiguration();
        MixedSqlNode sqlNode = mixedContents(contents);
        return new DynamicSqlSource(configuration, sqlNode);
    }

    /**
     * 将sql节点转化成混合节点
     *
     * @param contents
     * @return
     */
    private MixedSqlNode mixedContents(SqlNode... contents) {
        return new MixedSqlNode(Arrays.asList(contents));
    }

    /**
     * 测试如果参数是对象 那么如果对听的参数的值是null的话 那么替换成空
     */
    @Test
    public void shouldMapNullStringsToEmptyStrings() {
        final String expected = "id=${id}";
        final MixedSqlNode sqlNode = mixedContents(new TextSqlNode(expected));
        final DynamicSqlSource source = new DynamicSqlSource(new Configuration(), sqlNode);
        String sql = source.getBoundSql(new Bean(null)).getSql();
        Assert.assertEquals("id=", sql);
    }

    /**
     * bean对象
     */
    public static class Bean {
        public String id;

        public Bean(String property) {
            this.id = property;
        }

        public String getId() {
            return id;
        }

        public void setId(String property) {
            this.id = property;
        }
    }

}
