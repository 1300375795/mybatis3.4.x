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
package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * This class represents a cached set of class definition information that
 * allows for easy mapping between property names and getter/setter methods.
 * 这个类表示一组缓存的类定义信息
 * 允许在属性名和getter/setter方法之间进行简单的映射。
 *
 * @author Clinton Begin
 */
public class Reflector {

    /**
     * 目标class对象
     */
    private final Class<?> type;

    /**
     * 可读属性名称数组 类似getXx这种属性
     */
    private final String[] readablePropertyNames;

    /**
     * 可写属性名称数组 类似setXx这种属性
     */
    private final String[] writeablePropertyNames;

    /**
     * set方法map key字段名称 value方法调用器
     */
    private final Map<String, Invoker> setMethods = new HashMap<String, Invoker>();

    /**
     * get方法map key字段名称 value方法调用器
     */
    private final Map<String, Invoker> getMethods = new HashMap<String, Invoker>();

    /**
     * set属性的类型map
     */
    private final Map<String, Class<?>> setTypes = new HashMap<String, Class<?>>();

    /**
     * get属性的类型map
     */
    private final Map<String, Class<?>> getTypes = new HashMap<String, Class<?>>();

    /**
     * 目标class的默认构造器
     */
    private Constructor<?> defaultConstructor;

    /**
     * 不区分大小写的属性映射map
     */
    private Map<String, String> caseInsensitivePropertyMap = new HashMap<String, String>();

