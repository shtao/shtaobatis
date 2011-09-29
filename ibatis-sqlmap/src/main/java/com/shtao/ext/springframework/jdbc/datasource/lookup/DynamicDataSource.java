package com.shtao.ext.springframework.jdbc.datasource.lookup;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {

	protected Object determineCurrentLookupKey() {
		return DataSourceOperateTypeContextHolder.getOperateType();
	}

}
