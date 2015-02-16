package com.m1.bcc.spl.orderdbpoller;

import java.util.Date;
import common.util.TALogger;
import java.util.concurrent.TimeUnit;
import org.springframework.util.Assert;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.util.SPLCommonComponent;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 ******************************************************************************/

public class DBPollerPeriodicTrigger implements Trigger {

	private long period;
	private String pollerName;
	private final TimeUnit timeUnit;
	private volatile long initialDelay = 0;
	private volatile boolean fixedRate = false;

	//For logging purposes
	String inputIdentifier = "";
	String loggercategory = "dbpollerlogging";
	TALogger taLogger = TALogger.getTALogger();

	JdbcDatabaseDAO jdbcDatabaseDAO;

	public void setJdbcDatabaseDAO(JdbcDatabaseDAO jdbcDatabaseDAO) {
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}


	public DBPollerPeriodicTrigger(long period) {
		this(period, null);
	}

	public DBPollerPeriodicTrigger(long period, TimeUnit timeUnit) {
		Assert.isTrue(period >= 0, "period must not be negative");
		this.timeUnit = (timeUnit != null) ? timeUnit : TimeUnit.MILLISECONDS;
		this.period = this.timeUnit.toMillis(period);
	}


	public void setPollerName(String pollerName) {

		this.pollerName=pollerName;
		inputIdentifier=pollerName;
		}

	public void setPeriod(String pollerName) {

		try {

			inputIdentifier=pollerName;
	    	taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG, "Current Scheduling polling time of "+pollerName+" "+SPLCommonComponent.formatCurrentDateToDataTime(), loggercategory);
	    	this.period=jdbcDatabaseDAO.getTimePeriodOfDBPoller(pollerName);
	    	}

		catch (Exception e)
		{
			taLogger.log(inputIdentifier, ApplicationConstants.LOG_ERROR,"Error Inside setPeriod Method",loggercategory, e);
		}

	}


	public void setInitialDelay(long initialDelay) {
		this.initialDelay = this.timeUnit.toMillis(initialDelay);
	}

	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	public Date nextExecutionTime(TriggerContext triggerContext) {

		try {
				inputIdentifier=pollerName;

		    	if (triggerContext.lastScheduledExecutionTime() == null)
				{
				return new Date(System.currentTimeMillis() + this.initialDelay);
				}
				else if (this.fixedRate)
				{
				return new Date(triggerContext.lastScheduledExecutionTime().getTime() + this.period);
				}

		    	taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG, "Last Polling time of "+pollerName+" is : "+ new Date(triggerContext.lastCompletionTime().getTime()), loggercategory);

		    	jdbcDatabaseDAO.updateLastPollTime(new Date(triggerContext.lastCompletionTime().getTime()),pollerName);

		    	taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG, "Next polling time of "+pollerName+" is : "+ new Date(triggerContext.lastCompletionTime().getTime()+ this.period), loggercategory);

		    	return new Date(triggerContext.lastCompletionTime().getTime()+ this.period);
			}

		catch (Exception e)
			{
			taLogger.log(inputIdentifier, ApplicationConstants.LOG_ERROR,"Error Inside method of nextExecutionTimeOf poller","dbpollerlogging", e);
			}

		return null;

	}

}
