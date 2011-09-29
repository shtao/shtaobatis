package com.shtao.ext.ibatis.common.util;

public interface Paginator {
	/**
	 * 根据原始的sql和pageNo, pageSize生成分页的sql.
	 * @param sql
	 * @param pageNo  当前页号
	 * @param pageSize  每页的数据条数
	 * @return 分页sql
	 */
	public String getPaginatedListSql(String sql, int pageNo, int pageSize);
	
	public String getPaginatedCountSql(String sql);
}
