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

import java.util.regex.Pattern;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.scripting.ScriptingException;
import org.apache.ibatis.type.SimpleTypeRegistry;

/**
 * 文本sql节点
 *
 * @author Clinton Begin
 */
public class TextSqlNode implements SqlNode {

    /**
     * 文本内容
     */
    private final String text;

    /**
     * 注入的过滤器
     */
    private final Pattern injectionFilter;

    /**
     * 构造函数
     *
     * @param text
     */
    public TextSqlNode(String text) {
        this(text, null);
    }

    /**
     * 构造函数
     *
     * @param text
     * @param injectionFilter
     */
    public TextSqlNode(String text, Pattern injectionFilter) {
        this.text = text;
        this.injectionFilter = injectionFilter;
    }

    /**
     * 是否动态
     *
     * @return
     */
    public boolean isDynamic() {
        //创建动态校验令牌解析器
        DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
        //创建解析器
        GenericTokenParser parser = createParser(checker);
        //解析文本内容
        parser.parse(text);
        //返回是否动态 只有当GenericTokenParser中调用handler的时候会返回true
        return checker.isDynamic();
    }

    @Override
    public boolean apply(DynamicContext context) {
        //创建解析器
        GenericTokenParser parser = createParser(new BindingTokenParser(context, injectionFilter));
        //在动态内容中拼接上解析得到的结果
        context.appendSql(parser.parse(text));
        return true;
    }

    /**
     * 根据给出的处理器 创建通用解析器
     *
     * @param handler
     * @return
     */
    private GenericTokenParser createParser(TokenHandler handler) {
        return new GenericTokenParser("${", "}", handler);
    }

    /**
     * 绑定令牌解析器
     */
    private static class BindingTokenParser implements TokenHandler {

        /**
         * 动态内容
         */
        private DynamicContext context;

        /**
         * 注入的过滤器
         */
        private Pattern injectionFilter;

        /**
         * 构造函数
         *
         * @param context
         * @param injectionFilter
         */
        public BindingTokenParser(DynamicContext context, Pattern injectionFilter) {
            this.context = context;
            this.injectionFilter = injectionFilter;
        }

        /**
         * 处理令牌
         *
         * @param content
         * @return
         */
        @Override
        public String handleToken(String content) {
            Object parameter = context.getBindings().get("_parameter");
            if (parameter == null) {
                context.getBindings().put("value", null);
            } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
                context.getBindings().put("value", parameter);
            }
            Object value = OgnlCache.getValue(content, context.getBindings());
            String srtValue = (value == null ? "" : String.valueOf(value)); // issue #274 return "" instead of "null"
            checkInjection(srtValue);
            return srtValue;
        }

        private void checkInjection(String value) {
            if (injectionFilter != null && !injectionFilter.matcher(value).matches()) {
                throw new ScriptingException("Invalid input. Please conform to regex" + injectionFilter.pattern());
            }
        }
    }

    /**
     * 动态校验令牌解析器
     */
    private static class DynamicCheckerTokenParser implements TokenHandler {

        /**
         * 是否动态
         */
        private boolean isDynamic;

        /**
         * 构造函数
         */
        public DynamicCheckerTokenParser() {
            // Prevent Synthetic Access
        }

        /**
         * 返回是否动态
         *
         * @return
         */
        public boolean isDynamic() {
            return isDynamic;
        }

        /**
         * 处理令牌
         * 设置是否动态为true 返回null
         *
         * @param content
         * @return
         */
        @Override
        public String handleToken(String content) {
            this.isDynamic = true;
            return null;
        }
    }

}