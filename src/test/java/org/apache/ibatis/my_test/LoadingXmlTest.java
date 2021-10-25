package org.apache.ibatis.my_test;

import java.io.InputStream;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.apache.ibatis.builder.CachedAuthorMapper;
import org.apache.ibatis.builder.CustomLongTypeHandler;
import org.apache.ibatis.builder.CustomObjectWrapperFactory;
import org.apache.ibatis.builder.CustomReflectorFactory;
import org.apache.ibatis.builder.CustomStringTypeHandler;
import org.apache.ibatis.builder.ExampleObjectFactory;
import org.apache.ibatis.builder.ExamplePlugin;
import org.apache.ibatis.builder.mapper.CustomMapper;
import org.apache.ibatis.builder.typehandler.CustomIntegerTypeHandler;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Blog;
import org.apache.ibatis.domain.blog.mappers.BlogMapper;
import org.apache.ibatis.domain.blog.mappers.NestedBlogMapper;
import org.apache.ibatis.domain.jpetstore.Cart;
import org.apache.ibatis.executor.loader.cglib.CglibProxyFactory;
import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
import org.apache.ibatis.io.JBoss6VFS;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.EnumTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 测试mybatis的config.xml配置加载流程
 *
 * @author YDG
 * @description
 * @since 2021/10/21
 */
public class LoadingXmlTest {

    /**
     * 测试成功加载最低限度要求的xml配置文件
     *
     * @throws Exception
     */
    @Test
    public void shouldSuccessfullyLoadMinimalXMLConfigFile() throws Exception {
        //第一个是原来的测试用例用到的文件
        String resource = "org/apache/ibatis/builder/MinimalMapperConfig.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        XMLConfigBuilder builder = new XMLConfigBuilder(inputStream);
        Configuration config = builder.parse();
        assertNotNull(config);
        assertThat(config.getAutoMappingBehavior()).isEqualTo(AutoMappingBehavior.PARTIAL);
        assertThat(config.getAutoMappingUnknownColumnBehavior()).isEqualTo(AutoMappingUnknownColumnBehavior.NONE);
        assertThat(config.isCacheEnabled()).isTrue();
        assertThat(config.getProxyFactory()).isInstanceOf(JavassistProxyFactory.class);
        assertThat(config.isLazyLoadingEnabled()).isFalse();
        assertThat(config.isAggressiveLazyLoading()).isFalse();
        assertThat(config.isMultipleResultSetsEnabled()).isTrue();
        assertThat(config.isUseColumnLabel()).isTrue();
        assertThat(config.isUseGeneratedKeys()).isFalse();
        assertThat(config.getDefaultExecutorType()).isEqualTo(ExecutorType.SIMPLE);
        assertNull(config.getDefaultStatementTimeout());
        assertNull(config.getDefaultFetchSize());
        assertThat(config.isMapUnderscoreToCamelCase()).isFalse();
        assertThat(config.isSafeRowBoundsEnabled()).isFalse();
        assertThat(config.getLocalCacheScope()).isEqualTo(LocalCacheScope.SESSION);
        assertThat(config.getJdbcTypeForNull()).isEqualTo(JdbcType.OTHER);
        assertThat(config.getLazyLoadTriggerMethods())
                .isEqualTo((Set<String>) new HashSet<String>(Arrays.asList("equals", "clone", "hashCode", "toString")));
        assertThat(config.isSafeResultHandlerEnabled()).isTrue();
        assertThat(config.getDefaultScriptingLanuageInstance()).isInstanceOf(XMLLanguageDriver.class);
        assertThat(config.isCallSettersOnNulls()).isFalse();
        assertNull(config.getLogPrefix());
        assertNull(config.getLogImpl());
        assertNull(config.getConfigurationFactory());
        assertThat(config.getTypeHandlerRegistry().getTypeHandler(RoundingMode.class))
                .isInstanceOf(EnumTypeHandler.class);
        inputStream.close();
    }

