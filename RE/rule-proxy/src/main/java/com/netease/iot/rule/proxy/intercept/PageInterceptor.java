package com.netease.iot.rule.proxy.intercept;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;


/**
 * type 拦截的类型 四大对象之一( Executor,ResultSetHandler,ParameterHandler,StatementHandler)
 * method 拦截的方法
 * args 参数
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class PageInterceptor implements Interceptor {

    //每页显示的条目数
    private int pageSize;
    //当前现实的页数
    private int currPage;

    private String dbType;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //获取StatementHandler，默认是RoutingStatementHandler
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        //获取statementHandler包装类
        MetaObject metaObjectHandler = SystemMetaObject.forObject(statementHandler);

        //分离代理对象链
        while (metaObjectHandler.hasGetter("h")) {
            Object obj = metaObjectHandler.getValue("h");
            metaObjectHandler = SystemMetaObject.forObject(obj);
        }

        while (metaObjectHandler.hasGetter("target")) {
            Object obj = metaObjectHandler.getValue("target");
            metaObjectHandler = SystemMetaObject.forObject(obj);
        }

        //获取查询接口映射的相关信息
        MappedStatement mappedStatement = (MappedStatement) metaObjectHandler.getValue("delegate.mappedStatement");
        String mapId = mappedStatement.getId();

        if (mapId.matches(".+ByPage$")) {
            //获取进行数据库操作时管理参数的handler
            ParameterHandler parameterHandler = (ParameterHandler) metaObjectHandler.getValue("delegate.parameterHandler");
            //获取请求时的参数
            Map<String, Object> paraObject = (Map<String, Object>) parameterHandler.getParameterObject();
            //也可以这样获取
            //paraObject = (Map<String, Object>) statementHandler.getBoundSql().getParameterObject();

            currPage = (int) paraObject.get("currPage");
            pageSize = (int) paraObject.get("pageSize");

            String sql = (String) metaObjectHandler.getValue("delegate.boundSql.sql");

            //构建分页功能的sql语句
            String limitSql;
            sql = sql.trim();
            limitSql = sql + " limit " + (currPage - 1) * pageSize + "," + pageSize;

            //将构建完成的分页sql语句赋值个体'delegate.boundSql.sql'，偷天换日
            metaObjectHandler.setValue("delegate.boundSql.sql", limitSql);
        }

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties properties) {
        String limit1 = properties.getProperty("limit", "10");
        this.pageSize = Integer.valueOf(limit1);
        this.dbType = properties.getProperty("dbType", "mysql");
    }
}