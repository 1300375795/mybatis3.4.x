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

package org.apache.ibatis.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 参数名称分解器
 * 只要目的就是解析方法的参数列表
 */
public class ParamNameResolver {

    /**
     * 通用参数名称前缀
     */
    private static final String GENERIC_NAME_PREFIX = "param";

    /**
     * <p>
     * The key is the index and the value is the name of the parameter.<br />
     * The name is obtained from {@link Param} if specified. When {@link Param} is not specified,
     * the parameter index is used. Note that this index could be different from the actual index
     * when the method has special parameters (i.e. {@link RowBounds} or {@link ResultHandler}).
     * </p>
     * <ul>
     * <li>aMethod(@Param("M") int a, @Param("N") int b) -&gt; {{0, "M"}, {1, "N"}}</li>
     * <li>aMethod(int a, int b) -&gt; {{0, "0"}, {1, "1"}}</li>
     * <li>aMethod(int a, RowBounds rb, int b) -&gt; {{0, "0"}, {2, "1"}}</li>
     * </ul>
     * 参数名称索引map
     * key为参数索引 value是为参数名称
     */
    private final SortedMap<Integer, String> names;

    /**
     * 是否有@Param注解
     */
    private boolean hasParamAnnotation;

    /**
     * 构造函数
     *
     * @param config
     * @param method
     */
    public ParamNameResolver(Configuration config, Method method) {
        //获取方法参数类型
        final Class<?>[] paramTypes = method.getParameterTypes();
        //获取这个方法的注解
        final Annotation[][] paramAnnotations = method.getParameterAnnotations();
        final SortedMap<Integer, String> map = new TreeMap<Integer, String>();
        int paramCount = paramAnnotations.length;
        // get names from @Param annotations
        //遍历参数列表
        for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
            //如果是特殊的参数直接跳过
            if (isSpecialParameter(paramTypes[paramIndex])) {
                // skip special parameters
                continue;
            }
            String name = null;
            //遍历这个参数的注解集合
            for (Annotation annotation : paramAnnotations[paramIndex]) {
                //如果是@Param注解 那么拿到这个注解的value作为name 结束循环
                if (annotation instanceof Param) {
                    hasParamAnnotation = true;
                    name = ((Param) annotation).value();
                    break;
                }
            }
            //如果name为null 即@Param注解没有指定
            if (name == null) {
                // @Param was not specified.
                //如果配置要求使用实际的参数名称
                if (config.isUseActualParamName()) {
                    name = getActualParamName(method, paramIndex);
                }
                //如果根据直注解根据实际名称都没有获取到名称 那么根据参数index来
                if (name == null) {
                    // use the parameter index as the name ("0", "1", ...)
                    // gcode issue #71
                    name = String.valueOf(map.size());
                }
            }
            //将参数index跟参数名称存到map中
            map.put(paramIndex, name);
        }
        //转换map为不可修改names
        names = Collections.unmodifiableSortedMap(map);
    }

    /**
     * 获取实际的参数名称
     *
     * @param method
     * @param paramIndex
     * @return
     */
    private String getActualParamName(Method method, int paramIndex) {
        //java.lang.reflect.Parameter类是1.8开始有的
        // ParamNameUtil用到了这个类 如果不存在的话 那么直接返回null 存在的话 那么可以用ParamNameUtil类
        if (Jdk.parameterExists) {
            return ParamNameUtil.getParamNames(method).get(paramIndex);
        }
        return null;
    }

    /**
     * 判断是否是特别的参数
     * 如果是RowBounds或者ResultHandler的子类 那么返回为true
     *
     * @param clazz
     * @return
     */
    private static boolean isSpecialParameter(Class<?> clazz) {
        return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
    }

    /**
     * 返回参数名称集合
     * Returns parameter names referenced by SQL providers.
     */
    public String[] getNames() {
        return names.values().toArray(new String[0]);
    }

    /**
     * <p>
     * A single non-special parameter is returned without a name.<br />
     * Multiple parameters are named using the naming rule.<br />
     * In addition to the default names, this method also adds the generic names (param1, param2,
     * ...).
     * </p>
     * else的话 !names.containsValue操作会进行这个
     *  "size" -> 9223372036854775807
     *  "id" -> "foo"
     *  "param1" -> "foo"
     *  "param2" -> 9223372036854775807
     */
    public Object getNamedParams(Object[] args) {
        final int paramCount = names.size();
        if (args == null || paramCount == 0) {
            return null;
        } else if (!hasParamAnnotation && paramCount == 1) {
            return args[names.firstKey()];
        } else {
            //有参数注解并且参数数量大于等于1
            //无参数注解并且参数数量大于1
            final Map<String, Object> param = new ParamMap<Object>();
            int i = 0;
            for (Map.Entry<Integer, String> entry : names.entrySet()) {
                //将之前解析的参数名称进行反转 即得到参数名称对应的参数的值
                param.put(entry.getValue(), args[entry.getKey()]);
                // add generic param names (param1, param2, ...)
                final String genericParamName = GENERIC_NAME_PREFIX + String.valueOf(i + 1);
                // ensure not to overwrite parameter named with @Param
                //额外添加param1、param2这种给参数名称
                if (!names.containsValue(genericParamName)) {
                    param.put(genericParamName, args[entry.getKey()]);
                }
                i++;
            }
            return param;
        }
    }
}