    /**
     * 测试配置了相关的属性的配置文件的加载
     *
     * @throws Exception
     */
    @Test
    public void shouldSuccessfullyLoadXMLConfigFile() throws Exception {
        String resource = "org/apache/ibatis/builder/CustomizedSettingsMapperConfig.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        Properties props = new Properties();
        props.put("prop2", "cccc");
        XMLConfigBuilder builder = new XMLConfigBuilder(inputStream, null, props);
        Configuration config = builder.parse();

        assertThat(config.getAutoMappingBehavior()).isEqualTo(AutoMappingBehavior.NONE);
        assertThat(config.getAutoMappingUnknownColumnBehavior()).isEqualTo(AutoMappingUnknownColumnBehavior.WARNING);
        assertThat(config.isCacheEnabled()).isFalse();
        assertThat(config.getProxyFactory()).isInstanceOf(CglibProxyFactory.class);
        assertThat(config.isLazyLoadingEnabled()).isTrue();
        assertThat(config.isAggressiveLazyLoading()).isTrue();
        assertThat(config.isMultipleResultSetsEnabled()).isFalse();
        assertThat(config.isUseColumnLabel()).isFalse();
        assertThat(config.isUseGeneratedKeys()).isTrue();
        assertThat(config.getDefaultExecutorType()).isEqualTo(ExecutorType.BATCH);
        assertThat(config.getDefaultStatementTimeout()).isEqualTo(10);
        assertThat(config.getDefaultFetchSize()).isEqualTo(100);
        assertThat(config.isMapUnderscoreToCamelCase()).isTrue();
        assertThat(config.isSafeRowBoundsEnabled()).isTrue();
        assertThat(config.getLocalCacheScope()).isEqualTo(LocalCacheScope.STATEMENT);
        assertThat(config.getJdbcTypeForNull()).isEqualTo(JdbcType.NULL);
        assertThat(config.getLazyLoadTriggerMethods()).isEqualTo(
                (Set<String>) new HashSet<String>(Arrays.asList("equals", "clone", "hashCode", "toString", "xxx")));
        assertThat(config.isSafeResultHandlerEnabled()).isFalse();
        assertThat(config.getDefaultScriptingLanuageInstance()).isInstanceOf(RawLanguageDriver.class);
        assertThat(config.isCallSettersOnNulls()).isTrue();
        assertThat(config.getLogPrefix()).isEqualTo("mybatis_");
        assertThat(config.getLogImpl().getName()).isEqualTo(Slf4jImpl.class.getName());
        assertThat(config.getVfsImpl().getName()).isEqualTo(JBoss6VFS.class.getName());
        assertThat(config.getConfigurationFactory().getName()).isEqualTo(String.class.getName());

        assertTrue(config.getTypeAliasRegistry().getTypeAliases().get("blogauthor").equals(Author.class));
        assertTrue(config.getTypeAliasRegistry().getTypeAliases().get("blog").equals(Blog.class));
        assertTrue(config.getTypeAliasRegistry().getTypeAliases().get("cart").equals(Cart.class));

        assertThat(config.getTypeHandlerRegistry().getTypeHandler(Integer.class))
                .isInstanceOf(CustomIntegerTypeHandler.class);
        assertThat(config.getTypeHandlerRegistry().getTypeHandler(Long.class))
                .isInstanceOf(CustomLongTypeHandler.class);
        assertThat(config.getTypeHandlerRegistry().getTypeHandler(String.class))
                .isInstanceOf(CustomStringTypeHandler.class);
        assertThat(config.getTypeHandlerRegistry().getTypeHandler(String.class, JdbcType.VARCHAR))
                .isInstanceOf(CustomStringTypeHandler.class);
        assertThat(config.getTypeHandlerRegistry().getTypeHandler(RoundingMode.class))
                .isInstanceOf(EnumOrdinalTypeHandler.class);

        ExampleObjectFactory objectFactory = (ExampleObjectFactory) config.getObjectFactory();
        assertThat(objectFactory.getProperties().size()).isEqualTo(1);
        assertThat(objectFactory.getProperties().getProperty("objectFactoryProperty")).isEqualTo("100");

        assertThat(config.getObjectWrapperFactory()).isInstanceOf(CustomObjectWrapperFactory.class);

        assertThat(config.getReflectorFactory()).isInstanceOf(CustomReflectorFactory.class);

        ExamplePlugin plugin = (ExamplePlugin) config.getInterceptors().get(0);
        assertThat(plugin.getProperties().size()).isEqualTo(1);
        assertThat(plugin.getProperties().getProperty("pluginProperty")).isEqualTo("100");

        Environment environment = config.getEnvironment();
        assertThat(environment.getId()).isEqualTo("development");
        assertThat(environment.getDataSource()).isInstanceOf(UnpooledDataSource.class);
        assertThat(environment.getTransactionFactory()).isInstanceOf(JdbcTransactionFactory.class);

        assertThat(config.getDatabaseId()).isEqualTo("MySql");

        assertThat(config.getMapperRegistry().getMappers().size()).isEqualTo(4);
        assertThat(config.getMapperRegistry().hasMapper(CachedAuthorMapper.class)).isTrue();
        assertThat(config.getMapperRegistry().hasMapper(CustomMapper.class)).isTrue();
        assertThat(config.getMapperRegistry().hasMapper(BlogMapper.class)).isTrue();
        assertThat(config.getMapperRegistry().hasMapper(NestedBlogMapper.class)).isTrue();
        inputStream.close();
    }

}
