package com.shtao.test;

import java.io.IOException;
import java.io.InputStream;

import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.engine.builder.xml.SqlMapConfigParser;
import com.ibatis.sqlmap.engine.impl.ExtendedSqlMapClient;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;

public class TestSqlMapConfigParser {

	public static void main(String[] args) throws Exception {
		try {
			InputStream inputStream = Resources.getResourceAsStream("com/shtao/test/SQLMAPCONFIG.XML");
			SqlMapConfigParser parser = new SqlMapConfigParser();
			SqlMapClient client = parser.parse(inputStream);
			inputStream.close();
			System.out.print( client.getDataSource().getConnection().getMetaData().getDatabaseProductName() );
			SqlMapExecutorDelegate delegate = ((ExtendedSqlMapClient) client).getDelegate();
			MappedStatement ms = delegate.getMappedStatement("blog.SAVEBLOG");
			System.out.print(ms.getResource());
			
		} catch (IOException e) {
			throw new RuntimeException("Something bad happened while building the SqlMapClient instance." + e, e);
		}
	}
}
