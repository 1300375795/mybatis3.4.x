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
package org.apache.ibatis.executor.loader;

import java.io.ObjectStreamException;

/**
 * 代理的时候如果方法没有writeReplace方法
 * 那么会在接口数组里面加上这个接口
 * 从而让被代理的对象有这个方法
 *
 * @author Eduardo Macarron
 */
public interface WriteReplaceInterface {

    Object writeReplace() throws ObjectStreamException;

}
