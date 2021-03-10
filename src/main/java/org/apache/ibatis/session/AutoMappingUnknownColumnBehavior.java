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
package org.apache.ibatis.session;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.MappedStatement;

/**
 * Specify the behavior when detects an unknown column (or unknown property type) of automatic mapping target.
 * 指定当检测到自动映射目标的未知列(或未知属性类型)时的行为。
 *
 * @author Kazuki Shimizu
 * @since 3.4.0
 */
public enum AutoMappingUnknownColumnBehavior {

    /**
     * Do nothing (Default).
     * 不做任何处理 默认
     */
    NONE {
        @Override
        public void doAction(MappedStatement mappedStatement, String columnName, String property,
                Class<?> propertyType) {
            // do nothing
        }
    },

    /**
     * 输出警告日志
     * Output warning log.
     * Note: The log level of {@code 'org.apache.ibatis.session.AutoMappingUnknownColumnBehavior'} must be set to {@code WARN}.
     */
    WARNING {
        @Override
        public void doAction(MappedStatement mappedStatement, String columnName, String property,
                Class<?> propertyType) {
            log.warn(buildMessage(mappedStatement, columnName, property, propertyType));
        }
    },

    /**
     * 映射失败 抛出异常处理
     * Fail mapping.
     * Note: throw {@link SqlSessionException}.
     */
    FAILING {
        @Override
        public void doAction(MappedStatement mappedStatement, String columnName, String property,
                Class<?> propertyType) {
            throw new SqlSessionException(buildMessage(mappedStatement, columnName, property, propertyType));
        }
    };

    /**
     * Logger
     */
    private static final Log log = LogFactory.getLog(AutoMappingUnknownColumnBehavior.class);

    /**
     * Perform the action when detects an unknown column (or unknown property type) of automatic mapping target.
     * 当检测到自动映射目标的未知列(或未知属性类型)时执行操作。
     *
     * @param mappedStatement current mapped statement
     * @param columnName      column name for mapping target
     * @param propertyName    property name for mapping target
     * @param propertyType    property type for mapping target (If this argument is not null, {@link org.apache.ibatis.type.TypeHandler} for property type is not registered)
     */
    public abstract void doAction(MappedStatement mappedStatement, String columnName, String propertyName,
            Class<?> propertyType);

    /**
     * build error message.
     * 构建异常信息
     */
    private static String buildMessage(MappedStatement mappedStatement, String columnName, String property,
            Class<?> propertyType) {
        return new StringBuilder("Unknown column is detected on '").append(mappedStatement.getId())
                .append("' auto-mapping. Mapping parameters are ").append("[").append("columnName=").append(columnName)
                .append(",").append("propertyName=").append(property).append(",").append("propertyType=")
                .append(propertyType != null ? propertyType.getName() : null).append("]").toString();
    }

}
