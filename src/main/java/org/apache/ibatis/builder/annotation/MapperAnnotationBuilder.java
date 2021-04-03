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
package org.apache.ibatis.builder.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.CacheNamespaceRef;
import org.apache.ibatis.annotations.Case;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Options.FlushCachePolicy;
import org.apache.ibatis.annotations.Property;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.TypeDiscriminator;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;

/**
 * 映射注解构造器
 * 这个类主要就是解析class类mapper的各种注解（不写xml）
 * 通过MapperAnnotation构建之后同时会根据映射接口的路径加上.xml进行构建相应的xml文件
 *
 * @author Clinton Begin
 */
public class MapperAnnotationBuilder {

    /**
     * sql注解set
     * 默认就是这几个 Select、Insert、Update、Delete
     */
    private final Set<Class<? extends Annotation>> sqlAnnotationTypes = new HashSet<Class<? extends Annotation>>();

    /**
     * sql提供者注解set
     */
    private final Set<Class<? extends Annotation>> sqlProviderAnnotationTypes = new HashSet<Class<? extends Annotation>>();

    /**
     * 全局配置
     */
    private final Configuration configuration;

    /**
     * 映射构造器助手
     */
    private final MapperBuilderAssistant assistant;

    /**
     * 原始mapper class
     */
    private final Class<?> type;

    /**
     * 构造函数
     *
     * @param configuration
     * @param type
     */
    public MapperAnnotationBuilder(Configuration configuration, Class<?> type) {
        //从类名称替换成其他
        String resource = type.getName().replace('.', '/') + ".java (best guess)";
        //映射构造器助手
        this.assistant = new MapperBuilderAssistant(configuration, resource);
        this.configuration = configuration;
        this.type = type;

        //加上这些注解
        sqlAnnotationTypes.add(Select.class);
        sqlAnnotationTypes.add(Insert.class);
        sqlAnnotationTypes.add(Update.class);
        sqlAnnotationTypes.add(Delete.class);

        //加上这些注解
        sqlProviderAnnotationTypes.add(SelectProvider.class);
        sqlProviderAnnotationTypes.add(InsertProvider.class);
        sqlProviderAnnotationTypes.add(UpdateProvider.class);
        sqlProviderAnnotationTypes.add(DeleteProvider.class);
    }

    /**
     * 解析
     */
    public void parse() {
        //原始mapper class类的string
        String resource = type.toString();
        //如果不是已经加载过的资源 进行相关加载操作
        if (!configuration.isResourceLoaded(resource)) {
            //加载xml资源
            loadXmlResource();
            //添加已经加载过的资源到map中
            configuration.addLoadedResource(resource);
            //映射构造器助手设置当前命名空间
            assistant.setCurrentNamespace(type.getName());
            parseCache();
            parseCacheRef();
            Method[] methods = type.getMethods();
            for (Method method : methods) {
                try {
                    // issue #237
                    //泛型为了兼容1.5之前的字节码自动生成一些桥接方法 这个时候就需要根据这个判断 以去掉编译器生成的那些桥接方法
                    //如果不是桥架方法 那么解析声明
                    if (!method.isBridge()) {
                        parseStatement(method);
                    }
                } catch (IncompleteElementException e) {
                    configuration.addIncompleteMethod(new MethodResolver(this, method));
                }
            }
        }
        parsePendingMethods();
    }

    private void parsePendingMethods() {
        Collection<MethodResolver> incompleteMethods = configuration.getIncompleteMethods();
        synchronized (incompleteMethods) {
            Iterator<MethodResolver> iter = incompleteMethods.iterator();
            while (iter.hasNext()) {
                try {
                    iter.next().resolve();
                    iter.remove();
                } catch (IncompleteElementException e) {
                    // This method is still missing a resource
                }
            }
        }
    }

