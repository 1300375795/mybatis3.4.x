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
package org.apache.ibatis.builder;

import java.util.List;

import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;

/**
 * 结果map解析器
 *
 * @author Eduardo Macarron
 */
public class ResultMapResolver {

    /**
     * 映射构造器助手
     */
    private final MapperBuilderAssistant assistant;

    /**
     * 结果map id
     */
    private final String id;

    /**
     * 结果type对应的class
     */
    private final Class<?> type;

    /**
     *
     */
    private final String extend;

    /**
     * 鉴别器
     */
    private final Discriminator discriminator;

    /**
     * 结果映射 对应resultMap中的各种属性
     */
    private final List<ResultMapping> resultMappings;

    /**
     * 是否自动映射
     */
    private final Boolean autoMapping;

    /**
     * 构造函数
     *
     * @param assistant
     * @param id
     * @param type
     * @param extend
     * @param discriminator
     * @param resultMappings
     * @param autoMapping
     */
    public ResultMapResolver(MapperBuilderAssistant assistant, String id, Class<?> type, String extend,
            Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
        this.assistant = assistant;
        this.id = id;
        this.type = type;
        this.extend = extend;
        this.discriminator = discriminator;
        this.resultMappings = resultMappings;
        this.autoMapping = autoMapping;
    }

    /**
     * 转换成结果map
     *
     * @return
     */
    public ResultMap resolve() {
        return assistant.addResultMap(this.id, this.type, this.extend, this.discriminator, this.resultMappings,
                this.autoMapping);
    }

}