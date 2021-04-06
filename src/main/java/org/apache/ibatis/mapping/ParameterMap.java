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
package org.apache.ibatis.mapping;

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.Configuration;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * 参数map 对应xml的parameterMap配置
 *
 * @author Clinton Begin
 */
public class ParameterMap {

    /**
     * parameterMap的id配置
     */
    private String id;

    /**
     * parameterMap的type配置
     */
    private Class<?> type;

    /**
     * parameterMap的parameter配置集合
     */
    private List<ParameterMapping> parameterMappings;

    /**
     * 构造函数
     */
    private ParameterMap() {
    }

    /**
     * 构建者模式
     */
    public static class Builder {
        private ParameterMap parameterMap = new ParameterMap();

        /**
         * 构造函数
         *
         * @param configuration
         * @param id
         * @param type
         * @param parameterMappings
         */
        public Builder(Configuration configuration, String id, Class<?> type,
                List<ParameterMapping> parameterMappings) {
            parameterMap.id = id;
            parameterMap.type = type;
            parameterMap.parameterMappings = parameterMappings;
        }

        /**
         * 获取参数类型
         *
         * @return
         */
        public Class<?> type() {
            return parameterMap.type;
        }

        /**
         * 构建对象
         *
         * @return
         */
        public ParameterMap build() {
            //lock down collections
            parameterMap.parameterMappings = Collections.unmodifiableList(parameterMap.parameterMappings);
            return parameterMap;
        }
    }

    /**
     * 获取id
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * 获取类型class
     *
     * @return
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * 获取parameter集合
     *
     * @return
     */
    public List<ParameterMapping> getParameterMappings() {
        return parameterMappings;
    }

}
