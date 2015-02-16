package com.m1.sg.bcc.om.constant;

import org.apache.log4j.Level;

/************************************************************************************************************************************
 * 
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.constant
 * Description: This class use for define constant call by logger
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 15/05/2013		Apiluck Songwattanasakul		Initial Class and implement the logic inside				
 ************************************************************************************************************************************/

public class OMPollerLoggerConstant {
	
	/**
	 * 
	 * Constant for logger define for log level
	 *
	 */
	public static enum LogLevel{
		
		TRACE	("TRACE",Level.TRACE),
		DEBUG	("DEBUG",Level.DEBUG),
		ERROR	("ERROR",Level.ERROR),
		INFO	("INFO",Level.INFO);
		
		private final String pointVal;
		private final Level severity;

		private LogLevel(String pointVal, Level severity){
			this.pointVal = pointVal;
			this.severity = severity;
		}

		public String getPointVal() {
			return this.pointVal;
		}
		
		public Level getSeverity(){
			return this.severity;
		}
	}
	
	/**
	 * 
	 * Constant for logger define for logger name
	 *
	 */
	public static enum LoggerName{ORDERGENERATORLOGGER("OrderGeneratorLogger"), RESPONSEMESSAGELOGGER("ResponseMessageLogger");
		private final String name;
		private LoggerName(String name){
			this.name = name;
		}
		public String getValue(){
			return this.name;
		}
	}
}
