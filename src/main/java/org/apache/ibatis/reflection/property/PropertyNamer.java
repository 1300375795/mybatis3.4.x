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
package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * 属性名称工具类
 *
 * @author Clinton Begin
 */
public final class PropertyNamer {

    private PropertyNamer() {
        // Prevent Instantiation of Static Class
    }

    /**
     * 方法名称转换成属性名称
     *
     * @param name
     * @return
     */
    public static String methodToProperty(String name) {
        //如果方法名称是以is开头的 那么截掉前两位
        if (name.startsWith("is")) {
            name = name.substring(2);
        }
        //如果方法名称是一get开头或者set开头的 那么截掉前3位
        else if (name.startsWith("get") || name.startsWith("set")) {
            name = name.substring(3);
        } else {
            //其他情况直接抛出异常
            throw new ReflectionException(
                    "Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
        }
        //如果截取后的名称的长度是1或者名称的长度大于1并且名称的第二个字符不是大写的 那么将名称的第一个字符进行小写
        if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
            name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
        }
        return name;
    }

    /**
     * 判断给出的名称是get、set、is开头
     *
     * @param name
     * @return
     */
    public static boolean isProperty(String name) {
        return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
    }

    /**
     * 判断给出的名称是否是get、is开头
     *
     * @param name
     * @return
     */
    public static boolean isGetter(String name) {
        return name.startsWith("get") || name.startsWith("is");
    }

    /**
     * 判断给出的名称是否以set开头
     *
     * @param name
     * @return
     */
    public static boolean isSetter(String name) {
        return name.startsWith("set");
    }

}
