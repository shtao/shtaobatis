package com.shtao.ext.ibatis.common.util;


public class DatabaseDialectMapping {
	
	public static String getMappingName(String productName) {
		if (productName.toLowerCase().indexOf("mysql") != -1) {
			return "mysql";
		}
		if (productName.toLowerCase().indexOf("sqlserver") != -1) {
			return "sqlserver";
		}
		if (productName.toLowerCase().indexOf("sql server") != -1) {
			return "sqlserver";
		}
		if (productName.toLowerCase().indexOf("oracle") != -1) {
			return "oracle";
		}
		if (productName.toLowerCase().indexOf("db2") != -1) {
			return "db2";
		}
		return "";
	}

	public static Paginator getPaginator(String productName) {
		if (productName.toLowerCase().indexOf("mysql") != -1) {
			return new MySqlPaginator();
		}
		if (productName.toLowerCase().indexOf("sqlserver") != -1) {
			return new SqlserverPaginator();
		}
		if (productName.toLowerCase().indexOf("sql server") != -1) {
			return new SqlserverPaginator();
		}
		if (productName.toLowerCase().indexOf("oracle") != -1) {
			return new OraclePaginator();
		}
		return new OraclePaginator();
	}
	
	public static String getDialectResource(String resource, String productName) {
		String dialectMappingName = getMappingName(productName);
		StringBuffer buf = new StringBuffer();
		int idx = resource.lastIndexOf('.');
		if (idx != -1) {
			buf.append(resource.substring(0, idx));
		}
		if (dialectMappingName != null && !dialectMappingName.equals("")) {
			buf.append("-").append(dialectMappingName);
		}
		buf.append(resource.substring(idx));
		return buf.toString();
	}
	
	public static String getDialectUrl(String url, String productName) {
		String dialectMappingName = getMappingName(productName);
		StringBuffer buf = new StringBuffer();
		buf.append(url);
		if (dialectMappingName != null && !dialectMappingName.equals("")) {
			int idx = url.indexOf('?');
			if (idx == -1) {
				buf.append('?');
			} else {
				buf.append('&');
			}
			buf.append("sql_dialect=").append(dialectMappingName);
		}
		return buf.toString();
	}
}
