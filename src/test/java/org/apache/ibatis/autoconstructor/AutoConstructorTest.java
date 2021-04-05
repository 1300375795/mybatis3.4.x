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
package org.apache.ibatis.autoconstructor;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Reader;
import java.sql.Connection;
import java.util.List;

public class AutoConstructorTest {
    private static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void setUp() throws Exception {
        // create a SqlSessionFactory
        final Reader reader = Resources.getResourceAsReader("org/apache/ibatis/autoconstructor/mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        reader.close();

        // populate in-memory database
        final SqlSession session = sqlSessionFactory.openSession();
        final Connection conn = session.getConnection();
        final Reader dbReader = Resources.getResourceAsReader("org/apache/ibatis/autoconstructor/CreateDB.sql");
        final ScriptRunner runner = new ScriptRunner(conn);
        runner.setLogWriter(null);
        runner.runScript(dbReader);
        conn.close();
        dbReader.close();
        session.close();
    }

    /**
     * 测试全部匹配的构造函数
     */
    @Test
    public void fullyPopulatedSubject() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            final Object subject = mapper.getSubject(1);
            Assert.assertNotNull(subject);
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 测试基本类型构造函数集合
     */
    @Test(expected = PersistenceException.class)
    public void primitiveSubjects() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            mapper.getSubjects();
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 测试包装类型对象构造函数
     * 基本跟包装能够兼容
     */
    @Test
    public void wrapperSubject() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            verifySubjects(mapper.getWrapperSubjects());
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 测试注解构造函数
     * 注解构造函数优先级高于非注解的构造函数
     */
    @Test
    public void annotatedSubject() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            verifySubjects(mapper.getAnnotatedSubjects());
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 测试构造函数不匹配 抛出异常
     */
    @Test(expected = PersistenceException.class)
    public void badSubject() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            mapper.getBadSubjects();
        } finally {
            sqlSession.close();
        }
    }

    private void verifySubjects(final List<?> subjects) {
        Assert.assertNotNull(subjects);
        Assertions.assertThat(subjects.size()).isEqualTo(3);
    }
}
