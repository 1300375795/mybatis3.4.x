/**
 * Copyright 2009-2018 the original author or authors.
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

import static org.junit.Assert.*;

import java.util.HashMap;

import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Section;
import org.apache.ibatis.scripting.xmltags.ExpressionEvaluator;
import org.junit.Test;

/**
 * sql中的表达式测试
 */
public class ExpressionEvaluatorTest {

    private ExpressionEvaluator evaluator = new ExpressionEvaluator();

    /**
     * 测试==表达式成立
     */
    @Test
    public void shouldCompareStringsReturnTrue() {
        boolean value = evaluator.evaluateBoolean("username == 'cbegin'",
                new Author(1, "cbegin", "******", "cbegin@apache.org", "N/A", Section.NEWS));
        assertEquals(true, value);
    }

    /**
     * 测试==表达式不成立
     */
    @Test
    public void shouldCompareStringsReturnFalse() {
        boolean value = evaluator.evaluateBoolean("username == 'norm'",
                new Author(1, "cbegin", "******", "cbegin@apache.org", "N/A", Section.NEWS));
        assertEquals(false, value);
    }

    /**
     * 测试如果是字符串且只有表达式的左边 那么不为空就是true
     */
    @Test
    public void shouldReturnTrueIfNotNull() {
        boolean value = evaluator.evaluateBoolean("username",
                new Author(1, "cbegin", "******", "cbegin@apache.org", "N/A", Section.NEWS));
        assertEquals(true, value);
    }

    /**
     * 如果表达式是字符串且只有左边 那么null的话 返回false
     */
    @Test
    public void shouldReturnFalseIfNull() {
        boolean value = evaluator
                .evaluateBoolean("password", new Author(1, "cbegin", null, "cbegin@apache.org", "N/A", Section.NEWS));
        assertEquals(false, value);
    }

    /**
     * 测试如果是数字的话 那么不为0就是true
     */
    @Test
    public void shouldReturnTrueIfNotZero() {
        boolean value = evaluator
                .evaluateBoolean("id", new Author(1, "cbegin", null, "cbegin@apache.org", "N/A", Section.NEWS));
        assertEquals(true, value);
    }

    /**
     * 测试表达式如果是数字的话 那么0的话 就是false
     */
    @Test
    public void shouldReturnFalseIfZero() {
        boolean value = evaluator
                .evaluateBoolean("id", new Author(0, "cbegin", null, "cbegin@apache.org", "N/A", Section.NEWS));
        assertEquals(false, value);
    }

    /**
     * 测试如果是小数的话 如果返回0那么是false
     */
    @Test
    public void shouldReturnFalseIfZeroWithScale() {
        class Bean {
            @SuppressWarnings("unused")
            public double d = 0.0d;
        }
        assertFalse(evaluator.evaluateBoolean("d", new Bean()));
    }

    /**
     * 测试如果是迭代的话 那么顺序解析
     */
    @Test
    public void shouldIterateOverIterable() {
        final HashMap<String, String[]> parameterObject = new HashMap<String, String[]>() {{
            put("array", new String[] { "1", "2", "3" });
        }};
        final Iterable<?> iterable = evaluator.evaluateIterable("array", parameterObject);
        int i = 0;
        for (Object o : iterable) {
            assertEquals(String.valueOf(++i), o);
        }
    }

}
