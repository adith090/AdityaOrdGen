package com.m1.bcc.spl.webservice;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.util.BeanFactory;

import common.util.TALogger;

public class MyStoredProcedure extends StoredProcedure {

	TALogger taLogger;
	

	public MyStoredProcedure(JdbcTemplate jdbcTemplate) {

		super(jdbcTemplate, "SP_OM_CheckOrderPONRStatus");
		
		declareParameter(new SqlParameter(ApplicationConstants.MESSAGE_TRANSTYPE, Types.VARCHAR));
		declareParameter(new SqlParameter(ApplicationConstants.MESSAGE_TRANSID,Types.VARCHAR));
		declareParameter(new SqlParameter(ApplicationConstants.MESSAGE_REVISION, Types.INTEGER));
		
		declareParameter(new SqlOutParameter(ApplicationConstants.MESSAGE_ALLOWCANCEL, Types.VARCHAR));
		declareParameter(new SqlOutParameter(ApplicationConstants.MESSAGE_ERRORCODE, Types.INTEGER));
		declareParameter(new SqlOutParameter(ApplicationConstants.MESSAGE_ROWCOUNT, Types.INTEGER));
		
		compile();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<String, Object> execute( String TRANS_TYPE, String TRANS_ID,
			Integer REVISION) {
		Map ponrStatus=null;
		taLogger = (TALogger) BeanFactory.getBean("taLogger");

		try{

		Map<String, Object> inParams = new HashMap<String, Object>();
		
		inParams.put(ApplicationConstants.MESSAGE_TRANSTYPE, TRANS_TYPE);
		inParams.put(ApplicationConstants.MESSAGE_TRANSID, TRANS_ID);
		inParams.put(ApplicationConstants.MESSAGE_REVISION, REVISION);
		ponrStatus = execute(inParams);
		
		taLogger.log("WS", ApplicationConstants.LOG_INFO, "Status is: "
				+ ponrStatus,
				ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);

		/*
		 * taLogger.log("WS", ApplicationConstants.LOG_INFO, "ponr status is:" +
		 * ponrStatus,
		 * ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		 */
		
		}catch(Exception e){
			//System.out.println("error:"+e.getMessage());
		}
		return ponrStatus;
	}
}
