package com.shtao.ext.springframework.orm.ibatis;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.orm.ibatis.SqlMapClientCallback;
import org.springframework.orm.ibatis.SqlMapClientOperations;
import org.springframework.util.Assert;

import com.ibatis.common.logging.Log;
import com.ibatis.common.logging.LogFactory;
import com.ibatis.common.util.PaginatedList;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapException;
import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.ibatis.sqlmap.client.SqlMapSession;
import com.ibatis.sqlmap.client.event.RowHandler;
import com.ibatis.sqlmap.engine.impl.ExtendedSqlMapClient;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.statement.CachingStatement;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.shtao.ext.ibatis.common.util.PhysicalPaginatedList;
import com.shtao.ext.ibatis.sqlmap.engine.mapping.statement.PhysicalPaginatedCountStatement;
import com.shtao.ext.ibatis.sqlmap.engine.mapping.statement.PhysicalPaginatedListStatement;
import com.shtao.ext.ibatis.sqlmap.engine.mapping.statement.PhysicalPaginatedStatementUtil;
import com.shtao.ext.springframework.jdbc.datasource.lookup.DataSourceOperateType;
import com.shtao.ext.springframework.jdbc.datasource.lookup.DataSourceOperateTypeContextHolder;

public class ExtSqlMapClientTemplate extends JdbcAccessor implements SqlMapClientOperations {

	private static final Log log = LogFactory.getLog(ExtSqlMapClientTemplate.class);
	
	private SqlMapClient sqlMapClient;

	private boolean lazyLoadingAvailable = true;

	/**
	 * Create a new SqlMapClientTemplate.
	 */
	public ExtSqlMapClientTemplate() {
	}

	/**
	 * Create a new SqlMapTemplate.
	 * 
	 * @param sqlMapClient
	 *            iBATIS SqlMapClient that defines the mapped statements
	 */
	public ExtSqlMapClientTemplate(SqlMapClient sqlMapClient) {
		setSqlMapClient(sqlMapClient);
		afterPropertiesSet();
	}

	/**
	 * Create a new SqlMapTemplate.
	 * 
	 * @param dataSource
	 *            JDBC DataSource to obtain connections from
	 * @param sqlMapClient
	 *            iBATIS SqlMapClient that defines the mapped statements
	 */
	public ExtSqlMapClientTemplate(DataSource dataSource, SqlMapClient sqlMapClient) {
		setDataSource(dataSource);
		setSqlMapClient(sqlMapClient);
		afterPropertiesSet();
	}

	/**
	 * Set the iBATIS Database Layer SqlMapClient that defines the mapped
	 * statements.
	 */
	public void setSqlMapClient(SqlMapClient sqlMapClient) {
		this.sqlMapClient = sqlMapClient;
	}

	/**
	 * Return the iBATIS Database Layer SqlMapClient that this template works
	 * with.
	 */
	public SqlMapClient getSqlMapClient() {
		return this.sqlMapClient;
	}

	/**
	 * If no DataSource specified, use SqlMapClient's DataSource.
	 * 
	 * @see com.ibatis.sqlmap.client.SqlMapClient#getDataSource()
	 */
	public DataSource getDataSource() {
		DataSource ds = super.getDataSource();
		return (ds != null ? ds : this.sqlMapClient.getDataSource());
	}

	public void afterPropertiesSet() {
		if (this.sqlMapClient == null) {
			throw new IllegalArgumentException("Property 'sqlMapClient' is required");
		}
		if (this.sqlMapClient instanceof ExtendedSqlMapClient) {
			// Check whether iBATIS lazy loading is available, that is,
			// whether a DataSource was specified on the SqlMapClient itself.
			this.lazyLoadingAvailable = (((ExtendedSqlMapClient) this.sqlMapClient).getDelegate().getTxManager() != null);
		}
		super.afterPropertiesSet();
	}

