/**
 * Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * 属性配置解析器
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class PropertyParser {

    /**
     * 属性解析器的前缀
     */
    private static final String KEY_PREFIX = "org.apache.ibatis.parsing.PropertyParser.";

    /**
     * The special property key that indicate whether enable a default value on placeholder.
     * <p>
     * The default value is {@code false} (indicate disable a default value on placeholder)
     * If you specify the {@code true}, you can specify key and default value on placeholder (e.g. {@code ${db.username:postgres}}).
     * </p>
     * 是否允许默认值配置名称
     *
     * @since 3.4.2
     */
    public static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

    /**
     * The special property key that specify a separator for key and default value on placeholder.
     * <p>
     * The default separator is {@code ":"}.
     * </p>
     * 默认分割符配置名称
     *
     * @since 3.4.2
     */
    public static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";

    /**
     * 是否允许默认值 默认是不允许
     */
    private static final String ENABLE_DEFAULT_VALUE = "false";

    /**
     * 默认的配置的值的分隔符
     */
    private static final String DEFAULT_VALUE_SEPARATOR = ":";

    /**
     * 构造函数
     */
    private PropertyParser() {
        // Prevent Instantiation
    }

    /**
     * 解析配置
     *
     * @param string
     * @param variables
     * @return
     */
    public static String parse(String string, Properties variables) {
        VariableTokenHandler handler = new VariableTokenHandler(variables);
        GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
        return parser.parse(string);
    }

    /**
     * 变量令牌处理器
     */
    private static class VariableTokenHandler implements TokenHandler {

        /**
         * 变量
         */
        private final Properties variables;

        /**
         * 是否允许默认值
         */
        private final boolean enableDefaultValue;

        /**
         * 默认的分隔符
         */
        private final String defaultValueSeparator;

        /**
         * 构造函数
         *
         * @param variables
         */
        private VariableTokenHandler(Properties variables) {
            this.variables = variables;
            this.enableDefaultValue = Boolean
                    .parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
            this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
        }

        /**
         * 获取某个key的值
         * 如果属性为空 那么获取默认值
         * 如果属性不为空 但是属性中这个key的值为空 那么也是默认值
         *
         * @param key
         * @param defaultValue
         * @return
         */
        private String getPropertyValue(String key, String defaultValue) {
            return (variables == null) ? defaultValue : variables.getProperty(key, defaultValue);
        }

        /**
         * 处理令牌
         *
         * @param content
         * @return
         */
        @Override
        public String handleToken(String content) {
            if (variables != null) {
                String key = content;
                if (enableDefaultValue) {
                    final int separatorIndex = content.indexOf(defaultValueSeparator);
                    String defaultValue = null;
                    if (separatorIndex >= 0) {
                        key = content.substring(0, separatorIndex);
                        defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
                    }
                    if (defaultValue != null) {
                        return variables.getProperty(key, defaultValue);
                    }
                }
                if (variables.containsKey(key)) {
                    return variables.getProperty(key);
                }
            }
            return "${" + content + "}";
        }
    }

}
