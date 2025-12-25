package com.jnet.api.interceptor;/*
package com.jnet.api.interceptor;

import cn.staitech.common.core.annotation.dict.DictCache;
import cn.staitech.common.security.utils.SecurityUtils;
import cn.staitech.system.constant.SysConstant;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.parser.SqlInfo;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.DialectFactory;
import com.baomidou.mybatisplus.extension.plugins.pagination.DialectModel;
import com.baomidou.mybatisplus.extension.toolkit.JdbcUtils;
import com.baomidou.mybatisplus.extension.toolkit.SqlParserUtils;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.Connection;
import java.text.DateFormat;
import java.util.*;

@Slf4j
@Accessors(chain = true)
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})

public class ExportPlugin extends PaginationInnerInterceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // SQL 解析
        this.sqlParser(metaObject);

        // 先判断是不是SELECT操作  (2019-04-10 00:37:31 跳过存储过程)
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        if (SqlCommandType.SELECT != mappedStatement.getSqlCommandType()
                || StatementType.CALLABLE == mappedStatement.getStatementType()) {
            return invocation.proceed();
        }

        // 针对定义了rowBounds，做为mapper接口方法的参数
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        Object paramObj = boundSql.getParameterObject();

        // 判断参数里是否有page对象
        IPage<?> page = null;
        if (paramObj instanceof IPage) {
            page = (IPage<?>) paramObj;
        } else if (paramObj instanceof Map) {
            for (Object arg : ((Map<?, ?>) paramObj).values()) {
                if (arg instanceof IPage) {
                    page = (IPage<?>) arg;
                    break;
                }
            }
        }

         * 不需要分页的场合，如果 size 小于 0 返回结果集


        if (null == page || page.getSize() < 0) {
            return invocation.proceed();
        }

        if (this.limit > 0 && this.limit <= page.getSize()) {
            //处理单页条数限制
            handlerLimit(page);
        }

        String originalSql = boundSql.getSql();
        Connection connection = (Connection) invocation.getArgs()[0];
        DbType dbType = StringUtils.isNotBlank(dialectType) ? DbType.getDbType(dialectType)
                : JdbcUtils.getDbType(connection.getMetaData().getURL());

        if (page.isSearchCount()) {
            SqlInfo sqlInfo = SqlParserUtils.getOptimizeCountSql(page.optimizeCountSql(), countSqlParser, originalSql);
            this.queryTotal(sqlInfo.getSql(), mappedStatement, boundSql, page, connection);
            if (page.getTotal() <= 0) {
                return null;
            }
        }
        Configuration configuration = mappedStatement.getConfiguration();
        String exportSql = showSql(configuration, boundSql);
        Map<String,String> exportSqlMap = DictCache.getDictByType( SysConstant.EXPORT_SQL);
        if (exportSqlMap==null){
            exportSqlMap = new HashMap<>();
        }
        //获取返回类型
        ResultMap resultMap = mappedStatement.getResultMaps().get(0);
        Class<?> clazzResultType = resultMap.getType();
        exportSqlMap.put(SecurityUtils.getToken(), exportSql);
        exportSqlMap.put(SecurityUtils.getToken()+SysConstant.EXPORT_SQL_SUFFIX, clazzResultType.getName());
        DictCache.load(SysConstant.EXPORT_SQL,exportSqlMap);
        if (log.isDebugEnabled()) {
            log.debug("ExportPlugin: export sql is: {}", exportSql);
        }
        String buildSql = concatOrderBy(originalSql, page);
        DialectModel model = DialectFactory.buildPaginationSql(page, buildSql, dbType, dialectClazz);
        List<ParameterMapping> mappings = new ArrayList<>(boundSql.getParameterMappings());
        Map<String, Object> additionalParameters = (Map<String, Object>) metaObject.getValue("delegate.boundSql.additionalParameters");
        model.consumers(mappings, configuration, additionalParameters);
        metaObject.setValue("delegate.boundSql.sql", model.getDialectSql());
        metaObject.setValue("delegate.boundSql.parameterMappings", mappings);
        return invocation.proceed();
    }

*
     * 获取参数值
     * @param obj
     * @return


    private static String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "to_date('" + formatter.format(obj) + "','yyyy-MM-dd hh24:mi:ss')";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "";
            }

        }
        //System.err.println("获取值: "+value);
        return value;
    }


**
     * sql 参数替换
     * @param configuration
     * @param boundSql
     * @return


    public static String showSql(Configuration configuration, BoundSql boundSql) {
        //获取参数对象
        Object parameterObject = boundSql.getParameterObject();
        //获取当前的sql语句有绑定的所有parameterMapping属性
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        //去除空格
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (parameterMappings.size() > 0 && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
 如果参数满足：org.apache.ibatis.type.TypeHandlerRegistry#hasTypeHandler(java.lang.Class<?>)
            org.apache.ibatis.type.TypeHandlerRegistry#TYPE_HANDLER_MAP
            * 即是不是属于注册类型(TYPE_HANDLER_MAP...等/有没有相应的类型处理器)
             *

            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));
            } else {
                //装饰器，可直接操作属性值 ---》 以parameterObject创建装饰器
                //MetaObject 是 Mybatis 反射工具类，通过 MetaObject 获取和设置对象的属性值
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                //循环 parameterMappings 所有属性
                for (ParameterMapping parameterMapping : parameterMappings) {
                    //获取property属性
                    String propertyName = parameterMapping.getProperty();
                    //System.err.println("propertyName: "+propertyName);
                    //是否声明了propertyName的属性和get方法
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        //判断是不是sql的附加参数
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    }
                }
            }
        }
        return sql;
    }


}
*/