    /**
     * 加载这个mapper class对应的xml
     */
    private void loadXmlResource() {
        // Spring may not know the real resource name so we check a flag
        // to prevent loading again a resource twice
        // this flag is set at XMLMapperBuilder#bindMapperForNamespace
        //如果这个mapper class类的命名空间没有加载过
        if (!configuration.isResourceLoaded("namespace:" + type.getName())) {
            //转换mapper class成xml资源路径
            String xmlResource = type.getName().replace('.', '/') + ".xml";
            InputStream inputStream = null;
            try {
                //获取这个xml文件对应的输入流
                inputStream = Resources.getResourceAsStream(type.getClassLoader(), xmlResource);
            } catch (IOException e) {
                // ignore, resource is not required
            }
            //如果不为空 那么
            if (inputStream != null) {
                //xml文件解析器
                XMLMapperBuilder xmlParser = new XMLMapperBuilder(inputStream, assistant.getConfiguration(),
                        xmlResource, configuration.getSqlFragments(), type.getName());
                //解析xml文件
                xmlParser.parse();
            }
        }
    }

    /**
     * 解析注解形式缓存
     * 对应xml配置中的cache
     */
    private void parseCache() {
        //拿到这个mapper上面的注解
        CacheNamespace cacheDomain = type.getAnnotation(CacheNamespace.class);
        //如果有这个注解
        if (cacheDomain != null) {
            //获取缓存的大小
            Integer size = cacheDomain.size() == 0 ? null : cacheDomain.size();
            //获取缓存的周期
            Long flushInterval = cacheDomain.flushInterval() == 0 ? null : cacheDomain.flushInterval();
            //转换属性配置
            Properties props = convertToProperties(cacheDomain.properties());
            assistant.useNewCache(cacheDomain.implementation(), cacheDomain.eviction(), flushInterval, size,
                    cacheDomain.readWrite(), cacheDomain.blocking(), props);
        }
    }

    /**
     * 转换属性配置
     *
     * @param properties
     * @return
     */
    private Properties convertToProperties(Property[] properties) {
        //如果属性配置为空 那么返回null
        if (properties.length == 0) {
            return null;
        }
        //创建属性配置
        Properties props = new Properties();
        //循环给出的属性配置注解转换成属性配置
        for (Property property : properties) {
            props.setProperty(property.name(), PropertyParser.parse(property.value(), configuration.getVariables()));
        }
        //返回属性配置
        return props;
    }

    /**
     * 解析缓存引用
     */
    private void parseCacheRef() {
        CacheNamespaceRef cacheDomainRef = type.getAnnotation(CacheNamespaceRef.class);
        if (cacheDomainRef != null) {
            Class<?> refType = cacheDomainRef.value();
            String refName = cacheDomainRef.name();
            //命名空间class不能为void 命名空间名称不能为空 否则是无意义的名称
            if (refType == void.class && refName.isEmpty()) {
                throw new BuilderException(
                        "Should be specified either value() or name() attribute in the @CacheNamespaceRef");
            }
            //两个不能都有值 只能定义一个
            if (refType != void.class && !refName.isEmpty()) {
                throw new BuilderException("Cannot use both value() and name() attribute in the @CacheNamespaceRef");
            }
            //优先class类 如果class类为空 那么使用名称
            String namespace = (refType != void.class) ? refType.getName() : refName;
            assistant.useCacheRef(namespace);
        }
    }

    /**
     * 解析结果map
     *
     * @param method
     * @return
     */
    private String parseResultMap(Method method) {
        Class<?> returnType = getReturnType(method);
        ConstructorArgs args = method.getAnnotation(ConstructorArgs.class);
        Results results = method.getAnnotation(Results.class);
        TypeDiscriminator typeDiscriminator = method.getAnnotation(TypeDiscriminator.class);
        String resultMapId = generateResultMapName(method);
        applyResultMap(resultMapId, returnType, argsIf(args), resultsIf(results), typeDiscriminator);
        return resultMapId;
    }

