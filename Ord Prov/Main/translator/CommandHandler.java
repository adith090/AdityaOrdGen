package com.m1.bcc.spl.translator;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.transport.http.CommonsHttpMessageSender;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import common.util.TALogger;


/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/11/2012					Ravikumar G				Created
 * 01/05/2012					Ravikumar G				Modified for Bug#352
 * 01/05/2012					Ravikumar G				Modified for Bug#376
 * 01/07/2012					Ravikumar G				Modified to implement webservice callback to Arbor, Gazelle & proximity
 * 01/07/2012					Ravikumar G				Modified to interface name change as SIMA
 * 01/07/2012					Ravikumar G				Bug#351 added username and password for IDS using the callback
 * 01/14/2013					Yohan					Modified to implement interface connection for PRS
 * 22/02/2013					Ravikumar G				Modified for logging cleanup
 * 26/02/2013					Ravikumar G				Bug#1209-Modified instance variable srcTransId, transId, rowId, cmdRefId and
 * 														interfaceName as local variable
 * 15/04/2013					Sudharsan				Changes made for logger category
 * 11/04/2013					Ravikumar G				Bug#1901-Changed Request translator method using DB config flag
 * 														for search param tag or replace param tag Enhancement
 * 17/04/2013					Billy Lim				Bug 1908. Move class variables used by @Async function into local function. This is to prevent class variables from by being overridden when called asynchronously
 * 03/05/2013					Sudharsan				Roll backing to revision 2474 for changes in splCommonComponent
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 08/05/2013					Ravikumar G 			Bug#2077 - Modified to get U2KRSP Authentication details from configuration table and removed duplicate message logging statements
 * 10/05/2013					Billy Lim				Bug 2076. Move class variables used by @Async function into local function. This is to prevent class variables from by being overridden when called asynchronously
 * 17/05/2013					Sudharsan				Implemented Engineering Authentication server 
 * 21/05/2013					Ravikumar G				Bug#2184 : Update the STUB flag in cmd_trans table
 * 22/05/2013					Sudharsan				Implemented Radius server
 * 11/07/2013					Sudharsan				Added username and password in request message of RIM adaptor
 * 19/08/2013					Sudharsan				Added new system name - Database
 * 10/09/2013					Ravikumar G				implementation for IN interface of GAZELLE
 * 25/09/2013					Ravikumar G				Moved RIM implementation to webservice callback
 * 24/10/2013					Ravikumar G				updated for GAZELLE CMD 004
 * 12/11/2013					Ravikumar G				Bug#20421 - to implement multithreading using thread poll task executor
 * 17/11/2013					Ravikumar G				Bug#20538- Separate timeout setting for all interfaces
 * 28/11/2013					Ravikumar G				Bug#20592- Changed for DB Adaptor design change and removed unused imports
 * 16/01/2014					Ravikumar G				Bug#23590 - to update request message in cmd_trans table
 * 17/01/2014					Ravikumar G				Bug#23710 - Updated cmd_trans column row_id type to number
 ******************************************************************************/

@SuppressWarnings("deprecation")
public class CommandHandler {

	private DatabaseDAO commandHandlerDAO;
	private ThreadPoolTaskExecutor taskExecutor;

	private TALogger taLogger = null;

	private String appendReqDotXml = ApplicationConstants.SYSTEM_APPEND_REQ_DOTXML;


	//Bug 2076 Start Refer to Modification History
	//private SPLCommonComponent splCommonComponent;
	//private String authTokenResponse;
	//private String responseMessage;

	//private String inputIdentifier="-1";
	//private boolean isReadAllowed;
	//private Document requestDocument = null;	
	//Bug 2076 End Refer to Modification History

