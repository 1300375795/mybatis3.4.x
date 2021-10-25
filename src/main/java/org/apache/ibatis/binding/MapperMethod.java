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
package org.apache.ibatis.binding;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * 映射中的某个方法的一些详细的信息
 * 对应mapper接口中给的每一个方法
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
public class MapperMethod {

    /**
     * sql命令
     */
    private final SqlCommand command;

    /**
     * 方法签名 描述这个方法的一些信息
     * 后续execute执行的时候根据这个来调用SqlSession不同的流程
     */
    private final MethodSignature method;

    /**
     * 构造函数
     *
     * @param mapperInterface 映射接口
     * @param method          方法
     * @param config          配置
     */
    public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
        this.command = new SqlCommand(config, mapperInterface, method);
        this.method = new MethodSignature(config, mapperInterface, method);
    }

    /**
     * sql会话执行
     *
     * @param sqlSession
     * @param args
     * @return
     */
    public Object execute(SqlSession sqlSession, Object[] args) {
        Object result;
        //判断sql的类型
        switch (command.getType()) {
            //如果方法的类型是新增 执行新增sql
            case INSERT: {
                Object param = method.convertArgsToSqlCommandParam(args);
                result = rowCountResult(sqlSession.insert(command.getName(), param));
                break;
            }
            //如果是更新执行更新sql
            case UPDATE: {
                Object param = method.convertArgsToSqlCommandParam(args);
                result = rowCountResult(sqlSession.update(command.getName(), param));
                break;
            }
            //如果是删除执行删除sql
            case DELETE: {
                Object param = method.convertArgsToSqlCommandParam(args);
                result = rowCountResult(sqlSession.delete(command.getName(), param));
                break;
            }
            //如果是查询 执行查询sql
            case SELECT:
                //如果返回是void  并且有ResultHandler那么通过resultHandler执行
                if (method.returnsVoid() && method.hasResultHandler()) {
                    executeWithResultHandler(sqlSession, args);
                    result = null;
                }
                //如果返回多条数据
                else if (method.returnsMany()) {
                    result = executeForMany(sqlSession, args);
                }
                //如果returnMap为true
                else if (method.returnsMap()) {
                    result = executeForMap(sqlSession, args);
                }
                //如果返回的是游标
                else if (method.returnsCursor()) {
                    result = executeForCursor(sqlSession, args);
                }
                //其他情形
                else {
                    //转化参数
                    Object param = method.convertArgsToSqlCommandParam(args);
                    //直接调用调用单条数据查询
                    result = sqlSession.selectOne(command.getName(), param);
                }
                break;
            //如果是刷新 执行刷新sql
            case FLUSH:
                result = sqlSession.flushStatements();
                break;
            //其他情况抛出异常
            default:
                throw new BindingException("Unknown execution method for: " + command.getName());
        }
        //如果返回结果是null并且方法的返回类型是原始类型并且方法不是返回的void 抛出异常
        if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
            throw new BindingException("Mapper method '" + command.getName()
                    + " attempted to return null from a method with a primitive return type (" + method.getReturnType()
                    + ").");
        }
        return result;
    }

    /**
     * 影响行数量结果
     *
     * @param rowCount
     * @return
     */
    private Object rowCountResult(int rowCount) {
        final Object result;
        //如果方法是返回void 直接返回null
        if (method.returnsVoid()) {
            result = null;
        }
        //如果是返回Integer直接赋值
        else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
            result = rowCount;
        }
        //如果返回Long 进行强转
        else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
            result = (long) rowCount;
        }
        //如果是返回boolean类型 判断影响的行数是不是大于0
        else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
            result = rowCount > 0;
        } else {
            //其他类型不支持 比如short、double
            throw new BindingException(
                    "Mapper method '" + command.getName() + "' has an unsupported return type: " + method
                            .getReturnType());
        }
        //返回结果
        return result;
    }

    /**
     * 以ResultHandler的形式执行
     *
     * @param sqlSession
     * @param args
     */
    private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
        //拿到这个声明id的映射声明
        MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
        //如果声明类型不是回调并且返回类型是void 抛出异常
        if (!StatementType.CALLABLE.equals(ms.getStatementType()) && void.class
                .equals(ms.getResultMaps().get(0).getType())) {
            throw new BindingException(
                    "method " + command.getName() + " needs either a @ResultMap annotation, a @ResultType annotation,"
                            + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
        }
        //转换参数
        Object param = method.convertArgsToSqlCommandParam(args);
        //如果有分页参数 那么以分页执行
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
        } else {
            //如果没有分页参数 直接执行
            sqlSession.select(command.getName(), param, method.extractResultHandler(args));
        }
    }

    /**
     * 以返回多条的形式执行
     *
     * @param sqlSession
     * @param args
     * @param <E>
     * @return
     */
    private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
        List<E> result;
        //转换请求参数
        Object param = method.convertArgsToSqlCommandParam(args);
        //如果有分页参数
        if (method.hasRowBounds()) {
            //拿到分页参数
            RowBounds rowBounds = method.extractRowBounds(args);
            //以分页的形式执行
            result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
        } else {
            //没有分页参数的话 直接执行
            result = sqlSession.<E>selectList(command.getName(), param);
        }
        // issue #510 Collections & arrays support
        //如果方法方法定义的返回类型不是返回结果的class的父类或者本类
        if (!method.getReturnType().isAssignableFrom(result.getClass())) {
            //以方法的返回类型为准 如果方法的返回类型是数组
            if (method.getReturnType().isArray()) {
                return convertToArray(result);
            } else {
                //如多不是数组
                return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
            }
        }
        return result;
    }

    /**
     * 以游标的形式执行
     *
     * @param sqlSession
     * @param args
     * @param <T>
     * @return
     */
    private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
        Cursor<T> result;
        //转换参数
        Object param = method.convertArgsToSqlCommandParam(args);
        //如果有分页参数 那么以分页的形式执行
        if (method.hasRowBounds()) {
            //拿到分页参数
            RowBounds rowBounds = method.extractRowBounds(args);
            result = sqlSession.<T>selectCursor(command.getName(), param, rowBounds);
        } else {
            //没有分页参数话 直接执行
            result = sqlSession.<T>selectCursor(command.getName(), param);
        }
        return result;
    }

    /**
     * 转换成声明的集合
     *
     * @param config
     * @param list
     * @param <E>
     * @return
     */
    private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
        //根据方法的返回类型转换成相应的class
        Object collection = config.getObjectFactory().create(method.getReturnType());
        //转换成相应的元对象
        MetaObject metaObject = config.newMetaObject(collection);
        metaObject.addAll(list);
        return collection;
    }

    /**
     * 将返回结果转换成数组
     *
     * @param list
     * @param <E>
     * @return
     */
    @SuppressWarnings("unchecked")
    private <E> Object convertToArray(List<E> list) {
        //拿到数组存放的class对象 比如 MapperMethod[] 那么就是MapperMethod.class
        Class<?> arrayComponentType = method.getReturnType().getComponentType();
        //创建对象数组
        Object array = Array.newInstance(arrayComponentType, list.size());
        //如果是基础类型
        if (arrayComponentType.isPrimitive()) {
            //设置基础类型导致
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
            return array;
        } else {
            //不是私有类型的话 那么转成E数组并返回
            return list.toArray((E[]) array);
        }
    }

    /**
     * 以返回map的形式执行
     *
     * @param sqlSession
     * @param args
     * @param <K>
     * @param <V>
     * @return
     */
    private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
        Map<K, V> result;
        //转换参数
        Object param = method.convertArgsToSqlCommandParam(args);
        //如果有分页参数
        if (method.hasRowBounds()) {
            //拿到分页参数 以及分页的形式执行
            RowBounds rowBounds = method.extractRowBounds(args);
            result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey(), rowBounds);
        } else {
            //如果没有分页参数 那么直接执行
            result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey());
        }
        return result;
    }

    /**
     * 参数map
     *
     * @param <V>
     */
    public static class ParamMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -2212268410512043556L;

        /**
         * 重写了get方法 如果map中不包含改出的key 抛出异常
         *
         * @param key
         * @return
         */
        @Override
        public V get(Object key) {
            if (!super.containsKey(key)) {
                throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
            }
            return super.get(key);
        }

    }

    /**
     * sql命令
     */
    public static class SqlCommand {

        /**
         * 方法名称
         */
        private final String name;

        /**
         * sql命令类型
         */
        private final SqlCommandType type;

        /**
         * 构造函数
         *
         * @param configuration
         * @param mapperInterface
         * @param method
         */
        public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
            final String methodName = method.getName();
            //拿到这个方法所在的类
            final Class<?> declaringClass = method.getDeclaringClass();
            MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass, configuration);
            //如果ms为null
            if (ms == null) {
                //如果方法加了注解@Flush 那么name为null 类型为flush
                if (method.getAnnotation(Flush.class) != null) {
                    name = null;
                    type = SqlCommandType.FLUSH;
                }
                //否则抛出异常
                else {
                    throw new BindingException(
                            "Invalid bound statement (not found): " + mapperInterface.getName() + "." + methodName);
                }
            } else {
                //如果ms不为null 那么从ms的id为name sqlType为type 如果类型为位置 那么抛出异常
                name = ms.getId();
                type = ms.getSqlCommandType();
                if (type == SqlCommandType.UNKNOWN) {
                    throw new BindingException("Unknown execution method for: " + name);
                }
            }
        }

        public String getName() {
            return name;
        }

        public SqlCommandType getType() {
            return type;
        }

        /**
         * 解析获取MappedStatement
         *
         * @param mapperInterface
         * @param methodName
         * @param declaringClass
         * @param configuration
         * @return
         */
        private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
                Class<?> declaringClass, Configuration configuration) {
            //映射接口+方法名称 类似：org.apache.ibatis.binding.BoundBlogMapper.selectBlog
            String statementId = mapperInterface.getName() + "." + methodName;
            //如果存在这个声明id
            if (configuration.hasStatement(statementId)) {
                //从缓存中拿到对应的MappedStatement
                return configuration.getMappedStatement(statementId);
            }
            //如果映射接口类跟当前方法所属的class相同
            else if (mapperInterface.equals(declaringClass)) {
                return null;
            }
            for (Class<?> superInterface : mapperInterface.getInterfaces()) {
                if (declaringClass.isAssignableFrom(superInterface)) {
                    //递归获取这个mapperInterface的父接口进行获取
                    MappedStatement ms = resolveMappedStatement(superInterface, methodName, declaringClass,
                            configuration);
                    if (ms != null) {
                        return ms;
                    }
                }
            }
            return null;
        }
    }

    /**
     * 静态内部类 方法签名
     * 存放mapper接口中的所有的方法的方法签名
     */
    public static class MethodSignature {

        /**
         * 是否返回多条数据
         */
        private final boolean returnsMany;

        /**
         * 只有在mapKey属性不为null的时候才会为true
         * 其他都是false
         */
        private final boolean returnsMap;

        /**
         * 是否返回的void
         */
        private final boolean returnsVoid;

        /**
         * 是否返回的是游标
         */
        private final boolean returnsCursor;

        /**
         * 返回的类型
         */
        private final Class<?> returnType;

        /**
         * 方法如果返回类型是map 并且有@MapKey 值为这个注解的value
         */
        private final String mapKey;

        /**
         * resultHandler参数所在的索引
         */
        private final Integer resultHandlerIndex;

        /**
         * rowBounds参数所在的索引
         */
        private final Integer rowBoundsIndex;

        /**
         * 参数名称分解器
         */
        private final ParamNameResolver paramNameResolver;

        /**
         * 构造函数
         *
         * @param configuration
         * @param mapperInterface
         * @param method
         */
        public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
            //拿到实际返回的类型
            Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
            //如果就是一般类 那么直接转换成class
            if (resolvedReturnType instanceof Class<?>) {
                this.returnType = (Class<?>) resolvedReturnType;
            }
            //如果是参数化泛型 那么拿泛型对应的原始类 并转换成class
            else if (resolvedReturnType instanceof ParameterizedType) {
                this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
            }
            //其他情况直接拿返回类型
            else {
                this.returnType = method.getReturnType();
            }
            //设置返回值是否是void
            this.returnsVoid = void.class.equals(this.returnType);
            //设置返回值是否是集合
            this.returnsMany =
                    configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
            //设置返回是不是游标
            this.returnsCursor = Cursor.class.equals(this.returnType);
            //获取mapKey的值 只有在返回类型是map并且@MapKey不为空的时候才会有值
            this.mapKey = getMapKey(method);
            //设置是不是返回的map
            this.returnsMap = this.mapKey != null;
            //获取RowBounds参数的索引
            this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
            //如果ResultHandler的索引
            this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
            //参数名称分解器
            this.paramNameResolver = new ParamNameResolver(configuration, method);
        }

        /**
         * 根据给出的参数 拿到参数名称跟对应的值
         *
         * @param args
         * @return
         */
        public Object convertArgsToSqlCommandParam(Object[] args) {
            return paramNameResolver.getNamedParams(args);
        }

        /**
         * 判断是否有RowBounds参数
         *
         * @return
         */
        public boolean hasRowBounds() {
            return rowBoundsIndex != null;
        }

        /**
         * 拿到rowBounds参数
         *
         * @param args
         * @return
         */
        public RowBounds extractRowBounds(Object[] args) {
            return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
        }

        /**
         * 判断是否有resultHandler参数
         *
         * @return
         */
        public boolean hasResultHandler() {
            return resultHandlerIndex != null;
        }

        /**
         * 拿到resultHandler参数
         *
         * @param args
         * @return
         */
        public ResultHandler extractResultHandler(Object[] args) {
            return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
        }

        /**
         * 获取@MapKey注解的value
         * 只有在方法返回的是map并且有@MapKey才会有值
         *
         * @return
         */
        public String getMapKey() {
            return mapKey;
        }

        /**
         * 拿到返回的类型
         *
         * @return
         */
        public Class<?> getReturnType() {
            return returnType;
        }

        /**
         * 拿到是否返回多条
         *
         * @return
         */
        public boolean returnsMany() {
            return returnsMany;
        }

        /**
         * 拿到是否存在@MapKey
         *
         * @return
         */
        public boolean returnsMap() {
            return returnsMap;
        }

        /**
         * 是否返回void
         *
         * @return
         */
        public boolean returnsVoid() {
            return returnsVoid;
        }

        /**
         * 是否返回Cursor
         *
         * @return
         */
        public boolean returnsCursor() {
            return returnsCursor;
        }

        /**
         * 获取paramType类型的参数所在索引值
         * 并且方法参数中只能存在一个paramType类型的参数
         * 否则抛出异常
         *
         * @param method
         * @param paramType
         * @return
         */
        private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
            Integer index = null;
            final Class<?>[] argTypes = method.getParameterTypes();
            for (int i = 0; i < argTypes.length; i++) {
                if (paramType.isAssignableFrom(argTypes[i])) {
                    if (index == null) {
                        index = i;
                    } else {
                        throw new BindingException(
                                method.getName() + " cannot have multiple " + paramType.getSimpleName()
                                        + " parameters");
                    }
                }
            }
            return index;
        }

        /**
         * 如果方法的返回是map类型的子类
         * 并且这个方法有@MapKey
         * 那么获取这个注解的值
         *
         * @param method
         * @return
         */
        private String getMapKey(Method method) {
            String mapKey = null;
            if (Map.class.isAssignableFrom(method.getReturnType())) {
                final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
                if (mapKeyAnnotation != null) {
                    mapKey = mapKeyAnnotation.value();
                }
            }
            return mapKey;
        }
    }

}