    /**
     * 生成结果map名称
     *
     * @param method
     * @return
     */
    private String generateResultMapName(Method method) {
        //获取这个注解
        Results results = method.getAnnotation(Results.class);
        //如果有这个注解 并且注解的id不为空
        if (results != null && !results.id().isEmpty()) {
            //返回这个mapper接口+results的id
            return type.getName() + "." + results.id();
        }
        //否则
        StringBuilder suffix = new StringBuilder();
        //遍历参数列表 拼接上作为前缀
        for (Class<?> c : method.getParameterTypes()) {
            suffix.append("-");
            suffix.append(c.getSimpleName());
        }
        //如果没有参数 那么拼接上-void
        if (suffix.length() < 1) {
            suffix.append("-void");
        }
        //将mapper接口+方法名称+前缀拼接起来
        return type.getName() + "." + method.getName() + suffix;
    }

    /**
     * 申请结果map
     *
     * @param resultMapId
     * @param returnType
     * @param args
     * @param results
     * @param discriminator
     */
    private void applyResultMap(String resultMapId, Class<?> returnType, Arg[] args, Result[] results,
            TypeDiscriminator discriminator) {
        List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
        applyConstructorArgs(args, returnType, resultMappings);
        applyResults(results, returnType, resultMappings);
        Discriminator disc = applyDiscriminator(resultMapId, returnType, discriminator);
        // TODO add AutoMappingBehaviour
        //往全局配置里面添加结果map
        assistant.addResultMap(resultMapId, returnType, null, disc, resultMappings, null);
        createDiscriminatorResultMaps(resultMapId, returnType, discriminator);
    }

    /**
     * 创建鉴别器结果map
     *
     * @param resultMapId
     * @param resultType
     * @param discriminator
     */
    private void createDiscriminatorResultMaps(String resultMapId, Class<?> resultType,
            TypeDiscriminator discriminator) {
        if (discriminator != null) {
            for (Case c : discriminator.cases()) {
                String caseResultMapId = resultMapId + "-" + c.value();
                List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
                // issue #136
                applyConstructorArgs(c.constructArgs(), resultType, resultMappings);
                applyResults(c.results(), resultType, resultMappings);
                // TODO add AutoMappingBehaviour
                assistant.addResultMap(caseResultMapId, c.type(), resultMapId, null, resultMappings, null);
            }
        }
    }

    /**
     * 构建鉴别器对象
     *
     * @param resultMapId
     * @param resultType
     * @param discriminator
     * @return
     */
    private Discriminator applyDiscriminator(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
        if (discriminator != null) {
            String column = discriminator.column();
            //如果java类型是void 那么转换成string 否则就拿设置的java类型
            Class<?> javaType = discriminator.javaType() == void.class ? String.class : discriminator.javaType();
            //如果是UNDEFINED那么转换成null 否则就拿设置的jdbc类型
            JdbcType jdbcType = discriminator.jdbcType() == JdbcType.UNDEFINED ? null : discriminator.jdbcType();
            //如果是未定义类型处理器 那么就转换成null 否则就拿设置的类型处理器
            @SuppressWarnings("unchecked") Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (
                    discriminator.typeHandler() == UnknownTypeHandler.class ?
                            null :
                            discriminator.typeHandler());
            //拿到case数组
            Case[] cases = discriminator.cases();
            //鉴别器map
            Map<String, String> discriminatorMap = new HashMap<String, String>();
            //转换case的条件对应的value成map的key
            for (Case c : cases) {
                String value = c.value();
                String caseResultMapId = resultMapId + "-" + value;
                discriminatorMap.put(value, caseResultMapId);
            }
            return assistant.buildDiscriminator(resultType, column, javaType, jdbcType, typeHandler, discriminatorMap);
        }
        return null;
    }

