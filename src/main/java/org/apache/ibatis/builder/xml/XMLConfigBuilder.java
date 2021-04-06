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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * xml文件配置构建者
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLConfigBuilder extends BaseBuilder {

    /**
     * 是否解析过
     */
    private boolean parsed;

    /**
     * XPath解析器
     */
    private final XPathParser parser;

    /**
     * 环境
     */
    private String environment;

    /**
     * 反射工厂
     */
    private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

    /**
     * 构造函数
     *
     * @param reader
     */
    public XMLConfigBuilder(Reader reader) {
        this(reader, null, null);
    }

    /**
     * 构造函数
     *
     * @param reader
     * @param environment
     */
    public XMLConfigBuilder(Reader reader, String environment) {
        this(reader, environment, null);
    }

    /**
     * 构造函数
     *
     * @param reader
     * @param environment
     * @param props
     */
    public XMLConfigBuilder(Reader reader, String environment, Properties props) {
        this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
    }

    /**
     * 构造函数
     *
     * @param inputStream
     */
    public XMLConfigBuilder(InputStream inputStream) {
        this(inputStream, null, null);
    }

    /**
     * 构造函数
     *
     * @param inputStream
     * @param environment
     */
    public XMLConfigBuilder(InputStream inputStream, String environment) {
        this(inputStream, environment, null);
    }

    /**
     * 构造函数
     *
     * @param inputStream
     * @param environment
     * @param props
     */
    public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
        this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
    }

    /**
     * 构造函数
     *
     * @param parser
     * @param environment
     * @param props
     */
    private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
        // TODO: 2021/3/25 CallYeDeGuo 全局属性在这里创建
        super(new Configuration());
        ErrorContext.instance().resource("SQL Mapper Configuration");
        //这里的this.configuration就是上面的new Configuration()出来的实例
        this.configuration.setVariables(props);
        this.parsed = false;
        this.environment = environment;
        this.parser = parser;
    }

    /**
     * 解析xml文件为配置
     *
     * @return
     */
    public Configuration parse() {
        //如果解析过 抛出异常
        if (parsed) {
            throw new BuilderException("Each XMLConfigBuilder can only be used once.");
        }
        parsed = true;
        parseConfiguration(parser.evalNode("/configuration"));
        return configuration;
    }

    /**
     * 解析配置˙
     * /Users/yedeguo/mybatis3.4.x/src/test/java/org/apache/ibatis/builder/MapperConfig.xml
     * 可以参考上面这个位置的文件
     *
     * @param root
     */
    private void parseConfiguration(XNode root) {
        try {
            //issue #117 read properties first
            //解析xml中的属性配置
            propertiesElement(root.evalNode("properties"));
            //解析xml中的settings配置
            Properties settings = settingsAsProperties(root.evalNode("settings"));
            //解析xml中自定义的vfs
            loadCustomVfs(settings);
            //解析xml中的别名
            typeAliasesElement(root.evalNode("typeAliases"));
            //解析xml中的插件
            pluginElement(root.evalNode("plugins"));
            //解析xml中的对象工厂
            objectFactoryElement(root.evalNode("objectFactory"));
            //解析xml中的对象
            objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
            //解析xml中的放射工厂
            reflectorFactoryElement(root.evalNode("reflectorFactory"));
            //解析settings中的属性到全局配置中
            settingsElement(settings);
            //解析环境变量
            // read it after objectFactory and objectWrapperFactory issue #631
            environmentsElement(root.evalNode("environments"));
            //解析数据库id
            databaseIdProviderElement(root.evalNode("databaseIdProvider"));
            //解析类型处理器
            typeHandlerElement(root.evalNode("typeHandlers"));
            //解析mapper
            mapperElement(root.evalNode("mappers"));
        } catch (Exception e) {
            throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
        }
    }

    /**
     * <settings>
     * <setting name="cacheEnabled" value="true"/>
     * <setting name="lazyLoadingEnabled" value="false"/>
     * <setting name="multipleResultSetsEnabled" value="true"/>
     * <setting name="useColumnLabel" value="true"/>
     * <setting name="useGeneratedKeys" value="false"/>
     * <setting name="defaultExecutorType" value="SIMPLE"/>
     * <setting name="defaultStatementTimeout" value="25"/>
     * </settings>
     * 转换settings属性
     *
     * @param context
     * @return
     */
    private Properties settingsAsProperties(XNode context) {
        if (context == null) {
            return new Properties();
        }
        Properties props = context.getChildrenAsProperties();
        // Check that all settings are known to the configuration class
        MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
        //判断给出的setting配置是不是在全局配置中定义的配置 如果不是抛出异常
        for (Object key : props.keySet()) {
            if (!metaConfig.hasSetter(String.valueOf(key))) {
                throw new BuilderException(
                        "The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
            }
        }
        return props;
    }

    /**
     * 加载自定义的vfs
     *
     * @param props
     * @throws ClassNotFoundException
     */
    private void loadCustomVfs(Properties props) throws ClassNotFoundException {
        //拿到这个属性配置
        String value = props.getProperty("vfsImpl");
        //如果不为空
        if (value != null) {
            //将配置的值按照,进行分割
            String[] clazzes = value.split(",");
            for (String clazz : clazzes) {
                //如果class不为空 那么转换成VFSclass 必将这个class加到用户自定义的vfs列表中
                if (!clazz.isEmpty()) {
                    @SuppressWarnings("unchecked") Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources
                            .classForName(clazz);
                    configuration.setVfsImpl(vfsImpl);
                }
            }
        }
    }

    /**
     * 解析类型别名对象
     *
     * @param parent
     */
    private void typeAliasesElement(XNode parent) {
        if (parent != null) {
            for (XNode child : parent.getChildren()) {
                //如果子节点的名称是package那么拿到这个package的name的值
                if ("package".equals(child.getName())) {
                    String typeAliasPackage = child.getStringAttribute("name");
                    //将这个包类下面的所有文件别名化
                    configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
                }
                //其他的情形 就是子节点是typeAlias名称那么拿到这个别名跟他的类型
                else {
                    String alias = child.getStringAttribute("alias");
                    String type = child.getStringAttribute("type");
                    try {
                        Class<?> clazz = Resources.classForName(type);
                        //如果alias没写 那么直接拿这个类的简单名称作为别名
                        if (alias == null) {
                            typeAliasRegistry.registerAlias(clazz);
                        } else {
                            //如果alias写了 那么就把这个作为别名
                            typeAliasRegistry.registerAlias(alias, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
                    }
                }
            }
        }
    }

    /**
     * 解析插件元素
     *
     * @param parent
     * @throws Exception
     */
    private void pluginElement(XNode parent) throws Exception {
        if (parent != null) {
            for (XNode child : parent.getChildren()) {
                String interceptor = child.getStringAttribute("interceptor");
                //拿到这个拦截器的属性配置
                Properties properties = child.getChildrenAsProperties();
                //创建给出的拦截器class的实例对象
                Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
                //设置属性
                interceptorInstance.setProperties(properties);
                //在全局配置中加上这个拦截器
                configuration.addInterceptor(interceptorInstance);
            }
        }
    }

    /**
     * 解析对象工厂元素
     *
     * @param context
     * @throws Exception
     */
    private void objectFactoryElement(XNode context) throws Exception {
        if (context != null) {
            //对象工厂的class配置
            String type = context.getStringAttribute("type");
            //拿到配置的这个对象工厂配置的属性
            Properties properties = context.getChildrenAsProperties();
            //拿到这个class对应实例
            ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
            //设置对象工厂的属性配置
            factory.setProperties(properties);
            //修改全局配置中的对象工厂为用户给出的
            configuration.setObjectFactory(factory);
        }
    }

    /**
     * 解析对象包装工厂
     *
     * @param context
     * @throws Exception
     */
    private void objectWrapperFactoryElement(XNode context) throws Exception {
        if (context != null) {
            //拿到对象包装工厂class
            String type = context.getStringAttribute("type");
            //拿到这个class的实例
            ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
            //修改全局配置中的对象包装工厂为自定义
            configuration.setObjectWrapperFactory(factory);
        }
    }

    /**
     * 解析反射工厂
     *
     * @param context
     * @throws Exception
     */
    private void reflectorFactoryElement(XNode context) throws Exception {
        if (context != null) {
            //拿到class类名称
            String type = context.getStringAttribute("type");
            //创建class对象的实例
            ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
            //修改全局配置的反射工厂
            configuration.setReflectorFactory(factory);
        }
    }

    /**
     * 属性元素
     * <properties resource="org/apache/ibatis/databases/blog/blog-derby.properties"/>
     *
     * @param context
     * @throws Exception
     */
    private void propertiesElement(XNode context) throws Exception {
        if (context != null) {
            Properties defaults = context.getChildrenAsProperties();
            String resource = context.getStringAttribute("resource");
            String url = context.getStringAttribute("url");
            if (resource != null && url != null) {
                throw new BuilderException(
                        "The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
            }
            //在默认属性配置集合中加上其他资源文件的配置
            if (resource != null) {
                defaults.putAll(Resources.getResourceAsProperties(resource));
            } else if (url != null) {
                defaults.putAll(Resources.getUrlAsProperties(url));
            }
            //如果全局配置不为空 那么将全局配置中的属性配置也加到默认属性配置中
            Properties vars = configuration.getVariables();
            if (vars != null) {
                defaults.putAll(vars);
            }
            //解析器中加上配置
            parser.setVariables(defaults);
            //全局配置中加上配置
            configuration.setVariables(defaults);
        }
    }

    /**
     * 根据用户给出的settings配置重置全局配置中的相关配置
     * 如果用户没有给出 那么就是原来的默认值
     *
     * @param props
     * @throws Exception
     */
    private void settingsElement(Properties props) throws Exception {
        // TODO: 2021/3/25 CallYeDeGuo 全局配置中的一些属性的初始化在这里
        configuration.setAutoMappingBehavior(
                AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
        configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior
                .valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
        configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
        configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
        configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
        configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
        configuration
                .setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
        configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
        configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
        configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
        configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
        configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
        configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
        configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
        configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
        configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
        configuration.setLazyLoadTriggerMethods(
                stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
        configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
        configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
        @SuppressWarnings("unchecked") Class<? extends TypeHandler> typeHandler = (Class<? extends TypeHandler>) resolveClass(
                props.getProperty("defaultEnumTypeHandler"));
        configuration.setDefaultEnumTypeHandler(typeHandler);
        configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
        configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
        configuration
                .setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
        configuration.setLogPrefix(props.getProperty("logPrefix"));
        @SuppressWarnings("unchecked") Class<? extends Log> logImpl = (Class<? extends Log>) resolveClass(
                props.getProperty("logImpl"));
        configuration.setLogImpl(logImpl);
        configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
    }

    /**
     * 解析环境配置
     *
     * @param context
     * @throws Exception
     */
    private void environmentsElement(XNode context) throws Exception {
        if (context != null) {
            //如果环境配置为空 那么将配置的default作为环境配置
            if (environment == null) {
                environment = context.getStringAttribute("default");
            }
            //遍历所有的环境配置
            for (XNode child : context.getChildren()) {
                //拿到这个环境配置的id名称
                String id = child.getStringAttribute("id");
                //判断是否是特定的环境配置
                if (isSpecifiedEnvironment(id)) {
                    //如果是一样的  这个时候拿到这个环境配置的transactionManager以及dataSource
                    TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
                    DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
                    //从数据源工厂中拿到数据源
                    DataSource dataSource = dsFactory.getDataSource();
                    //创建环境配置 并设置事务工厂 数据源
                    Environment.Builder environmentBuilder = new Environment.Builder(id).transactionFactory(txFactory)
                            .dataSource(dataSource);
                    //设置环境
                    configuration.setEnvironment(environmentBuilder.build());
                }
            }
        }
    }

    /**
     * 解析数据库id
     *
     * @param context
     * @throws Exception
     */
    private void databaseIdProviderElement(XNode context) throws Exception {
        DatabaseIdProvider databaseIdProvider = null;
        if (context != null) {
            String type = context.getStringAttribute("type");
            // awful patch to keep backward compatibility
            if ("VENDOR".equals(type)) {
                type = "DB_VENDOR";
            }
            Properties properties = context.getChildrenAsProperties();
            databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
            databaseIdProvider.setProperties(properties);
        }
        Environment environment = configuration.getEnvironment();
        if (environment != null && databaseIdProvider != null) {
            String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
            configuration.setDatabaseId(databaseId);
        }
    }

    /**
     * 解析事务管理器元素
     *
     * @param context
     * @return
     * @throws Exception
     */
    private TransactionFactory transactionManagerElement(XNode context) throws Exception {
        //如果不为null
        if (context != null) {
            //拿到定义的类型
            String type = context.getStringAttribute("type");
            //拿到属性配置
            Properties props = context.getChildrenAsProperties();
            //拿到这个定义的类型对应的实例
            TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
            //设置属性
            factory.setProperties(props);
            //返回这个事务工厂
            return factory;
        }
        //如果环境配置中没有定义TransactionFactory属性 那么抛出异常
        throw new BuilderException("Environment declaration requires a TransactionFactory.");
    }

    /**
     * 解析数据源元素
     *
     * @param context
     * @return
     * @throws Exception
     */
    private DataSourceFactory dataSourceElement(XNode context) throws Exception {
        if (context != null) {
            //拿到定义的类型
            String type = context.getStringAttribute("type");
            //拿到熟悉配置
            Properties props = context.getChildrenAsProperties();
            //拿到数据源工厂
            DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
            //设置属性
            factory.setProperties(props);
            return factory;
        }
        //如果环境配置中没有定义DataSourceFactory 抛出异常
        throw new BuilderException("Environment declaration requires a DataSourceFactory.");
    }

    /**
     * 解析类型处理器元素
     *
     * @param parent
     * @throws Exception
     */
    private void typeHandlerElement(XNode parent) throws Exception {
        if (parent != null) {
            for (XNode child : parent.getChildren()) {
                //如果存在package属性
                if ("package".equals(child.getName())) {
                    //拿到拿到这个包名称注册这个包下面所有的类型处理器
                    String typeHandlerPackage = child.getStringAttribute("name");
                    typeHandlerRegistry.register(typeHandlerPackage);
                } else {
                    //如果不是package的 那么拿到javaType、jdbcType、handler
                    String javaTypeName = child.getStringAttribute("javaType");
                    String jdbcTypeName = child.getStringAttribute("jdbcType");
                    String handlerTypeName = child.getStringAttribute("handler");
                    //解析java类型
                    Class<?> javaTypeClass = resolveClass(javaTypeName);
                    //解析jdbc类型
                    JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
                    //解析类型处理器class
                    Class<?> typeHandlerClass = resolveClass(handlerTypeName);
                    //根据java类型是否存在注册的类型处理器
                    if (javaTypeClass != null) {
                        if (jdbcType == null) {
                            typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
                        } else {
                            typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
                        }
                    } else {
                        typeHandlerRegistry.register(typeHandlerClass);
                    }
                }
            }
        }
    }

    /**
     * 解析mapper元素
     *
     * @param parent
     * @throws Exception
     */
    private void mapperElement(XNode parent) throws Exception {
        if (parent != null) {
            for (XNode child : parent.getChildren()) {
                //如果名字是package元素 那么将这个包下面的所有的mapper class类进行加载
                if ("package".equals(child.getName())) {
                    String mapperPackage = child.getStringAttribute("name");
                    configuration.addMappers(mapperPackage);
                } else {
                    String resource = child.getStringAttribute("resource");
                    String url = child.getStringAttribute("url");
                    String mapperClass = child.getStringAttribute("class");
                    if (resource != null && url == null && mapperClass == null) {
                        ErrorContext.instance().resource(resource);
                        InputStream inputStream = Resources.getResourceAsStream(resource);
                        XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource,
                                configuration.getSqlFragments());
                        mapperParser.parse();
                    } else if (resource == null && url != null && mapperClass == null) {
                        ErrorContext.instance().resource(url);
                        InputStream inputStream = Resources.getUrlAsStream(url);
                        XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url,
                                configuration.getSqlFragments());
                        mapperParser.parse();
                    } else if (resource == null && url == null && mapperClass != null) {
                        Class<?> mapperInterface = Resources.classForName(mapperClass);
                        configuration.addMapper(mapperInterface);
                    } else {
                        throw new BuilderException(
                                "A mapper element may only specify a url, resource or class, but not more than one.");
                    }
                }
            }
        }
    }

    /**
     * 判断这个环境配置id是不是跟现有的environment一样
     * 如果一样为true 不一样为false
     *
     * @param id
     * @return
     */
    private boolean isSpecifiedEnvironment(String id) {
        if (environment == null) {
            throw new BuilderException("No environment specified.");
        } else if (id == null) {
            throw new BuilderException("Environment requires an id attribute.");
        } else if (environment.equals(id)) {
            return true;
        }
        return false;
    }

}