    /**
     * 构造函数
     *
     * @param clazz 目标class
     */
    public Reflector(Class<?> clazz) {
        type = clazz;
        //根据class获取默认的构造器
        addDefaultConstructor(clazz);
        //根据class获取get方法
        addGetMethods(clazz);
        //根据class获取set方法
        addSetMethods(clazz);
        //根据class获取字段数组
        addFields(clazz);
        //设置可读属性数组
        readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
        //设置可写属性数组
        writeablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);
        //设置不区分大小写的属性
        for (String propName : readablePropertyNames) {
            caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
        }
        //设置不区分大小写的属性
        for (String propName : writeablePropertyNames) {
            caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
        }
    }

    /**
     * 根据class获取默认构造器
     *
     * @param clazz
     */
    private void addDefaultConstructor(Class<?> clazz) {
        Constructor<?>[] consts = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : consts) {
            if (constructor.getParameterTypes().length == 0) {
                if (canAccessPrivateMethods()) {
                    try {
                        constructor.setAccessible(true);
                    } catch (Exception e) {
                        // Ignored. This is only a final precaution, nothing we can do.
                    }
                }
                if (constructor.isAccessible()) {
                    this.defaultConstructor = constructor;
                }
            }
        }
    }

    /**
     * 根据class添加get开头或者is开头的方法
     *
     * @param cls
     */
    private void addGetMethods(Class<?> cls) {
        Map<String, List<Method>> conflictingGetters = new HashMap<String, List<Method>>();
        Method[] methods = getClassMethods(cls);
        for (Method method : methods) {
            //如果方法的参数列表为空跳过
            if (method.getParameterTypes().length > 0) {
                continue;
            }
            String name = method.getName();
            //如果该方法名称是以get开头并且方法名称长度大于3或者该方法名称是以is开头并且方法名称长度大于2
            if ((name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2)) {
                name = PropertyNamer.methodToProperty(name);
                //把name对应的方法放到conflictingGetters map中
                addMethodConflict(conflictingGetters, name, method);
            }
        }
        resolveGetterConflicts(conflictingGetters);
    }

    private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
        for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
            Method winner = null;
            String propName = entry.getKey();
            for (Method candidate : entry.getValue()) {
                //如果一开始没有获胜者 那么直接设置获胜者
                if (winner == null) {
                    winner = candidate;
                    continue;
                }
                //获取候选方法跟获胜方法的返回类型
                Class<?> winnerType = winner.getReturnType();
                Class<?> candidateType = candidate.getReturnType();
                //如果两个返回类型相同 isXx跟getXx这种字段会存在类型相同的
                if (candidateType.equals(winnerType)) {
                    //如果返回类型是不是boolean 抛出异常
                    if (!boolean.class.equals(candidateType)) {
                        throw new ReflectionException(
                                "Illegal overloaded getter method with ambiguous type for property " + propName
                                        + " in class " + winner.getDeclaringClass()
                                        + ". This breaks the JavaBeans specification and can cause unpredictable results.");
                    } else if (candidate.getName().startsWith("is")) {
                        winner = candidate;
                    }
                }
                //如果候选方法返回类型是获胜者返回类型的父类 忽略
                else if (candidateType.isAssignableFrom(winnerType)) {
                    // OK getter type is descendant
                }
                //如果获胜方法返回类型是候选方法返回类型的父类 那么候选方法更加匹配 候选方法获胜
                else if (winnerType.isAssignableFrom(candidateType)) {
                    winner = candidate;
                } else {
                    //其他场景抛出异常
                    throw new ReflectionException(
                            "Illegal overloaded getter method with ambiguous type for property " + propName
                                    + " in class " + winner.getDeclaringClass()
                                    + ". This breaks the JavaBeans specification and can cause unpredictable results.");
                }
            }
            addGetMethod(propName, winner);
        }
    }

    /**
     * 添加get属性对应的方法
     *
     * @param name
     * @param method
     */
    private void addGetMethod(String name, Method method) {
        //如果是有效属性名称
        if (isValidPropertyName(name)) {
            //将属性对应的方法进行包装存放到getMethods中
            getMethods.put(name, new MethodInvoker(method));
            //解析这个目标方法对应的返回类型
            Type returnType = TypeParameterResolver.resolveReturnType(method, type);
            //将属性对应的返回类型进行设置
            getTypes.put(name, typeToClass(returnType));
        }
    }

    /**
     * 添加set方法
     *
     * @param cls
     */
    private void addSetMethods(Class<?> cls) {
        Map<String, List<Method>> conflictingSetters = new HashMap<String, List<Method>>();
        Method[] methods = getClassMethods(cls);
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("set") && name.length() > 3) {
                if (method.getParameterTypes().length == 1) {
                    name = PropertyNamer.methodToProperty(name);
                    addMethodConflict(conflictingSetters, name, method);
                }
            }
        }
        resolveSetterConflicts(conflictingSetters);
    }

    /**
     * 添加属性对应的方法
     *
     * @param conflictingMethods
     * @param name
     * @param method
     */
    private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
        List<Method> list = conflictingMethods.get(name);
        if (list == null) {
            list = new ArrayList<Method>();
            conflictingMethods.put(name, list);
        }
        list.add(method);
    }

    /**
     * 根据属性名称找到这个属性最匹配的set方法
     *
     * @param conflictingSetters
     */
    private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
        for (String propName : conflictingSetters.keySet()) {
            List<Method> setters = conflictingSetters.get(propName);
            Class<?> getterType = getTypes.get(propName);
            Method match = null;
            ReflectionException exception = null;
            for (Method setter : setters) {
                Class<?> paramType = setter.getParameterTypes()[0];
                if (paramType.equals(getterType)) {
                    // should be the best match
                    match = setter;
                    break;
                }
                if (exception == null) {
                    try {
                        match = pickBetterSetter(match, setter, propName);
                    } catch (ReflectionException e) {
                        // there could still be the 'best match'
                        match = null;
                        exception = e;
                    }
                }
            }
            if (match == null) {
                throw exception;
            } else {
                addSetMethod(propName, match);
            }
        }
    }

    /**
     * 挑选最匹配的set方法
     *
     * @param setter1
     * @param setter2
     * @param property
     * @return
     */
    private Method pickBetterSetter(Method setter1, Method setter2, String property) {
        if (setter1 == null) {
            return setter2;
        }
        Class<?> paramType1 = setter1.getParameterTypes()[0];
        Class<?> paramType2 = setter2.getParameterTypes()[0];
        if (paramType1.isAssignableFrom(paramType2)) {
            return setter2;
        } else if (paramType2.isAssignableFrom(paramType1)) {
            return setter1;
        }
        throw new ReflectionException(
                "Ambiguous setters defined for property '" + property + "' in class '" + setter2.getDeclaringClass()
                        + "' with types '" + paramType1.getName() + "' and '" + paramType2.getName() + "'.");
    }

    /**
     * 添加set方法
     *
     * @param name
     * @param method
     */
    private void addSetMethod(String name, Method method) {
        if (isValidPropertyName(name)) {
            setMethods.put(name, new MethodInvoker(method));
            Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
            setTypes.put(name, typeToClass(paramTypes[0]));
        }
    }

    /**
     * 将Type接口转换成class
     *
     * @param src
     * @return
     */
    private Class<?> typeToClass(Type src) {
        Class<?> result = null;
        //如果是class 转换成class
        if (src instanceof Class) {
            result = (Class<?>) src;
        }
        //如果是泛型 那么转换成原始类型
        else if (src instanceof ParameterizedType) {
            result = (Class<?>) ((ParameterizedType) src).getRawType();
        }
        //如果是泛型数组类型
        else if (src instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) src).getGenericComponentType();
            if (componentType instanceof Class) {
                result = Array.newInstance((Class<?>) componentType, 0).getClass();
            } else {
                Class<?> componentClass = typeToClass(componentType);
                result = Array.newInstance((Class<?>) componentClass, 0).getClass();
            }
        }
        //如果是null 置为Object
        if (result == null) {
            result = Object.class;
        }
        return result;
    }

    /**
     * 设置class中额外的字段到getMethods跟setMethods中
     *
     * @param clazz
     */
    private void addFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (canAccessPrivateMethods()) {
                try {
                    field.setAccessible(true);
                } catch (Exception e) {
                    // Ignored. This is only a final precaution, nothing we can do.
                }
            }
            if (field.isAccessible()) {
                if (!setMethods.containsKey(field.getName())) {
                    // issue #379 - removed the check for final because JDK 1.5 allows
                    // modification of final fields through reflection (JSR-133). (JGB)
                    // pr #16 - final static can only be set by the classloader
                    int modifiers = field.getModifiers();
                    if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
                        addSetField(field);
                    }
                }
                if (!getMethods.containsKey(field.getName())) {
                    addGetField(field);
                }
            }
        }
        //如果存在父类 那么递归进行设置
        if (clazz.getSuperclass() != null) {
            addFields(clazz.getSuperclass());
        }
    }

    /**
     * 添加set字段
     *
     * @param field
     */
    private void addSetField(Field field) {
        if (isValidPropertyName(field.getName())) {
            setMethods.put(field.getName(), new SetFieldInvoker(field));
            Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
            setTypes.put(field.getName(), typeToClass(fieldType));
        }
    }

    /**
     * 添加get字段
     *
     * @param field
     */
    private void addGetField(Field field) {
        if (isValidPropertyName(field.getName())) {
            getMethods.put(field.getName(), new GetFieldInvoker(field));
            Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
            getTypes.put(field.getName(), typeToClass(fieldType));
        }
    }

    /**
     * 判断是否是有用的属性名称
     *
     * @param name
     * @return
     */
    private boolean isValidPropertyName(String name) {
        //如果属性名称以$开头或者是serialVersionUID或者是class属性
        return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
    }

    /**
     * This method returns an array containing all methods
     * declared in this class and any superclass.
     * We use this method, instead of the simpler Class.getMethods(),
     * because we want to look for private methods as well.
     * 获取给出的类的所有的方法 包括接口以及父类中的方法
     * 根据签名判断 相同的方法只添加一次
     *
     * @param cls The class
     * @return An array containing all methods in this class
     */
    private Method[] getClassMethods(Class<?> cls) {
        Map<String, Method> uniqueMethods = new HashMap<String, Method>();
        Class<?> currentClass = cls;
        while (currentClass != null && currentClass != Object.class) {
            addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

            // we also need to look for interface methods -
            // because the class may be abstract
            Class<?>[] interfaces = currentClass.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                addUniqueMethods(uniqueMethods, anInterface.getMethods());
            }

            currentClass = currentClass.getSuperclass();
        }

        Collection<Method> methods = uniqueMethods.values();

        return methods.toArray(new Method[methods.size()]);
    }

    /**
     * 添加方法到uniqueMethods中
     * 根据签名判断是否已经添加过
     * 如果没有添加过 那么添加 否则跳过
     *
     * @param uniqueMethods
     * @param methods
     */
    private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
        for (Method currentMethod : methods) {
            if (!currentMethod.isBridge()) {
                String signature = getSignature(currentMethod);
                // check to see if the method is already known
                // if it is known, then an extended class must have
                // overridden a method
                if (!uniqueMethods.containsKey(signature)) {
                    if (canAccessPrivateMethods()) {
                        try {
                            currentMethod.setAccessible(true);
                        } catch (Exception e) {
                            // Ignored. This is only a final precaution, nothing we can do.
                        }
                    }

                    uniqueMethods.put(signature, currentMethod);
                }
            }
        }
    }

    /**
     * 获取方法签名
     *
     * @param method
     * @return
     */
    private String getSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        Class<?> returnType = method.getReturnType();
        if (returnType != null) {
            sb.append(returnType.getName()).append('#');
        }
        sb.append(method.getName());
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i == 0) {
                sb.append(':');
            } else {
                sb.append(',');
            }
            sb.append(parameters[i].getName());
        }
        return sb.toString();
    }

    /**
     * 是否能访问私有方法
     *
     * @return
     */
    private static boolean canAccessPrivateMethods() {
        try {
            SecurityManager securityManager = System.getSecurityManager();
            if (null != securityManager) {
                securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
            }
        } catch (SecurityException e) {
            return false;
        }
        return true;
    }

    /**
     * Gets the name of the class the instance provides information for
     * 获取原始这个reflector对应的class
     *
     * @return The class name
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * 获取目标class对应的默认构造函数
     *
     * @return
     */
    public Constructor<?> getDefaultConstructor() {
        if (defaultConstructor != null) {
            return defaultConstructor;
        } else {
            throw new ReflectionException("There is no default constructor for " + type);
        }
    }

    /**
     * 目标class是否有默认构造函数
     *
     * @return
     */
    public boolean hasDefaultConstructor() {
        return defaultConstructor != null;
    }

    /**
     * 根据属性名称拿到对应的set执行器
     *
     * @param propertyName
     * @return
     */
    public Invoker getSetInvoker(String propertyName) {
        Invoker method = setMethods.get(propertyName);
        if (method == null) {
            throw new ReflectionException(
                    "There is no setter for property named '" + propertyName + "' in '" + type + "'");
        }
        return method;
    }

    /**
     * 根据属性名称拿到对应的get执行器
     *
     * @param propertyName
     * @return
     */
    public Invoker getGetInvoker(String propertyName) {
        Invoker method = getMethods.get(propertyName);
        if (method == null) {
            throw new ReflectionException(
                    "There is no getter for property named '" + propertyName + "' in '" + type + "'");
        }
        return method;
    }

    /**
     * Gets the type for a property setter
     * 根据熟悉名称拿到这个属性对应的class类型
     *
     * @param propertyName - the name of the property
     * @return The Class of the propery setter
     */
    public Class<?> getSetterType(String propertyName) {
        Class<?> clazz = setTypes.get(propertyName);
        if (clazz == null) {
            throw new ReflectionException(
                    "There is no setter for property named '" + propertyName + "' in '" + type + "'");
        }
        return clazz;
    }

    /**
     * Gets the type for a property getter
     * 根据属性名称拿到这个属性对应的class类型
     *
     * @param propertyName - the name of the property
     * @return The Class of the propery getter
     */
    public Class<?> getGetterType(String propertyName) {
        Class<?> clazz = getTypes.get(propertyName);
        if (clazz == null) {
            throw new ReflectionException(
                    "There is no getter for property named '" + propertyName + "' in '" + type + "'");
        }
        return clazz;
    }

    /**
     * Gets an array of the readable properties for an object
     * 获取目标class对应所有的可读属性
     *
     * @return The array
     */
    public String[] getGetablePropertyNames() {
        return readablePropertyNames;
    }

    /**
     * Gets an array of the writeable properties for an object
     * 获取目标class对应的可写属性
     *
     * @return The array
     */
    public String[] getSetablePropertyNames() {
        return writeablePropertyNames;
    }

    /**
     * Check to see if a class has a writeable property by name
     * 判断给出的属性是否有set方法
     *
     * @param propertyName - the name of the property to check
     * @return True if the object has a writeable property by the name
     */
    public boolean hasSetter(String propertyName) {
        return setMethods.keySet().contains(propertyName);
    }

    /**
     * Check to see if a class has a readable property by name
     * 判断给出的属性是否有get方法
     *
     * @param propertyName - the name of the property to check
     * @return True if the object has a readable property by the name
     */
    public boolean hasGetter(String propertyName) {
        return getMethods.keySet().contains(propertyName);
    }

    /**
     * 根据不区分大小写的属性名称拿到这个名称对应的属性
     *
     * @param name
     * @return
     */
    public String findPropertyName(String name) {
        return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
    }
}