    /**
     * 解析声明
     *
     * @param method
     */
    void parseStatement(Method method) {
        //拿到这个方法参数类型class
        Class<?> parameterTypeClass = getParameterType(method);
        LanguageDriver languageDriver = getLanguageDriver(method);
        SqlSource sqlSource = getSqlSourceFromAnnotations(method, parameterTypeClass, languageDriver);
        if (sqlSource != null) {
            Options options = method.getAnnotation(Options.class);
            //已映射的声明id 类似：org.apache.ibatis.autoconstructor.AutoConstructorMapper.getSubjects
            final String mappedStatementId = type.getName() + "." + method.getName();
            Integer fetchSize = null;
            Integer timeout = null;
            //从注解中获取下面这些属性的值  定义一些默认值
            StatementType statementType = StatementType.PREPARED;
            ResultSetType resultSetType = ResultSetType.FORWARD_ONLY;
            SqlCommandType sqlCommandType = getSqlCommandType(method);
            //是否查询类型
            boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
            boolean flushCache = !isSelect;
            boolean useCache = isSelect;

            KeyGenerator keyGenerator;
            String keyProperty = "id";
            String keyColumn = null;
            //如果是新增或者更新
            if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
                // first check for SelectKey annotation - that overrides everything else
                //首先检查@SelectKey注解 这个会覆盖掉其他
                SelectKey selectKey = method.getAnnotation(SelectKey.class);
                //如果SelectKey注解不为空 处理这个注解
                if (selectKey != null) {
                    keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, getParameterType(method),
                            languageDriver);
                    keyProperty = selectKey.keyProperty();
                } else if (options == null) {
                    keyGenerator = configuration.isUseGeneratedKeys() ?
                            Jdbc3KeyGenerator.INSTANCE :
                            NoKeyGenerator.INSTANCE;
                } else {
                    keyGenerator = options.useGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
                    keyProperty = options.keyProperty();
                    keyColumn = options.keyColumn();
                }
            } else {
                keyGenerator = NoKeyGenerator.INSTANCE;
            }

            if (options != null) {
                if (FlushCachePolicy.TRUE.equals(options.flushCache())) {
                    flushCache = true;
                } else if (FlushCachePolicy.FALSE.equals(options.flushCache())) {
                    flushCache = false;
                }
                useCache = options.useCache();
                fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ?
                        options.fetchSize() :
                        null; //issue #348
                timeout = options.timeout() > -1 ? options.timeout() : null;
                statementType = options.statementType();
                resultSetType = options.resultSetType();
            }

