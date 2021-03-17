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
package org.apache.ibatis.type;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.JapaneseDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.reflection.Jdk;

/**
 * 类型处理器注册类
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public final class TypeHandlerRegistry {

    /**
     * jdbc类型对应的类型处理器map
     * 这个map只会在TypeHandlerRegistry的构造函数里面进行put操作 外部没有put操作
     */
    private final Map<JdbcType, TypeHandler<?>> JDBC_TYPE_HANDLER_MAP = new EnumMap<JdbcType, TypeHandler<?>>(
            JdbcType.class);

    /**
     * 类型处理器map
     * key java类型 value jdbc类型跟相应的类型处理器
     * 即一个java类型能存在多个Map<JdbcType, TypeHandler<?>>Entry
     * JAVA类型如果为空 不会put到这个map中
     */
    private final Map<Type, Map<JdbcType, TypeHandler<?>>> TYPE_HANDLER_MAP = new ConcurrentHashMap<Type, Map<JdbcType, TypeHandler<?>>>();

    /**
     * 未知类型处理器
     */
    private final TypeHandler<Object> UNKNOWN_TYPE_HANDLER = new UnknownTypeHandler(this);

    /**
     * 所有class类型对应的类型处理器map
     * 不管什么类型的处理器 都会添加在这个map中
     * 如果一个类型处理器的javaType是null的话 那么就只会存在在这个map中
     */
    private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<Class<?>, TypeHandler<?>>();

    /**
     * NULL类型处理器map
     */
    private static final Map<JdbcType, TypeHandler<?>> NULL_TYPE_HANDLER_MAP = Collections.emptyMap();

    /**
     * 默认的枚举类型处理器
     */
    private Class<? extends TypeHandler> defaultEnumTypeHandler = EnumTypeHandler.class;

    /**
     * 构造器
     * 初始化一些默认的类型处理器
     */
    public TypeHandlerRegistry() {
        register(Boolean.class, new BooleanTypeHandler());
        register(boolean.class, new BooleanTypeHandler());
        register(JdbcType.BOOLEAN, new BooleanTypeHandler());
        register(JdbcType.BIT, new BooleanTypeHandler());

        register(Byte.class, new ByteTypeHandler());
        register(byte.class, new ByteTypeHandler());
        register(JdbcType.TINYINT, new ByteTypeHandler());

        register(Short.class, new ShortTypeHandler());
        register(short.class, new ShortTypeHandler());
        register(JdbcType.SMALLINT, new ShortTypeHandler());

        register(Integer.class, new IntegerTypeHandler());
        register(int.class, new IntegerTypeHandler());
        register(JdbcType.INTEGER, new IntegerTypeHandler());

        register(Long.class, new LongTypeHandler());
        register(long.class, new LongTypeHandler());

        register(Float.class, new FloatTypeHandler());
        register(float.class, new FloatTypeHandler());
        register(JdbcType.FLOAT, new FloatTypeHandler());

        register(Double.class, new DoubleTypeHandler());
        register(double.class, new DoubleTypeHandler());
        register(JdbcType.DOUBLE, new DoubleTypeHandler());

        register(Reader.class, new ClobReaderTypeHandler());
        register(String.class, new StringTypeHandler());
        register(String.class, JdbcType.CHAR, new StringTypeHandler());
        register(String.class, JdbcType.CLOB, new ClobTypeHandler());
        register(String.class, JdbcType.VARCHAR, new StringTypeHandler());
        register(String.class, JdbcType.LONGVARCHAR, new ClobTypeHandler());
        register(String.class, JdbcType.NVARCHAR, new NStringTypeHandler());
        register(String.class, JdbcType.NCHAR, new NStringTypeHandler());
        register(String.class, JdbcType.NCLOB, new NClobTypeHandler());
        register(JdbcType.CHAR, new StringTypeHandler());
        register(JdbcType.VARCHAR, new StringTypeHandler());
        register(JdbcType.CLOB, new ClobTypeHandler());
        register(JdbcType.LONGVARCHAR, new ClobTypeHandler());
        register(JdbcType.NVARCHAR, new NStringTypeHandler());
        register(JdbcType.NCHAR, new NStringTypeHandler());
        register(JdbcType.NCLOB, new NClobTypeHandler());

        register(Object.class, JdbcType.ARRAY, new ArrayTypeHandler());
        register(JdbcType.ARRAY, new ArrayTypeHandler());

        register(BigInteger.class, new BigIntegerTypeHandler());
        register(JdbcType.BIGINT, new LongTypeHandler());

        register(BigDecimal.class, new BigDecimalTypeHandler());
        register(JdbcType.REAL, new BigDecimalTypeHandler());
        register(JdbcType.DECIMAL, new BigDecimalTypeHandler());
        register(JdbcType.NUMERIC, new BigDecimalTypeHandler());

        register(InputStream.class, new BlobInputStreamTypeHandler());
        register(Byte[].class, new ByteObjectArrayTypeHandler());
        register(Byte[].class, JdbcType.BLOB, new BlobByteObjectArrayTypeHandler());
        register(Byte[].class, JdbcType.LONGVARBINARY, new BlobByteObjectArrayTypeHandler());
        register(byte[].class, new ByteArrayTypeHandler());
        register(byte[].class, JdbcType.BLOB, new BlobTypeHandler());
        register(byte[].class, JdbcType.LONGVARBINARY, new BlobTypeHandler());
        register(JdbcType.LONGVARBINARY, new BlobTypeHandler());
        register(JdbcType.BLOB, new BlobTypeHandler());

        register(Object.class, UNKNOWN_TYPE_HANDLER);
        register(Object.class, JdbcType.OTHER, UNKNOWN_TYPE_HANDLER);
        register(JdbcType.OTHER, UNKNOWN_TYPE_HANDLER);

        register(Date.class, new DateTypeHandler());
        register(Date.class, JdbcType.DATE, new DateOnlyTypeHandler());
        register(Date.class, JdbcType.TIME, new TimeOnlyTypeHandler());
        register(JdbcType.TIMESTAMP, new DateTypeHandler());
        register(JdbcType.DATE, new DateOnlyTypeHandler());
        register(JdbcType.TIME, new TimeOnlyTypeHandler());

        register(java.sql.Date.class, new SqlDateTypeHandler());
        register(java.sql.Time.class, new SqlTimeTypeHandler());
        register(java.sql.Timestamp.class, new SqlTimestampTypeHandler());

        // mybatis-typehandlers-jsr310
        if (Jdk.dateAndTimeApiExists) {
            this.register(Instant.class, InstantTypeHandler.class);
            this.register(LocalDateTime.class, LocalDateTimeTypeHandler.class);
            this.register(LocalDate.class, LocalDateTypeHandler.class);
            this.register(LocalTime.class, LocalTimeTypeHandler.class);
            this.register(OffsetDateTime.class, OffsetDateTimeTypeHandler.class);
            this.register(OffsetTime.class, OffsetTimeTypeHandler.class);
            this.register(ZonedDateTime.class, ZonedDateTimeTypeHandler.class);
            this.register(Month.class, MonthTypeHandler.class);
            this.register(Year.class, YearTypeHandler.class);
            this.register(YearMonth.class, YearMonthTypeHandler.class);
            this.register(JapaneseDate.class, JapaneseDateTypeHandler.class);
        }

        // issue #273
        register(Character.class, new CharacterTypeHandler());
        register(char.class, new CharacterTypeHandler());
    }

    /**
     * 设置默认的枚举处理器
     * Set a default {@link TypeHandler} class for {@link Enum}.
     * A default {@link TypeHandler} is {@link org.apache.ibatis.type.EnumTypeHandler}.
     *
     * @param typeHandler a type handler class for {@link Enum}
     * @since 3.4.5
     */
    public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
        this.defaultEnumTypeHandler = typeHandler;
    }

    /**
     * 判断是否存在指定类型的类型处理器
     *
     * @param javaType
     * @return
     */
    public boolean hasTypeHandler(Class<?> javaType) {
        return hasTypeHandler(javaType, null);
    }

    /**
     * 判断是否存在指定类型的类型处理器
     *
     * @param javaTypeReference
     * @return
     */
    public boolean hasTypeHandler(TypeReference<?> javaTypeReference) {
        return hasTypeHandler(javaTypeReference, null);
    }

    /**
     * 判断是否存在执行类型的类型处理器
     *
     * @param javaType
     * @param jdbcType
     * @return
     */
    public boolean hasTypeHandler(Class<?> javaType, JdbcType jdbcType) {
        return javaType != null && getTypeHandler((Type) javaType, jdbcType) != null;
    }

    /**
     * 判断是否存在执行类型的类型处理器
     *
     * @param javaTypeReference
     * @param jdbcType
     * @return
     */
    public boolean hasTypeHandler(TypeReference<?> javaTypeReference, JdbcType jdbcType) {
        return javaTypeReference != null && getTypeHandler(javaTypeReference, jdbcType) != null;
    }

    /**
     * 拿到指定class类型的类型处理器
     *
     * @param handlerType
     * @return
     */
    public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
        return ALL_TYPE_HANDLERS_MAP.get(handlerType);
    }

    /**
     * 根据Type类型拿到执行类型的类型处理器
     *
     * @param type
     * @param <T>
     * @return
     */
    public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
        return getTypeHandler((Type) type, null);
    }

    /**
     * 根据TypeReference拿到指定类型的类型处理器
     *
     * @param javaTypeReference
     * @param <T>
     * @return
     */
    public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference) {
        return getTypeHandler(javaTypeReference, null);
    }

    /**
     * 根据jdbc类型拿到指定类型的类型处理器
     *
     * @param jdbcType
     * @return
     */
    public TypeHandler<?> getTypeHandler(JdbcType jdbcType) {
        return JDBC_TYPE_HANDLER_MAP.get(jdbcType);
    }

    /**
     * 根据class以及jdbc类型拿到指定的类型处理器
     *
     * @param type
     * @param jdbcType
     * @param <T>
     * @return
     */
    public <T> TypeHandler<T> getTypeHandler(Class<T> type, JdbcType jdbcType) {
        return getTypeHandler((Type) type, jdbcType);
    }

    /**
     * 根据类型引用以及jdbc类型拿到指定的类型处理器
     *
     * @param javaTypeReference
     * @param jdbcType
     * @param <T>
     * @return
     */
    public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference, JdbcType jdbcType) {
        return getTypeHandler(javaTypeReference.getRawType(), jdbcType);
    }

    /**
     * 根据类型以及jdbc类型获取类型处理器
     *
     * @param type
     * @param jdbcType
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> TypeHandler<T> getTypeHandler(Type type, JdbcType jdbcType) {
        //如果是参数map类型直接返回null
        if (ParamMap.class.equals(type)) {
            return null;
        }
        Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = getJdbcHandlerMap(type);
        TypeHandler<?> handler = null;
        if (jdbcHandlerMap != null) {
            handler = jdbcHandlerMap.get(jdbcType);
            if (handler == null) {
                handler = jdbcHandlerMap.get(null);
            }
            if (handler == null) {
                // #591
                handler = pickSoleHandler(jdbcHandlerMap);
            }
        }
        // type drives generics here
        return (TypeHandler<T>) handler;
    }

    private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMap(Type type) {
        //根据类型对应的jdbc 类型处理器map
        Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(type);
        //如果是NULL对应的map 那么直接返回null
        if (NULL_TYPE_HANDLER_MAP.equals(jdbcHandlerMap)) {
            return null;
        }
        if (jdbcHandlerMap == null && type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isEnum()) {
                jdbcHandlerMap = getJdbcHandlerMapForEnumInterfaces(clazz, clazz);
                if (jdbcHandlerMap == null) {
                    register(clazz, getInstance(clazz, defaultEnumTypeHandler));
                    return TYPE_HANDLER_MAP.get(clazz);
                }
            } else {
                jdbcHandlerMap = getJdbcHandlerMapForSuperclass(clazz);
            }
        }
        TYPE_HANDLER_MAP.put(type, jdbcHandlerMap == null ? NULL_TYPE_HANDLER_MAP : jdbcHandlerMap);
        return jdbcHandlerMap;
    }

    private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForEnumInterfaces(Class<?> clazz, Class<?> enumClazz) {
        for (Class<?> iface : clazz.getInterfaces()) {
            Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(iface);
            if (jdbcHandlerMap == null) {
                jdbcHandlerMap = getJdbcHandlerMapForEnumInterfaces(iface, enumClazz);
            }
            if (jdbcHandlerMap != null) {
                // Found a type handler regsiterd to a super interface
                HashMap<JdbcType, TypeHandler<?>> newMap = new HashMap<JdbcType, TypeHandler<?>>();
                for (Entry<JdbcType, TypeHandler<?>> entry : jdbcHandlerMap.entrySet()) {
                    // Create a type handler instance with enum type as a constructor arg
                    newMap.put(entry.getKey(), getInstance(enumClazz, entry.getValue().getClass()));
                }
                return newMap;
            }
        }
        return null;
    }

    private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForSuperclass(Class<?> clazz) {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || Object.class.equals(superclass)) {
            return null;
        }
        Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(superclass);
        if (jdbcHandlerMap != null) {
            return jdbcHandlerMap;
        } else {
            return getJdbcHandlerMapForSuperclass(superclass);
        }
    }

    private TypeHandler<?> pickSoleHandler(Map<JdbcType, TypeHandler<?>> jdbcHandlerMap) {
        TypeHandler<?> soleHandler = null;
        for (TypeHandler<?> handler : jdbcHandlerMap.values()) {
            if (soleHandler == null) {
                soleHandler = handler;
            } else if (!handler.getClass().equals(soleHandler.getClass())) {
                // More than one type handlers registered.
                return null;
            }
        }
        return soleHandler;
    }

    public TypeHandler<Object> getUnknownTypeHandler() {
        return UNKNOWN_TYPE_HANDLER;
    }

    /**
     * 注册jdbc类型的类型处理器
     *
     * @param jdbcType
     * @param handler
     */
    public void register(JdbcType jdbcType, TypeHandler<?> handler) {
        JDBC_TYPE_HANDLER_MAP.put(jdbcType, handler);
    }

    //
    // REGISTER INSTANCE
    //

    // Only handler

    @SuppressWarnings("unchecked")
    public <T> void register(TypeHandler<T> typeHandler) {
        boolean mappedTypeFound = false;
        MappedTypes mappedTypes = typeHandler.getClass().getAnnotation(MappedTypes.class);
        if (mappedTypes != null) {
            for (Class<?> handledType : mappedTypes.value()) {
                register(handledType, typeHandler);
                mappedTypeFound = true;
            }
        }
        // @since 3.1.0 - try to auto-discover the mapped type
        if (!mappedTypeFound && typeHandler instanceof TypeReference) {
            try {
                TypeReference<T> typeReference = (TypeReference<T>) typeHandler;
                register(typeReference.getRawType(), typeHandler);
                mappedTypeFound = true;
            } catch (Throwable t) {
                // maybe users define the TypeReference with a different type and are not assignable, so just ignore it
            }
        }
        if (!mappedTypeFound) {
            register((Class<T>) null, typeHandler);
        }
    }

    // java type + handler

    public <T> void register(Class<T> javaType, TypeHandler<? extends T> typeHandler) {
        register((Type) javaType, typeHandler);
    }

    private <T> void register(Type javaType, TypeHandler<? extends T> typeHandler) {
        MappedJdbcTypes mappedJdbcTypes = typeHandler.getClass().getAnnotation(MappedJdbcTypes.class);
        if (mappedJdbcTypes != null) {
            for (JdbcType handledJdbcType : mappedJdbcTypes.value()) {
                register(javaType, handledJdbcType, typeHandler);
            }
            if (mappedJdbcTypes.includeNullJdbcType()) {
                register(javaType, null, typeHandler);
            }
        } else {
            register(javaType, null, typeHandler);
        }
    }

    public <T> void register(TypeReference<T> javaTypeReference, TypeHandler<? extends T> handler) {
        register(javaTypeReference.getRawType(), handler);
    }

    // java type + jdbc type + handler

    public <T> void register(Class<T> type, JdbcType jdbcType, TypeHandler<? extends T> handler) {
        register((Type) type, jdbcType, handler);
    }

    private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
        if (javaType != null) {
            Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.get(javaType);
            if (map == null || map == NULL_TYPE_HANDLER_MAP) {
                map = new HashMap<JdbcType, TypeHandler<?>>();
                TYPE_HANDLER_MAP.put(javaType, map);
            }
            map.put(jdbcType, handler);
        }
        ALL_TYPE_HANDLERS_MAP.put(handler.getClass(), handler);
    }

    /**
     * 根据类型处理器class对象进行注册
     * 如果class对象上面存在MappedTypes注解
     * 那么拿到这个注解里面说明的这个处理器处理的java类型进行注册
     * 如果没有注解那么根据这个处理器的实例进行注册
     *
     * @param typeHandlerClass
     */
    public void register(Class<?> typeHandlerClass) {
        boolean mappedTypeFound = false;
        MappedTypes mappedTypes = typeHandlerClass.getAnnotation(MappedTypes.class);
        if (mappedTypes != null) {
            for (Class<?> javaTypeClass : mappedTypes.value()) {
                register(javaTypeClass, typeHandlerClass);
                mappedTypeFound = true;
            }
        }
        if (!mappedTypeFound) {
            register(getInstance(null, typeHandlerClass));
        }
    }

    /**
     * 根据java类型跟处理器类型class字符串进行注册
     *
     * @param javaTypeClassName
     * @param typeHandlerClassName
     * @throws ClassNotFoundException
     */
    public void register(String javaTypeClassName, String typeHandlerClassName) throws ClassNotFoundException {
        register(Resources.classForName(javaTypeClassName), Resources.classForName(typeHandlerClassName));
    }

    /**
     * 根据java类型跟处理器类型class对象进行注册
     *
     * @param javaTypeClass
     * @param typeHandlerClass
     */
    public void register(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
        register(javaTypeClass, getInstance(javaTypeClass, typeHandlerClass));
    }

    /**
     * 根据java类型class对象 jdbc类型以及类型处理器class对象进行注册
     *
     * @param javaTypeClass
     * @param jdbcType
     * @param typeHandlerClass
     */
    public void register(Class<?> javaTypeClass, JdbcType jdbcType, Class<?> typeHandlerClass) {
        register(javaTypeClass, jdbcType, getInstance(javaTypeClass, typeHandlerClass));
    }

    /**
     * 获取类型处理器实例
     *
     * @param javaTypeClass    构造器参数class
     * @param typeHandlerClass 类型处理器class
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> getInstance(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
        if (javaTypeClass != null) {
            try {
                Constructor<?> c = typeHandlerClass.getConstructor(Class.class);
                return (TypeHandler<T>) c.newInstance(javaTypeClass);
            } catch (NoSuchMethodException ignored) {
                // ignored
            } catch (Exception e) {
                throw new TypeException("Failed invoking constructor for handler " + typeHandlerClass, e);
            }
        }
        try {
            Constructor<?> c = typeHandlerClass.getConstructor();
            return (TypeHandler<T>) c.newInstance();
        } catch (Exception e) {
            throw new TypeException("Unable to find a usable constructor for " + typeHandlerClass, e);
        }
    }

    /**
     * 将某个包下面所有的TypeHandler子类进行注册
     *
     * @param packageName
     */
    public void register(String packageName) {
        ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
        resolverUtil.find(new ResolverUtil.IsA(TypeHandler.class), packageName);
        Set<Class<? extends Class<?>>> handlerSet = resolverUtil.getClasses();
        for (Class<?> type : handlerSet) {
            //Ignore inner classes and interfaces (including package-info.java) and abstract classes
            if (!type.isAnonymousClass() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                register(type);
            }
        }
    }

    /**
     * 返回所有的类型处理器map 不可修改
     *
     * @since 3.2.2
     */
    public Collection<TypeHandler<?>> getTypeHandlers() {
        return Collections.unmodifiableCollection(ALL_TYPE_HANDLERS_MAP.values());
    }

}
