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
package org.apache.ibatis.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.datasource.jndi.JndiDataSourceFactory;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ReuseExecutor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.loader.cglib.CglibProxyFactory;
import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.InterceptorChain;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 全局配置
 * // TODO: 2021/3/10 CallYeDeGuo 非常核心的一个类
 *
 * @author Clinton Begin
 */
public class Configuration {

    /**
     * 环境信息
     */
    protected Environment environment;

    protected boolean safeRowBoundsEnabled;
    protected boolean safeResultHandlerEnabled = true;
    protected boolean mapUnderscoreToCamelCase;
    protected boolean aggressiveLazyLoading;
    protected boolean multipleResultSetsEnabled = true;

    /**
     * 是否使用主键生成器
     */
    protected boolean useGeneratedKeys;

    protected boolean useColumnLabel = true;

    /**
     * 是否允许缓存
     */
    protected boolean cacheEnabled = true;
    protected boolean callSettersOnNulls;
    protected boolean useActualParamName = true;
    protected boolean returnInstanceForEmptyRow;

    /**
     * 日志前缀
     */
    protected String logPrefix;

    /**
     * 日志操作实现类
     */
    protected Class<? extends Log> logImpl;

    /**
     * 虚拟文件操作实现类
     */
    protected Class<? extends VFS> vfsImpl;

    /**
     * 本地环境设置 一级缓存
     */
    protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;

    /**
     * null的jdbc类型
     */
    protected JdbcType jdbcTypeForNull = JdbcType.OTHER;

    /**
     * 延迟加载触发方法
     */
    protected Set<String> lazyLoadTriggerMethods = new HashSet<String>(
            Arrays.asList(new String[] { "equals", "clone", "hashCode", "toString" }));

    /**
     * 默认的Statement超时时间
     */
    protected Integer defaultStatementTimeout;

    /**
     * 这是尝试影响驱动程序每次批量返回的结果行数和这个设置值相等
     */
    protected Integer defaultFetchSize;

    /**
     * 默认的执行器类型
     */
    protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;

    /**
     * 默认的自动映射设置
     */
    protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;

    /**
     * 默认的未知字段自动映射设置
     */
    protected AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior = AutoMappingUnknownColumnBehavior.NONE;

    /**
     * 属性配置变量配置map
     */
    protected Properties variables = new Properties();

    /**
     * 默认的反射器工厂 里面会缓存对象的所有的方法以及属性等
     */
    protected ReflectorFactory reflectorFactory = new DefaultReflectorFactory();

    /**
     * 默认的对象工厂 负责通过反射创建对象
     */
    protected ObjectFactory objectFactory = new DefaultObjectFactory();

    /**
     * 默认的对象包装工厂
     */
    protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();

    /**
     * 是否允许懒加载
     */
    protected boolean lazyLoadingEnabled = false;

    /**
     * 代理工厂 实现类为javassist 一个开源的字节码类库
     */
    protected ProxyFactory proxyFactory = new JavassistProxyFactory(); // #224 Using internal Javassist instead of OGNL

    /**
     * 数据库id 如果没有额外设置 这个的值就是对应的数据库的产品名称 比如MySQL
     */
    protected String databaseId;

    /**
     * Configuration factory class.
     * Used to create Configuration for loading deserialized unread properties.
     * 配置工厂class对象
     * 用于创建用于加载反序列化的未读属性的配置。
     *
     * @see <a href='https://code.google.com/p/mybatis/issues/detail?id=300'>Issue 300 (google code)</a>
     */
    protected Class<?> configurationFactory;

    /**
     * 映射注册器
     */
    protected final MapperRegistry mapperRegistry = new MapperRegistry(this);

    /**
     * 拦截器链
     */
    protected final InterceptorChain interceptorChain = new InterceptorChain();

    /**
     * 类型处理器注册器
     */
    protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();

    /**
     * 类型别名注册器
     */
    protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();

    /**
     * 语言驱动注册器
     */
    protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();

    /**
     * 已映射的声明map
     */
    protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>(
            "Mapped Statements collection");

    /**
     * 缓存集合 id为namespace
     */
    protected final Map<String, Cache> caches = new StrictMap<Cache>("Caches collection");
    protected final Map<String, ResultMap> resultMaps = new StrictMap<ResultMap>("Result Maps collection");
    protected final Map<String, ParameterMap> parameterMaps = new StrictMap<ParameterMap>("Parameter Maps collection");
    protected final Map<String, KeyGenerator> keyGenerators = new StrictMap<KeyGenerator>("Key Generators collection");

