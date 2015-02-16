package com.m1.bcc.spl.translator;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.httpclient.HttpClient;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.http.CommonsHttpMessageSender;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.model.OrderTransactonDetail;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.sender.OrderTransactionSender;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import com.m1.bcc.spl.util.XPathReader;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/11/2012					Ravikumar G				Created
 * 11/01/2012					Ravikumar G				Changed VMS to OM .NET Module
 * 22/02/2013					Ravikumar G				Modified for logging cleanup
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 21/05/2013					Ravikumar G				Bug#2184 : Update the STUB flag in cmd_trans table
 * 06/11/2013					Ravikumar G				Bug#20397: removed method createOmDnetModuleMessage and createOmDnetTaskMessage, added method createOmDnetModuleRequest
 * 12/11/2013					Ravikumar G				Bug#20421 - to implement multithreading using thread poll task executor
 * 20/11/2013					Ravikumar G				Bug#20645 - updated paramColumn for resource properties to empty
 * 20/11/2013					Ravikumar G				Bug#20538 - Added timeout and read timeout
 * 06/12/2013					Ravikumar G				Bug#21337 - update xml element type ResourceProperties to ResourceProperty
 * 08/01/2014					Ravikumar G				Bug#22917 - Moved PARAM_TYPE_RESOURCE_PROPERTIES to constants
 * 16/01/2014					Ravikumar G				Bug#23590 - to update request message in cmd_trans table
 * 17/01/2014					Ravikumar G				Bug#23710 - Updated cmd_trans column row_id type to number
 * 27/01/2014					Ravikumar G				Bug#23086 - added condition if source=OSM then send tomresponse
 ******************************************************************************/

@SuppressWarnings("deprecation")
public class WcfTranslator {

		private String loggerCategory = ApplicationConstants.LOG_CATEGORY_CRM_LOGGING;
		private String inputIdentifier="";
		private TALogger taLogger;
		private String srcTransId = "-1";
		private String cmdRefId = "-1";
		private SPLCommonComponent splCommonComponent;
		private static String PARAM_TYPE_RESOURCE = "Resource";
		//private static String PARAM_TYPE_RESOURCE_PROPERTIES = "ResourceProperty";
		private static String PARAM_TYPE_MAINLINEITEMATTRIBUTE = "MainLineItemAttribute";
		private static String PARAM_TYPE_LINEITEMATTRIBUTE = "LineItemAttribute";
		private static String PARAM_TYPE_WORKORDER="WorkOrder";
		private static String PARAM_TYPE_LINEITEM="LineItem";
		private static String PARAM_MODULE_MESSAGE_NAME_FIELD ="InvokeOMDnetModule";
		private static String PARAM_MODULE_VERSION_FIELD= "Version1";
		private Properties properties;
		String soapAction = "";
		String cmdTransId = "";
		String transId="";
		Properties propertiesKey = (Properties) BeanFactory.getBean("properties");
		String Key = propertiesKey.getProperty("KEY");

		private Document requestDocument = null;