            String resultMapId = null;
            ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
            if (resultMapAnnotation != null) {
                String[] resultMaps = resultMapAnnotation.value();
                StringBuilder sb = new StringBuilder();
                for (String resultMap : resultMaps) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(resultMap);
                }
                resultMapId = sb.toString();
            } else if (isSelect) {
                resultMapId = parseResultMap(method);
            }

            assistant
                    .addMappedStatement(mappedStatementId, sqlSource, statementType, sqlCommandType, fetchSize, timeout,
                            // ParameterMapID
                            null, parameterTypeClass, resultMapId, getReturnType(method), resultSetType, flushCache,
                            useCache,
                            // TODO gcode issue #577
                            false, keyGenerator, keyProperty, keyColumn,
                            // DatabaseID
                            null, languageDriver,
                            // ResultSets
                            options != null ? nullOrEmpty(options.resultSets()) : null);
        }
    }

    /**
     * 获取语言解析驱动
     *
     * @param method
     * @return
     */
    private LanguageDriver getLanguageDriver(Method method) {
        Lang lang = method.getAnnotation(Lang.class);
        Class<?> langClass = null;
        if (lang != null) {
            langClass = lang.value();
        }
        return assistant.getLanguageDriver(langClass);
    }

    /**
     * 获取参数类型
     *
     * @param method
     * @return
     */
    private Class<?> getParameterType(Method method) {
        Class<?> parameterType = null;
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> currentParameterType : parameterTypes) {
            //如果这个参数不是RowBounds跟ResultHandler的子类
            if (!RowBounds.class.isAssignableFrom(currentParameterType) && !ResultHandler.class
                    .isAssignableFrom(currentParameterType)) {
                //如果参数类型为空 那么设置当前参数 否则设置为ParamMap
                if (parameterType == null) {
                    parameterType = currentParameterType;
                } else {
                    // issue #135
                    parameterType = ParamMap.class;
                }
            }
        }
        return parameterType;
    }

    /**
     * 拿到方法的返回类型
     *
     * @param method
     * @return
     */
    private Class<?> getReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, type);
        //如果解析出来的返回类型是class
        if (resolvedReturnType instanceof Class) {
            //转换成class
            returnType = (Class<?>) resolvedReturnType;
            //如果是数组类型
            if (returnType.isArray()) {
                //拿到这个数组类型的原始类型
                returnType = returnType.getComponentType();
            }
            // gcode issue #508
            //如果是void发回
            if (void.class.equals(returnType)) {
                //如果有这个注解 那么拿到这个注解的值
                ResultType rt = method.getAnnotation(ResultType.class);
                if (rt != null) {
                    returnType = rt.value();
                }
            }
        } else if (resolvedReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (Collection.class.isAssignableFrom(rawType) || Cursor.class.isAssignableFrom(rawType)) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments != null && actualTypeArguments.length == 1) {
                    Type returnTypeParameter = actualTypeArguments[0];
                    if (returnTypeParameter instanceof Class<?>) {
                        returnType = (Class<?>) returnTypeParameter;
                    } else if (returnTypeParameter instanceof ParameterizedType) {
                        // (gcode issue #443) actual type can be a also a parameterized type
                        returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
                    } else if (returnTypeParameter instanceof GenericArrayType) {
                        Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter)
                                .getGenericComponentType();
                        // (gcode issue #525) support List<byte[]>
                        returnType = Array.newInstance(componentType, 0).getClass();
                    }
                }
            } else if (method.isAnnotationPresent(MapKey.class) && Map.class.isAssignableFrom(rawType)) {
                // (gcode issue 504) Do not look into Maps if there is not MapKey annotation
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments != null && actualTypeArguments.length == 2) {
                    Type returnTypeParameter = actualTypeArguments[1];
                    if (returnTypeParameter instanceof Class<?>) {
                        returnType = (Class<?>) returnTypeParameter;
                    } else if (returnTypeParameter instanceof ParameterizedType) {
                        // (gcode issue 443) actual type can be a also a parameterized type
                        returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
                    }
                }
            }
        }

        return returnType;
    }

    /**
     * 从注解中获取sql源
     *
     * @param method
     * @param parameterType
     * @param languageDriver
     * @return
     */
    private SqlSource getSqlSourceFromAnnotations(Method method, Class<?> parameterType,
            LanguageDriver languageDriver) {
        try {
            Class<? extends Annotation> sqlAnnotationType = getSqlAnnotationType(method);
            Class<? extends Annotation> sqlProviderAnnotationType = getSqlProviderAnnotationType(method);
            //如果sql类型不为空
            if (sqlAnnotationType != null) {
                //不能同时提供sql类型注解跟sql提供者注解
                if (sqlProviderAnnotationType != null) {
                    throw new BindingException(
                            "You cannot supply both a static SQL and SqlProvider to method named " + method.getName());
                }
                //拿到这个注解
                Annotation sqlAnnotation = method.getAnnotation(sqlAnnotationType);
                //拿到这个注解的值
                final String[] strings = (String[]) sqlAnnotation.getClass().getMethod("value").invoke(sqlAnnotation);
                //根据注解中的sql信息构建sql源
                return buildSqlSourceFromStrings(strings, parameterType, languageDriver);
            } else if (sqlProviderAnnotationType != null) {
                Annotation sqlProviderAnnotation = method.getAnnotation(sqlProviderAnnotationType);
                return new ProviderSqlSource(assistant.getConfiguration(), sqlProviderAnnotation, type, method);
            }
            return null;
        } catch (Exception e) {
            throw new BuilderException("Could not find value method on SQL annotation.  Cause: " + e, e);
        }
    }

    /**
     * 从sql片段中构建一个sql源
     *
     * @param strings
     * @param parameterTypeClass
     * @param languageDriver
     * @return
     */
    private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass,
            LanguageDriver languageDriver) {
        final StringBuilder sql = new StringBuilder();
        //拼接sql片段成完整的sql
        for (String fragment : strings) {
            sql.append(fragment);
            sql.append(" ");
        }
        return languageDriver.createSqlSource(configuration, sql.toString().trim(), parameterTypeClass);
    }

    /**
     * 获取sql命令类型
     *
     * @param method
     * @return
     */
    private SqlCommandType getSqlCommandType(Method method) {
        Class<? extends Annotation> type = getSqlAnnotationType(method);

        if (type == null) {
            type = getSqlProviderAnnotationType(method);

            if (type == null) {
                return SqlCommandType.UNKNOWN;
            }

            if (type == SelectProvider.class) {
                type = Select.class;
            } else if (type == InsertProvider.class) {
                type = Insert.class;
            } else if (type == UpdateProvider.class) {
                type = Update.class;
            } else if (type == DeleteProvider.class) {
                type = Delete.class;
            }
        }

        return SqlCommandType.valueOf(type.getSimpleName().toUpperCase(Locale.ENGLISH));
    }

    /**
     * 从方法中拿到这个方法的sql类型注解
     * Select
     * Insert
     * Update
     * Delete
     *
     * @param method
     * @return
     */
    private Class<? extends Annotation> getSqlAnnotationType(Method method) {
        return chooseAnnotationType(method, sqlAnnotationTypes);
    }

    /**
     * 从方法中拿到sql提供者类型注解
     *
     * @param method
     * @return
     */
    private Class<? extends Annotation> getSqlProviderAnnotationType(Method method) {
        return chooseAnnotationType(method, sqlProviderAnnotationTypes);
    }

    /**
     * 从方法中拿到types中的注解
     *
     * @param method
     * @param types
     * @return
     */
    private Class<? extends Annotation> chooseAnnotationType(Method method, Set<Class<? extends Annotation>> types) {
        for (Class<? extends Annotation> type : types) {
            Annotation annotation = method.getAnnotation(type);
            if (annotation != null) {
                return type;
            }
        }
        return null;
    }

    /**
     * 遍历Result构建ResultMapping
     *
     * @param results
     * @param resultType
     * @param resultMappings
     */
    private void applyResults(Result[] results, Class<?> resultType, List<ResultMapping> resultMappings) {
        for (Result result : results) {
            List<ResultFlag> flags = new ArrayList<ResultFlag>();
            if (result.id()) {
                flags.add(ResultFlag.ID);
            }
            @SuppressWarnings("unchecked") Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (
                    (result.typeHandler() == UnknownTypeHandler.class) ?
                            null :
                            result.typeHandler());
            ResultMapping resultMapping = assistant
                    .buildResultMapping(resultType, nullOrEmpty(result.property()), nullOrEmpty(result.column()),
                            result.javaType() == void.class ? null : result.javaType(),
                            result.jdbcType() == JdbcType.UNDEFINED ? null : result.jdbcType(),
                            hasNestedSelect(result) ? nestedSelectId(result) : null, null, null, null, typeHandler,
                            flags, null, null, isLazy(result));
            resultMappings.add(resultMapping);
        }
    }

    private String nestedSelectId(Result result) {
        String nestedSelect = result.one().select();
        if (nestedSelect.length() < 1) {
            nestedSelect = result.many().select();
        }
        if (!nestedSelect.contains(".")) {
            nestedSelect = type.getName() + "." + nestedSelect;
        }
        return nestedSelect;
    }

    private boolean isLazy(Result result) {
        boolean isLazy = configuration.isLazyLoadingEnabled();
        if (result.one().select().length() > 0 && FetchType.DEFAULT != result.one().fetchType()) {
            isLazy = result.one().fetchType() == FetchType.LAZY;
        } else if (result.many().select().length() > 0 && FetchType.DEFAULT != result.many().fetchType()) {
            isLazy = result.many().fetchType() == FetchType.LAZY;
        }
        return isLazy;
    }

    /**
     * 是否有嵌套的查询
     *
     * @param result
     * @return
     */
    private boolean hasNestedSelect(Result result) {
        //不能同时给出one和many注解
        if (result.one().select().length() > 0 && result.many().select().length() > 0) {
            throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
        }
        //如果有其中的一个 那么就是true
        return result.one().select().length() > 0 || result.many().select().length() > 0;
    }

    /**
     * 遍历Arg数组构建ResultMapping
     *
     * @param args
     * @param resultType
     * @param resultMappings
     */
    private void applyConstructorArgs(Arg[] args, Class<?> resultType, List<ResultMapping> resultMappings) {
        for (Arg arg : args) {
            List<ResultFlag> flags = new ArrayList<ResultFlag>();
            flags.add(ResultFlag.CONSTRUCTOR);
            if (arg.id()) {
                flags.add(ResultFlag.ID);
            }
            @SuppressWarnings("unchecked") Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (
                    arg.typeHandler() == UnknownTypeHandler.class ?
                            null :
                            arg.typeHandler());
            ResultMapping resultMapping = assistant
                    .buildResultMapping(resultType, nullOrEmpty(arg.name()), nullOrEmpty(arg.column()),
                            arg.javaType() == void.class ? null : arg.javaType(),
                            arg.jdbcType() == JdbcType.UNDEFINED ? null : arg.jdbcType(), nullOrEmpty(arg.select()),
                            nullOrEmpty(arg.resultMap()), null, null, typeHandler, flags, null, null, false);
            resultMappings.add(resultMapping);
        }
    }

    private String nullOrEmpty(String value) {
        return value == null || value.trim().length() == 0 ? null : value;
    }

    /**
     * 如果有Results注解 那么返回直接中的value
     * 否则新建一个空数组
     *
     * @param results
     * @return
     */
    private Result[] resultsIf(Results results) {
        return results == null ? new Result[0] : results.value();
    }

    /**
     * 如果有ConstructorArgs注解 那么返回注解中的value
     * 否则返回一个新的数组
     *
     * @param args
     * @return
     */
    private Arg[] argsIf(ConstructorArgs args) {
        return args == null ? new Arg[0] : args.value();
    }

    /**
     * 处理SelectKey注解
     *
     * @param selectKeyAnnotation
     * @param baseStatementId
     * @param parameterTypeClass
     * @param languageDriver
     * @return
     */
    private KeyGenerator handleSelectKeyAnnotation(SelectKey selectKeyAnnotation, String baseStatementId,
            Class<?> parameterTypeClass, LanguageDriver languageDriver) {
        String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
        //拿到注解中的这些属性
        Class<?> resultTypeClass = selectKeyAnnotation.resultType();
        StatementType statementType = selectKeyAnnotation.statementType();
        String keyProperty = selectKeyAnnotation.keyProperty();
        String keyColumn = selectKeyAnnotation.keyColumn();
        boolean executeBefore = selectKeyAnnotation.before();

        // defaults 默认不适用缓存
        boolean useCache = false;
        KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
        // 这是尝试影响驱动程序每次批量返回的结果行数和这个设置值相等
        Integer fetchSize = null;
        Integer timeout = null;
        boolean flushCache = false;
        String parameterMap = null;
        String resultMap = null;
        ResultSetType resultSetTypeEnum = null;
        //根据注解中的sql声明以及参数类型class以及语言驱动创建sql源
        SqlSource sqlSource = buildSqlSourceFromStrings(selectKeyAnnotation.statement(), parameterTypeClass,
                languageDriver);
        SqlCommandType sqlCommandType = SqlCommandType.SELECT;
        //添加已映射的声明
        assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
                parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum, flushCache, useCache, false,
                keyGenerator, keyProperty, keyColumn, null, languageDriver, null);
        //申请当前命名空间
        id = assistant.applyCurrentNamespace(id, false);
        //
        MappedStatement keyStatement = configuration.getMappedStatement(id, false);
        SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
        configuration.addKeyGenerator(id, answer);
        return answer;
    }

}
