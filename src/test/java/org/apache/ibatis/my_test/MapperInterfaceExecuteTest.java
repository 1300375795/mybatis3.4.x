package org.apache.ibatis.my_test;

import java.io.Reader;
import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Section;
import org.apache.ibatis.domain.blog.mappers.AuthorMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 测试mapper接口执行调用数据库的流程
 *
 * @author YDG
 * @description
 * @since 2021/10/21
 */
public class MapperInterfaceExecuteTest extends BaseDataTest {

    private static SqlSessionFactory sqlMapper;

    @BeforeClass
    public static void setup() throws Exception {
        createBlogDataSource();
        final String resource = "org/apache/ibatis/builder/MapperConfig.xml";
        final Reader reader = Resources.getResourceAsReader(resource);
        sqlMapper = new SqlSessionFactoryBuilder().build(reader);
    }

    /**
     * 测试简单查询流程 mapper接口形式
     */
    @Test
    public void shouldExecuteSelectOneAuthorUsingMapperClass() {
        SqlSession session = sqlMapper.openSession();
        try {
            AuthorMapper mapper = session.getMapper(AuthorMapper.class);
            Author author = mapper.selectAuthor(101);
            assertEquals(101, author.getId());
        } finally {
            session.close();
        }
    }

    /**
     * 测试简单查询流程 直接基于SqlSession进行操作
     *
     * @throws Exception
     */
    @Test
    public void shouldSelectOneAuthor() throws Exception {
        SqlSession session = sqlMapper.openSession();
        try {
            Author author = session
                    .selectOne("org.apache.ibatis.domain.blog.mappers.AuthorMapper.selectAuthor", new Author(101));
            assertEquals(101, author.getId());
            assertEquals(Section.NEWS, author.getFavouriteSection());
        } finally {
            session.close();
        }
    }

    /**
     * 查询插入数据流程
     *
     * @throws Exception
     */
    @Test
    public void shouldInsertAuthorUsingMapperClass() throws Exception {
        SqlSession session = sqlMapper.openSession();
        try {
            AuthorMapper mapper = session.getMapper(AuthorMapper.class);
            Author expected = new Author(500, "cbegin", "******", "cbegin@somewhere.com", "Something...", null);
            mapper.insertAuthor(expected);
            Author actual = mapper.selectAuthor(500);
            assertNotNull(actual);
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getUsername(), actual.getUsername());
            assertEquals(expected.getPassword(), actual.getPassword());
            assertEquals(expected.getEmail(), actual.getEmail());
            assertEquals(expected.getBio(), actual.getBio());
        } finally {
            session.close();
        }
    }

}
