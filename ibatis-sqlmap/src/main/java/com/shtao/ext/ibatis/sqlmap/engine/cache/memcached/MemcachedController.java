package com.shtao.ext.ibatis.sqlmap.engine.cache.memcached;

import java.util.Properties;

import com.danga.MemCached.MemCachedClient;
import com.danga.MemCached.SockIOPool;
import com.ibatis.sqlmap.engine.cache.CacheController;
import com.ibatis.sqlmap.engine.cache.CacheModel;

public class MemcachedController implements CacheController {

	private static MemCachedClient memCachedClient;

	@Override
	public void flush(CacheModel cacheModel) {
		memCachedClient.flushAll();
	}

	@Override
	public Object getObject(CacheModel cacheModel, Object key) {
		Object o = memCachedClient.get(key.toString());
		return o;
	}

	@Override
	public Object removeObject(CacheModel cacheModel, Object key) {
		Object o = memCachedClient.get(key.toString());
		memCachedClient.delete(key.toString());
		return o;
	}

	@Override
	public void putObject(CacheModel cacheModel, Object key, Object object) {
		memCachedClient.add(key.toString(), object);
	}

	@Override
	public void setProperties(Properties props) {		
		String memcachedDomain = "IBATIS_CACHED"; // memcached 域名
		String serverlist = props.getProperty("server-list");// "127.0.0.1:11211";
		if (serverlist != null) {
			serverlist = "127.0.0.1:11211";
		}
		int initConn = 5;
		int minConn = 5;
		String maxConnProp = props.getProperty("max-conn");
		int maxConn;
		if (maxConnProp != null) {
			maxConn = Integer.parseInt(maxConnProp);
		} else {
			maxConn = 50;
		}

		SockIOPool pool = SockIOPool.getInstance(memcachedDomain);
		pool.setServers(serverlist.split(","));
		if (!pool.isInitialized()) {
			pool.setInitConn(initConn);
			pool.setMinConn(minConn);
			pool.setMaxConn(maxConn);
			pool.setMaintSleep(30);
			pool.setNagle(false);
			pool.setSocketTO(60 * 60);
			pool.setSocketConnectTO(0);
			pool.setHashingAlg(SockIOPool.CONSISTENT_HASH);
			pool.initialize();
		}
		memCachedClient = new MemCachedClient(memcachedDomain);
		memCachedClient.setCompressEnable(false);
		memCachedClient.setCompressThreshold(0);
	}

}
