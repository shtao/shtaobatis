package com.shtao.ext.ibatis.sqlmap.engine.mapping.statement;


public class PhysicalPaginatedStatementUtil {

	public static String getPhysicalPaginatedCountStatementId(String selectStatementId) {
		return "__" + selectStatementId + "_count__";
	}

	public static String getPhysicalPaginatedListStatementId(String selectStatementId,int pageIndex,int pageSize) {
		return "__" + selectStatementId + "_pageIndex_" + pageIndex +"_pageSize_" + pageSize + "__" ;
	}
}


