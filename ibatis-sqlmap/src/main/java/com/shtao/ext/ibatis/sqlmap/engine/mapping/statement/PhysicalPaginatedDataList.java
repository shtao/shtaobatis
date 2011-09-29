package com.shtao.ext.ibatis.sqlmap.engine.mapping.statement;

import java.sql.SQLException;
import java.util.List;

import com.ibatis.common.logging.Log;
import com.ibatis.common.logging.LogFactory;
import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.shtao.ext.ibatis.common.util.PhysicalPaginatedList;

public class PhysicalPaginatedDataList<E> implements PhysicalPaginatedList<Object> {

	private static final Log log = LogFactory.getLog(PhysicalPaginatedDataList.class);

	private SqlMapExecutor sqlMapExecutor;
	private String statementName;
	private Object parameterObject;

	private int pageSize;
	private int pageIndex;
	private int pageCount;
	private int totalCount;

	private List<E> pageList;


	public PhysicalPaginatedDataList(SqlMapExecutor sqlMapExecutor, String statementName, Object parameterObject, int pageIndex, int pageSize, boolean allowCount) throws SQLException {
		this.sqlMapExecutor = sqlMapExecutor;
		
		this.statementName = statementName;
		this.parameterObject = parameterObject;
		this.pageSize = pageSize;
		this.pageIndex = pageIndex;
		if (allowCount) {
			this.totalCount = getCount();
			if (this.totalCount % this.pageSize == 0) {
				this.pageCount = totalCount / pageSize;
			} else {
				this.pageCount = (totalCount / pageSize) + 1;
			}
		}
		this.pageList = getList(pageIndex, pageSize);
		
	}

	private List<E> getList(int pageIndex, int pageSize) throws SQLException {
		log.debug("PhysicalPaginatedDataList.getList---begin");		
		String physicalPaginatedListStatementName = PhysicalPaginatedStatementUtil.getPhysicalPaginatedListStatementId(statementName,pageIndex, pageSize);
		return sqlMapExecutor.queryForList(physicalPaginatedListStatementName, parameterObject, pageIndex, pageSize);
	}

	private int getCount() throws SQLException {
		log.debug("PhysicalPaginatedDataList.getCount---begin");
		String physicalPaginatedCountStatementName = PhysicalPaginatedStatementUtil.getPhysicalPaginatedCountStatementId(statementName);
		log.debug("physicalPaginatedCountStatementName===" + physicalPaginatedCountStatementName);
		return (Integer) sqlMapExecutor.queryForObject(physicalPaginatedCountStatementName, parameterObject);
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}	

	@Override
	public int getPageCount() {
		return pageCount;
	}

	@Override
	public int getPageIndex() {
		return pageIndex;
	}

	@Override
	public int getNextPageIndex() {
		return pageIndex + 1;
	}

	@Override
	public int getPreviousPageIndex() {
		return pageIndex - 1;
	}

	@Override
	public boolean isFirstPage() {
		return pageIndex == 1;
	}

	@Override
	public boolean isLastPage() {
		return pageIndex == this.pageCount;
	}

	@Override
	public int getTotalCount() {
		return totalCount;
	}


	@Override
	public List getPageList() {
		return pageList;
	}

}