	/**
	 *
	 * @param orderTransactionDAO
	 */
	public void setCommandHandlerDAO(DatabaseDAO commandHandlerDAO) {
		this.commandHandlerDAO = commandHandlerDAO;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	
	/**
	 *
	 * @param message
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws TransformerException
	 * @throws TransformerConfigurationException
	 */
	@Async
	@ServiceActivator
	public void translateCommandTransaction(Message<ArrayList<Map<String, Object>>> message) throws ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, Exception {
		translateCommandTransaction(message, false);
	}
	
	/**
	 *
	 * @param message
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws TransformerException
	 * @throws TransformerConfigurationException
	 */
	@Async
	@ServiceActivator
	public void translateCommandTransaction(Message<ArrayList<Map<String, Object>>> message, final boolean immediatelyFlag) throws ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, Exception {

			ArrayList<Map<String, Object>> commandDetailsList = (ArrayList<Map<String, Object>>) message.getPayload();
			for(final Map<String, Object> commandDetails : commandDetailsList) {
				String cmdRefId = (String)commandDetails.get(ApplicationConstants.COLUMN_CMD_REF_ID);
				if(this.commandHandlerDAO == null){this.commandHandlerDAO = (DatabaseDAO) BeanFactory.getBean("databaseDAO");}
				if(this.taskExecutor == null){this.taskExecutor = (ThreadPoolTaskExecutor) BeanFactory.getBean("wsTaskExecutor");}
				final CommandTransDetails cmdSystemDetails = commandHandlerDAO.getSystemDetails(cmdRefId);
				String systemName = cmdSystemDetails.getSystemName();
				final String techMethod = cmdSystemDetails.getTechMethod();
				if(ApplicationConstants.SYSTEM_CRM.equals(systemName)) {
					taskExecutor.submit(new Runnable() {
						
						@Override
						public void run() {
							WcfTranslator wcfTranslator = new WcfTranslator();
							wcfTranslator.handleWcfCommands(commandDetails, cmdSystemDetails, commandHandlerDAO, immediatelyFlag);
						}
					});
				}else {
					taskExecutor.submit(new Runnable() {
						
						@Override
						public void run() {
							if(ApplicationConstants.SYSTEM_TECH_METHOD_DB.equalsIgnoreCase(techMethod)) {
								DBAdaptorCmdHandler dbAdaptorCmdHandler = new DBAdaptorCmdHandler();
								dbAdaptorCmdHandler.processDbAdaptorCmd(commandDetails, cmdSystemDetails, commandHandlerDAO);
							}else {
								processCommand(commandDetails, cmdSystemDetails);
							}
						}
					});
				}
			}
	}

	/**
	 *
	 * @param isStub
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 * @throws SPLExceptionHandler
	 * @throws SQLException
	 * @throws SOAPException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	// Bug#352 - Modified to read from request file for PROXIMITY_002
	//Sudharsan : 20130415 : Bug 1894 : Added one more parameter for logger category : Start
	private String getAuthToken(final String rowId, final String transId, final String srcTransId, String cmdRefId, Properties properties,final String loggerCategory) throws FileNotFoundException, IOException, TransformerConfigurationException, TransformerException, SPLExceptionHandler, SQLException, SOAPException, ParserConfigurationException, SAXException, Exception {
	//Sudharsan : 20130415 : Bug 1894 : Added one more parameter for logger category : End
		//Bug 2076 Start Refer to Modification History
		final SPLCommonComponent splCommonComponent = new SPLCommonComponent(TALogger.getTALogger(), loggerCategory);
		splCommonComponent.setLogSrcTransId(srcTransId);
		splCommonComponent.setLogTransId(transId);
		splCommonComponent.setLogCmdRowId(rowId);
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][getAuthToken][cmdRefId=" + cmdRefId + "]Getting AuthToken", loggerCategory);
		//Bug 2076 End Refer to Modification History
		Properties propertiesKey = (Properties) BeanFactory.getBean("properties");
		final String key = propertiesKey.getProperty("KEY");
		// Ravi: changed from PROXIMITY to SIMA
		CommandTransDetails authTokenTransDetails = commandHandlerDAO.getAuthTokenParams(ApplicationConstants.SYSTEM_SIMA);
		if(authTokenTransDetails==null)
			throw new SPLExceptionHandler("Exception in getAuthToken: No records found for system " + ApplicationConstants.SYSTEM_SIMA);
		String userName = authTokenTransDetails.getUserName();
		String password = authTokenTransDetails.getPwd();
		String accountType = authTokenTransDetails.getAccountType();
		final String interfaceName = authTokenTransDetails.getInterfaceName();
		String systemName = authTokenTransDetails.getSystemName();
		String wsdlLocation = authTokenTransDetails.getLocation();
		//Bug 2076 Refer to Modification History
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][getAuthToken][cmdRefId=" + cmdRefId + "]System name :" + systemName, loggerCategory);
		String filePath = splCommonComponent.getFilePath(ApplicationConstants.SYSTEM_SIMA, interfaceName, ApplicationConstants.APPEND_REQUEST, appendReqDotXml);
		final Document authTokenReqDocument = splCommonComponent.getDocument(filePath);

		//Bug 2076 Refer to Modification History
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][getAuthToken][cmdRefId=" + cmdRefId + "]"+"Interface name :" + interfaceName, loggerCategory);

		String decryptedKey="";
    	//Comment next line
    	taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Password :"+password+"Key :"+key, loggerCategory);
    	decryptedKey=getDecryptedPwd(password,key);
    	//Comment next line
    	taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]DecryptedKey :"+decryptedKey, loggerCategory);

		setNodeValue(authTokenReqDocument, ApplicationConstants.PARAMETER_USERNAME, userName, srcTransId, transId, rowId,loggerCategory);
		setNodeValue(authTokenReqDocument, ApplicationConstants.PARAMETER_PASSWORD, decryptedKey, srcTransId, transId, rowId,loggerCategory);
		setNodeValue(authTokenReqDocument, ApplicationConstants.PARAMETER_ACCOUNTTYPE, accountType, srcTransId, transId, rowId,loggerCategory);
		//Bug 2076 Refer to Modification History
		taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[CommandHandler][getAuthToken][cmdRefId=" + cmdRefId + "]"+"Username :" + userName, loggerCategory);
		taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[CommandHandler][getAuthToken][cmdRefId=" + cmdRefId + "]"+"Password :" + decryptedKey, loggerCategory);
		taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[CommandHandler][getAuthToken][cmdRefId=" + cmdRefId + "]"+"Accounttype :" + accountType, loggerCategory);

		splCommonComponent.saveMessage(authTokenReqDocument, interfaceName, rowId, transId, appendReqDotXml);
		String authTokenReq = splCommonComponent.convertDocumentToString(authTokenReqDocument);
		//Bug 2076 Refer to Modification History
		taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[CommandHandler][getAuthToken][cmdRefId=" + cmdRefId + "]"+"AuthTokenReqDocument :"+authTokenReq, loggerCategory);
		boolean isStub = SPLCommonComponent.getStubbing(properties, systemName);
		taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][getAuthToken]Stub is : " + isStub, loggerCategory);

        String authToken = "";
		if(isStub) {
			String stubResponsePath = splCommonComponent.getFilePath(ApplicationConstants.SYSTEM_SIMA, interfaceName, ApplicationConstants.APPEND_RESPONSE, ApplicationConstants.SYSTEM_APPEND_RES_DOTXML);
			String responseMessage = splCommonComponent.getMessage(stubResponsePath);
			Document responseDocument = splCommonComponent.convertStringToXmlDocument(responseMessage);
			splCommonComponent.saveMessage(responseDocument, interfaceName, rowId, transId, ApplicationConstants.SYSTEM_APPEND_RES_DOTXML);
			commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);
			commandHandlerDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessage, interfaceName, false);
            NodeList userNameList = responseDocument.getElementsByTagName(ApplicationConstants.PARAMETER_AUTHTICKET);
            Node userNameNode = userNameList.item(0);
            authToken = userNameNode.getTextContent();
            taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][getAuthToken]"+"authToken " + authToken, loggerCategory);
		}else {
	        // Bug 352 - Modified webservice call with message callback to add soap action and soap envelope
			WebServiceTemplate wsTemplate = new WebServiceTemplate();
			wsTemplate.setDefaultUri(wsdlLocation);
			commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);
			//Bug 2076 Start Refer to Modification History
			String authTokenResponseCallBack = wsTemplate.sendAndReceive(new WebServiceMessageCallback() {
				//Bug 2076 End Refer to Modification History
				@Override
				public void doWithMessage(WebServiceMessage requestCallback) throws IOException, TransformerException {

					SoapMessage soapMessage = (SoapMessage) requestCallback;
					soapMessage.setDocument(authTokenReqDocument);
					String soapAction = splCommonComponent.getWSSoapAction(interfaceName);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][getAuthToken][interfaceName=" + interfaceName+"]Soapaction :" + soapAction, loggerCategory);
					soapMessage.setSoapAction(soapAction);
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					soapMessage.writeTo(byteArrayOutputStream);
					String request = new String(byteArrayOutputStream.toByteArray());
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO, "[CommandHandler][getAuthToken][interfaceName=" + interfaceName+"]Message in request callback :" + request, loggerCategory);
				}
				//Bug 2076 Start Refer to Modification History
			},  new WebServiceMessageExtractor<String>()  {
				@Override
				public String extractData(WebServiceMessage responseCallback) throws IOException, TransformerException {
					SoapMessage soapMessage = (SoapMessage) responseCallback;
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					soapMessage.writeTo(byteArrayOutputStream);
					String authTokenResponse = new String(byteArrayOutputStream.toByteArray());
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[CommandHandler][getAuthToken][interfaceName=" + interfaceName +"]Message in response callback :" + authTokenResponse, loggerCategory);
					return authTokenResponse;
				}
				//Bug 2076 End Refer to Modification History
			});
			//Bug 2076 Refer to Modification History
			Document responseDocument = splCommonComponent.convertStringToXmlDocument(authTokenResponseCallBack);
			NodeList authTokenList = responseDocument.getElementsByTagName(ApplicationConstants.PARAMETER_AUTHTICKET);
            Node authTokenNode = authTokenList.item(0);
            authToken = authTokenNode.getTextContent();
            taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][getAuthToken]"+ "authToken :" + authToken, loggerCategory);
			//Bug 2076 Refer to Modification History
			commandHandlerDAO.saveCommandResponse(rowId, cmdRefId, transId, authTokenResponseCallBack, interfaceName, false);
		}
		return authToken;
	}

	/**
	 *
	 * @param document
	 * @param paramName
	 * @param paramValue
	 * @param srcTransId
	 * @param transId
	 * @param rowId
	 * @throws SPLExceptionHandler
	 */
	//Sudharsan : 20130415 : Bug 1894 : Added one more parameter for logger category : Start
	private void setNodeValue(Document document, String paramName, String paramValue, String srcTransId, String transId, String rowId,String Log) throws SPLExceptionHandler {
		//Sudharsan : 20130415 : Bug 1894 : Added one more parameter for logger category : End
		taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][setNodeValue] "+"Param name :" + paramName + " , and Param value :" + paramValue,Log);
		NodeList userNameList = document.getElementsByTagName(paramName);
		int index=0;
		while(index <userNameList.getLength()){
			Node userNameNode = userNameList.item(index);
			if(userNameNode!=null)

				userNameNode.setTextContent(paramValue);


			else
				throw new SPLExceptionHandler("Element " +  paramName + " not available in XML document");
			index++;
		}
	}
	/**
	 *
	 * @param doc
	 * @param paramValue
	 * @param srcTransId
	 * @param transId
	 * @param rowId
	 * @return
	 */
	private Document setGazelleXmlValues(Document doc, String paramValue, String srcTransId, String transId, String rowId){

		try{
			//Bug 2076 Start Refer to Modification History
			SPLCommonComponent splCommonComponent = new SPLCommonComponent(TALogger.getTALogger(), ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
			splCommonComponent.setLogSrcTransId(srcTransId);
			splCommonComponent.setLogTransId(transId);
			splCommonComponent.setLogCmdRowId(rowId);
			//Bug 2076 End Refer to Modification History			
			String gazelleRequestMessage = splCommonComponent.convertDocumentToString(doc);
			taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[CommandHandler][setGazelleXmlValues]Request message : " + gazelleRequestMessage, "GAZELLElogging");

			StringTokenizer token = new StringTokenizer(paramValue, "=,|");
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][setGazelleXmlValues]Token : " + token, "GAZELLElogging");
			while (token.hasMoreTokens()) {

				String key1 = token.nextToken();
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][setGazelleXmlValues]Key1 : " + key1,"GAZELLElogging");

				String value1 = token.nextToken();
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][setGazelleXmlValues]Value1 : " + value1, "GAZELLElogging");

				String key2 = token.nextToken();
				taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[CommandHandler][setGazelleXmlValues]Key2 : " + key2, "GAZELLElogging");

				String value2 = token.nextToken();
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][setGazelleXmlValues]Value2 : " + value2, "GAZELLElogging");

