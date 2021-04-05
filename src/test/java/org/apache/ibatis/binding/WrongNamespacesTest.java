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
package org.apache.ibatis.binding;

import com.sun.xml.internal.bind.v2.TODO;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

public class WrongNamespacesTest {

    /**
     * mapper接口跟对应的xml文件namespace不一样的话报错
     *
     * @throws Exception
     */
    @Test(expected = RuntimeException.class)
    public void shouldFailForWrongNamespace() throws Exception {
        Configuration configuration = new Configuration();
        configuration.addMapper(WrongNamespaceMapper.class);
    }

    /**
     * mapper接口对应的xml文件的namespace没有报错
     *
     * @throws Exception
     */
    @Test(expected = RuntimeException.class)
    public void shouldFailForMissingNamespace() throws Exception {
        Configuration configuration = new Configuration();
        configuration.addMapper(MissingNamespaceMapper.class);
    }

}
