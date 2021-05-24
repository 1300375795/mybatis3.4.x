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
package org.apache.ibatis.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * 结果映射
 * 对应xml中resultMap属性中的各种子属性
 *
 * @author Clinton Begin
 */
public class ResultMapping {

    private Configuration configuration;

    /**
     * ˙这个是java类中的属性
     */
    private String property;

    /**
     * 这个是数据库中的字段
     */
    private String column;

    /**
     * 这个是java类型
     */
    private Class<?> javaType;

    /**
     * 这个是jdbc的类型
     */
    private JdbcType jdbcType;

    /**
     * 这个是类型处理器
     */
    private TypeHandler<?> typeHandler;

    /**
     * 这个是嵌套的resultMapId
     */
    private String nestedResultMapId;

    /**
     * 这个是嵌套的查询id
     */
    private String nestedQueryId;

    /**
     * 这个是不能为空的数据库字段集合
     */
    private Set<String> notNullColumns;

    /**
     * 数据库字段前缀
     */
    private String columnPrefix;

    /**
     * 结果标识集合
     */
    private List<ResultFlag> flags;

    /**
     *
     */
    private List<ResultMapping> composites;
    private String resultSet;
    private String foreignColumn;

    /**
     * 是否懒加载
     */
    private boolean lazy;

    /**
     * 构造函数
     */
    ResultMapping() {
    }

    /**
     * 构造器
     */
    public static class Builder {

        /**
         * 对应result map中的子属性
         */
        private ResultMapping resultMapping = new ResultMapping();

        /**
         * 构造器构造函数
         *
         * @param configuration
         * @param property
         * @param column
         * @param typeHandler
         */
        public Builder(Configuration configuration, String property, String column, TypeHandler<?> typeHandler) {
            this(configuration, property);
            resultMapping.column = column;
            resultMapping.typeHandler = typeHandler;
        }

        /**
         * 构造器构造函数
         *
         * @param configuration
         * @param property
         * @param column
         * @param javaType
         */
        public Builder(Configuration configuration, String property, String column, Class<?> javaType) {
            this(configuration, property);
            resultMapping.column = column;
            resultMapping.javaType = javaType;
        }

        /**
         * 构造器构造函数
         *
         * @param configuration
         * @param property
         */
        public Builder(Configuration configuration, String property) {
            resultMapping.configuration = configuration;
            resultMapping.property = property;
            resultMapping.flags = new ArrayList<ResultFlag>();
            resultMapping.composites = new ArrayList<ResultMapping>();
            resultMapping.lazy = configuration.isLazyLoadingEnabled();
        }

        /**
         * 设置java类型
         *
         * @param javaType
         * @return
         */
        public Builder javaType(Class<?> javaType) {
            resultMapping.javaType = javaType;
            return this;
        }

        /**
         * 设置jdbc类型
         *
         * @param jdbcType
         * @return
         */
        public Builder jdbcType(JdbcType jdbcType) {
            resultMapping.jdbcType = jdbcType;
            return this;
        }

        /**
         * 设置嵌套的resultMapId
         *
         * @param nestedResultMapId
         * @return
         */
        public Builder nestedResultMapId(String nestedResultMapId) {
            resultMapping.nestedResultMapId = nestedResultMapId;
            return this;
        }

        /**
         * 设置嵌套的查询id
         *
         * @param nestedQueryId
         * @return
         */
        public Builder nestedQueryId(String nestedQueryId) {
            resultMapping.nestedQueryId = nestedQueryId;
            return this;
        }

        /**
         * 设置结果集合
         *
         * @param resultSet
         * @return
         */
        public Builder resultSet(String resultSet) {
            resultMapping.resultSet = resultSet;
            return this;
        }

        /**
         * 设置外联的数据库字段
         *
         * @param foreignColumn
         * @return
         */
        public Builder foreignColumn(String foreignColumn) {
            resultMapping.foreignColumn = foreignColumn;
            return this;
        }

        /**
         * 设置不为null的字段集合
         *
         * @param notNullColumns
         * @return
         */
        public Builder notNullColumns(Set<String> notNullColumns) {
            resultMapping.notNullColumns = notNullColumns;
            return this;
        }

        /**
         * 设置数据库字段前缀
         *
         * @param columnPrefix
         * @return
         */
        public Builder columnPrefix(String columnPrefix) {
            resultMapping.columnPrefix = columnPrefix;
            return this;
        }

        /**
         * 设置结果标识集合
         *
         * @param flags
         * @return
         */
        public Builder flags(List<ResultFlag> flags) {
            resultMapping.flags = flags;
            return this;
        }

        /**
         * 设置类型处理器
         *
         * @param typeHandler
         * @return
         */
        public Builder typeHandler(TypeHandler<?> typeHandler) {
            resultMapping.typeHandler = typeHandler;
            return this;
        }

