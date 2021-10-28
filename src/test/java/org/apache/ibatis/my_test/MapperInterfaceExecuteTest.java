package org.apache.ibatis.my_test;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Section;
import org.apache.ibatis.domain.blog.mappers.AuthorMapper;
import org.apache.ibatis.domain.blog.mappers.BlogMapper;
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
    public void testShouldExecuteSelectOneAuthorUsingMapperClass() {
        SqlSession session = sqlMapper.openSession();
        try {
            AuthorMapper authorMapper = session.getMapper(AuthorMapper.class);
            Author author = authorMapper.selectAuthor(101);
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

    /**
     * 测试二级缓存
     */
    @Test
    public void testMapperCache() {
        try (SqlSession session1 = sqlMapper.openSession(); SqlSession session2 = sqlMapper.openSession();) {
            //其他会话查询
            AuthorMapper authorMapper1 = session1.getMapper(AuthorMapper.class);
            BlogMapper blogMapper1 = session1.getMapper(BlogMapper.class);
            Author author1 = authorMapper1.selectAuthor(101);
            List<Map> maps1 = blogMapper1.selectAllPosts();
            //update、insert、delete 等默认flushCache的值 是true  在XMLStatementBuilder的parseStatementNode方法中处理
            //如果是update、insert、delete等操作会清空缓存 （如果commit在前 update在后  那么清空失败）
            authorMapper1.updateAuthor(author1);
            //需要提交其他会话才能获取到
            session1.commit();
            //其他会话查询
            //可以通过在xml中对应的select 语句中加属性来控制二级缓存的一些逻辑
            AuthorMapper authorMapper2 = session2.getMapper(AuthorMapper.class);
            BlogMapper blogMapper2 = session2.getMapper(BlogMapper.class);
            //如果flushCache=true的话 会在查询之前情况掉缓存中的内容
            Author author2 = authorMapper2.selectAuthor(101);
            //如果设置useCache=false 那么这个sql就不会启用二级缓存
            List<Map> maps2 = blogMapper2.selectAllPosts();
        }
    }

}
