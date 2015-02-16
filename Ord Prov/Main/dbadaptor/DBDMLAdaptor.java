package com.m1.bcc.spl.dbadaptor;

import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.model.CommandTransDetails;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 13/06/2013					Viknesh  				Created for executing DBSPLAdaptor Insert Statement  
 *******************************************************************************/

public class DBDMLAdaptor {

	private String JNDIName;
	private DataSource dataSource;
	private TALogger taLogger;
	
	
	
	private String replaceRequestParam( List<CommandTransDetails> cmdTransDetailsList, String sqlCommandRequest,String srcTransId,String transId,String rowId, String loggerCategory){
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBDMLAdaptor][replaceRequestParam]ReplaceRequestParam", loggerCategory);
		Iterator<CommandTransDetails> cmdTransDetails = cmdTransDetailsList.iterator();
		
		while(cmdTransDetails.hasNext()) {
			CommandTransDetails commandTransDetails = (CommandTransDetails) cmdTransDetails.next();
			
			if(commandTransDetails!=null) {
				String paramName = commandTransDetails.getParamName();
				String paramValue = commandTransDetails.getParamValue();				
				
				if (paramValue.equalsIgnoreCase("SYSDATE"))
					sqlCommandRequest = sqlCommandRequest.replace(":"+paramName, paramValue);
				else
					sqlCommandRequest = sqlCommandRequest.replace(":"+paramName, "'"+paramValue+"'");
			}
		}
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBDMLAdaptor][replaceRequestParam]SqlCommandRequest : "+sqlCommandRequest, loggerCategory);
		return sqlCommandRequest;
	}
	

	public String executeDMLDBCommand(List<CommandTransDetails> cmdTransDetailsList, String sqlCommandRequest, String location,String srcTransId,String transId,String rowId, String loggerCategory){
		try
		{	
			taLogger = TALogger.getTALogger();
			String splRequest = replaceRequestParam(cmdTransDetailsList, sqlCommandRequest,srcTransId,transId,rowId, loggerCategory);
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBDMLAdaptor][replaceRequestParam]SplRequest : "+splRequest , loggerCategory);
			InitialContext context = new InitialContext();
			
			DataSource dataSource = (DataSource)context.lookup(location);
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBDMLAdaptor][replaceRequestParam]DataSource : "+dataSource.toString(), loggerCategory);
			JdbcTemplate jdbcTemplate =  new JdbcTemplate(dataSource);
			int numberofrowsaffected  = jdbcTemplate.update(splRequest);
			
			if(numberofrowsaffected >0)
				return "SUCCESS|"+numberofrowsaffected+" updated";
			return  "Failed|"+numberofrowsaffected+" updated";
		}
		catch(Exception e)
		{
			return  "Failed|"+e.toString();
		}
		
	}
	

}
