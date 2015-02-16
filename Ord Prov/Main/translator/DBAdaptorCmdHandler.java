package com.m1.bcc.spl.translator;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 29/11/2013					Ravikumar G				Created to process DB Adaptor Command
 * 16/01/2014					Ravikumar G				Bug#23590 - to update request message in cmd_trans table
 * 17/01/2014					Ravikumar G				Bug#23710 - Updated cmd_trans column row_id type to number
 * 31/01/2014					Ravikumar G				Bug#24614 - Updated to get OPCO billing datasource and for call opco stored procedure
 ******************************************************************************/

public class DBAdaptorCmdHandler {
	
	private TALogger taLogger = null;
	
	public void processDbAdaptorCmd(Map<String, Object> commandDetails, CommandTransDetails cmdSystemDetails, DatabaseDAO dbAdaptorDAO) {
		String appendReqDotXml = ApplicationConstants.SYSTEM_APPEND_REQ_DOTXML;
		boolean isStub = false;
		String cmdRefId = "";
		String systemName = "";
		String rowId = "";
		String transId = "";
		String srcTransId = "";
		String interfaceName = "";
		String loggerCategory = "DATABASElogging";
		SPLCommonComponent splCommonComponent = null;
		taLogger = TALogger.getTALogger();
		
		try {
			Properties properties = SPLCommonComponent.getSystemStubProperty();
			
			if(commandDetails!=null) {
				cmdRefId = (String)commandDetails.get(ApplicationConstants.COLUMN_CMD_REF_ID);
				systemName = cmdSystemDetails.getSystemName();
				loggerCategory =systemName.toUpperCase()+"logging";
				splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
				splCommonComponent.setLoggerCategory(loggerCategory);
				rowId = "" + (BigDecimal)commandDetails.get(ApplicationConstants.COLUMN_ROW_ID);
				transId = (String)commandDetails.get(ApplicationConstants.COLUMN_TRANS_ID);
				srcTransId = (String)commandDetails.get(ApplicationConstants.COLUMN_SRC_TRANS_ID);
	
				dbAdaptorDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_RECEIVED);
	
				splCommonComponent.setLogSrcTransId(srcTransId);
				splCommonComponent.setLogTransId(transId);
				splCommonComponent.setLogCmdRowId(rowId);
	
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[DBAdaptorCmdHandler][processDbAdaptorCmd]Command reference id :" + cmdRefId, loggerCategory);
				String status = (String)commandDetails.get(ApplicationConstants.COLUMN_STATUS);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][processDbAdaptorCmd]Status of command :" + status, loggerCategory);
	
				String systemTechMethod = cmdSystemDetails.getTechMethod();
					
				String isAuthTokenRequired = cmdSystemDetails.getAuthTokenRequired();
				interfaceName = cmdSystemDetails.getInterfaceName();
				String searchParamTag = cmdSystemDetails.getSearchParamTag();
				String replaceParamTag = cmdSystemDetails.getReplaceParamTag();
	
				isStub = SPLCommonComponent.getStubbing(properties, systemName);
	
				String storedProcName = cmdSystemDetails.getParameterValue();
				String requestMsgFlag = cmdSystemDetails.getRequestMsgFlag();
				
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[DBAdaptorCmdHandler][processDbAdaptorCmd]storedProcName :" + storedProcName, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[DBAdaptorCmdHandler][processDbAdaptorCmd]System tech method :" + systemTechMethod, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[DBAdaptorCmdHandler][processDbAdaptorCmd]System name :" + systemName, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][processDbAdaptorCmd]Is auth token required :" + isAuthTokenRequired, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[DBAdaptorCmdHandler][processDbAdaptorCmd]Interface name :" + interfaceName, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][processDbAdaptorCmd]Stub :" + isStub, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][processDbAdaptorCmd]searchParamTag :" + searchParamTag, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][processDbAdaptorCmd]replaceParamTag :" + replaceParamTag, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][processDbAdaptorCmd]requestMsgFlag :" + requestMsgFlag, loggerCategory);
	
				List<CommandTransDetails> cmdTransDetailsList = dbAdaptorDAO.getCmdTransDetails(rowId, null, srcTransId, transId,systemName);
				
				if(ApplicationConstants.SYSTEM_OPCOBILLING.equalsIgnoreCase(systemName) || ApplicationConstants.SYSTEM_PSBILLING.equalsIgnoreCase(systemName)) {
					dbAdaptorDAO.updateCmdTrans(srcTransId,  transId, rowId, requestMsgFlag, null, isStub, loggerCategory);
					dbAdaptorDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);
					if(isStub) {
						String responseMessage = getResponse(splCommonComponent, rowId, ApplicationConstants.TOM_RESPONSE_SUCCESS, ApplicationConstants.MESSAGE_SUCCESS);
						dbAdaptorDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessage, interfaceName, false);
					}else {
						dbAdaptorDAO.processDBAdaptorRequest(cmdTransDetailsList, storedProcName, srcTransId, transId, rowId, loggerCategory);
						String responseMessage = getResponse(splCommonComponent, rowId, ApplicationConstants.TOM_RESPONSE_SUCCESS, ApplicationConstants.MESSAGE_SUCCESS);
						taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR, "[DBAdaptorCmdHandler][processDbAdaptorCmd]"+"Response message :" + responseMessage, loggerCategory);
						dbAdaptorDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessage, interfaceName, false);
					}
				}else {
					String filePath = splCommonComponent.getFilePath(systemName, interfaceName, ApplicationConstants.APPEND_REQUEST, appendReqDotXml);
					Document requestDocument = splCommonComponent.getDocument(filePath);
					String requestMessage = splCommonComponent.convertDocumentToString(requestDocument);
					taLogger.log(srcTransId, transId, rowId,ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][processDbAdaptorCmd]Request message :" + requestMessage, loggerCategory);
					Iterator<CommandTransDetails> cmdTransDetails = cmdTransDetailsList.iterator();
					while(cmdTransDetails.hasNext()) {
						CommandTransDetails commandTransDetails = (CommandTransDetails) cmdTransDetails.next();
						if(commandTransDetails!=null) {
							String paramName = commandTransDetails.getParamName();
							String paramValue = commandTransDetails.getParamValue();
							taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_DEBUG, "[DBAdaptorCmdHandler][processDbAdaptorCmd]"+"PARAM NAME : " + paramName + " , PARAM VALUE : " + paramValue, loggerCategory);
			
							if(paramName.equalsIgnoreCase("PARAMETER")){
								setArrayOfExtTransDtls(requestDocument, paramValue, srcTransId, transId, rowId, splCommonComponent, loggerCategory);
							}else {
								if(searchParamTag.equalsIgnoreCase("Y"))
									setNodeValue(requestDocument, paramName, paramValue);
								if(replaceParamTag.equalsIgnoreCase("Y"))
									requestMessage = requestMessage.replace("#"+paramName+"#", paramValue);
							}
						}
					}
					if(isStub) {
						requestMessage = splCommonComponent.convertDocumentToString(requestDocument);
						dbAdaptorDAO.updateCmdTrans(srcTransId,  transId, rowId, requestMsgFlag, requestMessage, isStub, loggerCategory);
						splCommonComponent.saveMessage(requestDocument, interfaceName, rowId, transId, appendReqDotXml);
						String stubResponsePath = splCommonComponent.getFilePath(systemName, interfaceName, ApplicationConstants.APPEND_RESPONSE, ApplicationConstants.SYSTEM_APPEND_RES_DOTXML);
						String responseMessage = splCommonComponent.getMessage(stubResponsePath);
						Document responseDocument = splCommonComponent.convertStringToXmlDocument(responseMessage);
						splCommonComponent.saveMessage(responseDocument, interfaceName, rowId, transId, ApplicationConstants.SYSTEM_APPEND_RES_DOTXML);
						dbAdaptorDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);
						dbAdaptorDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessage, interfaceName, false);
					}else {
						requestMessage = splCommonComponent.convertDocumentToString(requestDocument);
						dbAdaptorDAO.updateCmdTrans(srcTransId,  transId, rowId, requestMsgFlag, requestMessage, isStub, loggerCategory);
						dbAdaptorDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);
						dbAdaptorDAO.processDBAdaptorRequest(requestMessage, storedProcName, loggerCategory, srcTransId, transId, rowId);
					}
				}
			}
		}catch(Exception exception) {
			dbAdaptorDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_ERROR);
			dbAdaptorDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "DBAdaptorCmdHandler", ApplicationConstants.TOM_RESPONSE_ERROR, SPLCommonComponent.getStackTrace(exception));
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[DBAdaptorCmdHandler][processDbAdaptorCmd][catch block] Exception: ", loggerCategory, exception);
			String responseMessage = getResponse(splCommonComponent, rowId, ApplicationConstants.TOM_RESPONSE_ERROR, exception.getMessage());
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR, "[DBAdaptorCmdHandler][processDbAdaptorCmd][catch block]"+"Response message :" + responseMessage, loggerCategory);
			try {
				dbAdaptorDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessage, interfaceName, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	} 
	
	private Document setArrayOfExtTransDtls(Document doc, String paramValue, String srcTransId, String transId, String rowId, SPLCommonComponent splCommonComponent, String loggerCategory) throws Exception {
		String requestMessage = splCommonComponent.convertDocumentToString(doc);
		taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][setArrayOfExtTransDtls]Request message : " + requestMessage, loggerCategory);

		StringTokenizer token = new StringTokenizer(paramValue, "=,|");
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][setArrayOfExtTransDtls]Token : " + token, loggerCategory);
		while (token.hasMoreTokens()) {

			String key1 = token.nextToken();
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][setArrayOfExtTransDtls]Key1 : " + key1, loggerCategory);

			String value1 = token.nextToken();
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[DBAdaptorCmdHandler][setArrayOfExtTransDtls]Value1 : " + value1, loggerCategory);

			Element createTaskElement = doc.createElement("PARAM_INFO");
			NodeList nodeList = doc.getElementsByTagName("DBAdaptorRequest");
			Node heirarchy = nodeList.item(0);
			heirarchy.appendChild(createTaskElement);

			Element dsElement = doc.createElement("PARAM_NAME");
			createTaskElement.appendChild(dsElement);
			dsElement.setTextContent(key1);

			Element rootElement = doc.createElement("PARAM_VALUE");
			createTaskElement.appendChild(rootElement);
			rootElement.setTextContent(value1);
			requestMessage = splCommonComponent.convertDocumentToString(doc);
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[DBAdaptorCmdHandler][setGazelleXmlValues]Request message :" + requestMessage, "GAZELLElogging");
		}
		return doc;
	}
	
	private void setNodeValue(Document document, String paramName, String paramValue) throws SPLExceptionHandler {
		NodeList userNameList = document.getElementsByTagName(paramName);
		int index=0;
		while(index <userNameList.getLength()){
	        Node userNameNode = userNameList.item(index);
	        if(userNameNode!=null)
	        	userNameNode.setTextContent(paramValue);
	        else
	        	throw new SPLExceptionHandler("[DBAdaptorCmdHandler] Element " +  paramName + " not available in XML document");
	        index++;
		}
	}
	
	private String getResponse(SPLCommonComponent splCommonComponent, String cmdRowId, String returnCode, String returnMsg) {
		String responseMessage = null;
		try {
			Document document = splCommonComponent.getDocument();
			
			Element rootElement = document.createElement("DBAdaptorResponse");
		    rootElement.setAttribute("xmlns", "http://tempuri.org/");
			document.appendChild(rootElement);
			
			Node msgHeaderNode = splCommonComponent.createElement(document, rootElement, "msgHeader", "");
			splCommonComponent.createElement(document, msgHeaderNode, "transId", cmdRowId);
			splCommonComponent.createElement(document, msgHeaderNode, "returnCode", returnCode);
			splCommonComponent.createElement(document, msgHeaderNode, "returnMessage", returnMsg);
			responseMessage = splCommonComponent.convertDocumentToString(document);
		}catch (Exception e) {
			e.printStackTrace();
		}
		return responseMessage;
	}
}