	/**
	 * Execute the given data access action on a SqlMapExecutor.
	 * 
	 * @param action
	 *            callback object that specifies the data access action
	 * @return a result object returned by the action, or <code>null</code>
	 * @throws DataAccessException
	 *             in case of SQL Maps errors
	 */
	public Object execute(SqlMapClientCallback action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");
		Assert.notNull(this.sqlMapClient, "No SqlMapClient specified");

		// We always needs to use a SqlMapSession, as we need to pass a
		// Spring-managed
		// Connection (potentially transactional) in. This shouldn't be
		// necessary if
		// we run against a TransactionAwareDataSourceProxy underneath, but
		// unfortunately
		// we still need it to make iBATIS batch execution work properly: If
		// iBATIS
		// doesn't recognize an existing transaction, it automatically executes
		// the
		// batch for every single statement...

		SqlMapSession session = this.sqlMapClient.openSession();
		if (logger.isDebugEnabled()) {
			logger.debug("Opened SqlMapSession [" + session + "] for iBATIS operation");
		}
		Connection ibatisCon = null;

		try {
			Connection springCon = null;
			DataSource dataSource = getDataSource();
			boolean transactionAware = (dataSource instanceof TransactionAwareDataSourceProxy);

			// Obtain JDBC Connection to operate on...
			try {
				ibatisCon = session.getCurrentConnection();
				if (ibatisCon == null) {
					springCon = (transactionAware ? dataSource.getConnection() : DataSourceUtils.doGetConnection(dataSource));
					session.setUserConnection(springCon);
					if (logger.isDebugEnabled()) {
						logger.debug("Obtained JDBC Connection [" + springCon + "] for iBATIS operation");
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Reusing JDBC Connection [" + ibatisCon + "] for iBATIS operation");
					}
				}
			} catch (SQLException ex) {
				throw new CannotGetJdbcConnectionException("Could not get JDBC Connection", ex);
			}

			// Execute given callback...
			try {
				return action.doInSqlMapClient(session);
			} catch (SQLException ex) {
				throw getExceptionTranslator().translate("SqlMapClient operation", null, ex);
			} finally {
				try {
					if (springCon != null) {
						if (transactionAware) {
							springCon.close();
						} else {
							DataSourceUtils.doReleaseConnection(springCon, dataSource);
						}
					}
				} catch (Throwable ex) {
					logger.debug("Could not close JDBC Connection", ex);
				}
			}

			// Processing finished - potentially session still to be closed.
		} finally {
			// Only close SqlMapSession if we know we've actually opened it
			// at the present level.
			if (ibatisCon == null) {
				session.close();
			}
		}
	}

	/**
	 * Execute the given data access action on a SqlMapExecutor, expecting a
	 * List result.
	 * 
	 * @param action
	 *            callback object that specifies the data access action
	 * @return the List result
	 * @throws DataAccessException
	 *             in case of SQL Maps errors
	 */
	public List executeWithListResult(SqlMapClientCallback action) throws DataAccessException {
		return (List) execute(action);
	}

	/**
	 * Execute the given data access action on a SqlMapExecutor, expecting a Map
	 * result.
	 * 
	 * @param action
	 *            callback object that specifies the data access action
	 * @return the Map result
	 * @throws DataAccessException
	 *             in case of SQL Maps errors
	 */
	public Map executeWithMapResult(SqlMapClientCallback action) throws DataAccessException {
		return (Map) execute(action);
	}

	public Object queryForObject(String statementName) throws DataAccessException {
		return queryForObject(statementName, null);
	}

	// 主库查询
	public Object queryForObjectFromWriteDB(String statementName) throws DataAccessException {
		return queryForObjectFromWriteDB(statementName, null);
	}

