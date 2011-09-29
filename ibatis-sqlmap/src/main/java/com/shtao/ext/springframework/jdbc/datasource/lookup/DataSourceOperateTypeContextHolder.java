package com.shtao.ext.springframework.jdbc.datasource.lookup;

public class DataSourceOperateTypeContextHolder {
	private static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();

	public static void setOperateType(String operateType) {
		contextHolder.set(operateType);
	}

	public static String getOperateType() {
		return (String) contextHolder.get();
	}

	public static void clearOperateType() {
		contextHolder.remove();
	}
}
