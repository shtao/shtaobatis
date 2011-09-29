package com.shtao.ext.ibatis.common.util;

import org.apache.log4j.Logger;

public class OraclePaginator implements Paginator {
	private static Logger log = Logger.getLogger(OraclePaginator.class);

	@Override
	public String getPaginatedListSql(String sql, int pageNo, int pageSize) {
		if (pageNo <= 0 || pageSize <= 0)
			return sql;
		int start = pageSize * (pageNo - 1);
		int end = pageSize * pageNo;
		/**
		 * Oracle的分页
		 */
		StringBuffer pageStr = new StringBuffer();
		pageStr.append("select * from ( select row_.*, rownum rownum_ from (");
		pageStr.append(sql);
		pageStr.append(" ) row_ where rownum <= ");
		pageStr.append(end);
		pageStr.append(" ) where rownum_ > ");
		pageStr.append(start);
		log.debug(pageStr);
		return pageStr.toString();
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
		log.debug(tempStr);
		return tempStr;
	}

}