    /**
     * 已经加载过的资源
     */
    protected final Set<String> loadedResources = new HashSet<String>();

    /**
     * sql片段map
     */
    protected final Map<String, XNode> sqlFragments = new StrictMap<XNode>(
            "XML fragments parsed from previous mappers");

    /**
     * 不完整的xml声明构造器
     * 为什么会不完整 可能图中抛出了IncompleteElementException异常
     */
    protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<XMLStatementBuilder>();

    /**
     * 不完整的缓存引用解析器
     */
    protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<CacheRefResolver>();

    /**
     * 不完整的结果map解析器
     */
    protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<ResultMapResolver>();

    /**
     * 不完整的方法解析器
     */
    protected final Collection<MethodResolver> incompleteMethods = new LinkedList<MethodResolver>();

    /**
     * A map holds cache-ref relationship. The key is the namespace that
     * references a cache bound to another namespace and the value is the
     * namespace which the actual cache is bound to.
     * 缓存引用map
     */
    protected final Map<String, String> cacheRefMap = new HashMap<String, String>();

    /**
     * 构造函数
     *
     * @param environment
     */
    public Configuration(Environment environment) {
        this();
        this.environment = environment;
    }

    /**
     * 构造函数
     */
    public Configuration() {
        //注册jdbc事务工厂class对应的别名
        typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
        //注册被管理的事务工class对应的别名
        typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);
        //注册jndi数据源工厂class对应的别名
        typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
        //注册池化数据源工厂class对应的别名
        typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
        //注册没有池化的数据源工厂class对应的别名
        typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);
        //注册无限期缓存对应的别名
        typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
        //注册新建新出缓存class对应的别名--装饰者
        typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
        //注册最新最近最少使用缓存class对应的别名--装饰者
        typeAliasRegistry.registerAlias("LRU", LruCache.class);
        //注册软引用缓存class对应的别名--装饰者
        //软引用会在系统即将抛出oom的时候回收掉这类引用 比较合适用来作为缓存
        typeAliasRegistry.registerAlias("SOFT", SoftCache.class);
        //注册弱引用缓存class对应的别名--装饰者
        //在垃圾回收器线程扫描它所管辖的内存区域的过程中，一旦发现了具有弱引用的对象，不管当前内存空间足够与否，都会回收它的内存。
        typeAliasRegistry.registerAlias("WEAK", WeakCache.class);
        //注册供应商数据库id提供者class的别名
        typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);
        //注册xml语言驱动class对应的别名  默认的语言驱动
        typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
        //注册原生语言驱动class对应的别名
        typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);
        //注册日志实现class对应的别名
        typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);

        typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
        typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
        typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
        typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
        typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
        typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);
        //注册cglib代理工厂class对应的别名
        typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
        //注册Javassist代理工厂class对应的别名
        typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);
        //设置默认的语言驱动
        languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
        //注册原生的语言驱动
        languageRegistry.register(RawLanguageDriver.class);
    }

    /**
     * 获取日志前缀
     *
     * @return
     */
    public String getLogPrefix() {
        return logPrefix;
    }

    /**
     * 设置日志前缀
     *
     * @param logPrefix
     */
    public void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    /**
     * 获取日志实现类class
     *
     * @return
     */
    public Class<? extends Log> getLogImpl() {
        return logImpl;
    }

    /**
     * 设置自定义的日志实现类class
     *
     * @param logImpl
     */
    public void setLogImpl(Class<? extends Log> logImpl) {
        if (logImpl != null) {
            this.logImpl = logImpl;
            LogFactory.useCustomLogging(this.logImpl);
        }
    }

    /**
     * 获取vfs实现class
     *
     * @return
     */
    public Class<? extends VFS> getVfsImpl() {
        return this.vfsImpl;
    }

    /**
     * 设置用户自定义的cfs实现class
     *
     * @param vfsImpl
     */
    public void setVfsImpl(Class<? extends VFS> vfsImpl) {
        if (vfsImpl != null) {
            this.vfsImpl = vfsImpl;
            VFS.addImplClass(this.vfsImpl);
        }
    }

    public boolean isCallSettersOnNulls() {
        return callSettersOnNulls;
    }

    public void setCallSettersOnNulls(boolean callSettersOnNulls) {
        this.callSettersOnNulls = callSettersOnNulls;
    }

    public boolean isUseActualParamName() {
        return useActualParamName;
    }

    public void setUseActualParamName(boolean useActualParamName) {
        this.useActualParamName = useActualParamName;
    }

    public boolean isReturnInstanceForEmptyRow() {
        return returnInstanceForEmptyRow;
    }

    public void setReturnInstanceForEmptyRow(boolean returnEmptyInstance) {
        this.returnInstanceForEmptyRow = returnEmptyInstance;
    }

    /**
     * 获取数据库id
     *
     * @return
     */
    public String getDatabaseId() {
        return databaseId;
    }

    /**
     * 设置数据库id
     *
     * @param databaseId
     */
    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }

    /**
     * 获取配置工厂class
     *
     * @return
     */
    public Class<?> getConfigurationFactory() {
        return configurationFactory;
    }

    /**
     * 设置配置工厂class
     *
     * @param configurationFactory
     */
    public void setConfigurationFactory(Class<?> configurationFactory) {
        this.configurationFactory = configurationFactory;
    }

    public boolean isSafeResultHandlerEnabled() {
        return safeResultHandlerEnabled;
    }

    public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled) {
        this.safeResultHandlerEnabled = safeResultHandlerEnabled;
    }

    public boolean isSafeRowBoundsEnabled() {
        return safeRowBoundsEnabled;
    }

    public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled) {
        this.safeRowBoundsEnabled = safeRowBoundsEnabled;
    }

    public boolean isMapUnderscoreToCamelCase() {
        return mapUnderscoreToCamelCase;
    }

    public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
        this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
    }

    /**
     * 添加已经添加过的资源
     *
     * @param resource
     */
    public void addLoadedResource(String resource) {
        loadedResources.add(resource);
    }

    /**
     * 是否已经加载过
     *
     * @param resource
     * @return
     */
    public boolean isResourceLoaded(String resource) {
        return loadedResources.contains(resource);
    }

    /**
     * 获取环境
     *
     * @return
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * 设置环境
     *
     * @param environment
     */
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 获取自动映射策略
     *
     * @return
     */
    public AutoMappingBehavior getAutoMappingBehavior() {
        return autoMappingBehavior;
    }

    /**
     * 设置自动映射策略
     *
     * @param autoMappingBehavior
     */
    public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {
        this.autoMappingBehavior = autoMappingBehavior;
    }

    /**
     * 获取自动映射位置字段策略
     *
     * @since 3.4.0
     */
    public AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior() {
        return autoMappingUnknownColumnBehavior;
    }

    /**
     * 设置自动映射位置字段策略
     *
     * @since 3.4.0
     */
    public void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior) {
        this.autoMappingUnknownColumnBehavior = autoMappingUnknownColumnBehavior;
    }

    /**
     * 获取是否允许懒加载
     *
     * @return
     */
    public boolean isLazyLoadingEnabled() {
        return lazyLoadingEnabled;
    }

    /**
     * 设置是否允许懒加载
     *
     * @param lazyLoadingEnabled
     */
    public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
        this.lazyLoadingEnabled = lazyLoadingEnabled;
    }

    /**
     * 获取代理工厂
     *
     * @return
     */
    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    /**
     * 设置代理工厂
     *
     * @param proxyFactory
     */
    public void setProxyFactory(ProxyFactory proxyFactory) {
        if (proxyFactory == null) {
            proxyFactory = new JavassistProxyFactory();
        }
        this.proxyFactory = proxyFactory;
    }

    public boolean isAggressiveLazyLoading() {
        return aggressiveLazyLoading;
    }

    public void setAggressiveLazyLoading(boolean aggressiveLazyLoading) {
        this.aggressiveLazyLoading = aggressiveLazyLoading;
    }

    public boolean isMultipleResultSetsEnabled() {
        return multipleResultSetsEnabled;
    }

    public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled) {
        this.multipleResultSetsEnabled = multipleResultSetsEnabled;
    }

    public Set<String> getLazyLoadTriggerMethods() {
        return lazyLoadTriggerMethods;
    }

    public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods) {
        this.lazyLoadTriggerMethods = lazyLoadTriggerMethods;
    }

    /**
     * 是否使用主键生成器
     *
     * @return
     */
    public boolean isUseGeneratedKeys() {
        return useGeneratedKeys;
    }

    /**
     * 设置是否使用主键生成器
     *
     * @param useGeneratedKeys
     */
    public void setUseGeneratedKeys(boolean useGeneratedKeys) {
        this.useGeneratedKeys = useGeneratedKeys;
    }

    /**
     * 获取默认的执行器类型
     *
     * @return
     */
    public ExecutorType getDefaultExecutorType() {
        return defaultExecutorType;
    }

    /**
     * 设置默认的执行器类型
     *
     * @param defaultExecutorType
     */
    public void setDefaultExecutorType(ExecutorType defaultExecutorType) {
        this.defaultExecutorType = defaultExecutorType;
    }

    /**
     * 获取是否允许缓存
     *
     * @return
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * 设置是否允许缓存
     *
     * @param cacheEnabled
     */
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * 获取默认的声明超时时间
     *
     * @return
     */
    public Integer getDefaultStatementTimeout() {
        return defaultStatementTimeout;
    }

    /**
     * 设置默认的声明超时时间
     *
     * @param defaultStatementTimeout
     */
    public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {
        this.defaultStatementTimeout = defaultStatementTimeout;
    }

    /**
     * @since 3.3.0
     */
    public Integer getDefaultFetchSize() {
        return defaultFetchSize;
    }

    /**
     * @since 3.3.0
     */
    public void setDefaultFetchSize(Integer defaultFetchSize) {
        this.defaultFetchSize = defaultFetchSize;
    }

    public boolean isUseColumnLabel() {
        return useColumnLabel;
    }

    public void setUseColumnLabel(boolean useColumnLabel) {
        this.useColumnLabel = useColumnLabel;
    }

    /**
     * 获取本地缓存类型
     *
     * @return
     */
    public LocalCacheScope getLocalCacheScope() {
        return localCacheScope;
    }

    /**
     * 设置本地缓存类型
     *
     * @param localCacheScope
     */
    public void setLocalCacheScope(LocalCacheScope localCacheScope) {
        this.localCacheScope = localCacheScope;
    }

    /**
     * 获取null类型对应的jdbc
     *
     * @return
     */
    public JdbcType getJdbcTypeForNull() {
        return jdbcTypeForNull;
    }

    /**
     * 设置null类型对应的jdbc
     *
     * @param jdbcTypeForNull
     */
    public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {
        this.jdbcTypeForNull = jdbcTypeForNull;
    }

    /**
     * 获取属性配置变量配置
     *
     * @return
     */
    public Properties getVariables() {
        return variables;
    }

    /**
     * 设置属性变量配置
     *
     * @param variables
     */
    public void setVariables(Properties variables) {
        this.variables = variables;
    }

    /**
     * 获取类型处理器注册器
     *
     * @return
     */
    public TypeHandlerRegistry getTypeHandlerRegistry() {
        return typeHandlerRegistry;
    }

    /**
     * Set a default {@link TypeHandler} class for {@link Enum}.
     * A default {@link TypeHandler} is {@link org.apache.ibatis.type.EnumTypeHandler}.
     * 注册默认的枚举类型处理器
     *
     * @param typeHandler a type handler class for {@link Enum}
     * @since 3.4.5
     */
    public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
        if (typeHandler != null) {
            getTypeHandlerRegistry().setDefaultEnumTypeHandler(typeHandler);
        }
    }

    /**
     * 获取类型别名处理器
     *
     * @return
     */
    public TypeAliasRegistry getTypeAliasRegistry() {
        return typeAliasRegistry;
    }

    /**
     * 获取映射注册器
     *
     * @since 3.2.2
     */
    public MapperRegistry getMapperRegistry() {
        return mapperRegistry;
    }

    /**
     * 获取反射工厂
     *
     * @return
     */
    public ReflectorFactory getReflectorFactory() {
        return reflectorFactory;
    }

    /**
     * 设置反射工厂
     *
     * @param reflectorFactory
     */
    public void setReflectorFactory(ReflectorFactory reflectorFactory) {
        this.reflectorFactory = reflectorFactory;
    }

    /**
     * 获取对象工厂
     *
     * @return
     */
    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    /**
     * 设置对象工厂
     *
     * @param objectFactory
     */
    public void setObjectFactory(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    /**
     * 获取对象包装工厂
     *
     * @return
     */
    public ObjectWrapperFactory getObjectWrapperFactory() {
        return objectWrapperFactory;
    }

    /**
     * 设置对应包装工厂
     *
     * @param objectWrapperFactory
     */
    public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
        this.objectWrapperFactory = objectWrapperFactory;
    }

    /**
     * 获取所有的拦截器
     *
     * @since 3.2.2
     */
    public List<Interceptor> getInterceptors() {
        return interceptorChain.getInterceptors();
    }

    /**
     * 获取语言驱动注册器
     *
     * @return
     */
    public LanguageDriverRegistry getLanguageRegistry() {
        return languageRegistry;
    }

    /**
     * 设置默认的缓存启动
     *
     * @param driver
     */
    public void setDefaultScriptingLanguage(Class<?> driver) {
        if (driver == null) {
            driver = XMLLanguageDriver.class;
        }
        getLanguageRegistry().setDefaultDriverClass(driver);
    }

    /**
     * 获取默认的语言驱动
     *
     * @return
     */
    public LanguageDriver getDefaultScriptingLanguageInstance() {
        return languageRegistry.getDefaultDriver();
    }

    /**
     * @deprecated Use {@link #getDefaultScriptingLanguageInstance()}
     */
    @Deprecated
    public LanguageDriver getDefaultScriptingLanuageInstance() {
        return getDefaultScriptingLanguageInstance();
    }

    /**
     * 创建一个对象的元对象
     *
     * @param object
     * @return
     */
    public MetaObject newMetaObject(Object object) {
        return MetaObject.forObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
    }

    /**
     * 创建参数处理器
     *
     * @param mappedStatement
     * @param parameterObject
     * @param boundSql
     * @return
     */
    public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject,
            BoundSql boundSql) {
        ParameterHandler parameterHandler = mappedStatement.getLang()
                .createParameterHandler(mappedStatement, parameterObject, boundSql);
        parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
        return parameterHandler;
    }

    /**
     * 创建结果集合处理器
     *
     * @param executor
     * @param mappedStatement
     * @param rowBounds
     * @param parameterHandler
     * @param resultHandler
     * @param boundSql
     * @return
     */
    public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds,
            ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql) {
        ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler,
                resultHandler, boundSql, rowBounds);
        resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
        return resultSetHandler;
    }

    /**
     * 创建声明处理器
     *
     * @param executor
     * @param mappedStatement
     * @param parameterObject
     * @param rowBounds
     * @param resultHandler
     * @param boundSql
     * @return
     */
    public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement,
            Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject,
                rowBounds, resultHandler, boundSql);
        statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
        return statementHandler;
    }

    /**
     * 创建执行器
     *
     * @param transaction
     * @return
     */
    public Executor newExecutor(Transaction transaction) {
        return newExecutor(transaction, defaultExecutorType);
    }

    /**
     * 创建执行器
     *
     * @param transaction
     * @param executorType
     * @return
     */
    public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
        executorType = executorType == null ? defaultExecutorType : executorType;
        executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
        Executor executor;
        //根据给出的执行器类型创建对应的执行器
        if (ExecutorType.BATCH == executorType) {
            executor = new BatchExecutor(this, transaction);
        } else if (ExecutorType.REUSE == executorType) {
            executor = new ReuseExecutor(this, transaction);
        } else {
            executor = new SimpleExecutor(this, transaction);
        }
        //如果允许缓存 那么执行器类型就是缓存执行器
        if (cacheEnabled) {
            executor = new CachingExecutor(executor);
        }
        executor = (Executor) interceptorChain.pluginAll(executor);
        return executor;
    }

    /**
     * 添加主键生成器
     *
     * @param id
     * @param keyGenerator
     */
    public void addKeyGenerator(String id, KeyGenerator keyGenerator) {
        keyGenerators.put(id, keyGenerator);
    }

    /**
     * 获取主键生成器map的键集合
     *
     * @return
     */
    public Collection<String> getKeyGeneratorNames() {
        return keyGenerators.keySet();
    }

    /**
     * 获取主键生成器集合
     *
     * @return
     */
    public Collection<KeyGenerator> getKeyGenerators() {
        return keyGenerators.values();
    }

    /**
     * 根据id获取主键生成器
     *
     * @param id
     * @return
     */
    public KeyGenerator getKeyGenerator(String id) {
        return keyGenerators.get(id);
    }

    /**
     * 判断给出的id是否存在主键生成器
     *
     * @param id
     * @return
     */
    public boolean hasKeyGenerator(String id) {
        return keyGenerators.containsKey(id);
    }

    /**
     * 添加缓存
     *
     * @param cache
     */
    public void addCache(Cache cache) {
        caches.put(cache.getId(), cache);
    }

    /**
     * 获取缓存map键集合
     *
     * @return
     */
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }

    /**
     * 获取缓存集合
     *
     * @return
     */
    public Collection<Cache> getCaches() {
        return caches.values();
    }

    /**
     * 根据id获取相应的缓存
     *
     * @param id 为namespace
     * @return
     */
    public Cache getCache(String id) {
        return caches.get(id);
    }

    /**
     * 判断给出的id是否存在缓存
     *
     * @param id
     * @return
     */
    public boolean hasCache(String id) {
        return caches.containsKey(id);
    }

    /**
     * 添加结果map
     *
     * @param rm
     */
    public void addResultMap(ResultMap rm) {
        resultMaps.put(rm.getId(), rm);
        checkLocallyForDiscriminatedNestedResultMaps(rm);
        checkGloballyForDiscriminatedNestedResultMaps(rm);
    }

    /**
     * 获取结果map键集合
     *
     * @return
     */
    public Collection<String> getResultMapNames() {
        return resultMaps.keySet();
    }

    /**
     * 获取结果map集合
     *
     * @return
     */
    public Collection<ResultMap> getResultMaps() {
        return resultMaps.values();
    }

    /**
     * 根据给出的id获取结果map
     *
     * @param id
     * @return
     */
    public ResultMap getResultMap(String id) {
        return resultMaps.get(id);
    }

    /**
     * 判断给出的id是否存在结果map
     *
     * @param id
     * @return
     */
    public boolean hasResultMap(String id) {
        return resultMaps.containsKey(id);
    }

    /**
     * 添加参数map
     *
     * @param pm
     */
    public void addParameterMap(ParameterMap pm) {
        parameterMaps.put(pm.getId(), pm);
    }

    /**
     * 获取参数map键集合
     *
     * @return
     */
    public Collection<String> getParameterMapNames() {
        return parameterMaps.keySet();
    }

    /**
     * 获取参数map集合
     *
     * @return
     */
    public Collection<ParameterMap> getParameterMaps() {
        return parameterMaps.values();
    }

    /**
     * 根据给出的id获取参数map
     *
     * @param id
     * @return
     */
    public ParameterMap getParameterMap(String id) {
        return parameterMaps.get(id);
    }

    /**
     * 判断给出的id是否存在参数map
     *
     * @param id
     * @return
     */
    public boolean hasParameterMap(String id) {
        return parameterMaps.containsKey(id);
    }

    /**
     * 添加已映射的声明
     *
     * @param ms
     */
    public void addMappedStatement(MappedStatement ms) {
        mappedStatements.put(ms.getId(), ms);
    }

    /**
     * 获取已映射的声明的map键集合
     *
     * @return
     */
    public Collection<String> getMappedStatementNames() {
        buildAllStatements();
        return mappedStatements.keySet();
    }

    /**
     * 获取已映射的声明集合
     *
     * @return
     */
    public Collection<MappedStatement> getMappedStatements() {
        buildAllStatements();
        return mappedStatements.values();
    }

    /**
     * 获取不完整的声明集合
     *
     * @return
     */
    public Collection<XMLStatementBuilder> getIncompleteStatements() {
        return incompleteStatements;
    }

    /**
     * 添加不完整的sql声明节点
     *
     * @param incompleteStatement
     */
    public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
        incompleteStatements.add(incompleteStatement);
    }

    /**
     * 获取不完整的缓存引用解析器集合
     *
     * @return
     */
    public Collection<CacheRefResolver> getIncompleteCacheRefs() {
        return incompleteCacheRefs;
    }

    /**
     * 添加不完整的缓存引用解析器
     *
     * @param incompleteCacheRef
     */
    public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
        incompleteCacheRefs.add(incompleteCacheRef);
    }

    /**
     * 获取不完整的结果map解析器集合
     *
     * @return
     */
    public Collection<ResultMapResolver> getIncompleteResultMaps() {
        return incompleteResultMaps;
    }

    /**
     * 添加不完整的结果map解析器
     *
     * @param resultMapResolver
     */
    public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
        incompleteResultMaps.add(resultMapResolver);
    }

    /**
     * 添加不完整的方法解析器
     *
     * @param builder
     */
    public void addIncompleteMethod(MethodResolver builder) {
        incompleteMethods.add(builder);
    }

    /**
     * 获取不完整的方法解析器集合
     *
     * @return
     */
    public Collection<MethodResolver> getIncompleteMethods() {
        return incompleteMethods;
    }

    /**
     * 拿到这个id的映射声明
     *
     * @param id mapperInterface+methodName
     * @return
     */
    public MappedStatement getMappedStatement(String id) {
        return this.getMappedStatement(id, true);
    }

    /**
     * 根据statementId获取已映射的声明
     *
     * @param id                           statementId
     * @param validateIncompleteStatements 是否验证不完整的声明
     * @return
     */
    public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
        //如果验证不完整的声明
        if (validateIncompleteStatements) {
            buildAllStatements();
        }
        return mappedStatements.get(id);
    }

    public Map<String, XNode> getSqlFragments() {
        return sqlFragments;
    }

    public void addInterceptor(Interceptor interceptor) {
        interceptorChain.addInterceptor(interceptor);
    }

    public void addMappers(String packageName, Class<?> superType) {
        mapperRegistry.addMappers(packageName, superType);
    }

    /**
     * 添加这个包下面的所有的mapper
     *
     * @param packageName
     */
    public void addMappers(String packageName) {
        mapperRegistry.addMappers(packageName);
    }

    /**
     * 映射注册器中加上这个映射
     *
     * @param type
     * @param <T>
     */
    public <T> void addMapper(Class<T> type) {
        mapperRegistry.addMapper(type);
    }

    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        return mapperRegistry.getMapper(type, sqlSession);
    }

    public boolean hasMapper(Class<?> type) {
        return mapperRegistry.hasMapper(type);
    }

    public boolean hasStatement(String statementName) {
        return hasStatement(statementName, true);
    }

    public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
        if (validateIncompleteStatements) {
            buildAllStatements();
        }
        return mappedStatements.containsKey(statementName);
    }

    /**
     * 添加缓存引用
     *
     * @param namespace           xml的namespace
     * @param referencedNamespace xml中cache-ref节点中写的namespace
     */
    public void addCacheRef(String namespace, String referencedNamespace) {
        cacheRefMap.put(namespace, referencedNamespace);
    }

    /*
     * Parses all the unprocessed statement nodes in the cache. It is recommended
     * to call this method once all the mappers are added as it provides fail-fast
     * statement validation.
     */
    protected void buildAllStatements() {
        if (!incompleteResultMaps.isEmpty()) {
            synchronized (incompleteResultMaps) {
                // This always throws a BuilderException.
                incompleteResultMaps.iterator().next().resolve();
            }
        }
        if (!incompleteCacheRefs.isEmpty()) {
            synchronized (incompleteCacheRefs) {
                // This always throws a BuilderException.
                incompleteCacheRefs.iterator().next().resolveCacheRef();
            }
        }
        if (!incompleteStatements.isEmpty()) {
            synchronized (incompleteStatements) {
                // This always throws a BuilderException.
                incompleteStatements.iterator().next().parseStatementNode();
            }
        }
        if (!incompleteMethods.isEmpty()) {
            synchronized (incompleteMethods) {
                // This always throws a BuilderException.
                incompleteMethods.iterator().next().resolve();
            }
        }
    }

    /*
     * Extracts namespace from fully qualified statement id.
     *
     * @param statementId
     * @return namespace or null when id does not contain period.
     */
    protected String extractNamespace(String statementId) {
        int lastPeriod = statementId.lastIndexOf('.');
        return lastPeriod > 0 ? statementId.substring(0, lastPeriod) : null;
    }

    // Slow but a one time cost. A better solution is welcome.
    protected void checkGloballyForDiscriminatedNestedResultMaps(ResultMap rm) {
        if (rm.hasNestedResultMaps()) {
            for (Map.Entry<String, ResultMap> entry : resultMaps.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof ResultMap) {
                    ResultMap entryResultMap = (ResultMap) value;
                    if (!entryResultMap.hasNestedResultMaps() && entryResultMap.getDiscriminator() != null) {
                        Collection<String> discriminatedResultMapNames = entryResultMap.getDiscriminator()
                                .getDiscriminatorMap().values();
                        if (discriminatedResultMapNames.contains(rm.getId())) {
                            entryResultMap.forceNestedResultMaps();
                        }
                    }
                }
            }
        }
    }

    // Slow but a one time cost. A better solution is welcome.
    protected void checkLocallyForDiscriminatedNestedResultMaps(ResultMap rm) {
        if (!rm.hasNestedResultMaps() && rm.getDiscriminator() != null) {
            for (Map.Entry<String, String> entry : rm.getDiscriminator().getDiscriminatorMap().entrySet()) {
                String discriminatedResultMapName = entry.getValue();
                if (hasResultMap(discriminatedResultMapName)) {
                    ResultMap discriminatedResultMap = resultMaps.get(discriminatedResultMapName);
                    if (discriminatedResultMap.hasNestedResultMaps()) {
                        rm.forceNestedResultMaps();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 严格的hashMap
     * 1:key只能为string
     * 2:不能存放重复的key  否则抛出异常
     *
     * @param <V>
     */
    protected static class StrictMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -4950446264854982944L;

        /**
         * map的名称
         */
        private final String name;

        /**
         * 构造函数
         *
         * @param name
         * @param initialCapacity
         * @param loadFactor
         */
        public StrictMap(String name, int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
            this.name = name;
        }

        /**
         * 构造函数
         *
         * @param name
         * @param initialCapacity
         */
        public StrictMap(String name, int initialCapacity) {
            super(initialCapacity);
            this.name = name;
        }

        /**
         * 构造函数
         *
         * @param name
         */
        public StrictMap(String name) {
            super();
            this.name = name;
        }

        /**
         * 构造函数
         *
         * @param name
         * @param m
         */
        public StrictMap(String name, Map<String, ? extends V> m) {
            super(m);
            this.name = name;
        }

        /**
         * 存放key对应的value
         * 如果map中已经存在这个key 那么抛出异常
         *
         * @param key
         * @param value
         * @return
         */
        @SuppressWarnings("unchecked")
        public V put(String key, V value) {
            if (containsKey(key)) {
                throw new IllegalArgumentException(name + " already contains value for " + key);
            }
            //如果是键中包含.
            if (key.contains(".")) {
                //根据.分割 然后获取简称
                final String shortKey = getShortName(key);
                //获取简称对应的value 如果为null 直接存放简称对应的value
                if (super.get(shortKey) == null) {
                    super.put(shortKey, value);
                } else {
                    //如果不为null 那么将包装后的简称作为value 表示存在多个这个key对应的value
                    super.put(shortKey, (V) new Ambiguity(shortKey));
                }
            }
            //存放key对应的value
            return super.put(key, value);
        }

        /**
         * 获取这个key对应的value
         * 如果没有这个key对应的value 抛出异常
         * 如果value是Ambiguity类型那么抛出异常
         *
         * @param key
         * @return
         */
        public V get(Object key) {
            V value = super.get(key);
            if (value == null) {
                throw new IllegalArgumentException(name + " does not contain value for " + key);
            }
            if (value instanceof Ambiguity) {
                throw new IllegalArgumentException(((Ambiguity) value).getSubject() + " is ambiguous in " + name
                        + " (try using the full name including the namespace, or rename one of the entries)");
            }
            return value;
        }

        /**
         * 获取简称
         * 以.分割后的最后一部分
         * aa.bb.cc
         * 就是cc
         *
         * @param key
         * @return
         */
        private String getShortName(String key) {
            final String[] keyParts = key.split("\\.");
            return keyParts[keyParts.length - 1];
        }

        /**
         * 模糊对象
         */
        protected static class Ambiguity {

            /**
             * 主题
             */
            final private String subject;

            /**
             * 构造函数
             *
             * @param subject
             */
            public Ambiguity(String subject) {
                this.subject = subject;
            }

            /**
             * 获取对象
             *
             * @return
             */
            public String getSubject() {
                return subject;
            }
        }
    }

}
