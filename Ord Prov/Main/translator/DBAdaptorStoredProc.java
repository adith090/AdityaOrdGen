package com.m1.bcc.spl.translator;

import java.sql.SQLXML;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.StatementCreatorUtils;
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
 * 28/11/2013					Ravikumar G				Created to call the stored procedure for DB Adaptor
 * 31/01/2014					Ravikumar G				Bug#24614 - Added method to call opco stored procedure
 ******************************************************************************/

public class DBAdaptorStoredProc extends StoredProcedure {

	private TALogger taLogger = TALogger.getTALogger();
	
	public DBAdaptorStoredProc(JdbcTemplate jdbcTemplate, String storedProcName) {

		super(jdbcTemplate, storedProcName);
		
		declareParameter(new SqlParameter("REQUEST_STR", Types.VARCHAR));
		declareParameter(new SqlOutParameter("EXT_TRANS_ROW_ID", Types.VARCHAR));
		
		compile();
	}

	public String execute(String value) throws Exception {

		Map<String, Object> inParams = new HashMap<String, Object>();
		String output = null;
		inParams.put("REQUEST_STR", value);
		Map<String, Object> map1 = execute(inParams);
		if(map1!=null) {
			output = (String)map1.get("EXT_TRANS_ROW_ID");
			System.out.println(output);
		}
		return output;
	}
	
	public DBAdaptorStoredProc(List<CommandTransDetails> cmdTransDetailsList, JdbcTemplate jdbcTemplate, String storedProcName, String srcTransId, String transId, String cmdRowId, String loggerCategory) {

		super(jdbcTemplate, storedProcName);
		
		Iterator<CommandTransDetails> cmdTransDetails = cmdTransDetailsList.iterator();
		
		while(cmdTransDetails.hasNext()) {
			CommandTransDetails commandTransDetails = (CommandTransDetails) cmdTransDetails.next();
			String paramName = commandTransDetails.getParamName();
			String paramDataType = commandTransDetails.getDbParamType();
			String paramMode = commandTransDetails.getDbParamMode();
			
			if(paramMode.equalsIgnoreCase("IN")) {
				declareParameter(new SqlParameter(paramName, getSqlParamDataType(paramDataType)));
			}else if(paramMode.equalsIgnoreCase("OUT")) {
				declareParameter(new SqlOutParameter(paramName, getSqlParamDataType(paramDataType)));
			}
		}

		compile();
	}
	
	public void execute(List<CommandTransDetails> cmdTransDetailsList, String srcTransId, String transId, String cmdRowId, String loggerCategory) throws Exception {

		Map<String, Object> inParams = new HashMap<String, Object>();
		Iterator<CommandTransDetails> cmdTransDetails = cmdTransDetailsList.iterator();
		
		while(cmdTransDetails.hasNext()) {
			CommandTransDetails commandTransDetails = (CommandTransDetails) cmdTransDetails.next();
			String paramName = commandTransDetails.getParamName();
			String paramValue = commandTransDetails.getParamValue();
			String paramDataType = commandTransDetails.getDbParamType();
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DBAdaptorStoredProc][execute]paramName=" + paramName , loggerCategory);
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DBAdaptorStoredProc][execute]paramValue=" + paramValue , loggerCategory);
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DBAdaptorStoredProc][execute]paramDataType=" + paramDataType, loggerCategory);
			if(paramDataType.equalsIgnoreCase("DATE"))
				inParams.put(paramName, SPLCommonComponent.parseDate(paramValue));
			else
				inParams.put(paramName, paramValue);
		}
		execute(inParams);
	}
	
	public int getSqlParamDataType(String paramDataType) {
		if(paramDataType.equalsIgnoreCase("VARCHAR"))
			return Types.VARCHAR;
		if(paramDataType.equalsIgnoreCase("DATE"))
			return Types.DATE;
		if(paramDataType.equalsIgnoreCase("NUMBER"))
			return Types.NUMERIC;
		
		return 0;		
	}
	
}
