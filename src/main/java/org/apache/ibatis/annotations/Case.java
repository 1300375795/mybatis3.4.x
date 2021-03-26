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
package org.apache.ibatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * case条件基于条件返回实际的类型
 *
 * @author Clinton Begin
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Case {

    /**
     * 如果字段的value等于这个那么匹配
     *
     * @return
     */
    String value();

    /**
     * 匹配成功对应的class类型
     *
     * @return
     */
    Class<?> type();

    /**
     * 其他属性参数
     *
     * @return
     */
    Result[] results() default {};

    /**
     * 构造函数参数
     *
     * @return
     */
    Arg[] constructArgs() default {};
}