		/*@Async*/
		//public void handleWcfCommands(Map<String, Object> commandDetails, CommandTransDetails cmdSystemDetails, boolean isStub, DatabaseDAO wcfTranslatorDAO) {
		public void handleWcfCommands(Map<String, Object> commandDetails, CommandTransDetails cmdSystemDetails, DatabaseDAO wcfTranslatorDAO, boolean immedialyFlag) {

			String orderRowId = "";
			try {
				taLogger = TALogger.getTALogger();
				String cmdRefId = (String)commandDetails.get(ApplicationConstants.COLUMN_CMD_REF_ID);
				String status = (String)commandDetails.get(ApplicationConstants.COLUMN_STATUS);
				cmdTransId = "" + (BigDecimal)commandDetails.get(ApplicationConstants.COLUMN_ROW_ID);
				wcfTranslatorDAO.updateCommandTransactionStatus(cmdTransId, ApplicationConstants.STATUS_RECEIVED);
				transId = (String)commandDetails.get(ApplicationConstants.COLUMN_TRANS_ID);
				srcTransId = (String)commandDetails.get(ApplicationConstants.COLUMN_SRC_TRANS_ID);
				orderRowId = (String)commandDetails.get("ORDER_ROW_ID");
				String systemName = cmdSystemDetails.getSystemName();
				String interfaceName = cmdSystemDetails.getInterfaceName();
				String location = cmdSystemDetails.getLocation();
				String userName = cmdSystemDetails.getUserName();
				String password = cmdSystemDetails.getPwd();
				String accountType=cmdSystemDetails.getAccountType();
				String requestMsgFlag = cmdSystemDetails.getRequestMsgFlag();
				Properties stubProperties = SPLCommonComponent.getSystemStubProperty();
				boolean isStub = SPLCommonComponent.getStubbing(stubProperties, systemName);
				inputIdentifier = cmdTransId;
				taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Initializing CRM Translator", loggerCategory);
				taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG, "[CRM Translator][handleWcfCommands]Command reference id is :" + cmdRefId, loggerCategory);
				taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG, "[CRM Translator][handleWcfCommands]Interface name is :"+interfaceName , loggerCategory);
				taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG, "[CRM Translator][handleWcfCommands]Order row id is :" + orderRowId, loggerCategory);
				taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG, "[CRM Translator][handleWcfCommands]Status of order is :" + status, loggerCategory);

				taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG, "[CRM Translator][handleWcfCommands]requestMsgFlag :" + requestMsgFlag, loggerCategory);
				
				//Properties propertiesTimeout = (Properties) BeanFactory.getBean("properties");
				//final String timeOutString = propertiesTimeout.getProperty("TIMEOUT");
				//int timeOut = Integer.parseInt(timeOutString);
				
				int conTimeOut = Integer.parseInt(cmdSystemDetails.getTimeOut());
				int readTimeOut = Integer.parseInt(cmdSystemDetails.getReadTimeOut());
				
				//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Setting splCommonComponent variables", loggerCategory);
				splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
				splCommonComponent.setLogSrcTransId(srcTransId);
				splCommonComponent.setLogTransId(transId);
				splCommonComponent.setLogCmdRowId(cmdTransId);
				//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Setting splCommonComponent variables has completed", loggerCategory);
				/*EventDrivenConsumer wcfGateway = (EventDrivenConsumer) BeanFactory.getBean("wcfGateway");
				taLogger.log("WcfTranslator][handleWcfCommands][Cmd Trans Id =" + inputIdentifier, ApplicationConstants.LOG_INFO, "wcfGateway=" + wcfGateway.getComponentName(), loggerCategory);
				taLogger.log("WcfTranslator][handleWcfCommands][Cmd Trans Id =" + inputIdentifier, ApplicationConstants.LOG_INFO, "wcfGateway=" + wcfGateway, loggerCategory);*/

				// Load the properties from the spring bean - configured in applicationcontext-datasource.xml

				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Initializing CRM Properties", loggerCategory);
				properties = (Properties) BeanFactory.getBean("properties");
				//taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Property of 'PARENTLINEITEMID' is:" + properties.getProperty("PARENTLINEITEMID"), loggerCategory);
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_DEBUG, "[CRM Translator][handleWcfCommands]Stub for CRM is :" + isStub, loggerCategory);

				Document wcfDocument = splCommonComponent.getDocument();
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Validating interface name (Task or OMDNET)", loggerCategory);
				// Bug 257 -. Changes for SPL to CRM integration, to call CRM webservices
				// Ravi: Changed VMS to OM .NET Module
				/*if(ApplicationConstants.CRM_OMDNETTASK.equals(interfaceName)) {
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Initializing OmDnetTask", loggerCategory);
					wcfDocument = createOmDnetModuleRequest(wcfDocument, cmdTransId, systemName, userName, password,accountType, wcfTranslatorDAO,interfaceName);
					soapAction =  splCommonComponent.getWSSoapAction(interfaceName);;
				}else if(ApplicationConstants.CRM_OMDNETMODULE.equals(interfaceName)) {
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Initializing OmDnetModule", loggerCategory);
					//wcfDocument = createOmDnetModuleMessage(wcfDocument, cmdTransId, systemName, userName, password,accountType, wcfTranslatorDAO,interfaceName);
					wcfDocument = createOmDnetModuleRequest(wcfDocument, cmdTransId, systemName, userName, password,accountType, wcfTranslatorDAO,interfaceName);
					soapAction =  splCommonComponent.getWSSoapAction(interfaceName);;
				}*/
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Initializing OmDnetModule", loggerCategory);
				wcfDocument = createOmDnetModuleRequest(wcfDocument, cmdTransId, systemName, userName, password,accountType, wcfTranslatorDAO,interfaceName);
				soapAction =  splCommonComponent.getWSSoapAction(interfaceName);
				
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_DEBUG, "[CRM Translator][handleWcfCommands]SoapAction :"+soapAction, loggerCategory);
				
				String wcfRequestMessage = splCommonComponent.convertDocumentToString(wcfDocument);
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Request Message to CRM is:\n" + wcfRequestMessage, loggerCategory);
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Checking for Stubs.. isStub value is "+isStub, loggerCategory);
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_DEBUG, "[CRM Translator][handleWcfCommands]Time out : "+ conTimeOut, loggerCategory);
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_DEBUG, "[CRM Translator][handleWcfCommands]Read Time out : "+ readTimeOut, loggerCategory);
				//Ravi : 20130521 : Bug 2184 : Update the STUB flag in cmd_trans : Start
				//wcfTranslatorDAO.updateStubbingStatus(srcTransId,  transId, inputIdentifier, isStub, loggerCategory);
				wcfTranslatorDAO.updateCmdTrans(srcTransId,  transId, inputIdentifier, requestMsgFlag, wcfRequestMessage, isStub, loggerCategory);
				// Ravi : 20130521 : Bug 2184 : Update the STUB flag in cmd_trans : End
				if(isStub) {
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Creating Stub file", loggerCategory);
					splCommonComponent.saveMessage(wcfDocument, interfaceName, cmdTransId, transId, ApplicationConstants.SYSTEM_APPEND_REQ_DOTXML);
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Stub file Created", loggerCategory);
					wcfTranslatorDAO.updateCommandTransactionStatus(cmdTransId, ApplicationConstants.STATUS_SENT);
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Reading Response file", loggerCategory);
					String stubResponsePath = splCommonComponent.getFilePath(systemName, interfaceName, ApplicationConstants.APPEND_RESPONSE, ApplicationConstants.SYSTEM_APPEND_RES_DOTXML);
					String responseMessage = splCommonComponent.getMessage(stubResponsePath);
					XPathReader xPathReader = new XPathReader();
					Document responseDocument = xPathReader.getDocument(responseMessage);
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Response file retrieved", loggerCategory);
					splCommonComponent.saveMessage(responseDocument, cmdRefId, cmdTransId, transId, ApplicationConstants.SYSTEM_APPEND_RES_DOTXML);
					wcfTranslatorDAO.saveCommandResponse(cmdTransId, cmdRefId, transId, responseMessage, interfaceName, immedialyFlag);
				}else {
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_DEBUG, "[WcfTranslator][Inside Call Back]Initializing CRM web service", loggerCategory);
					String decryptedKey="";
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_DEBUG,"[WcfTranslator][Inside Call Back]Key :"+Key+"Password :"+password, loggerCategory);
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[WcfTranslator][Inside Call Back]WcfRequestMessage :"+wcfRequestMessage, loggerCategory);
					if(password!=null && !password.trim().equals("")){
					decryptedKey=SPLCommonComponent.Crypt(0, Key, password);
					}
					if(wcfRequestMessage.contains("#password#")){
					wcfRequestMessage=wcfRequestMessage.replaceAll("#password#", decryptedKey);
					}
					StreamSource request = new StreamSource(new StringReader(wcfRequestMessage));
					//Comment the next line
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_DEBUG, "[WcfTranslator][Inside Call Back]DecryptedKey :"+decryptedKey, loggerCategory);
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[WcfTranslator][Inside Call Back]WcfRequestMessage after replacing the decrypted Key = "+wcfRequestMessage, loggerCategory);
					StreamResult response = new StreamResult(new StringWriter());
			        WebServiceTemplate template = new WebServiceTemplate();

			        HttpClient httpClient = new HttpClient();
			       // HttpClientParams httpClientParams = httpClient.getParams();
			       // taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Time out going to set is :"+timeOut , loggerCategory);
			        //httpClientParams.setSoTimeout(timeOut);

			        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(conTimeOut);
			        httpClient.getHttpConnectionManager().getParams().setSoTimeout(readTimeOut);
					int Gettimeout = httpClient.getHttpConnectionManager().getParams().getSoTimeout();


			        taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_DEBUG, "[CRM Translator][handleWcfCommands]Time out set is:"+Gettimeout, loggerCategory);
			        CommonsHttpMessageSender commonsHttpMessageSender = new CommonsHttpMessageSender(httpClient);
			        template.setMessageSender(commonsHttpMessageSender);
			        //taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]setMessageSender:"+template, loggerCategory);
			        
			        //taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]soapAction="+soapAction , loggerCategory);
			        SoapActionCallback actionCallBack = new SoapActionCallback(location) {
			            public void doWithMessage(WebServiceMessage msg) {
			                SoapMessage smsg = (SoapMessage)msg;
			                smsg.setSoapAction(soapAction);
			            }
			        };
			        //taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Updating T_OM_CMD_TRANS table", loggerCategory);
			        wcfTranslatorDAO.updateCommandTransactionStatus(cmdTransId, ApplicationConstants.STATUS_SENT);
			        //taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]T_OM_CMD_TRANS table updated ", loggerCategory);
			        taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Sending request message to CRM..", loggerCategory);
			        template.sendSourceAndReceiveToResult(location, request, actionCallBack, response);
			        //taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Request="+request+" actionCallBack= "+actionCallBack+" Response= "+response, loggerCategory);
					String responseMessage = response.getWriter().toString();
					taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Received CRM response ", loggerCategory);
			        taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]CRM Response Message is :" + responseMessage, loggerCategory);
			        //taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][handleWcfCommands]Saving Command response..", loggerCategory);
			        wcfTranslatorDAO.saveCommandResponse(cmdTransId, cmdRefId, transId, responseMessage, interfaceName, immedialyFlag);

				}

			} catch (ParserConfigurationException parserConfigurationException) {
				wcfTranslatorDAO.updateCommandTransactionStatus(cmdTransId, ApplicationConstants.STATUS_ERROR);
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_ERROR, "[CRM Translator][handleWcfCommands][Catch]parserConfigurationException", loggerCategory, parserConfigurationException);
				wcfTranslatorDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CRM Translator][handleWcfCommands][Catch]", cmdRefId, SPLCommonComponent.getStackTrace(parserConfigurationException));
			} catch (TransformerConfigurationException transformerConfigurationException) {
				wcfTranslatorDAO.updateCommandTransactionStatus(cmdTransId, ApplicationConstants.STATUS_ERROR);
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_ERROR, "[CRM Translator][handleWcfCommands][Catch]transformerConfigurationException", loggerCategory, transformerConfigurationException);
				wcfTranslatorDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CRM Translator][handleWcfCommands][Catch]", cmdRefId, SPLCommonComponent.getStackTrace(transformerConfigurationException));
			} catch (TransformerException transformerException) {
				wcfTranslatorDAO.updateCommandTransactionStatus(cmdTransId, ApplicationConstants.STATUS_ERROR);
				taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_ERROR, "[CRM Translator][handleWcfCommands][Catch]transformerException", loggerCategory, transformerException);
				wcfTranslatorDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CRM Translator][handleWcfCommands][Catch]", cmdRefId, SPLCommonComponent.getStackTrace(transformerException));
			} catch (SQLException sqlException) {
				wcfTranslatorDAO.updateCommandTransactionStatus(cmdTransId, ApplicationConstants.STATUS_ERROR);
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_ERROR, "[CRM Translator][handleWcfCommands][Catch]sqlException", loggerCategory, sqlException);
				wcfTranslatorDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CRM Translator][handleWcfCommands][Catch]", cmdRefId, SPLCommonComponent.getStackTrace(sqlException));
			}catch(Exception exception) {
				wcfTranslatorDAO.updateCommandTransactionStatus(cmdTransId, orderRowId, "1", SPLCommonComponent.getStackTrace(exception));
				taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_ERROR, "[CRM Translator][handleWcfCommands][Catch]WCF Translator Exception", loggerCategory, exception);
				wcfTranslatorDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CRM Translator][handleWcfCommands][Catch]", cmdRefId, SPLCommonComponent.getStackTrace(exception));

				OrderTransactonDetail orderTransDtls = wcfTranslatorDAO.getOrderTransDtlsByRowId(cmdTransId, transId, srcTransId, orderRowId, loggerCategory);
				String source = orderTransDtls.getSource();
				
				if(ApplicationConstants.SOURCE_OSM.equalsIgnoreCase(source)) {
					// Added to send error response
					String tomResponse = "";
					try {
						tomResponse = splCommonComponent.createTomResponse(cmdTransId);
					} catch (Exception tomException) {
					}
	
					taLogger.log(srcTransId, transId, cmdTransId, ApplicationConstants.LOG_ERROR,"[CRM Translator][handleWcfCommands][Catch]tomResponse: "+ tomResponse, loggerCategory);
	
					OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
					
					//JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("jdbcDatabaseDAO");
					//String crmCorrId=jdbcDatabaseDAO.getCorrId(orderRowId);
					String crmCorrId = orderTransDtls.getCorrId();
					taLogger.log(srcTransId, transId, cmdTransId, ApplicationConstants.LOG_ERROR,"[CRM Translator][handleWcfCommands][Catch]CRM Correlation Id: "+crmCorrId, loggerCategory);
					orderTransactionSender.postMessage(tomResponse, crmCorrId, "TOMOrderProv");
				}

			}
		}

		/**
		 *
		 * @param systemName
		 * @param cmdName
		 * @param requestOrResponseFolder
		 * @param appendDotXml
		 * @return
		 */
		private String getFilePath(String systemName, String cmdName, String requestOrResponseFolder, String appendDotXml) {
			taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "Inside getFilePath method", loggerCategory);
			taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "filePath for " + cmdName, loggerCategory);
			String filePath = ApplicationConstants.FOLDER_ADAPTORS + systemName + requestOrResponseFolder + cmdName + appendDotXml;
			taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "filePath = " + filePath, loggerCategory);
			taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "getFilePath method called", loggerCategory);
			return filePath;

		}

		public String getCurrentValue(String xmlEleCurrValue,String paramValue){
			//taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][getCurrentValue]" , loggerCategory);
			//taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][getCurrentValue]Validating xmlEleCurrValue" , loggerCategory);
			if(xmlEleCurrValue.trim().equals(""))
			{
				xmlEleCurrValue =  paramValue;
				//taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[WcfTranslator]xmlEleCurrValue = " + xmlEleCurrValue, loggerCategory);
			}
			taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_DEBUG, "[CRM Translator][getCurrentValue]XmlEleCurrValue :" + xmlEleCurrValue , loggerCategory);
			return xmlEleCurrValue;

		}
		public String  getXmlElementName(String xmlEleType,String xmlEleName,String xmlEleParamName){

			//taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO,"[CRM Translator][getXmlElementName]" , loggerCategory);
			//taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO,"[CRM Translator][getXmlElementName]Validating  xmlEleType" , loggerCategory);
			if(PARAM_TYPE_RESOURCE.equalsIgnoreCase(xmlEleType) || PARAM_TYPE_MAINLINEITEMATTRIBUTE.equalsIgnoreCase(xmlEleType) || PARAM_TYPE_LINEITEMATTRIBUTE.equalsIgnoreCase(xmlEleType))
				//xmlEleName = "CURRENT_VALUE";
				xmlEleName = "VALUE";
			else if(PARAM_TYPE_WORKORDER.equalsIgnoreCase(xmlEleType))
				xmlEleName = "APPT_START_DT";
			else if(PARAM_TYPE_LINEITEM.equalsIgnoreCase(xmlEleType)) {
				xmlEleName = properties.getProperty(xmlEleName.toUpperCase());
				if((xmlEleName==null)||(xmlEleName.equals(""))) {
					xmlEleName=xmlEleParamName;
					taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO,"[CRM Translator][getXmlElementName]Property file not found or XmlEleName value is  '' " , loggerCategory);
				}
			}else if(ApplicationConstants.PARAM_TYPE_RESOURCE_PROPERTY.equalsIgnoreCase(xmlEleType)) {
				xmlEleName = "";
			}else{
				//taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[WcfTranslator]xmlEleParamName =" + xmlEleParamName, loggerCategory);
				xmlEleName = xmlEleParamName;
				//taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][getXmlElementName]xmlEleName =" + xmlEleName, loggerCategory);
				}

			taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_DEBUG,"[CRM Translator][getXmlElementName]XmlEleName :"+xmlEleName , loggerCategory);
			return xmlEleName;

		}

		private void setNodeValue(Document document, String paramName, String paramValue) throws SPLExceptionHandler {
			//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][setNodeValue]" + "paramName="+paramName +" paramValue="+ paramValue, loggerCategory);
			//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][setNodeValue]" + "paramName="+paramName +" paramValue="+ paramValue, loggerCategory);
			NodeList userNameList = document.getElementsByTagName(paramName);
			//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][setNodeValue]list length " + userNameList.getLength(), loggerCategory);
			int index=0;
			while(index <userNameList.getLength()){
				//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][setNodeValue]index: " + index, loggerCategory);
	        Node userNameNode = userNameList.item(index);
	        if(userNameNode!=null)

	        	userNameNode.setTextContent(paramValue);


	        else
	        	throw new SPLExceptionHandler("[WcfTranslator-setNodeValue-SPLExceptionHandler]    Element " +  paramName + " not available in XML document");
	        index++;
			}
			taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG, "[CRM Translator][setNodeValue]ParamName="+paramName +" , ParamValue="+ paramValue, loggerCategory);
		}
		
		private Document createOmDnetModuleRequest(Document wcfDocument, String cmdTransId, String systemName, String userName, String password, String accountType, DatabaseDAO wcfTranslatorDAO, String interfaceName) throws SQLException, ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, TransformerException, SPLExceptionHandler {
			String filePath = splCommonComponent.getFilePath(systemName, interfaceName, ApplicationConstants.APPEND_REQUEST, ApplicationConstants.SYSTEM_APPEND_REQ_DOTXML);
			taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][createOmDnetModuleMessage]Initializing Request Document", loggerCategory);
			requestDocument = splCommonComponent.getDocument(filePath);
			//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][createOmDnetModuleMessage]Converting Request Document to String", loggerCategory);
			String crmRequestMessage = splCommonComponent.convertDocumentToString(requestDocument);
			taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG, "[CRM Translator][createOmDnetModuleMessage]CRM Xml template request message is :"+crmRequestMessage, loggerCategory);
			taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO,"[CRM Translator][createOmDnetModuleMessage]Setting values for CRM parameters", loggerCategory);
			
			//setNodeValue(requestDocument, "crm:messageNameField",PARAM_MODULE_MESSAGE_NAME_FIELD);
			setNodeValue(requestDocument, "correlationID", cmdTransId);
			setNodeValue(requestDocument, "acctType", accountType);
			setNodeValue(requestDocument, "password", "#password#");
			setNodeValue(requestDocument, "userName", userName);
			setNodeValue(requestDocument, "operation", wcfTranslatorDAO.getParamValue(cmdTransId, ApplicationConstants.PARAMETER_OPERATION));
			//setNodeValue(requestDocument, "crm:operationStatusField", "");
			taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG, "[CRM Translator][createOmDnetModuleMessage]MessageNameField="+PARAM_MODULE_MESSAGE_NAME_FIELD+",AccountTypeField="+accountType+",UserNameField="+userName+",PasswordField="+password+",OperationField="+wcfTranslatorDAO.getParamValue(cmdTransId, ApplicationConstants.PARAMETER_OPERATION)+",OperationStatusField="+"", loggerCategory);
			
			NodeList nodeList = requestDocument.getElementsByTagName("arrayOfParamInfoType");
			Element arrayOfFuncParamSetField = (Element) nodeList.item(0);

			List<CommandTransDetails> cmdTransDetailsList = wcfTranslatorDAO.getWcfCmdTransDtls(cmdTransId, systemName);
			for(CommandTransDetails commandTransDetails : cmdTransDetailsList) {

				String xmlEleParamName = commandTransDetails.getParamName();
				Element funcParamsSetElement = SPLCommonComponent.addElement(requestDocument, arrayOfFuncParamSetField, "paramInfoType", "");

				String xmlEleType = commandTransDetails.getXmlEleType();
				String xmlEleName = commandTransDetails.getXmlEleName();
				String xmlEleCurrValue = commandTransDetails.getParamValue();
				//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO,"[CRM Translator][createOmDnetModuleMessage]Checking System Name", loggerCategory);
				taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][createOmDnetModuleMessage]Validating system name ", loggerCategory);
				taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG,"[CRM Translator][createOmDnetModuleMessage]System name is :"+systemName, loggerCategory);
				if(ApplicationConstants.SYSTEM_VMS.equals(systemName)) {
					xmlEleName = commandTransDetails.getParamName();
					xmlEleCurrValue = commandTransDetails.getParamValue();
				}else if(ApplicationConstants.SYSTEM_CRM.equals(systemName)) {

					//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][createOmDnetModuleMessage]Calling getCurrentValue method", loggerCategory);
					xmlEleCurrValue=getCurrentValue(xmlEleCurrValue,  commandTransDetails.getParamValue());
					//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][createOmDnetModuleMessage]Calling getXmlElementName method", loggerCategory);
					xmlEleName=getXmlElementName(xmlEleType, xmlEleName, xmlEleParamName);
				}
				if(xmlEleName==null)
					xmlEleName = "";
				
				SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramType", xmlEleType.toUpperCase());
				SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramID", commandTransDetails.getXmlEleId());
				if(ApplicationConstants.PARAM_TYPE_RESOURCE_PROPERTY.equalsIgnoreCase(xmlEleType))
					SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramParentID", commandTransDetails.getXmlParEleId());
				SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramName", xmlEleParamName);
				SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramColumn", xmlEleName);
				SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "currValue", xmlEleCurrValue);
				//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][createOmDnetModuleMessage]systemName: \n" + systemName, loggerCategory);
				//taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][createOmDnetModuleMessage]xmlEleName: \n" + xmlEleName, loggerCategory);
				//taLogger.log(srcTransId,transId,inputIdentifier,ApplicationConstants.LOG_INFO, "[CRM Translator][createOmDnetModuleMessage]xmlEleCurrValue: \n" + xmlEleCurrValue, loggerCategory);
				String xmlElePrevValue = commandTransDetails.getXmlElePrevValue();
				SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "prevValue", xmlElePrevValue);
				taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG,"[WcfTransltor][createOmDnetModuleMessage]Parameters for element '"+xmlEleName+"' are CurrentValueField="+xmlEleCurrValue+",ParamIDField="+commandTransDetails.getXmlEleId()+",ParamTypeField="+ xmlEleType+",PrevValueField="+ xmlElePrevValue, loggerCategory);

			}

			String returnCode = "";
			String returnMsg = "";
			//setNodeValue(requestDocument, "crm:returnCodeField", returnCode);
			//setNodeValue(requestDocument, "crm:returnMessageField", returnMsg);
			//setNodeValue(requestDocument, "crm:tranStatusField", ApplicationConstants.STATUS_NEW);
			//setNodeValue(requestDocument, "crm:versionField",PARAM_MODULE_VERSION_FIELD);
			taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_DEBUG,"[CRM Translator][createOmDnetModuleMessage]CorrelationIDField="+cmdTransId+",ReturnCodeField="+returnCode+",ReturnMessageField="+returnMsg+",TranStatusField="+ApplicationConstants.STATUS_NEW+",VersionField="+PARAM_MODULE_VERSION_FIELD, loggerCategory);
			taLogger.log(srcTransId,transId,inputIdentifier, ApplicationConstants.LOG_INFO, "[CRM Translator][createOmDnetModuleMessage]CRM parameters has been set", loggerCategory);

			return requestDocument;

		}

}
