package com.shtao.test;

import org.springframework.context.ApplicationContext;

import com.ibatis.sqlmap.client.SqlMapClient;

public class ArticleDaoImplTest {

	public static void main(String[] args) throws Exception {
		ApplicationContext cxt = InitApplicationContext.getApplicationContext();
		SqlMapClient sqlMapClient = (SqlMapClient) cxt.getBean("sqlMapClient");
		System.out.print( sqlMapClient.getDataSource().getConnection().getMetaData().getDatabaseProductName() );
				
	}
}
