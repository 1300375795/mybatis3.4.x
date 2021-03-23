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
package org.apache.ibatis.parsing;

/**
 * 通用令牌解析器
 *
 * @author Clinton Begin
 */
public class GenericTokenParser {

    /**
     * 开始的令牌
     */
    private final String openToken;

    /**
     * 结束的令牌
     */
    private final String closeToken;

    /**
     * 令牌处理器
     */
    private final TokenHandler handler;

    /**
     * 构造函数
     *
     * @param openToken
     * @param closeToken
     * @param handler
     */
    public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
        this.openToken = openToken;
        this.closeToken = closeToken;
        this.handler = handler;
    }

    /**
     * 解析文本内容
     *
     * @param text
     * @return
     */
    public String parse(String text) {
        //如果给出的内容为null或者空 那么直接返回空字符串
        if (text == null || text.isEmpty()) {
            return "";
        }
        // search open token
        //搜索开始令牌
        int start = text.indexOf(openToken, 0);
        //如果没有找到开始令牌 直接返回
        if (start == -1) {
            return text;
        }
        //转换成字节数组
        char[] src = text.toCharArray();
        //初始化偏移量
        int offset = 0;
        final StringBuilder builder = new StringBuilder();
        StringBuilder expression = null;
        //当start大于-1的
        while (start > -1) {
            if (start > 0 && src[start - 1] == '\\') {
                // this open token is escaped. remove the backslash and continue.
                builder.append(src, offset, start - offset - 1).append(openToken);
                offset = start + openToken.length();
            } else {
                // found open token. let's search close token.
                if (expression == null) {
                    expression = new StringBuilder();
                } else {
                    expression.setLength(0);
                }
                builder.append(src, offset, start - offset);
                offset = start + openToken.length();
                int end = text.indexOf(closeToken, offset);
                while (end > -1) {
                    if (end > offset && src[end - 1] == '\\') {
                        // this close token is escaped. remove the backslash and continue.
                        expression.append(src, offset, end - offset - 1).append(closeToken);
                        offset = end + closeToken.length();
                        end = text.indexOf(closeToken, offset);
                    } else {
                        expression.append(src, offset, end - offset);
                        offset = end + closeToken.length();
                        break;
                    }
                }
                if (end == -1) {
                    // close token was not found.
                    builder.append(src, start, src.length - start);
                    offset = src.length;
                } else {
                    builder.append(handler.handleToken(expression.toString()));
                    offset = end + closeToken.length();
                }
            }
            start = text.indexOf(openToken, offset);
        }
        if (offset < src.length) {
            builder.append(src, offset, src.length - offset);
        }
        return builder.toString();
    }
}
