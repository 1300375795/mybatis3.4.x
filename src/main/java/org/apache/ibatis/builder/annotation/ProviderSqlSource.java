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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 * sql源提供者
 */
public class ProviderSqlSource implements SqlSource {

    /**
     * 全局配置
     */
    private final Configuration configuration;

    /**
     * sql源构建器
     */
    private final SqlSourceBuilder sqlSourceParser;

    /**
     * 提供者class类型
     */
    private final Class<?> providerType;

    /**
     * 提供者方法
     */
    private Method providerMethod;

    /**
     * 提供者方法参数名称
     */
    private String[] providerMethodArgumentNames;

    /**
     * 提供者方法参数类型
     */
    private Class<?>[] providerMethodParameterTypes;

    /**
     * 提供者内容
     */
    private ProviderContext providerContext;

    /**
     * 提供者索引序号
     */
    private Integer providerContextIndex;

    /**
     * @deprecated Please use the {@link #ProviderSqlSource(Configuration, Object, Class, Method)} instead of this.
     * 构造函数
     */
    @Deprecated
    public ProviderSqlSource(Configuration configuration, Object provider) {
        this(configuration, provider, null, null);
    }

    /**
     * @since 3.4.5
     * 构造函数
     */
    public ProviderSqlSource(Configuration configuration, Object provider, Class<?> mapperType, Method mapperMethod) {
        String providerMethodName;
        try {
            this.configuration = configuration;
            this.sqlSourceParser = new SqlSourceBuilder(configuration);
            //拿到提供者类型class
            this.providerType = (Class<?>) provider.getClass().getMethod("type").invoke(provider);
            //拿到提供者具体对应class里的哪个方法
            providerMethodName = (String) provider.getClass().getMethod("method").invoke(provider);

            for (Method m : this.providerType.getMethods()) {
                //如果是同名方法 并且这个方法的返回类型是CharSequence的子类 并且已经在之前找到过providerMethod 那么抛出异常
                if (providerMethodName.equals(m.getName()) && CharSequence.class.isAssignableFrom(m.getReturnType())) {
                    if (providerMethod != null) {
                        throw new BuilderException(
                                "Error creating SqlSource for SqlProvider. Method '" + providerMethodName
                                        + "' is found multiple in SqlProvider '" + this.providerType.getName()
                                        + "'. Sql provider method can not overload.");
                    }
                    this.providerMethod = m;
                    //创建参数名称分解器
                    this.providerMethodArgumentNames = new ParamNameResolver(configuration, m).getNames();
                    this.providerMethodParameterTypes = m.getParameterTypes();
                }
            }
        } catch (BuilderException e) {
            throw e;
        } catch (Exception e) {
            throw new BuilderException("Error creating SqlSource for SqlProvider.  Cause: " + e, e);
        }
        //如果没有找到那么抛出异常
        if (this.providerMethod == null) {
            throw new BuilderException("Error creating SqlSource for SqlProvider. Method '" + providerMethodName
                    + "' not found in SqlProvider '" + this.providerType.getName() + "'.");
        }
        for (int i = 0; i < this.providerMethodParameterTypes.length; i++) {
            Class<?> parameterType = this.providerMethodParameterTypes[i];
            //如果参数类型是ProviderContext 那么转换mapper接口跟被@XxProvider注解的方法为ProviderContext
            // 不能存在多个ProviderContext参数 否则抛出异常
            if (parameterType == ProviderContext.class) {
                if (this.providerContext != null) {
                    throw new BuilderException(
                            "Error creating SqlSource for SqlProvider. ProviderContext found multiple in SqlProvider method ("
                                    + this.providerType.getName() + "." + providerMethod.getName()
                                    + "). ProviderContext can not define multiple in SqlProvider method argument.");
                }
                this.providerContext = new ProviderContext(mapperType, mapperMethod);
                this.providerContextIndex = i;
            }
        }
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        SqlSource sqlSource = createSqlSource(parameterObject);
        return sqlSource.getBoundSql(parameterObject);
    }

