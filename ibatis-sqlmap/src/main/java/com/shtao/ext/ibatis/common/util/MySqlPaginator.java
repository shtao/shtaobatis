package com.shtao.ext.ibatis.common.util;

public class MySqlPaginator implements Paginator {

	@Override
	public String getPaginatedListSql(String sql, int pageNo, int pageSize) {
		if (pageNo <= 0 || pageSize <= 0)
			return sql;
		
		int start = pageSize * (pageNo - 1);
		/**
		 * mySQL的分页
		 */
		return (sql) + (" limit " + start + ", " + pageSize + " ");
	}

	@Override
	public String getPaginatedCountSql(String sql) {
		int fromIndex = sql.toLowerCase().indexOf("from");
		StringBuffer countStr = new StringBuffer("select count(*) ");
		int sql_orderby = sql.toLowerCase().indexOf("order by");
		if (sql_orderby > 0) {
			countStr.append(sql.substring(fromIndex, sql_orderby));
		} else {
			countStr.append(sql.substring(fromIndex));
		}
		String tempStr = countStr.toString();
		return tempStr;
	}
}
