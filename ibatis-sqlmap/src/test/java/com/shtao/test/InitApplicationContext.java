package com.shtao.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class InitApplicationContext {
	private static ApplicationContext context = null;

	private InitApplicationContext() {

	}

	public static ApplicationContext getApplicationContext() {
		
		if (context == null) {
			String springConfigPath = "classpath*:com/shtao/test/applicationContext.xml";
			String[] locations = springConfigPath.split(",");
			context = new ClassPathXmlApplicationContext(locations);
		}
		return context;
	}
}