    /**
     * 创建sql源
     *
     * @param parameterObject
     * @return
     */
    private SqlSource createSqlSource(Object parameterObject) {
        try {
            //绑定的参数的数量 不算providerContext参数
            int bindParameterCount = providerMethodParameterTypes.length - (providerContext == null ? 0 : 1);
            String sql;
            //如果没有参数
            if (providerMethodParameterTypes.length == 0) {
                sql = invokeProviderMethod();
            }
            //如果绑定的参数数量为0 即只有providerContext参数
            else if (bindParameterCount == 0) {
                sql = invokeProviderMethod(providerContext);
            }
            //如果除了providerContext外只有一个参数 那么根据providerContextIndex的情况 获取providerContext的参数索引位置 拿到这个providerContext
            else if (bindParameterCount == 1 && (parameterObject == null || providerMethodParameterTypes[(
                    providerContextIndex == null || providerContextIndex == 1) ? 0 : 1]
                    .isAssignableFrom(parameterObject.getClass()))) {
                sql = invokeProviderMethod(extractProviderMethodArguments(parameterObject));
            }
            //如果参数类型是map
            else if (parameterObject instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) parameterObject;
                sql = invokeProviderMethod(extractProviderMethodArguments(params, providerMethodArgumentNames));
            }
            //其他类型抛出异常
            else {
                throw new BuilderException(
                        "Error invoking SqlProvider method (" + providerType.getName() + "." + providerMethod.getName()
                                + "). Cannot invoke a method that holds " + (bindParameterCount == 1 ?
                                "named argument(@Param)" :
                                "multiple arguments")
                                + " using a specifying parameterObject. In this case, please specify a 'java.util.Map' object.");
            }
            Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
            //解析sql
            return sqlSourceParser.parse(replacePlaceholder(sql), parameterType, new HashMap<String, Object>());
        } catch (BuilderException e) {
            throw e;
        } catch (Exception e) {
            throw new BuilderException(
                    "Error invoking SqlProvider method (" + providerType.getName() + "." + providerMethod.getName()
                            + ").  Cause: " + e, e);
        }
    }

    /**
     * 提取提供者方法参数
     *
     * @param parameterObject
     * @return
     */
    private Object[] extractProviderMethodArguments(Object parameterObject) {
        if (providerContext != null) {
            Object[] args = new Object[2];
            args[providerContextIndex == 0 ? 1 : 0] = parameterObject;
            args[providerContextIndex] = providerContext;
            return args;
        } else {
            return new Object[] { parameterObject };
        }
    }

    /**
     * 提取提供者方法参数
     *
     * @param params
     * @param argumentNames
     * @return
     */
    private Object[] extractProviderMethodArguments(Map<String, Object> params, String[] argumentNames) {
        Object[] args = new Object[argumentNames.length];
        for (int i = 0; i < args.length; i++) {
            if (providerContextIndex != null && providerContextIndex == i) {
                args[i] = providerContext;
            } else {
                args[i] = params.get(argumentNames[i]);
            }
        }
        return args;
    }

    /**
     * 执行提供者方法
     *
     * @param args
     * @return
     * @throws Exception
     */
    private String invokeProviderMethod(Object... args) throws Exception {
        Object targetObject = null;
        //如果不是静态方法那么创建相应的对象执行 拿到执行方法返回的sql
        if (!Modifier.isStatic(providerMethod.getModifiers())) {
            targetObject = providerType.newInstance();
        }
        CharSequence sql = (CharSequence) providerMethod.invoke(targetObject, args);
        return sql != null ? sql.toString() : null;
    }

    /**
     * 替换占位符
     *
     * @param sql
     * @return
     */
    private String replacePlaceholder(String sql) {
        return PropertyParser.parse(sql, configuration.getVariables());
    }

}
