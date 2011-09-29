package com.shtao.ext.ibatis.common.util;

import org.apache.log4j.Logger;

public class SqlserverPaginator implements Paginator {
	private static Logger log = Logger.getLogger(SqlserverPaginator.class);

	/**
	 * ms Sql 分页代码 SELECT * FROM ( SELECT TOP( pageSize ) * FROM ( SELECT TOP (
	 * pageSize * pageNo ) col1,col2 FROM Articles ORDER BY PubTime DESC ) ORDER
	 * BY PubTime ASC ) ORDER BY PubTime DESC 这个方法最后一页的时候会有问题，提出来的数据是最后topN的数据行，
	 * 而不是剩余的数据行， 可以考虑和count一起使用 不过问题不大
	 */
	@Override
	public String getPaginatedListSql(String sql, int pageNo, int pageSize) {
		if (pageNo <= 0 || pageSize <= 0)
			return sql;
		int selectBeginIndex = sql.toLowerCase().indexOf("select ");
		int orderByBeginIndex = sql.toLowerCase().indexOf("order by");
		String orderByString = "ORDER BY CURRENT_TIMESTAMP asc";
		if (orderByBeginIndex >= 0) {
			orderByString = sql.substring(orderByBeginIndex);
		}
		String orderByStringD = "";
		if (orderByString.toLowerCase().indexOf("desc") > 0) {
			orderByStringD = orderByString.toLowerCase().replaceAll("desc", "asc");
		} else {
			orderByStringD = orderByString.toLowerCase().replaceAll("asc", "desc");
		}
		StringBuffer pageStr = new StringBuffer();
		pageStr.append("SELECT * FROM ( SELECT TOP ");
		pageStr.append(pageSize);
		pageStr.append(" * FROM ( SELECT TOP ");
		pageStr.append(pageSize * pageNo);
		pageStr.append(" ");
		if (orderByBeginIndex >= 0) {
			pageStr.append(sql.substring(selectBeginIndex + 7, orderByBeginIndex));
		} else {
			pageStr.append(sql.substring(selectBeginIndex + 7));
		}
		pageStr.append(" ");
		pageStr.append(orderByString);
		pageStr.append(") ta ");
		pageStr.append(orderByStringD);
		pageStr.append(") tb ");
		pageStr.append(orderByString);
		log.debug(pageStr.toString());
		return pageStr.toString();
	}

	@Override
	public String getPaginatedCountSql(String sql) {
		int fromIndex = sql.toLowerCase().indexOf("from");
		StringBuffer countStr = new StringBuffer("select count(*) ");
		int sql_orderby = sql.toLowerCase().indexOf("order by");
		if (sql_orderby >= 0) {
			countStr.append(sql.substring(fromIndex, sql_orderby));
		} else {
			countStr.append(sql.substring(fromIndex));
		}
		String tempStr = countStr.toString();
		log.debug(tempStr);
		return tempStr;
	}
}
