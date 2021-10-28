package org.apache.ibatis.my_test.plugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

@Intercepts({ @Signature(type = Interceptor.class, method = "intercept", args = { Invocation.class }) })
public class ErrorInterceptorTest implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object[] args = invocation.getArgs();
        Object target = invocation.getTarget();
        System.out.println("InterceptorTest2当前被拦截的目标是:" + target);
        System.out.println("InterceptorTest2目标方法是:" + method.toString());
        System.out.println("InterceptorTest2参数是:" + Arrays.toString(args));
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
