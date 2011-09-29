package com.shtao.ext.ibatis.sqlmap.engine.mapping.statement;

import java.sql.Connection;
import java.sql.SQLException;

import com.ibatis.common.jdbc.exception.NestedSQLException;
import com.ibatis.common.logging.Log;
import com.ibatis.common.logging.LogFactory;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.event.RowHandler;
import com.ibatis.sqlmap.engine.execution.SqlExecutor;
import com.ibatis.sqlmap.engine.impl.ExtendedSqlMapClient;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.result.AutoResultMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.sql.Sql;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.ibatis.sqlmap.engine.mapping.statement.RowHandlerCallback;
import com.ibatis.sqlmap.engine.mapping.statement.SelectStatement;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.shtao.ext.ibatis.common.util.DatabaseDialectMapping;
import com.shtao.ext.ibatis.common.util.DatabaseDialectMapping;
import com.shtao.ext.ibatis.common.util.Paginator;

public class PhysicalPaginatedCountStatement extends MappedStatement {

	private static final Log log = LogFactory.getLog(PhysicalPaginatedListStatement.class);
	
	private MappedStatement statement;

	public PhysicalPaginatedCountStatement(MappedStatement statement, SqlMapClient sqlMapClient) {
		// super();
		this.statement = statement;
		setId(PhysicalPaginatedStatementUtil.getPhysicalPaginatedCountStatementId(statement.getId()));
		setResultSetType(statement.getResultSetType());
		setFetchSize(1);
		setParameterMap(statement.getParameterMap());
		setParameterClass(statement.getParameterClass());
		setSql(statement.getSql());
		setResource(statement.getResource());
		setTimeout(statement.getTimeout());
		setSqlMapClient(sqlMapClient);


		
	}

	// taotao add 物理分页，总行数
	protected void executeQueryWithCallback(StatementScope statementScope, Connection conn, Object parameterObject, Object resultObject, RowHandler rowHandler, int skipResults, int maxResults) throws SQLException {
		log.debug("PhysicalPaginatedCountStatement.executeQueryWithCallback----->begin");
		ErrorContext errorContext = statementScope.getErrorContext();
		errorContext.setActivity("preparing the mapped statement for execution");
		errorContext.setObjectId(this.getId());
		errorContext.setResource(this.getResource());

		try {
			parameterObject = validateParameter(parameterObject);

			Sql sql = getSql();

			errorContext.setMoreInfo("Check the parameter map.");
			ParameterMap parameterMap = sql.getParameterMap(statementScope, parameterObject);

			errorContext.setMoreInfo("Check the result map.");
			//ResultMap resultMap =getResultMap();
			ResultMap resultMap = new AutoResultMap(((ExtendedSqlMapClient) getSqlMapClient()).getDelegate(), false);
			resultMap.setId(getId() + "-AutoResultMap");
			resultMap.setResultClass(Integer.class);
			resultMap.setResource(getResource());
			//setResultMap(resultMap);
			
			statementScope.setResultMap(resultMap);
			statementScope.setParameterMap(parameterMap);

			errorContext.setMoreInfo("Check the parameter map.");
			Object[] parameters = parameterMap.getParameterObjectValues(statementScope, parameterObject);

			errorContext.setMoreInfo("Check the SQL statement.");
			String sqlString = sql.getSql(statementScope, parameterObject);
			// taotao modify
			// 增加 count sql
			// 得到数据库类型：
			String dbName = conn.getMetaData().getDatabaseProductName();
			Paginator paginator = DatabaseDialectMapping.getPaginator(dbName);
			String countSqlString = paginator.getPaginatedCountSql(sqlString);

			errorContext.setActivity("executing mapped statement");
			errorContext.setMoreInfo("Check the SQL statement or the result map.");
			// Object resultObject = new Integer(0);
			// error;
			RowHandlerCallback callback = new RowHandlerCallback(resultMap, null, rowHandler);
			// 执行count sql

			sqlExecuteQuery(statementScope, conn, countSqlString, parameters, SqlExecutor.NO_SKIPPED_RESULTS, SqlExecutor.NO_MAXIMUM_RESULTS, callback);

			errorContext.setMoreInfo("Check the output parameters.");
			if (parameterObject != null) {
				postProcessParameterObject(statementScope, parameterObject, parameters);
			}

			errorContext.reset();
			sql.cleanup(statementScope);

			notifyListeners();
		} catch (SQLException e) {
			errorContext.setCause(e);
			throw new NestedSQLException(errorContext.toString(), e.getSQLState(), e.getErrorCode(), e);
		} catch (Exception e) {
			errorContext.setCause(e);
			throw new NestedSQLException(errorContext.toString(), e);
		}
	}

}