        public Builder composites(List<ResultMapping> composites) {
            resultMapping.composites = composites;
            return this;
        }

        /**
         * 设置是否懒加载
         *
         * @param lazy
         * @return
         */
        public Builder lazy(boolean lazy) {
            resultMapping.lazy = lazy;
            return this;
        }

        /**
         * 构建resultMapping
         *
         * @return
         */
        public ResultMapping build() {
            // lock down collections
            resultMapping.flags = Collections.unmodifiableList(resultMapping.flags);
            resultMapping.composites = Collections.unmodifiableList(resultMapping.composites);
            resolveTypeHandler();
            validate();
            return resultMapping;
        }

        private void validate() {
            // Issue #697: cannot define both nestedQueryId and nestedResultMapId
            if (resultMapping.nestedQueryId != null && resultMapping.nestedResultMapId != null) {
                throw new IllegalStateException(
                        "Cannot define both nestedQueryId and nestedResultMapId in property " + resultMapping.property);
            }
            // Issue #5: there should be no mappings without typehandler
            if (resultMapping.nestedQueryId == null && resultMapping.nestedResultMapId == null
                    && resultMapping.typeHandler == null) {
                throw new IllegalStateException("No typehandler found for property " + resultMapping.property);
            }
            // Issue #4 and GH #39: column is optional only in nested resultmaps but not in the rest
            if (resultMapping.nestedResultMapId == null && resultMapping.column == null && resultMapping.composites
                    .isEmpty()) {
                throw new IllegalStateException(
                        "Mapping is missing column attribute for property " + resultMapping.property);
            }
            if (resultMapping.getResultSet() != null) {
                int numColumns = 0;
                if (resultMapping.column != null) {
                    numColumns = resultMapping.column.split(",").length;
                }
                int numForeignColumns = 0;
                if (resultMapping.foreignColumn != null) {
                    numForeignColumns = resultMapping.foreignColumn.split(",").length;
                }
                if (numColumns != numForeignColumns) {
                    throw new IllegalStateException(
                            "There should be the same number of columns and foreignColumns in property "
                                    + resultMapping.property);
                }
            }
        }

        private void resolveTypeHandler() {
            if (resultMapping.typeHandler == null && resultMapping.javaType != null) {
                Configuration configuration = resultMapping.configuration;
                TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
                resultMapping.typeHandler = typeHandlerRegistry
                        .getTypeHandler(resultMapping.javaType, resultMapping.jdbcType);
            }
        }

        public Builder column(String column) {
            resultMapping.column = column;
            return this;
        }
    }

    public String getProperty() {
        return property;
    }

    public String getColumn() {
        return column;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public JdbcType getJdbcType() {
        return jdbcType;
    }

    public TypeHandler<?> getTypeHandler() {
        return typeHandler;
    }

    public String getNestedResultMapId() {
        return nestedResultMapId;
    }

    public String getNestedQueryId() {
        return nestedQueryId;
    }

    public Set<String> getNotNullColumns() {
        return notNullColumns;
    }

    public String getColumnPrefix() {
        return columnPrefix;
    }

    public List<ResultFlag> getFlags() {
        return flags;
    }

    public List<ResultMapping> getComposites() {
        return composites;
    }

    public boolean isCompositeResult() {
        return this.composites != null && !this.composites.isEmpty();
    }

    public String getResultSet() {
        return this.resultSet;
    }

    public String getForeignColumn() {
        return foreignColumn;
    }

    public void setForeignColumn(String foreignColumn) {
        this.foreignColumn = foreignColumn;
    }

    public boolean isLazy() {
        return lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResultMapping that = (ResultMapping) o;

        if (property == null || !property.equals(that.property)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        if (property != null) {
            return property.hashCode();
        } else if (column != null) {
            return column.hashCode();
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResultMapping{");
        //sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
        sb.append("property='").append(property).append('\'');
        sb.append(", column='").append(column).append('\'');
        sb.append(", javaType=").append(javaType);
        sb.append(", jdbcType=").append(jdbcType);
        //sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
        sb.append(", nestedResultMapId='").append(nestedResultMapId).append('\'');
        sb.append(", nestedQueryId='").append(nestedQueryId).append('\'');
        sb.append(", notNullColumns=").append(notNullColumns);
        sb.append(", columnPrefix='").append(columnPrefix).append('\'');
        sb.append(", flags=").append(flags);
        sb.append(", composites=").append(composites);
        sb.append(", resultSet='").append(resultSet).append('\'');
        sb.append(", foreignColumn='").append(foreignColumn).append('\'');
        sb.append(", lazy=").append(lazy);
        sb.append('}');
        return sb.toString();
    }

}
