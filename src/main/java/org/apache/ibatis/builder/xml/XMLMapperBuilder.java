/**
 * Copyright 2009-2018 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * xml类型的mapper文件构造器
 * 通过xml形式构造映射之后同时会根据xml中的命名空间去找到对应的映射接口进行构建映射
 *
 * @author Clinton Begin
 */
public class XMLMapperBuilder extends BaseBuilder {

    /**
     * x path解析器
     */
    private final XPathParser parser;

    /**
     * mapper构造器助手
     */
    private final MapperBuilderAssistant builderAssistant;

    /**
     * sql片段map  xml中sql节点的信息
     */
    private final Map<String, XNode> sqlFragments;

    /**
     * 所属来源 xx/xx/xx/xx.xml
     */
    private final String resource;

    /**
     * 构造函数
     *
     * @param reader
     * @param configuration
     * @param resource
     * @param sqlFragments
     * @param namespace
     */
    @Deprecated
    public XMLMapperBuilder(Reader reader, Configuration configuration, String resource,
            Map<String, XNode> sqlFragments, String namespace) {
        this(reader, configuration, resource, sqlFragments);
        this.builderAssistant.setCurrentNamespace(namespace);
    }

    /**
     * 构造函数
     *
     * @param reader
     * @param configuration
     * @param resource
     * @param sqlFragments
     */
    @Deprecated
    public XMLMapperBuilder(Reader reader, Configuration configuration, String resource,
            Map<String, XNode> sqlFragments) {
        this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()), configuration,
                resource, sqlFragments);
    }

    /**
     * 构造函数
     *
     * @param inputStream
     * @param configuration
     * @param resource
     * @param sqlFragments
     * @param namespace
     */
    public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
            Map<String, XNode> sqlFragments, String namespace) {
        this(inputStream, configuration, resource, sqlFragments);
        this.builderAssistant.setCurrentNamespace(namespace);
    }

    /**
     * 构造函数
     *
     * @param inputStream
     * @param configuration
     * @param resource
     * @param sqlFragments
     */
    public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
            Map<String, XNode> sqlFragments) {
        this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
                configuration, resource, sqlFragments);
    }

    /**
     * 构造函数
     *
     * @param parser
     * @param configuration
     * @param resource
     * @param sqlFragments
     */
    private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource,
            Map<String, XNode> sqlFragments) {
        super(configuration);
        this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
        this.parser = parser;
        this.sqlFragments = sqlFragments;
        this.resource = resource;
    }

    /**
     * 解析xml文件
     */
    public void parse() {
        //如果没有加载过
        if (!configuration.isResourceLoaded(resource)) {
            //解析得到这个xml文件的根节点mapper以及下面的所有的元素
            configurationElement(parser.evalNode("/mapper"));
            //将这个文件加到已经加载过的资源集合中
            configuration.addLoadedResource(resource);
            //绑定命名空间 xml加载完毕 如果映射接口没有被加载的话 那么就会再加载对应的映射接口
            bindMapperForNamespace();
        }
        //再次进行解析这几个
        parsePendingResultMaps();
        parsePendingCacheRefs();
        parsePendingStatements();
    }

    public XNode getSqlFragment(String refid) {
        return sqlFragments.get(refid);
    }

    /**
     * 解析mapper根节点信息
     *
     * @param context
     */
    private void configurationElement(XNode context) {
        try {
            //拿到这个mapper的命名空间 不能为null或者空
            String namespace = context.getStringAttribute("namespace");
            if (namespace == null || namespace.equals("")) {
                throw new BuilderException("Mapper's namespace cannot be empty");
            }
            //设置当前命名空间
            builderAssistant.setCurrentNamespace(namespace);
            //获取cache-ref节点信息
            cacheRefElement(context.evalNode("cache-ref"));
            //解析缓存 org/apache/ibatis/submitted/global_variables/Mapper.xml
            cacheElement(context.evalNode("cache"));
            parameterMapElement(context.evalNodes("/mapper/parameterMap"));
            resultMapElements(context.evalNodes("/mapper/resultMap"));
            sqlElement(context.evalNodes("/mapper/sql"));
            buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
        } catch (Exception e) {
            throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e,
                    e);
        }
    }

    /**
     * 解析select、update、insert、delete节点信息
     *
     * @param list
     */
    private void buildStatementFromContext(List<XNode> list) {
        if (configuration.getDatabaseId() != null) {
            buildStatementFromContext(list, configuration.getDatabaseId());
        }
        buildStatementFromContext(list, null);
    }

    /**
     * 从节点中解析sql声明
     *
     * @param list
     * @param requiredDatabaseId
     */
    private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
        //遍历节点构建xml声明
        for (XNode context : list) {
            final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant,
                    context, requiredDatabaseId);
            try {
                statementParser.parseStatementNode();
            } catch (IncompleteElementException e) {
                configuration.addIncompleteStatement(statementParser);
            }
        }
    }

    /**
     * 解析挂起的结果映射
     */
    private void parsePendingResultMaps() {
        Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
        synchronized (incompleteResultMaps) {
            Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
            while (iter.hasNext()) {
                try {
                    iter.next().resolve();
                    iter.remove();
                } catch (IncompleteElementException e) {
                    // ResultMap is still missing a resource...
                }
            }
        }
    }

    private void parsePendingCacheRefs() {
        Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
        synchronized (incompleteCacheRefs) {
            Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
            while (iter.hasNext()) {
                try {
                    iter.next().resolveCacheRef();
                    iter.remove();
                } catch (IncompleteElementException e) {
                    // Cache ref is still missing a resource...
                }
            }
        }
    }

    private void parsePendingStatements() {
        Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
        synchronized (incompleteStatements) {
            Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
            while (iter.hasNext()) {
                try {
                    iter.next().parseStatementNode();
                    iter.remove();
                } catch (IncompleteElementException e) {
                    // Statement is still missing a resource...
                }
            }
        }
    }

    /**
     * 解析其他命名空间缓存配置的引用
     *
     * @param context
     */
    private void cacheRefElement(XNode context) {
        if (context != null) {
            //将xml文件的名称空间跟cache-ref节点填写的命名空间保存起来
            configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
            //拿到缓存引用解析器
            CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant,
                    context.getStringAttribute("namespace"));
            try {
                //解析缓存引用
                cacheRefResolver.resolveCacheRef();
            } catch (IncompleteElementException e) {
                //如果抛出了这个异常 即这个class没有过缓存
                configuration.addIncompleteCacheRef(cacheRefResolver);
            }
        }
    }

    /**
     * 解析缓存节点
     *
     * @param context
     * @throws Exception
     */
    private void cacheElement(XNode context) throws Exception {
        if (context != null) {
            //拿到缓存的类型 class 如果为空的话 就设置为系统默认的PERPETUAL
            String type = context.getStringAttribute("type", "PERPETUAL");
            //从别名中拿到type对应的class
            Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
            //拿到缓存class的过期策略缓存 即装饰对象 默认是 LRU
            String eviction = context.getStringAttribute("eviction", "LRU");
            //拿到这个装饰缓存的class
            Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
            //拿到过期刷新时间
            Long flushInterval = context.getLongAttribute("flushInterval");
            //拿到缓存大小
            Integer size = context.getIntAttribute("size");
            //拿到是否只读
            boolean readWrite = !context.getBooleanAttribute("readOnly", false);
            //拿到是否阻断
            boolean blocking = context.getBooleanAttribute("blocking", false);
            //拿到相应的属性配置
            Properties props = context.getChildrenAsProperties();
            //根据这些信息创建一个新的缓存
            builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
        }
    }

    /**
     * 构建参数map
     * org/apache/ibatis/builder/AuthorMapper.xml
     *
     * @param list
     * @throws Exception
     */
    private void parameterMapElement(List<XNode> list) throws Exception {
        for (XNode parameterMapNode : list) {
            //拿到这个参数map的id
            String id = parameterMapNode.getStringAttribute("id");
            //拿到这个参数map的类型
            String type = parameterMapNode.getStringAttribute("type");
            //拿到这个type对应的class类
            Class<?> parameterClass = resolveClass(type);
            //拿到这个参数map的属性
            List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
            List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
            //循环每个属性配置
            for (XNode parameterNode : parameterNodes) {
                //拿到这个属性配置属性名称
                String property = parameterNode.getStringAttribute("property");
                //拿到这个属性的java类型
                String javaType = parameterNode.getStringAttribute("javaType");
                //拿到这个属性的jdbc类型
                String jdbcType = parameterNode.getStringAttribute("jdbcType");
                //拿到这个属性的返回map
                String resultMap = parameterNode.getStringAttribute("resultMap");
                //拿到这个属性的模式
                String mode = parameterNode.getStringAttribute("mode");
                //拿到这个属性的参数的类型处理器
                String typeHandler = parameterNode.getStringAttribute("typeHandler");
                //拿到这个属性的数值范围
                Integer numericScale = parameterNode.getIntAttribute("numericScale");
                //解析参数模式
                ParameterMode modeEnum = resolveParameterMode(mode);
                //解析获取java类型的class
                Class<?> javaTypeClass = resolveClass(javaType);
                //解析获取jdbc类型
                JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
                //解析类型处理器
                @SuppressWarnings("unchecked") Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(
                        typeHandler);
                //构建参数mapping
                ParameterMapping parameterMapping = builderAssistant
                        .buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap,
                                modeEnum, typeHandlerClass, numericScale);
                parameterMappings.add(parameterMapping);
            }
            //将parameterMap配置的id、type对应的class类以及里面的参数添加到参数map中
            builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
        }
    }

    /**
     * 解析resultMap配置
     *
     * @param list
     * @throws Exception
     */
    private void resultMapElements(List<XNode> list) throws Exception {
        for (XNode resultMapNode : list) {
            try {
                //解析resultMap配置
                resultMapElement(resultMapNode);
            } catch (IncompleteElementException e) {
                // ignore, it will be retried
            }
        }
    }

    /**
     * 解析resultMap元素
     *
     * @param resultMapNode
     * @return
     * @throws Exception
     */
    private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
        return resultMapElement(resultMapNode, Collections.<ResultMapping>emptyList());
    }

    /**
     * 解析resultMap节点
     *
     * @param resultMapNode
     * @param additionalResultMappings
     * @return
     * @throws Exception
     */
    private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings)
            throws Exception {
        ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
        //获取基本的一些属性
        String id = resultMapNode.getStringAttribute("id", resultMapNode.getValueBasedIdentifier());
        String type = resultMapNode.getStringAttribute("type", resultMapNode.getStringAttribute("ofType",
                resultMapNode.getStringAttribute("resultType", resultMapNode.getStringAttribute("javaType"))));
        String extend = resultMapNode.getStringAttribute("extends");
        Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
        Class<?> typeClass = resolveClass(type);
        Discriminator discriminator = null;
        List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
        resultMappings.addAll(additionalResultMappings);
        List<XNode> resultChildren = resultMapNode.getChildren();
        //解析这个节点的子节点
        for (XNode resultChild : resultChildren) {
            //如果是构造函数
            if ("constructor".equals(resultChild.getName())) {
                processConstructorElement(resultChild, typeClass, resultMappings);
            }
            //如果是辨别器
            else if ("discriminator".equals(resultChild.getName())) {
                discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
            }
            //其他情况
            else {
                List<ResultFlag> flags = new ArrayList<ResultFlag>();
                //如果是id 那么返回标识集合中加上id
                if ("id".equals(resultChild.getName())) {
                    flags.add(ResultFlag.ID);
                }
                //结果映射集合中添加相应的结果map
                resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
            }
        }
        ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend,
                discriminator, resultMappings, autoMapping);
        try {
            return resultMapResolver.resolve();
        } catch (IncompleteElementException e) {
            configuration.addIncompleteResultMap(resultMapResolver);
            throw e;
        }
    }

    private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings)
            throws Exception {
        List<XNode> argChildren = resultChild.getChildren();
        for (XNode argChild : argChildren) {
            List<ResultFlag> flags = new ArrayList<ResultFlag>();
            flags.add(ResultFlag.CONSTRUCTOR);
            if ("idArg".equals(argChild.getName())) {
                flags.add(ResultFlag.ID);
            }
            resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));

        }
    }

    private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType,
            List<ResultMapping> resultMappings) throws Exception {
        String column = context.getStringAttribute("column");
        String javaType = context.getStringAttribute("javaType");
        String jdbcType = context.getStringAttribute("jdbcType");
        String typeHandler = context.getStringAttribute("typeHandler");
        Class<?> javaTypeClass = resolveClass(javaType);
        @SuppressWarnings("unchecked") Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(
                typeHandler);
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        Map<String, String> discriminatorMap = new HashMap<String, String>();
        for (XNode caseChild : context.getChildren()) {
            String value = caseChild.getStringAttribute("value");
            String resultMap = caseChild
                    .getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings));
            discriminatorMap.put(value, resultMap);
        }
        return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass,
                discriminatorMap);
    }

    /**
     * 解析sql片段
     *
     * @param list
     * @throws Exception
     */
    private void sqlElement(List<XNode> list) throws Exception {
        //如果数据库id不为空
        if (configuration.getDatabaseId() != null) {
            sqlElement(list, configuration.getDatabaseId());
        }
        sqlElement(list, null);
    }

    /**
     * 解析sql节点
     *
     * @param list
     * @param requiredDatabaseId
     * @throws Exception
     */
    private void sqlElement(List<XNode> list, String requiredDatabaseId) throws Exception {
        for (XNode context : list) {
            //拿到配置中的数据库id
            String databaseId = context.getStringAttribute("databaseId");
            //拿到sql片段的id
            String id = context.getStringAttribute("id");
            //申请命名空间
            id = builderAssistant.applyCurrentNamespace(id, false);
            if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
                //往sql片段map中添加sql片段
                sqlFragments.put(id, context);
            }
        }
    }

    /**
     * 数据库id匹配当前的
     *
     * @param id
     * @param databaseId         xml文件中配置的数据库id
     * @param requiredDatabaseId 全局配置中的数据库id
     * @return
     */
    private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
        //如果全局配置中的数据库id不为null
        if (requiredDatabaseId != null) {
            //判断是全局配置中的数据库id跟xml中的配置是否相同 不同的话 返回false
            if (!requiredDatabaseId.equals(databaseId)) {
                return false;
            }
        }
        //如果全局配置中的数据库id为空
        else {
            //如果xml配置中的数据库id不为空 返回false
            if (databaseId != null) {
                return false;
            }
            // skip this fragment if there is a previous one with a not null databaseId
            //如果sql片段中包含这个id 那么从sql片段map中拿到这个id对应的节点
            if (this.sqlFragments.containsKey(id)) {
                XNode context = this.sqlFragments.get(id);
                //如果保存的sql节点中存在数据库id 返回false
                if (context.getStringAttribute("databaseId") != null) {
                    return false;
                }
            }
        }
        //其他情况返回true
        return true;
    }

    /**
     * 从节点上下文中构建结果映射
     *
     * @param context
     * @param resultType
     * @param flags
     * @return
     * @throws Exception
     */
    private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags)
            throws Exception {
        String property;
        //如果包含构造函数 那么从节点上下文中拿到name属性
        if (flags.contains(ResultFlag.CONSTRUCTOR)) {
            property = context.getStringAttribute("name");
        } else {
            //其他则拿到property属性
            property = context.getStringAttribute("property");
        }
        //从节点信息中拿到这些属性
        String column = context.getStringAttribute("column");
        String javaType = context.getStringAttribute("javaType");
        String jdbcType = context.getStringAttribute("jdbcType");
        //嵌套的select
        String nestedSelect = context.getStringAttribute("select");
        //嵌套的result map
        String nestedResultMap = context.getStringAttribute("resultMap",
                processNestedResultMappings(context, Collections.<ResultMapping>emptyList()));
        String notNullColumn = context.getStringAttribute("notNullColumn");
        String columnPrefix = context.getStringAttribute("columnPrefix");
        String typeHandler = context.getStringAttribute("typeHandler");
        String resultSet = context.getStringAttribute("resultSet");
        String foreignColumn = context.getStringAttribute("foreignColumn");
        //如果允许懒加载
        boolean lazy = "lazy".equals(context
                .getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
        //解析java类型类
        Class<?> javaTypeClass = resolveClass(javaType);
        //解析类型处理器
        @SuppressWarnings("unchecked") Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(
                typeHandler);
        //获取jdbc类型
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        //构建结果映射
        return builderAssistant
                .buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect,
                        nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn,
                        lazy);
    }

    private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings) throws Exception {
        if ("association".equals(context.getName()) || "collection".equals(context.getName()) || "case"
                .equals(context.getName())) {
            if (context.getStringAttribute("select") == null) {
                ResultMap resultMap = resultMapElement(context, resultMappings);
                return resultMap.getId();
            }
        }
        return null;
    }

    /**
     * 绑定命名空间
     */
    private void bindMapperForNamespace() {
        //拿到当前的命名空间
        String namespace = builderAssistant.getCurrentNamespace();
        //绑定这个命名空间
        if (namespace != null) {
            Class<?> boundType = null;
            try {
                boundType = Resources.classForName(namespace);
            } catch (ClassNotFoundException e) {
                //ignore, bound type is not required
            }
            if (boundType != null) {
                if (!configuration.hasMapper(boundType)) {
                    // Spring may not know the real resource name so we set a flag
                    // to prevent loading again this resource from the mapper interface
                    // look at MapperAnnotationBuilder#loadXmlResource
                    configuration.addLoadedResource("namespace:" + namespace);
                    configuration.addMapper(boundType);
                }
            }
        }
    }

}
