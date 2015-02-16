package com.m1.sg.bcc.om.logger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/************************************************************************************************************************************
 * 
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.logger
 * Description: This class created for log message in our application base on log4j framework
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMPollerLogger {
	
	/**
	 * log4j configuration file path
	 */
	private static String log4jConfFile = "./conf/omordergenerator/log4j_conf.xml";
	
	/**
	 * log4j configuration value for lookup the configuration file again for update value
	 * in the log4j internal configuration
	 */
	private static final long logConfWatchMS = 30*1000;
	
	static {
		
		/* Method call for load the log4j configuration */
		DOMConfigurator.configureAndWatch(log4jConfFile, logConfWatchMS);
	}
	/**
	 * this method implement for log message you can put more than one message. It will
	 * provide concatenate text format
	 * 
	 * logger format is [%time][%crmorderid] + Text Message
	 * 
	 * @param logLevel		Level log enable for example DEBUG, INFO and ERROR
	 * @param loggerName	log message into target logger
	 * @param orderId		CRMOrderId poll from table t_order_header field name order_id
	 * @param freeTextList	text message list
	 */
	public static void log(Level logLevel, String loggerName, String orderId, String[] freeTextList){
		String logMessage = composeLogMessage(orderId, freeTextList);
		Logger logger = Logger.getLogger(loggerName);
		logger.log(logLevel, logMessage);
	}
	
	/**
	 * this method provide for log one message
	 * 
	 * logger format is [%time][%crmorderid] + Text Message
	 * 
	 * @param logLevel		Level log enable for example DEBUG, INFO and ERROR
	 * @param loggerName	log message into target logger
	 * @param orderId		CRMOrderId poll from table t_order_header field name order_id
	 * @param freeText		Text Message
	 */
	public static void log(Level logLevel, String loggerName, String orderId, String freeText){
		String logMessage = composeLogMessage(orderId, new String[]{freeText});
		Logger logger = Logger.getLogger(loggerName);
		logger.log(logLevel, logMessage);
	}
	
	/**
	 * this method implement for log message you can log normal message 
	 * and exception from java application
	 * 
	 * logger format is [%time][%crmorderid] + Text Message + Java Exception
	 * 
	 * @param logLevel		Level log enable for example DEBUG, INFO and ERROR
	 * @param loggerName	log message into target logger
	 * @param orderId		CRMOrderId poll from table t_order_header field name order_id
	 * @param freeText		Text Message
	 * @param e				Java Exception
	 */
	public static void log(Level logLevel, String loggerName, String orderId, String freeText, Throwable e){
		String logMessage = composeLogMessage(orderId, new String[]{freeText});
		Logger logger = Logger.getLogger(loggerName);
		logger.log(logLevel, logMessage, e);
	}
	
	/**
	 * This method created for compose log message into the format we define
	 * 
	 * @param orderId		CRMOrderId poll from table t_order_header field name order_id
	 * @param freeTextList	Text message list
	 * @return				log Message created format already
	 */
	private static String composeLogMessage(String orderId, String[] freeTextList){
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append("[");
		strBuffer.append(orderId);
		strBuffer.append("] ");
		for(String eachOfTextLog : freeTextList){
			strBuffer.append(eachOfTextLog);
		}
		return strBuffer.toString();
	}
}