	public Object queryForObject(final String statementName, final Object parameterObject) throws DataAccessException {
		//(只读数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Read);
		return execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForObject(statementName, parameterObject);
			}
		});
	}

	public Object queryForObjectFromWriteDB(final String statementName, final Object parameterObject) throws DataAccessException {
		// 操作写的数据库(主数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		return execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForObject(statementName, parameterObject);
			}
		});
	}

	public Object queryForObject(final String statementName, final Object parameterObject, final Object resultObject) throws DataAccessException {
		//(只读数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Read);
		return execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForObject(statementName, parameterObject, resultObject);
			}
		});
	}

	public Object queryForObjectFromWriteDB(final String statementName, final Object parameterObject, final Object resultObject) throws DataAccessException {
		// 操作写的数据库(主数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		return execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForObject(statementName, parameterObject, resultObject);
			}
		});
	}

	public List queryForList(String statementName) throws DataAccessException {
		return queryForList(statementName, null);
	}

	public List queryForListFromWriteDB(String statementName) throws DataAccessException {
		return queryForListFromWriteDB(statementName, null);
	}

	public List queryForList(final String statementName, final Object parameterObject) throws DataAccessException {
		//(只读数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Read);
		return executeWithListResult(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForList(statementName, parameterObject);
			}
		});
	}

	public List queryForListFromWriteDB(final String statementName, final Object parameterObject) throws DataAccessException {
		// 操作写的数据库(主数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		return executeWithListResult(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForList(statementName, parameterObject);
			}
		});
	}

	public List queryForList(String statementName, int skipResults, int maxResults) throws DataAccessException {
		return queryForList(statementName, null, skipResults, maxResults);
	}

	public List queryForList(final String statementName, final Object parameterObject, final int skipResults, final int maxResults) throws DataAccessException {
		//(只读数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Read);
		return executeWithListResult(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForList(statementName, parameterObject, skipResults, maxResults);
			}
		});
	}

	public void queryWithRowHandler(String statementName, RowHandler rowHandler) throws DataAccessException {
		queryWithRowHandler(statementName, null, rowHandler);
	}

	public void queryWithRowHandler(final String statementName, final Object parameterObject, final RowHandler rowHandler) throws DataAccessException {

		execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				executor.queryWithRowHandler(statementName, parameterObject, rowHandler);
				return null;
			}
		});
	}

	public Map queryForMap(final String statementName, final Object parameterObject, final String keyProperty) throws DataAccessException {
		//(只读数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Read);
		return executeWithMapResult(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForMap(statementName, parameterObject, keyProperty);
			}
		});
	}

	public Map queryForMap(final String statementName, final Object parameterObject, final String keyProperty, final String valueProperty) throws DataAccessException {
		//(只读数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Read);
		return executeWithMapResult(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForMap(statementName, parameterObject, keyProperty, valueProperty);
			}
		});
	}

	public Object insert(String statementName) throws DataAccessException {
		return insert(statementName, null);
	}

	public Object insert(final String statementName, final Object parameterObject) throws DataAccessException {
		// 操作写的数据库(主数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		return execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.insert(statementName, parameterObject);
			}
		});
	}

	public int update(String statementName) throws DataAccessException {
		return update(statementName, null);
	}

	public int update(final String statementName, final Object parameterObject) throws DataAccessException {
		// 操作写的数据库
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		Integer result = (Integer) execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return new Integer(executor.update(statementName, parameterObject));
			}
		});
		return result.intValue();
	}

	public void update(String statementName, Object parameterObject, int requiredRowsAffected) throws DataAccessException {
		// 操作写的数据库
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		int actualRowsAffected = update(statementName, parameterObject);
		if (actualRowsAffected != requiredRowsAffected) {
			throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(statementName, requiredRowsAffected, actualRowsAffected);
		}
	}

	public int delete(String statementName) throws DataAccessException {
		return delete(statementName, null);
	}

	public int delete(final String statementName, final Object parameterObject) throws DataAccessException {
		// 操作写的数据库
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		Integer result = (Integer) execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return new Integer(executor.delete(statementName, parameterObject));
			}
		});
		return result.intValue();
	}

	public void delete(String statementName, Object parameterObject, int requiredRowsAffected) throws DataAccessException {
		// 操作写的数据库
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		int actualRowsAffected = delete(statementName, parameterObject);
		if (actualRowsAffected != requiredRowsAffected) {
			throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(statementName, requiredRowsAffected, actualRowsAffected);
		}
	}

	/**
	 * 执行存储过程 写数据库
	 * 
	 * @param statementName
	 * @return
	 * @throws DataAccessException
	 */
	public Object procedureForObject(String statementName) throws DataAccessException {
		return procedureForObject(statementName, null);
	}

	/**
	 * 执行存储过程 写数据库
	 * 
	 * @param statementName
	 * @param parameterObject
	 * @return
	 * @throws DataAccessException
	 */
	public Object procedureForObject(final String statementName, final Object parameterObject) throws DataAccessException {
		// 操作写的数据库
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		return execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForObject(statementName, parameterObject);
			}
		});
	}

	/**
	 * 执行存储过程 写数据库
	 * 
	 * @param statementName
	 * @param parameterObject
	 * @param resultObject
	 * @return
	 * @throws DataAccessException
	 */
	public Object procedureForObject(final String statementName, final Object parameterObject, final Object resultObject) throws DataAccessException {
		// 操作写的数据库
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		return execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				return executor.queryForObject(statementName, parameterObject, resultObject);
			}
		});
	}

	// add by taotao 物理分页
	public PhysicalPaginatedList queryForPhysicalPaginatedList(String statementName, int pageNo, int pageSize, boolean allowCount) throws DataAccessException {
		return queryForPhysicalPaginatedList(statementName, null, pageNo, pageSize, allowCount);
	}

	public PhysicalPaginatedList queryForPhysicalPaginatedList(final String statementName, final Object parameterObject, final int pageNo, final int pageSize, final boolean allowCount) throws DataAccessException {
		//(只读数据库)
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Read);
		// Throw exception if lazy loading will not work.
		if (!this.lazyLoadingAvailable) {
			throw new InvalidDataAccessApiUsageException("SqlMapClient needs to have DataSource to allow for lazy loading" + " - specify SqlMapClientFactoryBean's 'dataSource' property");
		}
		
		SqlMapExecutorDelegate delegate = ((ExtendedSqlMapClient) sqlMapClient).getDelegate();		
		MappedStatement currentMappedStatement = delegate.getMappedStatement(statementName);

		//添加list
		String physicalPaginatedListStatementName = PhysicalPaginatedStatementUtil.getPhysicalPaginatedListStatementId(statementName,pageNo, pageSize);
		try {
			delegate.getMappedStatement(physicalPaginatedListStatementName);
		} catch (SqlMapException e) {			
			if( currentMappedStatement  instanceof CachingStatement){				
				delegate.addMappedStatement(new CachingStatement( new PhysicalPaginatedListStatement(((CachingStatement) currentMappedStatement).getStatement(),sqlMapClient,pageNo, pageSize),((CachingStatement) currentMappedStatement).getCacheModel()));
			}else{				
				delegate.addMappedStatement(new PhysicalPaginatedListStatement(currentMappedStatement,sqlMapClient,pageNo, pageSize));
			}			
		}
		
		// 添加count语句
		String physicalPaginatedCountStatementName = PhysicalPaginatedStatementUtil.getPhysicalPaginatedCountStatementId(statementName);
		try {
			delegate.getMappedStatement(physicalPaginatedCountStatementName);
		} catch (SqlMapException e) {			
			if( currentMappedStatement instanceof CachingStatement){				
				delegate.addMappedStatement(new CachingStatement( new PhysicalPaginatedCountStatement(((CachingStatement) currentMappedStatement).getStatement(),sqlMapClient),((CachingStatement) currentMappedStatement).getCacheModel()));
			}else{				
				delegate.addMappedStatement(new PhysicalPaginatedCountStatement(currentMappedStatement,sqlMapClient));
			}			
		}
		
		return (PhysicalPaginatedList) execute(new SqlMapClientCallback() {
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {				
				return executor.queryForPhysicalPaginatedList(statementName, parameterObject, pageNo, pageSize, allowCount);
			}
		});

	}

	// add by taotao
	public PhysicalPaginatedList queryForPhysicalPaginatedListFromWriteDB(String statementName, int pageNo, int pageSize, boolean allowCount) throws DataAccessException {
		return queryForPhysicalPaginatedListFromWriteDB(statementName, null, pageNo, pageSize, allowCount);
	}

	public PhysicalPaginatedList queryForPhysicalPaginatedListFromWriteDB(final String statementName, final Object parameterObject, final int pageNo, final int pageSize, final boolean allowCount) throws DataAccessException {
		// 操作写的数据库
		DataSourceOperateTypeContextHolder.setOperateType(DataSourceOperateType.Write);
		return queryForPhysicalPaginatedList(statementName, pageNo, pageSize, allowCount);
	}

	@Deprecated
	public PaginatedList queryForPaginatedList(String statementName, int pageSize) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Deprecated
	public PaginatedList queryForPaginatedList(String statementName, Object parameterObject, int pageSize) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

}
