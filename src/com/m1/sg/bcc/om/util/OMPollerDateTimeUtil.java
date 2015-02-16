package com.m1.sg.bcc.om.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/************************************************************************************************************************************
 *  
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.util
 * Description: This utility class created for activity about date time format and date time utility
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMPollerDateTimeUtil {

	/**
	 * XML Date Format
	 */
	public static final String XML_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	
	/**
	 * change time for Object Date 
	 * 
	 * @param currentDateTime	Current Date Time you need to convert
	 * @param millisecond		Milisecond for change date time if you need to change to future date time please input more than 0
	 * 							but you need to change to past you put minus
	 * @return					new date time
	 */
	public static Date changeDateTime(Date currentDateTime, Integer millisecond){
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(currentDateTime);
		
		cal.add(Calendar.MILLISECOND, millisecond);
		
		return cal.getTime();
		
	}
	
	/**
	 * Convert Date time format input as string
	 * 
	 * @param dateTime				current date time
	 * @param inDateTimeFormat		original format date time
	 * @param outDateTimeFormat		expect format date time
	 * @return						string date time new format
	 * @throws Exception			
	 */
	public static String convert(String dateTime, String inDateTimeFormat, String outDateTimeFormat) throws Exception {
		try {
			return format(parse(dateTime, inDateTimeFormat), outDateTimeFormat);
		} catch (Exception e) {
			throw e;
		}
	}
	
	/**
	 * Parse string date time to object date
	 * 
	 * @param dateTime				string date time
	 * @param dateTimeFormat		date time format
	 * @return						object date in the date time you expect
	 * @throws Exception
	 */
	public static Date parse(String dateTime, String dateTimeFormat) throws Exception {
		try {
			boolean isNull = dateTime == null || dateTime.trim().length() == 0;
			if (isNull) {
				return null;
			}
			
			return new SimpleDateFormat(dateTimeFormat).parse(dateTime);
		} catch (Exception e) {
			throw e;
		}
	}
	
	/**
	 * convert object date time to string date time
	 * 
	 * @param dateTime			object date time
	 * @param dateTimeFormat	date time format
	 * @return					string date time under date time format you define in the parameter
	 * @throws Exception
	 */
	public static String format(Date dateTime, String dateTimeFormat) throws Exception {
		try {
			boolean isNull = dateTime == null;
			if (isNull) {
				return "";
			}
			
			return new SimpleDateFormat(dateTimeFormat).format(dateTime);
		} catch (Exception e) {
			throw e;
		}
	}
	
}
