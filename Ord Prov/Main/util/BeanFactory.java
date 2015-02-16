package com.m1.bcc.spl.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 19/11/2012					Ravikumar G				Created
 ******************************************************************************/

public class BeanFactory implements ApplicationContextAware {
	//private static org.springframework.beans.factory.BeanFactory beanFactorys;
	private static ApplicationContext applicationContext;

/*	public static org.springframework.beans.factory.BeanFactory getInstance() {
		beanFactory =new ClassPathXmlApplicationContext("applicationcontext-datasource.xml");
		return beanFactory;
	}
*/
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
		// TODO Auto-generated method stub
	}

	/** getBean - used to get the bean instance from context
	 *
	 * @param beanName
	 * @return
	 */
	public static Object getBean(String beanName) {
		return applicationContext.getBean(beanName);
	}

}
