package com.m1.bcc.spl.translator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import common.util.TALogger;
/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 15/04/2013					Sudharsan				Removed import statements that invokes tcpadapter simple gateway for IL
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 ******************************************************************************/

public class ILMessage {


	JdbcDatabaseDAO jdbcDatabaseDAO;
	TALogger taLogger = TALogger.getTALogger();

	public final static String EOF_IL = "$END$";
    public final static String responseResultKeyword_IL = "ACK_STATUS";
    public final static String responseDescKeyword_IL = "ACK_SMESSAGE";
    public final static String successKeyword_IL = "0";
    public final static String IL_LOGIN = "LOGIN";
    public final static String IL_ACK = "ACK";

    public  ILMessage() {}

    public  ILMessage(JdbcDatabaseDAO jdbcDatabaseDAO) {
    	this.jdbcDatabaseDAO = jdbcDatabaseDAO;
    }

	/**
	 *
	 * @param userName
	 * @param password
	 * @return
	 */
	public String getLoginRequestMessage(String userName, String password) {
		String requestMessage = "SAS3\n" + "0\n" + "SAS_ACTION LOGIN\n"
				   + "LOGIN " + userName + "\n" + "PASSWORD " + password
				   + "\n" + "$END$\n";
		return requestMessage;
	}

	/**
	 *
	 * @param responseMessage
	 * @return
	 * @throws IOException
	 */
	public boolean isILRequestSuccessful (String responseMessage, String responseType,String loggerCategory) throws IOException {
	    String responseResult = "";

	    BufferedReader reader = new BufferedReader(new StringReader(responseMessage));
	    String message = "";
	    do {
	    	message = reader.readLine();
	    	if(message != null && message.indexOf("ACK_STATUS") != -1) {
				responseResult = message.substring("ACK_STATUS".length()).trim();
				taLogger.log("",ApplicationConstants.LOG_INFO, "[ILMessage][isILRequestSuccessful]Response result :" + responseResult, loggerCategory);
				break;
	    	}
		}while (message != null);

	   	if (responseResult.equalsIgnoreCase(successKeyword_IL))
	   		return true;
	   	else
	   		return false;
	}

	public boolean isILRequestSuccessful (String srcTransId, String transactionId, String parRowId, String responseMessage, String responseType,String loggerCategory) throws IOException {
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[ILMessage][isILRequestSuccessful]Processing message..", loggerCategory);
	    String sasAction = getResponseParamValue(responseMessage, "SAS_ACTION",loggerCategory,srcTransId,transactionId,parRowId);
	    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[ILMessage][isILRequestSuccessful]sasAction retrieved from the message is :" + sasAction, loggerCategory);
	    String status = "ACK_STATUS";
	    if(sasAction!=null && sasAction.equalsIgnoreCase("ERROR")) {
	    	status = "STATUS";
	    }
	    String returnCode = getResponseParamValue(responseMessage, status,loggerCategory,srcTransId,transactionId,parRowId);
	    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[ILMessage][isILRequestSuccessful]ReturnCode :" + returnCode, loggerCategory);
	    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[ILMessage][isILRequestSuccessful]Is IL Request message is successful :" + returnCode.equalsIgnoreCase(successKeyword_IL), loggerCategory);
	   	if (returnCode.equalsIgnoreCase(successKeyword_IL))
	   		return true;
	   	else
	   		return false;
	}

	public String getResponseParamValue(String responseMessage, String paramName,String loggerCategory,String srcTransId,String transId,String cmdRowId) throws IOException {
	    BufferedReader reader = new BufferedReader(new StringReader(responseMessage));
	    String message = "";
	    String paramValue = "";
	    do {
	    	message = reader.readLine();
	    	if(message != null && message.indexOf(paramName) != -1 && !message.contains("SMESSAGE_ID")) {
				paramValue = message.substring(paramName.length()).trim();
				taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[ILMessage][getResponseParamValue]Param value for param name'"+paramName+"' is :" + paramValue, loggerCategory);
				break;
	    	}
		}while (message != null);

	    return paramValue;
	}

	/**
	 *
	 * @param list
	 * @param cmdRefId
	 * @param srcTransId
	 */
	public String getRequestMessage(List<Map<String, Object>> list, String cmdRefId, String cmdRowId, String respQueueId,String loggerCategory,String srcTransId,String transactionId,String parRowId) throws Exception {
		String requestMessage = "";
		Iterator<Map<String, Object>> iterator = list.iterator();
		requestMessage = "SAS3\n" + "0\n" + "SAS_ACTION REQUEST\n"
				+ "ORDER_NO " + cmdRowId + "\n" + "RESP_QUEUE_ID " + respQueueId + "\n"
				+ "REQ_USER 0\n" + "PRIORITY 3\n";
		int count = 0;
		while (iterator.hasNext()) {
			count = count + 1;
			Map<String, Object> paramMap = iterator.next();
			String paramValue=(String) paramMap.get("PARAM_VALUE");
			if(paramValue==null){
				paramValue="";
			}
			requestMessage = requestMessage + paramMap.get("PARAM_NAME") + " " +paramValue+ "\n";
			//taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG, "[ILMessage][getRequestMessage]Appended values are "+paramMap.get("PARAM_NAME")+" "+paramValue, loggerCategory);

		}
		requestMessage = requestMessage + "$END$";
		taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_INFO, "[ILMessage][getRequestMessage]Final request message formed is :" + requestMessage, loggerCategory);
		return requestMessage;
	}

	public String getRegisterAck(String respQueueId) {
		String registerMessage = "";
		registerMessage = "SAS3\n" + "0\n" + "SAS_ACTION REGISTER\n" + "RESP_QUEUE_ID "+ respQueueId +"\n" + "RESP_TYPE ALL\n" + "$END$";
		return registerMessage;
	}

	public String getResponseAck() {
		String responseAck = "";
		responseAck = "SAS3\n" + "0\n" + "SAS_ACTION RESPONSE_ACK\n" + "ACK_STATUS 0\n" + "ACK_SMESSAGE OK\n" + "$END$\n";
		return responseAck;
	}

}
