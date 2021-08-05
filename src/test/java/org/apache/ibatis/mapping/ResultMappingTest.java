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

import org.apache.ibatis.session.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * 测试result mapping
 */
@RunWith(MockitoJUnitRunner.class)
public class ResultMappingTest {
    @Mock
    private Configuration configuration;

    /**
     * 测试同时给出了nestedQueryId、nestedResultMapId会报错
     */
    // Issue 697: Association with both a resultMap and a select attribute should throw exception
    @Test(expected = IllegalStateException.class)
    public void shouldThrowErrorWhenBothResultMapAndNestedSelectAreSet() {
        new ResultMapping.Builder(configuration, "prop").nestedQueryId("nested query ID")
                .nestedResultMapId("nested resultMap").build();
    }

    /**
     * 测试nestedQueryId缺失字段会报错
     *
     * @throws Exception
     */
    //Issue 4: column is mandatory on nested queries
    @Test(expected = IllegalStateException.class)
    public void shouldFailWithAMissingColumnInNetstedSelect() throws Exception {
        new ResultMapping.Builder(configuration, "prop").nestedQueryId("nested query ID").build();
    }

}
