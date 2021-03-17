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
package org.apache.ibatis.reflection;

import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * 系统元对象
 *
 * @author Clinton Begin
 */
public final class SystemMetaObject {

    /**
     * 默认的对象工厂
     */
    public static final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();

    /**
     * 默认的对象包装工厂
     */
    public static final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();

    /**
     * 空对象所对应的元对象
     */
    public static final MetaObject NULL_META_OBJECT = MetaObject
            .forObject(NullObject.class, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY,
                    new DefaultReflectorFactory());

    /**
     * 构造函数
     */
    private SystemMetaObject() {
        // Prevent Instantiation of Static Class
    }

    /**
     * 空对象
     */
    private static class NullObject {
    }

    /**
     * 构建某个对象的源对象
     *
     * @param object
     * @return
     */
    public static MetaObject forObject(Object object) {
        return MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY,
                new DefaultReflectorFactory());
    }

}
