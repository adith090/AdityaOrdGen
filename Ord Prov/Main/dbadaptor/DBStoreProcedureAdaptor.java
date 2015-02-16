package com.m1.bcc.spl.dbadaptor;

import java.sql.CallableStatement;

import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;



import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.util.SPLCommonComponent;

import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 13/06/2013					Viknesh  				Created for executing DBSPLAdaptor Stored procedure  
 *******************************************************************************/

public class DBStoreProcedureAdaptor extends StoredProcedure  {
	
	String parameter_mode;
	public List<CommandTransDetails> cmdTransDetails;
	private TALogger taLogger;
	
	
	public DBStoreProcedureAdaptor(String sqlCommandRequest,List<CommandTransDetails> cmdTransDetailsList,JdbcTemplate jdbcTemplate ,String srcTransId,String transId,String rowId,String loggerCategory)
	{
		
			super(jdbcTemplate,sqlCommandRequest);
			taLogger = TALogger.getTALogger();
			
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor]SqlCommandRequest :  " +sqlCommandRequest, loggerCategory);
			Iterator<CommandTransDetails> cmdTransDetails = cmdTransDetailsList.iterator();
			
			Map outputValue = null;
			
			while(cmdTransDetails.hasNext()) {
				CommandTransDetails commandTransDetails = (CommandTransDetails) cmdTransDetails.next();
				
				
				String paramName=commandTransDetails.getParamName();
				String paramType=commandTransDetails.getDbParamType();
				String paramMode=commandTransDetails.getDbParamMode();
				String paramValue = commandTransDetails.getParamValue();
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][Declaring]ParamName : "+paramName+" ParamType : "+paramType+" ParamMode : "+paramMode+" ParamValue : "+paramValue , loggerCategory);
				
				if(commandTransDetails!=null) {
					if(paramMode.equalsIgnoreCase("IN")){
						
						
						declareParameter(new SqlParameter(paramName, Get_DB_PARAM_TYPE(paramType)));
						taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][Declaring][IN] "+Get_DB_PARAM_TYPE(paramType), loggerCategory);								
						
					}
					
					if(paramMode.equalsIgnoreCase("OUT")){
						declareParameter(new SqlOutParameter(paramName, Get_DB_PARAM_TYPE(paramType)));
						taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][Declaring][OUT] "+Get_DB_PARAM_TYPE(paramType), loggerCategory);
					}
				}
				
			}
						compile();
			
			
	}
	
	public int Get_DB_PARAM_TYPE(String DB_PARAM_TYPE){
		
		if(DB_PARAM_TYPE.equalsIgnoreCase("VARCHAR"))
			return Types.VARCHAR;
		if(DB_PARAM_TYPE.equalsIgnoreCase("DATE"))
			return Types.DATE;
		if(DB_PARAM_TYPE.equalsIgnoreCase("NUMBER"))
			return Types.NUMERIC;
		
		return 0;		
	}
	
	public Map<String, Object> inPut(List<CommandTransDetails> cmdTransDetailsList,Map<String, Object> inParams ,String srcTransId,String transId,String rowId,String loggerCategory)
	{
		Map outputValue=null;

		Iterator<CommandTransDetails> cmdTransDetailsIN = cmdTransDetailsList.iterator();
		
		
		while(cmdTransDetailsIN.hasNext()) {
			CommandTransDetails cmdTransDetailsParam = (CommandTransDetails) cmdTransDetailsIN.next();
			
			
			String paramName=cmdTransDetailsParam.getParamName();
			String paramValue = cmdTransDetailsParam.getParamValue();
			String paramMode=cmdTransDetailsParam.getDbParamMode();
			
			
			
			if(cmdTransDetailsParam!=null) {
				if(paramMode.equalsIgnoreCase("IN") && (!paramValue.equalsIgnoreCase("SYSDATE"))){
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][inParams]Param Value is not Sysdate", loggerCategory);
					inParams.put(paramName, paramValue);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][inParams]ParamName : "+paramName+" ParamValue : "+paramValue , loggerCategory);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][inParams]SIZE:"+inParams.size() +" Value set is :"+inParams.get(paramName), loggerCategory);
				}
				else if(paramMode.equalsIgnoreCase("IN") && paramValue.equalsIgnoreCase("SYSDATE"))
				{
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][inParams]Param Value is SySdate....", loggerCategory);//+SPLCommonComponent.formatDate("14/06/2013" ,"dd-MM-yyyy") 
					inParams.put(paramName,new Date());

					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][inParams]ParamName : "+paramName+" ParamValue : "+ new Date(), loggerCategory);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][inParams]SIZE:"+inParams.size() +" Value set is :"+inParams.get(paramName), loggerCategory);

				}
			}
		}
		
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][inParams]Compiling......"+inParams.size(), loggerCategory);
		
		
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][inParams]Values :"+inParams.values(), loggerCategory);
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][inParams]Size :"+inParams.size(), loggerCategory);
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][outputValue] Output :"+outputValue, loggerCategory);
		outputValue=execute(inParams);
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBStoreProcedureAdaptor][outputValue] Output :"+outputValue, loggerCategory);
		return outputValue;
		

}
}
