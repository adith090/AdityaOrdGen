package com.m1.sg.bcc.om.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/************************************************************************************************************************************
 *  
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.util
 * Description: This utility class created for get beans class from spring framework properties
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMBeanFactory implements ApplicationContextAware {

	private static ApplicationContext appContext;
	
	/**
	 * setter method for application context
	 */
	@Override
	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		// TODO Auto-generated method stub
		appContext = context;
	}
	
	/**
	 * getter method for get beans name from the id
	 * 
	 * @param beanName	bean id
	 * @return			bean object
	 */
	public static Object getBean(String beanName){
		return appContext.getBean(beanName);
	}
	

}
