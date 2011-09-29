package com.shtao.ext.ibatis.sqlmap.engine.mapping.statement;

import java.sql.Connection;
import java.sql.SQLException;

import com.ibatis.common.jdbc.exception.NestedSQLException;
import com.ibatis.common.logging.Log;
import com.ibatis.common.logging.LogFactory;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.event.RowHandler;
import com.ibatis.sqlmap.engine.execution.SqlExecutor;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.sql.Sql;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.ibatis.sqlmap.engine.mapping.statement.RowHandlerCallback;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.shtao.ext.ibatis.common.util.DatabaseDialectMapping;
import com.shtao.ext.ibatis.common.util.Paginator;

public class PhysicalPaginatedListStatement extends MappedStatement {

	private static final Log log = LogFactory.getLog(PhysicalPaginatedListStatement.class);
	
	private int pageIndex;
	private int pageSize;
	private MappedStatement statement;

	public PhysicalPaginatedListStatement(MappedStatement statement,SqlMapClient sqlMapClient, int pageIndex, int pageSize) {
		//super();
		this.statement = statement;
		this.pageIndex = pageIndex;
		this.pageSize = pageSize;

		setId(PhysicalPaginatedStatementUtil.getPhysicalPaginatedListStatementId(statement.getId(), pageIndex, pageSize));
		setResultSetType(statement.getResultSetType());
		setFetchSize(1);
		setParameterMap(statement.getParameterMap());
		setParameterClass(statement.getParameterClass());
		setSql(statement.getSql());
		setResource(statement.getResource());
		setTimeout(statement.getTimeout());		
		setResultMap(statement.getResultMap());	
		setSqlMapClient(sqlMapClient);
	}

	// taotao add 物理分页
	protected void executeQueryWithCallback(StatementScope statementScope, Connection conn, Object parameterObject, Object resultObject, RowHandler rowHandler, int pageIndex, int pageSize) throws SQLException {
		log.debug("PhysicalPaginatedListStatement.executeQueryWithCallback------>begin");
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
			ResultMap resultMap = sql.getResultMap(statementScope, parameterObject);

			statementScope.setResultMap(resultMap);
			statementScope.setParameterMap(parameterMap);

			errorContext.setMoreInfo("Check the parameter map.");
			Object[] parameters = parameterMap.getParameterObjectValues(statementScope, parameterObject);

			errorContext.setMoreInfo("Check the SQL statement.");
			String sqlString = sql.getSql(statementScope, parameterObject);
			// taotao modify
			// 增加分页sql
			String dbName = conn.getMetaData().getDatabaseProductName();
			Paginator paginator = DatabaseDialectMapping.getPaginator(dbName);
			String pgSqlString = paginator.getPaginatedListSql(sqlString, pageIndex, pageSize);

			errorContext.setActivity("executing mapped statement");
			errorContext.setMoreInfo("Check the SQL statement or the result map.");
			log.debug("PhysicalPaginatedListStatement.executeQueryWithCallback------>RowHandlerCallback-->begin");
			RowHandlerCallback callback = new RowHandlerCallback(resultMap, resultObject, rowHandler);
			log.debug("PhysicalPaginatedListStatement.executeQueryWithCallback------>RowHandlerCallback-->end");
			// 执行分页sql
			log.debug("PhysicalPaginatedListStatement.executeQueryWithCallback------>sqlExecuteQuery-->begin");
			sqlExecuteQuery(statementScope, conn, pgSqlString, parameters, SqlExecutor.NO_SKIPPED_RESULTS, SqlExecutor.NO_MAXIMUM_RESULTS, callback);
			log.debug("PhysicalPaginatedListStatement.executeQueryWithCallback------>sqlExecuteQuery-->end");

			errorContext.setMoreInfo("Check the output parameters.");
			if (parameterObject != null) {
				postProcessParameterObject(statementScope, parameterObject, parameters);
			}

			errorContext.reset();
			sql.cleanup(statementScope);
			log.debug("PhysicalPaginatedListStatement.executeQueryWithCallback------>notifyListeners-->begin");
			notifyListeners();
			log.debug("PhysicalPaginatedListStatement.executeQueryWithCallback------>notifyListeners-->end");
		} catch (SQLException e) {
			errorContext.setCause(e);
			throw new NestedSQLException(errorContext.toString(), e.getSQLState(), e.getErrorCode(), e);
		} catch (Exception e) {
			errorContext.setCause(e);
			throw new NestedSQLException(errorContext.toString(), e);
		}
	}

}
