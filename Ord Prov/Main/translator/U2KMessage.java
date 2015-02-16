package com.m1.bcc.spl.translator;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.SPLCommonComponent;
import common.util.TALogger;
/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 15/04/2013					Sudharsan				Removed import statements that invokes tcpadapter simple gateway for U2K
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 ******************************************************************************/

public class U2KMessage

{
	JdbcDatabaseDAO jdbcDatabaseDAO;
	TALogger taLogger = TALogger.getTALogger();
	SPLCommonComponent splCommonComponent;
    public final static String successKeyword_U2KOPCO = "0";
    public final static String failedKeyword_U2KOPCO = "1";
    public final static String EOF_U2KOPCO = ";";
    public final static String responseResultKeyword_U2KOPCO = "EN=";
    public final static String responseDescKeyword_U2KOPCO = "ENDESC=";
    
	public  U2KMessage() {}

	public  U2KMessage(JdbcDatabaseDAO jdbcDatabaseDAO)
	{
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}
	
	/**
	 * Added for get the request message
	 * @param list
	 * @throws SQLException 
	 */
  public String getRequestMessage(List<Map<String, Object>> list, String cmdRefId,String srcTransId,String transactionId,String parRowId,String u2kopcoLoggerCategory) throws SQLException {

	String requestMessage = "";
	taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_INFO, "[U2KMessage][getRequestMessage]Creating request message..",u2kopcoLoggerCategory);
	CommandTransDetails commandTransDetails = jdbcDatabaseDAO.getCmdDestRefDtls(cmdRefId);
	requestMessage = commandTransDetails.getParameterValue();
	
		taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_INFO, "[U2KMessage][getRequestMessage]Processing request message ",u2kopcoLoggerCategory);
		taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG,"[U2KMessage][getRequestMessage]Request message is :"+requestMessage,u2kopcoLoggerCategory);

		Iterator<Map<String, Object>> iterator = list.iterator();
		Map<String, Object> listMap = null;
		String pName = "";
		String pNameString = "";
		String pValue = "";

		//taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_INFO, "[U2KMessage][getRequestMessage]Iteration starts..",u2kopcoLoggerCategory);
		while (iterator.hasNext()) {

			listMap = (Map<String, Object>) iterator.next();
			pName = (String) listMap.get("PARAM_NAME");
			pNameString = "#" + pName + "#";
			pValue = (String) listMap.get("PARAM_VALUE");
			if ((pValue == null) || (pValue == "")) {
				pValue = "";
			}
			requestMessage = requestMessage.replaceAll(
					pNameString, pValue);

		}
		taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_INFO, "[U2KMessage][getRequestMessage]Final request message formed :"+requestMessage, u2kopcoLoggerCategory);
		return requestMessage;
	}

	public boolean U2KOPCOrequestIsSuccessful (String responseMessage,String srcTransId,String transactionId,String parRowId,String u2kopcoLoggerCategory) {
	    String responseResult;
	    String responseDesc;
	    int indexOfResponseResult;
	    int indexOfResponseDesc;
	    taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_INFO, "[U2KMessage][U2KOPCOrequestIsSuccessful]Checking whether opco request is successful..",u2kopcoLoggerCategory);
    	indexOfResponseResult = responseMessage.indexOf(responseResultKeyword_U2KOPCO) + responseResultKeyword_U2KOPCO.length();
    	indexOfResponseDesc = responseMessage.indexOf(responseDescKeyword_U2KOPCO) + responseDescKeyword_U2KOPCO.length();
    	responseResult = responseMessage.substring(indexOfResponseResult, responseMessage.indexOf(" ", indexOfResponseResult));
    	responseDesc = responseMessage.substring(indexOfResponseDesc, responseMessage.indexOf(EOF_U2KOPCO, indexOfResponseDesc));
    

    	if (responseResult.equalsIgnoreCase(successKeyword_U2KOPCO))
    		return true;
    	else
    		return false;
    }

}
