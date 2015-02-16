package com.m1.sg.bcc.om.database.poller.configuration;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

import com.m1.sg.bcc.om.config.OMPollerConfigurationLoader;
import com.m1.sg.bcc.om.constant.OMPollerConstant;
import com.m1.sg.bcc.om.constant.SQLStatementPool;
import com.m1.sg.bcc.om.database.dao.OMJDBCDao;
import com.m1.sg.bcc.om.util.OMPollerDateTimeUtil;

/************************************************************************************************************************************
 *  
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.database.poller.configuration
 * Description: This class created for configure time interval medium priority poller spring integration framework 
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMMediumPriorityPollerTriggerConfiguration implements Trigger {

	/**
	 * 	For the Medium priority poller configuration
	 * 
	 * 	poller configuration retrieve all properties attribute define in the spring framework properties element
	 * 	for this class we use properties MEDIUM_PRIORITY_DEFAULT_TIME_INTERVAL for default value time interval 
	 */
	private Properties pollerPropertiesConfiguration = OMPollerConfigurationLoader.getPollerPropertiesConfiguration();
	
	/**
	 * 	define for poller type in the spring integration framework configuration
	 */
	private String pollerid;

	/**
	 * setter method for the set the priority
	 * 
	 * @param priority	poller priority
	 */
	public void setPollerid(String pollerid){
		this.pollerid = pollerid;
	}
	
	/** 
	 * Override this method from Trigger class in spring integration framework for configure time inteval 
	 * this method we provide logic for next execution time base on configuration in the database table 
	 * and default value
	 */
	@Override
	public Date nextExecutionTime(TriggerContext triggerContext) {
		// TODO Auto-generated method stub
		
		Date nextExecutionTime = null;
		Integer defaultValue = Integer.valueOf(pollerPropertiesConfiguration.getProperty(OMPollerConstant.PropertiesAttribute.MEDIUM_PRIORITY_DEFAULT_TIME_INTERVAL.getValue()));
		
		try {
		
			OMJDBCDao omConnection = new OMJDBCDao();
			JdbcTemplate omJdbcTemplete = omConnection.getOMJDBCTemplate();
			
			if(triggerContext.lastCompletionTime() == null){
				nextExecutionTime = OMPollerDateTimeUtil.changeDateTime(new Date(), defaultValue);
				Object[] paramsList = new Object[]{new Date(), this.pollerid};
				omJdbcTemplete.update(SQLStatementPool.SQL_UPDATE_HEARTBEAT_TIME, paramsList, OMJDBCDao.jdbcTypeMapping(paramsList));
			} else {
				List<Map<String, Object>> resultQuery = omJdbcTemplete.queryForList(SQLStatementPool.SQL_SELECT_POLLER_CONFIG, new Object[]{this.pollerid});
				if(resultQuery.size() > 0 && resultQuery.get(0) != null){
					BigDecimal pollingTimeInterval = (BigDecimal) resultQuery.get(0).get(OMPollerConstant.PollerConfigurationSPLColumnName.TIME_INT.getValue());
					Integer pollingTimeIntervalInt = pollingTimeInterval.intValue();
					nextExecutionTime = OMPollerDateTimeUtil.changeDateTime(triggerContext.lastCompletionTime(), pollingTimeIntervalInt);
				} else {
					nextExecutionTime = OMPollerDateTimeUtil.changeDateTime(triggerContext.lastCompletionTime(), defaultValue);
					Object[] paramsList = new Object[]{triggerContext.lastActualExecutionTime(), this.pollerid};
					omJdbcTemplete.update(SQLStatementPool.SQL_UPDATE_HEARTBEAT_TIME, paramsList, OMJDBCDao.jdbcTypeMapping(paramsList));
				}
			}
			
			return nextExecutionTime;
		} catch (Exception e){
			
			e.printStackTrace();
			nextExecutionTime = OMPollerDateTimeUtil.changeDateTime(triggerContext.lastCompletionTime(), defaultValue);
			return nextExecutionTime;
		}
	}

}
