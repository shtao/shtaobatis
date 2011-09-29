对ibatis-2.3.4.726的改进

1. 物理分页功能，支持cache


2. 读写分离
<bean id="datumDataSource" class="com.shtao.ext.springframework.jdbc.datasource.lookup.DynamicDataSource">
		<property name="targetDataSources">
			<map>
				<entry key="read" value-ref="datum${datum.datasource.type}ReadDataSource" />
				<entry key="write" value-ref="datum${datum.datasource.type}WriteDataSource" />
			</map>
		</property>
		<property name="defaultTargetDataSource" ref="datum${datum.datasource.type}ReadDataSource" />
	</bean>
	
3. 添加数据库方言，透明支持多种数据库
Article.xml
Article-sqlserver.xml
Article-oracle.xml
Article-mysql.xml
Article-db2.xml

sqlMapConfig 文件的头部这样写，因为改过  sql-map-config-2.dtd

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE sqlMapConfig PUBLIC "-//ibatis.apache.org//DTD SQL Map Config 2.0//EN"
 "http://www.csc108.com/dtd/sql-map-config-2.dtd">
<sqlMapConfig>
  <settings cacheModelsEnabled="false" enhancementEnabled="true" 
  useStatementNamespaces="true" lazyLoadingEnabled="true" 
  databaseProductName="sqlserver"
  />   
   <sqlMap resource="com/shtao/test/SQLMAP.XML" />
 </sqlMapConfig>