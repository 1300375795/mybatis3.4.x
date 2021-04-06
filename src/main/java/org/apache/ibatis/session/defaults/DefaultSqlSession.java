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
package org.apache.ibatis.session.defaults;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.result.DefaultMapResultHandler;
import org.apache.ibatis.executor.result.DefaultResultContext;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

/**
 * // TODO: 2021/4/7 CallYeDeGuo 核心类
 * 默认的sql会话
 * The default implementation for {@link SqlSession}.
 * Note that this class is not Thread-Safe.
 *
 * @author Clinton Begin
 */
public class DefaultSqlSession implements SqlSession {

    /**
     * 全局配置
     */
    private final Configuration configuration;

    /**
     * 执行器
     */
    private final Executor executor;

    /**
     * 是否自动提交
     */
    private final boolean autoCommit;

    /**
     * 是否脏
     */
    private boolean dirty;

    /**
     * 游标集合
     */
    private List<Cursor<?>> cursorList;

    /**
     * 构造函数
     *
     * @param configuration
     * @param executor
     * @param autoCommit
     */
    public DefaultSqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
        this.configuration = configuration;
        this.executor = executor;
        this.dirty = false;
        this.autoCommit = autoCommit;
    }

    /**
     * 构造函数
     *
     * @param configuration
     * @param executor
     */
    public DefaultSqlSession(Configuration configuration, Executor executor) {
        this(configuration, executor, false);
    }

    /**
     * 查询一条
     *
     * @param statement
     * @param <T>
     * @return
     */
    @Override
    public <T> T selectOne(String statement) {
        return this.<T>selectOne(statement, null);
    }

    /**
     * 查询一条
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param <T>
     * @return
     */
    @Override
    public <T> T selectOne(String statement, Object parameter) {
        // Popular vote was to return null on 0 results and throw exception on too many
        //调用查询多条的接口
        List<T> list = this.<T>selectList(statement, parameter);
        //如果长度是1 返回这一条
        if (list.size() == 1) {
            return list.get(0);
        }
        //如果多于1条那么抛出异常
        else if (list.size() > 1) {
            throw new TooManyResultsException(
                    "Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
        } else {
            //其他情况返回null
            return null;
        }
    }

    /**
     * 查询mp
     *
     * @param statement Unique identifier matching the statement to use.
     * @param mapKey    The property to use as key for each value in the list.
     * @param <K>
     * @param <V>
     * @return
     */
    @Override
    public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
        return this.selectMap(statement, null, mapKey, RowBounds.DEFAULT);
    }

    /**
     * 查询map
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param mapKey    The property to use as key for each value in the list.
     * @param <K>
     * @param <V>
     * @return
     */
    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
        return this.selectMap(statement, parameter, mapKey, RowBounds.DEFAULT);
    }

    /**
     * 查询map
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param mapKey    The property to use as key for each value in the list.
     * @param rowBounds Bounds to limit object retrieval
     * @param <K>
     * @param <V>
     * @return
     */
    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
        final List<? extends V> list = selectList(statement, parameter, rowBounds);
        final DefaultMapResultHandler<K, V> mapResultHandler = new DefaultMapResultHandler<K, V>(mapKey,
                configuration.getObjectFactory(), configuration.getObjectWrapperFactory(),
                configuration.getReflectorFactory());
        final DefaultResultContext<V> context = new DefaultResultContext<V>();
        for (V o : list) {
            context.nextResultObject(o);
            mapResultHandler.handleResult(context);
        }
        return mapResultHandler.getMappedResults();
    }

    /**
     * 查询游标
     *
     * @param statement Unique identifier matching the statement to use.
     * @param <T>
     * @return
     */
    @Override
    public <T> Cursor<T> selectCursor(String statement) {
        return selectCursor(statement, null);
    }

    /**
     * 查询游标
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param <T>
     * @return
     */
    @Override
    public <T> Cursor<T> selectCursor(String statement, Object parameter) {
        return selectCursor(statement, parameter, RowBounds.DEFAULT);
    }

    /**
     * 查询游标
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param rowBounds Bounds to limit object retrieval
     * @param <T>
     * @return
     */
    @Override
    public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
        try {
            MappedStatement ms = configuration.getMappedStatement(statement);
            Cursor<T> cursor = executor.queryCursor(ms, wrapCollection(parameter), rowBounds);
            registerCursor(cursor);
            return cursor;
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 查询集合
     *
     * @param statement Unique identifier matching the statement to use.
     * @param <E>
     * @return
     */
    @Override
    public <E> List<E> selectList(String statement) {
        return this.selectList(statement, null);
    }

    /**
     * 查询集合
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param <E>
     * @return
     */
    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        return this.selectList(statement, parameter, RowBounds.DEFAULT);
    }

    /**
     * 查询集合
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param rowBounds Bounds to limit object retrieval
     * @param <E>
     * @return
     */
    @Override
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        try {
            MappedStatement ms = configuration.getMappedStatement(statement);
            return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 基于结果处理器查询
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param handler   ResultHandler that will handle each retrieved row
     */
    @Override
    public void select(String statement, Object parameter, ResultHandler handler) {
        select(statement, parameter, RowBounds.DEFAULT, handler);
    }

    /**
     * 基于结果处理器查询
     *
     * @param statement Unique identifier matching the statement to use.
     * @param handler   ResultHandler that will handle each retrieved row
     */
    @Override
    public void select(String statement, ResultHandler handler) {
        select(statement, null, RowBounds.DEFAULT, handler);
    }

    /**
     * 基于结果处理器查询
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter
     * @param rowBounds RowBound instance to limit the query results
     * @param handler   ResultHandler that will handle each retrieved row
     */
    @Override
    public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        try {
            MappedStatement ms = configuration.getMappedStatement(statement);
            executor.query(ms, wrapCollection(parameter), rowBounds, handler);
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 新增
     *
     * @param statement Unique identifier matching the statement to execute.
     * @return
     */
    @Override
    public int insert(String statement) {
        return insert(statement, null);
    }

    /**
     * 新增
     *
     * @param statement Unique identifier matching the statement to execute.
     * @param parameter A parameter object to pass to the statement.
     * @return
     */
    @Override
    public int insert(String statement, Object parameter) {
        return update(statement, parameter);
    }

    /**
     * 新增
     *
     * @param statement Unique identifier matching the statement to execute.
     * @return
     */
    @Override
    public int update(String statement) {
        return update(statement, null);
    }

    /**
     * 更新
     *
     * @param statement Unique identifier matching the statement to execute.
     * @param parameter A parameter object to pass to the statement.
     * @return
     */
    @Override
    public int update(String statement, Object parameter) {
        try {
            dirty = true;
            MappedStatement ms = configuration.getMappedStatement(statement);
            return executor.update(ms, wrapCollection(parameter));
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 删除
     *
     * @param statement Unique identifier matching the statement to execute.
     * @return
     */
    @Override
    public int delete(String statement) {
        return update(statement, null);
    }

    /**
     * 删除
     *
     * @param statement Unique identifier matching the statement to execute.
     * @param parameter A parameter object to pass to the statement.
     * @return
     */
    @Override
    public int delete(String statement, Object parameter) {
        return update(statement, parameter);
    }

    /**
     * 提交事务
     */
    @Override
    public void commit() {
        commit(false);
    }

    /**
     * 提交事务
     *
     * @param force forces connection commit
     */
    @Override
    public void commit(boolean force) {
        try {
            executor.commit(isCommitOrRollbackRequired(force));
            dirty = false;
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error committing transaction.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 回滚
     */
    @Override
    public void rollback() {
        rollback(false);
    }

    /**
     * 回滚
     *
     * @param force forces connection rollback
     */
    @Override
    public void rollback(boolean force) {
        try {
            executor.rollback(isCommitOrRollbackRequired(force));
            dirty = false;
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error rolling back transaction.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 刷新声明
     *
     * @return
     */
    @Override
    public List<BatchResult> flushStatements() {
        try {
            return executor.flushStatements();
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error flushing statements.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 关闭
     */
    @Override
    public void close() {
        try {
            executor.close(isCommitOrRollbackRequired(false));
            closeCursors();
            dirty = false;
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 关闭游标
     */
    private void closeCursors() {
        if (cursorList != null && cursorList.size() != 0) {
            for (Cursor<?> cursor : cursorList) {
                try {
                    cursor.close();
                } catch (IOException e) {
                    throw ExceptionFactory.wrapException("Error closing cursor.  Cause: " + e, e);
                }
            }
            cursorList.clear();
        }
    }

    /**
     * 获取全局配置
     *
     * @return
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * 获取给出type对应的mapper
     *
     * @param type Mapper interface class
     * @param <T>
     * @return
     */
    @Override
    public <T> T getMapper(Class<T> type) {
        return configuration.<T>getMapper(type, this);
    }

    /**
     * 获取连接
     *
     * @return
     */
    @Override
    public Connection getConnection() {
        try {
            return executor.getTransaction().getConnection();
        } catch (SQLException e) {
            throw ExceptionFactory.wrapException("Error getting a new connection.  Cause: " + e, e);
        }
    }

    /**
     * 清理缓存
     */
    @Override
    public void clearCache() {
        executor.clearLocalCache();
    }

    /**
     * 注册游标
     *
     * @param cursor
     * @param <T>
     */
    private <T> void registerCursor(Cursor<T> cursor) {
        if (cursorList == null) {
            cursorList = new ArrayList<Cursor<?>>();
        }
        cursorList.add(cursor);
    }

    /**
     * 是否必须提交或者回滚
     *
     * @param force
     * @return
     */
    private boolean isCommitOrRollbackRequired(boolean force) {
        return (!autoCommit && dirty) || force;
    }

    /**
     * 包装集合
     *
     * @param object
     * @return
     */
    private Object wrapCollection(final Object object) {
        if (object instanceof Collection) {
            StrictMap<Object> map = new StrictMap<Object>();
            map.put("collection", object);
            if (object instanceof List) {
                map.put("list", object);
            }
            return map;
        } else if (object != null && object.getClass().isArray()) {
            StrictMap<Object> map = new StrictMap<Object>();
            map.put("array", object);
            return map;
        }
        return object;
    }

    /**
     * 严格的map
     *
     * @param <V>
     */
    public static class StrictMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -5741767162221585340L;

        @Override
        public V get(Object key) {
            if (!super.containsKey(key)) {
                throw new BindingException(
                        "Parameter '" + key + "' not found. Available parameters are " + this.keySet());
            }
            return super.get(key);
        }

    }

}