				Element createTaskElement = doc.createElement("BundleSubscriptionInput");
				NodeList nodeList = doc.getElementsByTagName("ArrayOfBundleSubscriptionInput");
				Node heirarchy = nodeList.item(0);
				heirarchy.appendChild(createTaskElement);

				gazelleRequestMessage = splCommonComponent.convertDocumentToString(doc);

				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][setGazelleXmlValues]Request message: " + gazelleRequestMessage, "GAZELLElogging");

				Element dsElement = doc.createElement("BundleId");
				createTaskElement.appendChild(dsElement);
				dsElement.setTextContent(value1);

				Element rootElement = doc.createElement("Action");
				createTaskElement.appendChild(rootElement);
				rootElement.setTextContent(value2);
				String gazelleRequestMessage1 = splCommonComponent.convertDocumentToString(doc);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][setGazelleXmlValues]Request message :" + gazelleRequestMessage1, "GAZELLElogging");

			}
		}catch(Exception e){
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR, "[CommandHandler][setGazelleXmlValues]Exception :"+e, "GAZELLElogging");
		}
		return doc;
	}
	private String getDecryptedPwd(String password,String key) throws Exception
	{
		String decryptedKey="";
		if(password!=null && !password.trim().equals(""))
		{
			decryptedKey = SPLCommonComponent.Crypt(0, key, password);
		}
		return decryptedKey;
	}
	
	private void processCommand(Map<String, Object> commandDetails, CommandTransDetails cmdSystemDetails) {
		String srcTransId = "";
		String cmdRefId = "";
		String rowId = "";
		String transId="";
		String interfaceName="";
		String sqlCommandRequest="";
		//Sudharsan : 20130415 : Bug 1894 : Initializing local variable for this method translateCommandTransaction: Start
		String loggerCategory = ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING;
		//Sudharsan : 20130415 : Bug 1894 : Initializing local variable for this method translateCommandTransaction: End
		//Bug 2076 Start Refer to Modification History
		String inputIdentifier="-1";
		boolean isReadAllowed;
		Document requestDocument = null;
		taLogger = TALogger.getTALogger();
		try {
			final SPLCommonComponent splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
			//Bug 2076 End Refer to Modification History

			boolean isStub = false;
			//ArrayList<Map<String, Object>> commandDetailsList = (ArrayList<Map<String, Object>>) message.getPayload();
			Properties properties = SPLCommonComponent.getSystemStubProperty();
			//for(Map<String, Object> commandDetails : commandDetailsList) {
				if(commandDetails!=null) {
					isStub = false;

					cmdRefId = (String)commandDetails.get(ApplicationConstants.COLUMN_CMD_REF_ID);
					String systemName = cmdSystemDetails.getSystemName();
					int conTimeOut = 0;
					if(!"-".equals(cmdSystemDetails.getTimeOut()))
						conTimeOut = Integer.parseInt(cmdSystemDetails.getTimeOut());
					int readTimeOut = 0;
					if(!"-".equals(cmdSystemDetails.getReadTimeOut()))
						readTimeOut = Integer.parseInt(cmdSystemDetails.getReadTimeOut());
					loggerCategory =systemName.toUpperCase()+"logging";
					splCommonComponent.setLoggerCategory(loggerCategory);
					inputIdentifier = cmdRefId;
					rowId = "" + (BigDecimal)commandDetails.get(ApplicationConstants.COLUMN_ROW_ID);
					transId = (String)commandDetails.get(ApplicationConstants.COLUMN_TRANS_ID);
					srcTransId = (String)commandDetails.get(ApplicationConstants.COLUMN_SRC_TRANS_ID);

					splCommonComponent.setLogSrcTransId(srcTransId);
					splCommonComponent.setLogTransId(transId);
					splCommonComponent.setLogCmdRowId(rowId);

					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Command reference id :" + cmdRefId, loggerCategory);
					String status = (String)commandDetails.get(ApplicationConstants.COLUMN_STATUS);
					//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Status of command :" + status, loggerCategory);

					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Connection Time Out :" + conTimeOut, loggerCategory);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Read Time Out :" + readTimeOut, loggerCategory);

					String authToken = "";
					String systemTechMethod = cmdSystemDetails.getTechMethod();
					
					if(ApplicationConstants.SYSTEM_TECH_METHOD.equals(systemTechMethod)) {
						commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_RECEIVED);
					}

					String isAuthTokenRequired = cmdSystemDetails.getAuthTokenRequired();
					interfaceName = cmdSystemDetails.getInterfaceName();
					String wsdlLocation = cmdSystemDetails.getLocation();
					final String userName = cmdSystemDetails.getUserName();
					final String password = cmdSystemDetails.getPwd();
					String acctType=cmdSystemDetails.getAccountType();
					String searchParamTag = cmdSystemDetails.getSearchParamTag();
					String replaceParamTag = cmdSystemDetails.getReplaceParamTag();
					String requestMsgFlag = cmdSystemDetails.getRequestMsgFlag();
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Username is: "+userName+" , Password is: "+password, loggerCategory);
					isStub = SPLCommonComponent.getStubbing(properties, systemName);

					//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]System tech method :" + systemTechMethod, loggerCategory);
					//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]System name :" + systemName, loggerCategory);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Is auth token required :" + isAuthTokenRequired, loggerCategory);
					//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Interface name :" + interfaceName, loggerCategory);
					//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Wsdl location :" + wsdlLocation, loggerCategory);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Stub :" + isStub, loggerCategory);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]requestMsgFlag :" + requestMsgFlag, loggerCategory);
					//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]searchParamTag :" + searchParamTag, loggerCategory);
					//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]replaceParamTag :" + replaceParamTag, loggerCategory);

					/*if(ApplicationConstants.SYSTEM_CRM.equals(systemName)) {
						commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_RECEIVED);
						WcfTranslator wcfTranslator = new WcfTranslator();
						wcfTranslator.handleWcfCommands(commandDetails, cmdSystemDetails, isStub, commandHandlerDAO);
						continue;
					}*/
					List<CommandTransDetails> cmdTransDetailsList = commandHandlerDAO.getCmdTransDetails(rowId, null, srcTransId, transId,systemName);
					
					if(ApplicationConstants.SYSTEM_TECH_METHOD.equals(systemTechMethod)) {

						if(ApplicationConstants.AUTH_TOKEN_REQUIRED.equalsIgnoreCase(isAuthTokenRequired)) {
							authToken = getAuthToken(rowId, transId, srcTransId, cmdRefId, properties,loggerCategory);
							taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]authToken :" + authToken, loggerCategory);
						}

						

						Iterator<CommandTransDetails> cmdTransDetails = cmdTransDetailsList.iterator();
						isReadAllowed = true;
						String requestMessage = "";
						String cmdName = "";

						int cmdParamCount = 1;
						int totalCmdParamSize = cmdTransDetailsList.size();

						String filePath = splCommonComponent.getFilePath(systemName, interfaceName, ApplicationConstants.APPEND_REQUEST, appendReqDotXml);
						requestDocument = splCommonComponent.getDocument(filePath);
						requestMessage = splCommonComponent.convertDocumentToString(requestDocument);
						taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Request message :" + splCommonComponent.convertDocumentToString(requestDocument), loggerCategory);
						while(cmdTransDetails.hasNext()) {
							CommandTransDetails commandTransDetails = (CommandTransDetails) cmdTransDetails.next();
							if(commandTransDetails!=null) {
								String paramName = commandTransDetails.getParamName();
								String paramValue = commandTransDetails.getParamValue();
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][translateCommandTransaction]"+"PARAM NAME : " + paramName + " , PARAM VALUE : " + paramValue, loggerCategory);

								if(paramName.equalsIgnoreCase("ArrayOfBundleSubscriptionInput")){
									setGazelleXmlValues(requestDocument, paramValue, srcTransId, transId, rowId);
								}else {
									if(searchParamTag.equalsIgnoreCase("Y"))
										setNodeValue(requestDocument, paramName, paramValue, srcTransId, transId, rowId, loggerCategory);
									if(replaceParamTag.equalsIgnoreCase("Y"))
										requestMessage = requestMessage.replace("#"+paramName+"#", paramValue);
								}
								// Ravi: 20130411: Bug#1901: Added as part of search param tag or replace param tag Enhancement - End

								if(ApplicationConstants.SYSTEM_ARBOR.equals(systemName)) {

									setNodeValue(requestDocument, ApplicationConstants.PARAMETER_CREDENTIAL, authToken, srcTransId, transId, rowId,loggerCategory);
									if(paramName.equalsIgnoreCase(ApplicationConstants.SYSTEM_ARBOR_REQDATE))
									{
									}
								}else if(ApplicationConstants.SYSTEM_B2B.equals(systemName)) {

								}else if(ApplicationConstants.SYSTEM_BIS.equals(systemName)) {

								}else if (ApplicationConstants.SYSTEM_BBRR.equals(systemName)) {

								}else if(ApplicationConstants.SYSTEM_ENS.equals(systemName)) {

								}else if(ApplicationConstants.SYSTEM_ENTMSG.equals(systemName)) {

								}else if(ApplicationConstants.SYSTEM_GAZELLE.equals(systemName)) {



									if(ApplicationConstants.INTERFACE_GAZELLE_001.equals(interfaceName) ||
											ApplicationConstants.INTERFACE_GAZELLE_002.equals(interfaceName) ||
											ApplicationConstants.INTERFACE_GAZELLE_003.equals(interfaceName) ||
											ApplicationConstants.INTERFACE_GAZELLE_004.equals(interfaceName))
										setNodeValue(requestDocument, ApplicationConstants.PARAMETER_GAZELLE_AUTHTICKET, authToken, srcTransId, transId, rowId,loggerCategory);
									if(ApplicationConstants.INTERFACE_GAZELLE_005.equals(interfaceName))
										setNodeValue(requestDocument, ApplicationConstants.PARAMETER_GAZELLE_AUTH_TICKET, authToken, srcTransId, transId, rowId,loggerCategory);

								}else if(ApplicationConstants.SYSTEM_IMEI.equals(systemName)){
								}else if(ApplicationConstants.SYSTEM_IS.equals(systemName)) {
								}else if(ApplicationConstants.SYSTEM_MMA.equals(systemName)) {
								}else if(ApplicationConstants.SYSTEM_PRS.equals(systemName)) {

								}else if(ApplicationConstants.SYSTEM_PROXIMITY.equals(systemName)) {
									setNodeValue(requestDocument, ApplicationConstants.PARAMETER_CREDENTIAL, authToken, srcTransId, transId, rowId,loggerCategory);
								}else if(ApplicationConstants.SYSTEM_LOCKCUBE.equals(systemName)) {
								}else if(ApplicationConstants.SYSTEM_SDP.equals(systemName)) {}
							}
							else {
								taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]CommandTransDetails :" + commandTransDetails, loggerCategory);
							}
						}
						//requestMessage = removeEmptyTags(requestDocument);

						if(replaceParamTag.equalsIgnoreCase("Y"))
							requestDocument=splCommonComponent.convertStringToXmlDocument(requestMessage);

						requestDocument.normalize();

						if(ApplicationConstants.SYSTEM_PRS.equals(systemName)) {
							String namespaceMarker = "#splprshostaddress#";
							String rpsHostName = wsdlLocation.substring(0, (wsdlLocation.indexOf("/", wsdlLocation.indexOf("://")+ 3)));;
							requestDocument = splCommonComponent.UpdateSystemNamespace(requestDocument, namespaceMarker, rpsHostName);
							taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]"+ "Updated PRS host :" + rpsHostName, loggerCategory);
						}

						// Ravi: Modified for replace < and > with &lt; and &gt;
						if(ApplicationConstants.SYSTEM_PROXIMITY.equals(systemName)) {

							if(ApplicationConstants.INTERFACE_PROXIMITY_003.equalsIgnoreCase(interfaceName))
							{
								setNodeValue(requestDocument, ApplicationConstants.PARAMETER_CREDENTIAL, authToken, srcTransId, transId, rowId,loggerCategory);
							}
							String delimiter = "inputXML";
							String inputXml = splCommonComponent.nodeToString(requestDocument, delimiter);
							inputXml = inputXml.replaceAll("&lt;"+delimiter+"&gt;", "").replaceAll("&lt;"+"/"+delimiter+"&gt;", "");
							Node inputNode = requestDocument.getElementsByTagName(delimiter).item(0);
							if(inputNode != null)
							{
								while(inputNode.hasChildNodes()) {
									inputNode.removeChild(inputNode.getFirstChild());
								}
								inputNode.setTextContent(StringEscapeUtils.unescapeXml(inputXml));
							}
						}else if(ApplicationConstants.SYSTEM_GAZELLE.equals(systemName)) {
							//Ravi : 20130910 : changed delimiter to CommandInput and added CDATA for the data roaming cap commands : Start
							//String delimiter = "input";
							String delimiter = "CommandInput";
							String inputXml = splCommonComponent.nodeToString(requestDocument, delimiter);
							inputXml = inputXml.replaceAll("&lt;"+delimiter+"&gt;", "").replaceAll("&lt;"+"/"+delimiter+"&gt;", "");
							Node inputNode = requestDocument.getElementsByTagName(delimiter).item(0);

							if(inputNode != null)
							{
								while(inputNode.hasChildNodes()) {
									inputNode.removeChild(inputNode.getFirstChild());
								}

								//inputNode.setTextContent(StringEscapeUtils.unescapeXml(inputXml));
								inputXml = StringEscapeUtils.unescapeXml(inputXml);
								CDATASection cdataSection = requestDocument.createCDATASection(inputXml);
								inputNode.appendChild(cdataSection);
								
							}
							//Ravi : 20130910 : changed delimiter to CommandInput and added CDATA for the data roaming cap commands : Start
						}else if(ApplicationConstants.SYSTEM_IDS.equals(systemName)) {

							//Ravi: changed for Bug#592
							String req = splCommonComponent.convertDocumentToString(requestDocument);
							req = req.replaceAll("&amp;amp;", "&amp;");
							requestDocument = splCommonComponent.convertStringToXmlDocument(req);
						}
						if(ApplicationConstants.SYSTEM_U2KRSP.equals(systemName) || ApplicationConstants.SYSTEM_VMS.equals(systemName)){
							if(ApplicationConstants.SYSTEM_VMS.equals(systemName)) {
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Setting values for Username,PWD,Account type, Correlation id", loggerCategory);
								setNodeValue(requestDocument, "crm:userNameField", userName, srcTransId, transId, rowId,loggerCategory);
								setNodeValue(requestDocument, "crm:passwordField", "#password#", srcTransId, transId, rowId,loggerCategory);
								setNodeValue(requestDocument, "crm:acctTypeField", acctType, srcTransId, transId, rowId,loggerCategory);
								setNodeValue(requestDocument, "crm:correlationIDField", rowId, srcTransId, transId, rowId,loggerCategory);
							}
							requestMessage = splCommonComponent.convertDocumentToString(requestDocument);
						}else {
							requestMessage = splCommonComponent.convertDocumentToString(requestDocument);
						}
						//splCommonComponent.saveMessage(requestDocument, interfaceName, rowId, transId, appendReqDotXml);
						taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Final request message after setting values :" + requestMessage, loggerCategory);
						// declared as final to use in inner class
						final String logSrcTransId = srcTransId;
						final String logTransId = transId;
						final String logRowId = rowId;
						taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Stub is :"+isStub, loggerCategory);
						//Ravi : 20130521 : Bug 2184 : Update the STUB flag in cmd_trans : Start
						//commandHandlerDAO.updateStubbingStatus(srcTransId,  transId, rowId, isStub, loggerCategory);
						commandHandlerDAO.updateCmdTrans(srcTransId,  transId, rowId, requestMsgFlag, requestMessage, isStub, loggerCategory);
						//Ravi : 20130521 : Bug 2184 : Update the STUB flag in cmd_trans : End
						if(isStub) {
							splCommonComponent.saveMessage(requestDocument, interfaceName, rowId, transId, appendReqDotXml);
							String stubResponsePath = splCommonComponent.getFilePath(systemName, interfaceName, ApplicationConstants.APPEND_RESPONSE, ApplicationConstants.SYSTEM_APPEND_RES_DOTXML);
							String responseMessage = splCommonComponent.getMessage(stubResponsePath);
							Document responseDocument = splCommonComponent.convertStringToXmlDocument(responseMessage);
							splCommonComponent.saveMessage(responseDocument, interfaceName, rowId, transId, ApplicationConstants.SYSTEM_APPEND_RES_DOTXML);
							commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);
							commandHandlerDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessage, interfaceName, false);
						}else {
							Properties propertiesKey = (Properties) BeanFactory.getBean("properties");
							final String key = propertiesKey.getProperty("KEY");
							final String decryptedKey=getDecryptedPwd(password,key);
							//Sudharsan : 20130415 : Bug 1894 : To use in inner class we have declared final variable: Start
							final String loggerCategoryWebservice=loggerCategory;
							//Sudharsan : 20130415 : Bug 1894 : To use in inner class we have declared final variable: End
							if(ApplicationConstants.SYSTEM_BBRR.equals(systemName) || ApplicationConstants.SYSTEM_ARBOR.equals(systemName)
									|| ApplicationConstants.SYSTEM_GAZELLE.equals(systemName) || ApplicationConstants.SYSTEM_PROXIMITY.equals(systemName)
									|| ApplicationConstants.SYSTEM_U2KRSP.equals(systemName) || ApplicationConstants.SYSTEM_PRS.equals(systemName)
									|| ApplicationConstants.SYSTEM_VMS.equals(systemName) || ApplicationConstants.SYSTEM_RIM.equals(systemName)) {
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Stub is "+isStub +" for "+systemName, loggerCategory);
								WebServiceTemplate template = new WebServiceTemplate();

								HttpClient httpClient = new HttpClient();
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Setting timeout", loggerCategory);
								httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(conTimeOut);
								httpClient.getHttpConnectionManager().getParams().setSoTimeout(readTimeOut);
								int Gettimeout = httpClient.getHttpConnectionManager().getParams().getSoTimeout();
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Getting timeout ", loggerCategory);
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Timeout set is :" +Gettimeout  , loggerCategory);
								CommonsHttpMessageSender commonsHttpMessageSender = new CommonsHttpMessageSender(httpClient);
								template.setMessageSender(commonsHttpMessageSender);
								template.setDefaultUri(wsdlLocation);
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Wsdl location :"+wsdlLocation, loggerCategory);
								final String soapAction = splCommonComponent.getWSSoapAction(interfaceName);
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][translateCommandTransaction]Soapaction :" + soapAction, loggerCategory);
								commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);

								//Bug 2076 Start Refer to Modification History
								final Document requestDocumentCallBack = requestDocument;	
								String responseMessageFromCallBack = template.sendAndReceive(new WebServiceMessageCallback() {
								//Bug 2076 End Refer to Modification History
										   // Ravi: Bug#376 Modified for save command response
				                           @Override
				                           public void doWithMessage(WebServiceMessage requestCallback) throws IOException, TransformerException {
				                        	   taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Sending webservice call back ", loggerCategoryWebservice);
												//Bug 2076 Start Refer to Modification History
												String requestMessage = splCommonComponent.convertDocumentToString(requestDocumentCallBack);
												//Bug 2076 End Refer to Modification History

				                        	   //Comment next line
				                        	   taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Password :"+password+"Key :"+key, loggerCategoryWebservice);
				                        	   taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]RequestMessage :"+requestMessage, loggerCategoryWebservice);


				                        	   //Comment next line
				                        	   taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]DecryptedKey :"+decryptedKey, loggerCategoryWebservice);
				                        	   // Ravi: 20130507: Bug#2077: Added for U2KRSP authentication details to set in header - Start
				                        	   if(requestMessage.contains("#username#")) {
				                        		   requestMessage=requestMessage.replaceAll("#username#", userName);
				                        	   }
				                        	   // Ravi: 20130507: Bug#2077: Added for U2KRSP authentication details to set in header - End
				                        	   if(requestMessage.contains("#password#"))
				                        	   {
				                        	   requestMessage=requestMessage.replaceAll("#password#", decryptedKey);
				                        	   }
				                        	   taLogger.log(logSrcTransId,logTransId,logRowId,ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Message in request callback :" + requestMessage, loggerCategoryWebservice);
				                               SoapMessage soapMessage = (SoapMessage) requestCallback;
				                               //Bug 2076 Start Refer to Modification History
				                               Document requestDocumentWSCallBack = null;
				                               try {
				                            	   requestDocumentWSCallBack=splCommonComponent.convertStringToXmlDocument(requestMessage);
				                               //Bug 2076 End Refer to Modification History
				                               } catch (ParserConfigurationException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
				                               } catch (SAXException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
				                               }
				                               String requestMessageConverted = splCommonComponent.convertDocumentToString(requestDocumentWSCallBack);
				                               taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Request document :"+requestDocumentWSCallBack, loggerCategoryWebservice);
				                               taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Request message converted from document  :"+requestMessageConverted, loggerCategoryWebservice);
				                               taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Soap message :"+soapMessage, loggerCategoryWebservice);
				                               soapMessage.setDocument(requestDocumentWSCallBack);
				                               soapMessage.setSoapAction(soapAction);
				                               taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Soap action :"+soapAction, loggerCategoryWebservice);
				                           }
									//Bug 2076 Start Refer to Modification History
								}, new WebServiceMessageExtractor<String>() {
									@Override
									public String extractData(WebServiceMessage responseCallback) throws IOException, TransformerException {
										taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Receiving response callback ", loggerCategoryWebservice);
										SoapMessage soapMessage = (SoapMessage) responseCallback;
										taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Soap message :"+soapMessage , loggerCategoryWebservice);
										ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
										soapMessage.writeTo(byteArrayOutputStream);
										taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Byte array output stream :"+byteArrayOutputStream, loggerCategoryWebservice);
										String responseMessageWSCallBack = new String(byteArrayOutputStream.toByteArray());
										taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Message in response callback :" + responseMessageWSCallBack, loggerCategoryWebservice);
										return responseMessageWSCallBack;
				                           }
				                     });
								commandHandlerDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessageFromCallBack, interfaceName, false);
								//Bug 2076 End Refer to Modification History
							}

							//Added for Bug-351[START]
							else if((interfaceName.equalsIgnoreCase("IDS_001"))&&(systemName.equalsIgnoreCase("IDS"))) {
								// Ravi: changed for the Bug#351 added username and password for Https using the callback

								HttpClient httpClient = new HttpClient();
								HttpState httpState = httpClient.getState( );


								//Comment next line
								taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Password :"+password+"Key :"+key, loggerCategory);

								//Comment next line
								taLogger.log(logSrcTransId,logTransId,logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]DecryptedKey :"+decryptedKey, loggerCategory);
								org.apache.commons.httpclient.Credentials credentials = new org.apache.commons.httpclient.UsernamePasswordCredentials( userName, decryptedKey );
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Credentials : "+ credentials, loggerCategory);
								httpState.setCredentials( null, null, credentials );

								httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(conTimeOut);
								httpClient.getHttpConnectionManager().getParams().setSoTimeout(readTimeOut);
								int Gettimeout = httpClient.getHttpConnectionManager().getParams().getSoTimeout();
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Getting timeout ", loggerCategory);
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Timeout : "+Gettimeout, loggerCategory);

								@SuppressWarnings("deprecation")
								CommonsHttpMessageSender messageSender = new CommonsHttpMessageSender(httpClient);
								WebServiceTemplate template = new WebServiceTemplate();
								template.setMessageSender(messageSender);
								template.setDefaultUri(wsdlLocation);
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Wsdl location is : "+ wsdlLocation, loggerCategory);
								final String soapAction = splCommonComponent.getWSSoapAction(interfaceName);
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Soapaction : " + soapAction, loggerCategory);

								commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);
								//Bug 2076 Start Refer to Modification History
								final Document requestDocumentCallBack = requestDocument;	
								String responseMessageCallBack = template.sendAndReceive(new WebServiceMessageCallback() {

			                           @Override
			                           public void doWithMessage(WebServiceMessage requestCallback) throws IOException, TransformerException {
			                        	   String requestMessage = splCommonComponent.convertDocumentToString(requestDocumentCallBack);
			                        	   taLogger.log(logSrcTransId, logTransId, logRowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Request message :" + requestMessage, loggerCategoryWebservice);
			                        	   taLogger.log(logSrcTransId, logTransId, logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Sending request message..", loggerCategoryWebservice);
			                               SoapMessage soapMessage = (SoapMessage) requestCallback;
			                               soapMessage.setDocument(requestDocumentCallBack);
			                               soapMessage.setSoapAction(soapAction);
			                           }
								},  new WebServiceMessageExtractor<String>()  {
									@Override
									public String extractData(WebServiceMessage responseCallback) throws IOException, TransformerException {
										SoapMessage soapMessage = (SoapMessage) responseCallback;
										ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
										soapMessage.writeTo(byteArrayOutputStream);
										String responseMessageWSCallBack = new String(byteArrayOutputStream.toByteArray());
			           					taLogger.log(logSrcTransId, logTransId, logRowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Receiving response message..", loggerCategoryWebservice);
			           					taLogger.log(logSrcTransId, logTransId, logRowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Message in Response callback :" + responseMessageWSCallBack, loggerCategoryWebservice);
										return responseMessageWSCallBack;
									}
			                     });
								commandHandlerDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessageCallBack, interfaceName, false);
								//Bug 2076 End Refer to Modification History
							}


							else {
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Row id is :"+ rowId, loggerCategory);


								//Comment next line
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][translateCommandTransaction]Password :"+password +"Key :"+key, loggerCategory);
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][translateCommandTransaction]RequestMessage :"+requestMessage, loggerCategory);
								if(requestMessage.contains("#password#")){
									requestMessage=requestMessage.replaceAll("#password#", decryptedKey);
								}
								//Comment next line
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][translateCommandTransaction]Decrypted Key :"+decryptedKey, loggerCategory);
								//Comment next line
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][translateCommandTransaction]New Request Message after replacing with decrypted password :"+requestMessage, loggerCategory);


								StreamSource request = new StreamSource(new StringReader(requestMessage));
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Request is :"+ requestMessage, loggerCategory);
								StreamResult response = new StreamResult(new StringWriter());
								WebServiceTemplate template = new WebServiceTemplate();

						        HttpClient httpClient = new HttpClient();
						        taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[CommandHandler][translateCommandTransaction]Setting timeout", loggerCategory);
						        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(conTimeOut);
								httpClient.getHttpConnectionManager().getParams().setSoTimeout(readTimeOut);
								int Gettimeout = httpClient.getHttpConnectionManager().getParams().getSoTimeout();
								taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Timeout set  is :"+Gettimeout, loggerCategory);
								CommonsHttpMessageSender commonsHttpMessageSender = new CommonsHttpMessageSender(httpClient);
								template.setMessageSender(commonsHttpMessageSender);

						        // Updated status as sent in cmd_trans
						        commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);
						        taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Wsdl location :"+wsdlLocation+" , Request :"+request+" , Response :"+response, loggerCategory);
						        template.sendSourceAndReceiveToResult(wsdlLocation, request, response);
						        String responseMessage = response.getWriter().toString();
						        taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[CommandHandler][translateCommandTransaction]Request message is :"+ requestMessage, loggerCategory);
						        taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[CommandHandler][translateCommandTransaction]Response message is :"+ responseMessage, loggerCategory);
						        commandHandlerDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessage, interfaceName, false);
							}
						}
					}
					
				}
			//}
		}catch(FileNotFoundException fileNotFoundException) {
			commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_ERROR);
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][translateCommandTransaction[Catch Block-FileNotFoundException]]"+ "FileNotFoundException", loggerCategory, fileNotFoundException);
			commandHandlerDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CommandHandler][translateCommandTransaction][Catch Block-FileNotFoundException]", cmdRefId, SPLCommonComponent.getStackTrace(fileNotFoundException));
		}catch(IOException ioException) {
			commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_ERROR);
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][translateCommandTransaction][Catch Block-IOException]"+ "IOException", loggerCategory, ioException);
			commandHandlerDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CommandHandler][translateCommandTransaction][Catch Block-IOException]", cmdRefId, SPLCommonComponent.getStackTrace(ioException));
		}catch(TransformerConfigurationException transformerConfigurationException) {
			commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_ERROR);
			taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_ERROR,"[CommandHandler][translateCommandTransaction][Catch Block-TransformerConfigurationException]"+ "transformerConfigurationException", loggerCategory, transformerConfigurationException);
			commandHandlerDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CommandHandler][translateCommandTransaction][Catch Block-TransformerConfigurationException]", cmdRefId, SPLCommonComponent.getStackTrace(transformerConfigurationException));
		}catch(TransformerException transformerException) {
			commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_ERROR);
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][translateCommandTransaction][Catch Block-TransformerException]"+ "transformerException", loggerCategory, transformerException);
			commandHandlerDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CommandHandler][translateCommandTransaction][Catch Block-TransformerException]", cmdRefId, SPLCommonComponent.getStackTrace(transformerException));
		}catch(SPLExceptionHandler splExceptionHandler) {
			commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_ERROR);
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][translateCommandTransaction][Catch Block-SPLExceptionHandler]"+ "splExceptionHandler", loggerCategory, splExceptionHandler);
			commandHandlerDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CommandHandler][translateCommandTransaction][Catch Block-SPLExceptionHandler]", cmdRefId, SPLCommonComponent.getStackTrace(splExceptionHandler));
		}catch (SQLException sqlException) {
			commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_ERROR);
			taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][translateCommandTransaction][Catch Block-SQLException]"+ "SQLException", loggerCategory, sqlException);
			commandHandlerDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CommandHandler][translateCommandTransaction][Catch Block-SQLException]", cmdRefId, SPLCommonComponent.getStackTrace(sqlException));
		}catch (SoapFaultClientException soapfault) {
			commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_ERROR);
			//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][translateCommandTransaction][Catch Block-SoapFaultClientException]", loggerCategory, soapfault);
			try {
//
				SPLCommonComponent commonComponent = new SPLCommonComponent(taLogger, loggerCategory);
				String soapFaultMessage = soapfault.getMessage();
				taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_ERROR,"[CommandHandler][try block-SoapFaultClientException]"+ "Soap fault message :" + soapFaultMessage, loggerCategory, soapfault);
				//Bug 2076 Start Refer to Modification History
				//responseMessage = splCommonComponent.getMessage(ApplicationConstants.U2KRSP_ERROR_RESPONSE_PATH);
				String responseMessage = commonComponent.getMessage(ApplicationConstants.U2KRSP_ERROR_RESPONSE_PATH);
				//Bug 2076 End Refer to Modification History
				//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][try block-SoapFaultClientException]"+ "Response message :" + responseMessage, loggerCategory, soapfault);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][try block-SoapFaultClientException]"+ "Response message :" + responseMessage, loggerCategory);
				Document responseDocument = commonComponent.convertStringToXmlDocument(responseMessage);

				//taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_ERROR,"[CommandHandler][try block-SoapFaultClientException]"+ "Responsedocument :" + responseDocument, loggerCategory, soapfault);
				setNodeValue(responseDocument, "errorDescription", soapFaultMessage, srcTransId, transId, rowId,loggerCategory);

				//splCommonComponent.saveMessage(responseDocument, interfaceName, rowId, transId, ApplicationConstants.SYSTEM_APPEND_RES_DOTXML);
				commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);

				responseMessage=commonComponent.convertDocumentToString(responseDocument);
				commandHandlerDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessage, interfaceName, false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			commandHandlerDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CommandHandler][translateCommandTransaction]", cmdRefId, SPLCommonComponent.getStackTrace(soapfault));
		}
		catch(Exception exception) {

			commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_ERROR);
			//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][translateCommandTransaction][catch block]"+ "Exception", loggerCategory, exception);
			commandHandlerDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CommandHandler][translateCommandTransaction]", cmdRefId, SPLCommonComponent.getStackTrace(exception));

			try {
				SPLCommonComponent commonComponent = new SPLCommonComponent(taLogger, loggerCategory);
				String exceptionMessage = exception.getMessage();
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][translateCommandTransaction][catch block]"+ "Exception message is :" + exceptionMessage, loggerCategory, exception);
				//Bug 2076 Start Refer to Modification History
				//responseMessage = splCommonComponent.getMessage(ApplicationConstants.U2KRSP_ERROR_RESPONSE_PATH);
				String responseMessage = commonComponent.getMessage(ApplicationConstants.U2KRSP_ERROR_RESPONSE_PATH);
				//Bug 2076 End Refer to Modification History
				//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR, "[CommandHandler][translateCommandTransaction][catch block]"+"Response message :" + responseMessage, loggerCategory, exception);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR, "[CommandHandler][translateCommandTransaction][catch block]"+"Response message :" + responseMessage, loggerCategory);
				Document responseDocument = commonComponent.convertStringToXmlDocument(responseMessage);
	
				//taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_ERROR,"[CommandHandler][translateCommandTransaction][catch block]"+ "Response document :" + responseDocument, loggerCategory, exception);
				setNodeValue(responseDocument, "errorDescription", exceptionMessage, srcTransId, transId, rowId,loggerCategory);
	
				commonComponent.saveMessage(responseDocument, interfaceName, rowId, transId, ApplicationConstants.SYSTEM_APPEND_RES_DOTXML);
				commandHandlerDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);
	
				responseMessage=commonComponent.convertDocumentToString(responseDocument);
				commandHandlerDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessage, interfaceName, false);
				commandHandlerDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "[CommandHandler][translateCommandTransaction]", cmdRefId, SPLCommonComponent.getStackTrace(exception));
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}
