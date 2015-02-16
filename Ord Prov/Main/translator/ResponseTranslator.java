package com.m1.bcc.spl.translator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.instructor.CommandParamRefDtls;
import com.m1.bcc.spl.instructor.CommandParameters;
import com.m1.bcc.spl.instructor.HandlerVariables;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.model.OrderTransactonDetail;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.sender.OrderTransactionSender;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 22/02/2013					Ravikumar G				Modified for logging cleanup
 * 15/04/2013					Sudharsan				Changes made for logger category
 * 17/04/2013					Billy Lim				Bug 1908. Move class variables used by @Async function into local function. This is to prevent class variables from by being overridden when called asynchronously
 * 23/04/2013					Sudharsan				Added 'responseCode' parameter 
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 09/05/2013					Ravikumar G				Bug#2097: Added for mockup error reponse change for interface GAZELLE_006
 * 17/05/2013					Sudharsan				Implemented Engineering Authentication server
 * 22/05/2013					Sudharsan				Implemented Radius server
 * 27/05/2013   				Sudharsan				Implemented ADS 
 * 05/06/2013   				Sudharsan				Implemented SAS
 * 07/06/2013					Ravikumar G				Bug#2286: no task response required for CRM_001 for stubbing, implemented auto complete
 * 25/06/2013					Sudharsan				Implemented RIM SIM Exchange error handling logic
 * 08/07/2013					Sudharsan				Implemented checking error code with the response code configured in table
 * 11/07/2013					Sudharsan				Added one more interface for RIM Adaptor 
 * 19/08/2013					Sudharsan				Added new system name - Database for implementing FMS,SINGTEL,SMARTROAM,STARHUB,WAP
 * 25/09/2013					Ravikumar G				xpath for error code and description same for all RIM interface
 * 24/10/2013					Ravikumar G				updated for GAZELLE CMD 004
 * 06/11/2013					Ravikumar G				Bug#20397: updated crm interface xpath
 * 12/11/2013					Ravikumar G				Bug#20421 - to implement multithreading using thread poll task executor
 * 29/11/2013					Ravikumar G				Bug#20592- Changed for DB Adaptor design change
 * 06/12/2013					Ravikumar G				Bug#21337 - updated response as success for Task response
 * 31/12/2013					Ravikumar G				Bug#22498 - Updated order_trans_dtls row id type from string to integer
 * 16/01/2014					Ravikumar G				Bug#23590 - removed SPL_FLAG column from cmd_trans
 * 17/01/2014					Ravikumar G				Bug#23710 - Updated cmd_trans column row_id type to number
 * 23/01/2014					Ravikumar G				Bug#24043 - Updated for column row_id type to number
 * 27/01/2014					Ravikumar G				Bug#23086 - added condition if source=OSM then send tomresponse
 * 31/01/2014					Ravikumar G				Bug#24614 - Updated to handle OPCO/PS billing response
 * 03/02/2014					Ravikumar G				Bug#24615 - Updated Http interface return code and message
 ******************************************************************************/

public class ResponseTranslator {

	private JdbcDatabaseDAO jdbcDatabaseDAO;
	private DatabaseDAO responseTranslatorDAO;
	private TALogger taLogger;
	private ThreadPoolTaskExecutor taskExecutor;
	
	private OrderTransactionSender orderTransactionSender;

	// For the B2B adaptors
	private String STATUS_B2B_SUCCESS = "SUCCESS";


	// For U2KOPCO
	public final static String EOF_U2KOPCO = ";";
	public final static String responseResultKeyword_U2KOPCO = "EN=";
	public final static String responseDescKeyword_U2KOPCO = "ENDESC=";


	public void setJdbcDatabaseDAO(JdbcDatabaseDAO jdbcDatabaseDAO) {
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	
	public void setResponseTranslatorDAO(DatabaseDAO responseTranslatorDAO) {
		this.responseTranslatorDAO = responseTranslatorDAO;
	}

	public void setOrderTransactionSender(OrderTransactionSender orderTransactionSender) {
		this.orderTransactionSender = orderTransactionSender;
	}

	@Async
	public void handleCommandResponse(org.springframework.integration.Message<ArrayList<Map<String, Object>>> responseMessage) throws IOException {
		ArrayList<Map<String, Object>> responseMessageList = responseMessage.getPayload();
		if(this.taskExecutor == null){this.taskExecutor = (ThreadPoolTaskExecutor) BeanFactory.getBean("resTaskExecutor");}
		for(final Map<String, Object> responseDetails : responseMessageList) {
			taskExecutor.submit(new Runnable() {
				
				@Override
				public void run() {
					processCRMCommand(responseDetails);
				}
			});
		}
	}

	public String evaluateXPath(XPath xpath, Document doc, String expr)
			throws XPathExpressionException {

		XPathExpression exp = xpath.compile(expr);
		String xmlDetails = (String) exp.evaluate(doc, XPathConstants.STRING);
		return xmlDetails;

	}
	
	private void processCRMCommand(Map<String, Object> responseDetails) {
		taLogger = TALogger.getTALogger();
		//Sudharsan : 20130415 : Bug 1894 : Initializing local variable for this method handleCommandResponse : Start
		String loggerCategory = ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING;
		//Sudharsan : 20130415 : Bug 1894 : Initializing local variable for this method handleCommandResponse : End
		taLogger.log("", ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]"+ "Initializing variables for Response ", loggerCategory);

		//Bug 1908 Start Refer to Modification History
		String inputIdentifier="";
		SPLCommonComponent splCommonComponent = null;
		//Bug 1908 End Refer to Modification History	
		String orderTransRowId="";
		String cmdRefId = "";
		String transId = "";
		String responseXml = "";
		String interfaceName = "";
		String corrId = "";
		String responseCode = "";
		String responseDescription = "";
		String cmdRowId = "";
		String tomResponse = "";
		String systemName;
		String srcTransId="";
		String source = "";
		

		try {
			splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO ,"[ResponseTranslator][handleCommandResponse]Initializing response messages in list", loggerCategory);

			if(this.responseTranslatorDAO == null){this.responseTranslatorDAO = (DatabaseDAO) BeanFactory.getBean("databaseDAO");}
			
				responseCode="";
				responseDescription="";
				cmdRefId = (String)responseDetails.get(ApplicationConstants.COLUMN_CMD_REF_ID);
				transId = (String)responseDetails.get(ApplicationConstants.COLUMN_TRANS_ID);
				responseXml = (String)responseDetails.get(ApplicationConstants.COLUMN_RESPONSE_XML);
				interfaceName = (String)responseDetails.get(ApplicationConstants.COLUMN_INTERFACE_NAME);
				cmdRowId = "" + (BigDecimal)responseDetails.get(ApplicationConstants.COLUMN_CMD_ROW_ID);
				systemName = responseTranslatorDAO.getSystemName(cmdRefId);
				//passing transId for logging
				srcTransId=jdbcDatabaseDAO.getCmdTransDetails(cmdRowId, transId);
				
				//Getting the success and error codes specific to given command 
				List<Map<String, Object>> listCodes=  jdbcDatabaseDAO.getSplHandlingParams(cmdRefId);
							
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[ResponseTranslator][handleCommandResponse]Validating the system name for initializing logger category", loggerCategory);
				if (systemName.equalsIgnoreCase("IL"))
				{
					loggerCategory="ILresponseadaptor"+"logging";
				}
				else
				{
					loggerCategory =systemName.toUpperCase()+"logging";
				}
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG, "[ResponseTranslator][handleCommandResponse]LoggerCategory assigned is :"+loggerCategory, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,  "[ResponseTranslator][handleCommandResponse]Setting values for logging has started", loggerCategory);
				splCommonComponent.setLoggerCategory(loggerCategory);
				splCommonComponent.setLogSrcTransId(srcTransId);
				splCommonComponent.setLogTransId(transId);
				splCommonComponent.setLogCmdRowId(cmdRowId);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG, "[ResponseTranslator][handleCommandResponse]Setting values for logging is complete", loggerCategory);

				//Kelvin: Moved comments to here
				taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Command reference id :" + cmdRefId, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Trans id :" + transId, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Response xml :" + responseXml, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Interface name :" + interfaceName, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Command row id :" + cmdRowId, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]System name :" + systemName, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG, "[ResponseTranslator][handleCommandResponse]Response code :"+responseCode+",Response description :"+responseDescription, loggerCategory);
				
				OrderTransactonDetail orderTransactonDetail = responseTranslatorDAO.getOrderTransDtlsByCmdRowId(cmdRowId, transId, srcTransId, loggerCategory);
				corrId = orderTransactonDetail.getCorrId();
				source = orderTransactonDetail.getSource();
				
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[ResponseTranslator][handleCommandResponse] Source " + source , loggerCategory);
				
				Document responseDocument=null;
				Node node =null;
				NodeList nodeList=null;
				taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Validating interface name ", loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Interface name :"+interfaceName, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Checking whether the interface name is IL or U2KOPCO", loggerCategory);
				if((systemName!=null && (systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_IL)
						|| systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_U2KOPCO)
						|| systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_EAS)
						|| systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_ADS)
						|| systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_SAS))))
						{
					//
				}else{
					taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Checking whether the interface name is PROXIMITY_001 or MMSC", loggerCategory);
					if(interfaceName.equalsIgnoreCase("PROXIMITY_001") || interfaceName.equalsIgnoreCase("MMSC")){
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Replacing '' & '' in the response", loggerCategory);
						responseXml = responseXml.replaceAll("&lt;","").replaceAll("&gt;","");//.replaceAll("\r", "") }
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]After replacing response :"+responseXml, loggerCategory);
					}
					else{
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Replacing '<' & '>' in the response", loggerCategory);
						responseXml = responseXml.replaceAll("&lt;","<").replaceAll("&gt;",">");//.replaceAll("\r", "")
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]After replacing response :"+responseXml, loggerCategory);
					}

					responseDocument = splCommonComponent.convertStringToXmlDocument(responseXml);
					String responseval = splCommonComponent.convertDocumentToString(responseDocument);
					taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_INFO, "[ResponseTranslator][handleCommandResponse]Response value :" + responseval, loggerCategory);
					nodeList = responseDocument.getElementsByTagName("splfaultcode");
					node = nodeList.item(0);
				}
				
				if(node!=null)
				{

					responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_GENERAL_ERROR_CODE, XPathConstants.STRING);
					responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_GENERAL_ERROR_DESCRIPTION, XPathConstants.STRING);

				}
				else
				{
					
					taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Checking interface & system name to retrieve response code and response message", loggerCategory);
					//Get Response Code and Response Message from Response Message  for each system
					if(interfaceName!=null && (systemName.contains(ApplicationConstants.SYSTEM_ENS)
							|| interfaceName.contains(ApplicationConstants.SYSTEM_ENTMSG)
							|| interfaceName.contains(ApplicationConstants.SYSTEM_NETSECURITY)
							|| interfaceName.contains(ApplicationConstants.SYSTEM_MMA)
							|| interfaceName.contains(ApplicationConstants.SYSTEM_SDP))) {
						String responseResult = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_PROCESS_REQUEST_RESULT, XPathConstants.STRING);
						StringTokenizer stringTokenizer = new StringTokenizer(responseResult, ApplicationConstants.SEPERATOR_PROCESS_REQUEST_RESULT);
						responseCode = stringTokenizer.nextToken();
						responseDescription = stringTokenizer.nextToken();

					}else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_ARBOR)) {
						responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_ARBOR_SRETURN_CODES, XPathConstants.STRING);
						responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_ARBOR_SRETURN_DESCS, XPathConstants.STRING);

					}else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_B2B)) {
						responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_B2B_RETURNCODE, XPathConstants.STRING);
						responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_B2B_RETURNMESSAGE, XPathConstants.STRING);
						if(STATUS_B2B_SUCCESS.equalsIgnoreCase(responseCode)) {
							// response code 'SUCCESS' in B2B, To Set TOM response code as ZERO
							responseCode = ApplicationConstants.TOM_RESPONSE_SUCCESS;
						}

					}else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_BIS)) {
						String responseResult = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_BIS_PROCESS_REQUEST_RESULT, XPathConstants.STRING);
						StringTokenizer stringTokenizer = new StringTokenizer(responseResult, ApplicationConstants.SEPERATOR_PROCESS_REQUEST_RESULT);
						responseCode = stringTokenizer.nextToken();
						responseDescription = stringTokenizer.nextToken();

					}else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_BBRR)) {
						responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_BBRR_RETURNCODE, XPathConstants.STRING);
						responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_BBRR_DESCRIPTION, XPathConstants.STRING);

					}else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_GAZELLE)) {

						if(interfaceName.equals(ApplicationConstants.XPATH_GAZELLE_INTERFACE_GAZELLE_001)||interfaceName.equals(ApplicationConstants.XPATH_GAZELLE_INTERFACE_GAZELLE_002) || interfaceName.equals(ApplicationConstants.XPATH_GAZELLE_INTERFACE_GAZELLE_003))
						{
							String responseResult = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_GAZELLE_ADDREMOVE_STATUS, XPathConstants.STRING);

							if (responseResult.equalsIgnoreCase("Success")) {
								responseCode =  ApplicationConstants.TOM_RESPONSE_SUCCESS;
								responseDescription = "Success";
							}
							else {
								responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_GAZELLE_ADDREMOVE_ERRORCODE, XPathConstants.STRING);
								responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_GAZELLE_ADDREMOVE_ERRORMESSAGE, XPathConstants.STRING);
							}

						} else if(interfaceName.equals(ApplicationConstants.XPATH_GAZELLE_INTERFACE_GAZELLE_004)) {
							String responseResult = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_GAZELLE_CHANGESIMSTATE_STATUS, XPathConstants.STRING);

							if (responseResult.equalsIgnoreCase(ApplicationConstants.MESSAGE_SUCCESS)) {
								responseCode =  ApplicationConstants.TOM_RESPONSE_SUCCESS;
								responseDescription = ApplicationConstants.MESSAGE_SUCCESS;
							} else {
								responseCode = responseResult;
								responseDescription = responseResult;
							}
						}
						else if(interfaceName.equals(ApplicationConstants.XPATH_GAZELLE_INTERFACE_GAZELLE_005))
						{
							String responseResult = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_GAZELLE_PORTOUT_STATUS, XPathConstants.STRING);

							if (responseResult.equalsIgnoreCase("true")) {
								responseCode =  ApplicationConstants.TOM_RESPONSE_SUCCESS;
								responseDescription = "Success";
							}
							else {
								responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_GAZELLE_PORTOUT_ERRORCODE, XPathConstants.STRING);
								responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_GAZELLE_PORTOUT_ERRORMESSAGE, XPathConstants.STRING);
							}


						}
						else if(interfaceName.equals(ApplicationConstants.XPATH_GAZELLE_INTERFACE_GAZELLE_006))
						{
							String responseResult = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_GAZELLE_WRAPUP_STATUS, XPathConstants.STRING);

							// Ravi: 20130509: Bug#2097: Added for mockup error reponse change for interface GAZELLE_006 - Start
							
							if (responseResult.equalsIgnoreCase("SUCCESS")) {
								responseCode =  ApplicationConstants.TOM_RESPONSE_SUCCESS;
								responseDescription = "Success";
							}else {
								responseCode = ApplicationConstants.TOM_RESPONSE_ERROR;
								responseDescription = responseResult;
							}
							//Ravi: 20130509: Bug#2097: Added for mock up error response change for interface GAZELLE_006 - End

						}


					}else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_IDS)) {

						String responseResult = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_IMEI_RESULT, XPathConstants.STRING);
						if (responseResult.equalsIgnoreCase("TRUE")) {
							responseCode = "0";
							responseDescription = "Success";
						}
						else {
							responseCode = responseResult;
							responseDescription = responseResult;
						}

					}else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_PRS)) {
						nodeList = responseDocument.getElementsByTagName("splfaultcode");
						node = nodeList.item(0);
						if(node!=null){

							responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_U2KRSP_ERROR_CODE, XPathConstants.STRING);
							responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_U2KRSP_ERROR_DESCRIPTION, XPathConstants.STRING);

						}
						else{
							responseCode = ApplicationConstants.TOM_RESPONSE_SUCCESS;
						}
					}else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_PROXIMITY)) {

						responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_PROXIMITY_ERROR_CODE, XPathConstants.STRING);
						responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_PROXIMITY_ERROR_MESSAGES, XPathConstants.STRING);
					}else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_LOCKCUBE)) {
						String responseResult = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_SS_PROCESS_RESPONSE_RESULT, XPathConstants.STRING);
						StringTokenizer stringTokenizer = new StringTokenizer(responseResult, ApplicationConstants.SEPERATOR_PROCESS_REQUEST_RESULT);
						responseCode = stringTokenizer.nextToken();
						responseDescription = stringTokenizer.nextToken();
					}else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_U2KRSP)) {
						nodeList = responseDocument.getElementsByTagName("splfaultcode");
						node = nodeList.item(0);
						if(node!=null){

							responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_U2KRSP_ERROR_CODE, XPathConstants.STRING);
							responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_U2KRSP_ERROR_DESCRIPTION, XPathConstants.STRING);

						}
						else{
							responseCode = ApplicationConstants.TOM_RESPONSE_SUCCESS;
						}
					}else if(interfaceName!=null && (interfaceName.contains(ApplicationConstants.SYSTEM_VMS) || interfaceName.contains(ApplicationConstants.SYSTEM_CRM))) {

						if(interfaceName.contains(ApplicationConstants.SYSTEM_VMS)) {

							String responseResult = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_VMS_RETURNMESSAGE, XPathConstants.STRING);
							StringTokenizer stringTokenizer = new StringTokenizer(responseResult, ApplicationConstants.SEPERATOR_PROCESS_REQUEST_RESULT);
							String code = stringTokenizer.nextToken();
							String description = stringTokenizer.nextToken();
							if(!code.equals(0)){

								responseCode = code;
								responseDescription = description;

							}else {
								responseCode = ApplicationConstants.TOM_RESPONSE_SUCCESS;
							}
						}else if(ApplicationConstants.CRM_OMDNETMODULE.equalsIgnoreCase(interfaceName)) {
							responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_OMDNETMODULE_RETURNCODE, XPathConstants.STRING);
							responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_OMDNETMODULE_RETURNMESSAGE, XPathConstants.STRING);
						}else {
							String returnCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_OMDNETMODULE_RETURNCODE, XPathConstants.STRING);
							String returnMessage = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_OMDNETMODULE_RETURNMESSAGE, XPathConstants.STRING);
							if(returnCode.equalsIgnoreCase(ApplicationConstants.TOM_RESPONSE_ERROR)) {
								responseCode = returnCode;
								responseDescription = returnMessage;
							}else {
								// Ravi : 20130607 : Bug#2286 : no task response required for CRM_001 for stubbing, implemented auto complete : Start
								Properties stubProperties = SPLCommonComponent.getSystemStubProperty();
								String stubFlag = (String) stubProperties.getProperty(systemName);
								taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG, "[ResponseTranslator][CRMTASK]STUB :" + stubFlag, loggerCategory);
								if("Y".equalsIgnoreCase(stubFlag)) {
									responseCode = ApplicationConstants.TOM_RESPONSE_SUCCESS;
								}else { 
									if(!interfaceName.equalsIgnoreCase(ApplicationConstants.CRM_OMDNETTASK))
									{
										responseCode = ApplicationConstants.TOM_RESPONSE_SUCCESS;
									}
								}
								// Ravi : 20130607 : Bug#2286 : no task response required for CRM_001 for stubbing, implemented auto complete : End
							}
						}
					}
					else if (systemName!=null && (systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_IL) || systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_SAS)) ) {
						ILMessage ilMessage = new ILMessage();
						String statusField = "STATUS";
						String sMessageField = "SMESSAGE";
						String sasAction = ilMessage.getResponseParamValue(responseXml, "SAS_ACTION",loggerCategory,srcTransId,transId,cmdRowId );
						if(sasAction!=null && sasAction.endsWith("_ACK")) {
							statusField = "ACK_STATUS";
							sMessageField = "ACK_SMESSAGE";
						}
						String status = ilMessage.getResponseParamValue(responseXml, statusField,loggerCategory,srcTransId,transId,cmdRowId );
						if(status!=null && status.equals("9")) {
							responseCode = ApplicationConstants.TOM_RESPONSE_SUCCESS;
						}else {
							responseCode = status;
							responseDescription = ilMessage.getResponseParamValue(responseXml, sMessageField,loggerCategory,srcTransId,transId,cmdRowId );
						}


					}
					else if (interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_U2KOPCO) ) {
						// Ravi: Modified to get response code and response description

						int indexOfResponseResult = responseXml.indexOf(responseResultKeyword_U2KOPCO) + responseResultKeyword_U2KOPCO.length();
						int indexOfResponseDesc = responseXml.indexOf(responseDescKeyword_U2KOPCO) + responseDescKeyword_U2KOPCO.length();
						responseCode = responseXml.substring(indexOfResponseResult, responseXml.indexOf(" ", indexOfResponseResult));
						responseDescription = responseXml.substring(indexOfResponseDesc, responseXml.indexOf(EOF_U2KOPCO, indexOfResponseDesc));



					}

					else if (interfaceName!=null && interfaceName.contains(ApplicationConstants.SYSTEM_MMSC) ) {
						String returnval = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_MMSC_RESULT, XPathConstants.STRING);

						if(returnval.equals("1")) {
							responseCode = ApplicationConstants.TOM_RESPONSE_SUCCESS;
						}else {
							responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_MMSC_MSGCODE, XPathConstants.STRING);
							responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_MMSC_MSGTEXT, XPathConstants.STRING);
						}
					}


					else if(interfaceName.equalsIgnoreCase(ApplicationConstants.CRM_TASK)) {
						responseCode = ApplicationConstants.TOM_RESPONSE_SUCCESS;
					}
					// Ravi: commented interface 002 and 003 as xpath for error code and description same for all interface and modified xpath- Start
					else if(interfaceName!=null && (interfaceName.contains(ApplicationConstants.INTERFACE_RIM_001))) {
						responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_RIM001_ERR_CODE, XPathConstants.STRING);
						responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_RIM001_ERR_MSG, XPathConstants.STRING);
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG, "[ResponseTranslator]responseCode:"+responseCode, loggerCategory);
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG, "[ResponseTranslator]ResponseResult:"+responseDescription, loggerCategory);
					}else if(systemName!=null && (systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_FMS) || systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_SMARTROAM) 
													|| systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_STARHUB) || systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_DP7)
													|| systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_WAP) || systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_OPCOBILLING)
													|| systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_PSBILLING))) {
						responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_DBADAPTOR_RETURNCODE, XPathConstants.STRING);
						responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_DBADAPTOR_RETURNMESSAGE, XPathConstants.STRING);
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG, "[ResponseTranslator]responseCode:"+responseCode, loggerCategory);
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG, "[ResponseTranslator]ResponseResult:"+responseDescription, loggerCategory);
					}
					// Ravi: commented as we are going to use one interface - End
					else if(interfaceName!=null && interfaceName.contains(ApplicationConstants.INTERFACE_EAS_001)) {
						
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG, "[ResponseTranslator][EAS_001]Responseval:"+responseXml, loggerCategory);
						if (responseXml.equalsIgnoreCase("200")) {
							responseCode = "0";
							responseDescription ="Success";
						}
						else {
							responseCode = "1";
							responseDescription = "Failure";
						}

					}
					else if(systemName!=null && systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_RS)){
						
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG, "[ResponseTranslator]["+interfaceName+"]Responseval:"+responseXml, loggerCategory);
						responseCode = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_RS_RETURN_CODE, XPathConstants.STRING);
						if (responseCode.equalsIgnoreCase(ApplicationConstants.TOM_RESPONSE_SUCCESS)) {
							responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_RS_SUCCESS_MSG, XPathConstants.STRING);
						}else {
							responseDescription = (String) splCommonComponent.getXpathValue(responseDocument, ApplicationConstants.XPATH_RS_ERROR_MSG, XPathConstants.STRING);
						}

					} else if(systemName!=null && (systemName.contains(ApplicationConstants.SYSTEM_ADS))) {
						
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG, "[ResponseTranslator]["+systemName+"]Responseval:"+responseXml, loggerCategory);
						CommandTransDetails cmdTransDetails = (CommandTransDetails) SPLCommonComponent.getStatusCodeAndMsg(responseXml);
						responseCode = cmdTransDetails.getReturnCode();
						responseDescription = cmdTransDetails.getReturnMsg();
						taLogger.log(srcTransId,transId,cmdRowId , ApplicationConstants.LOG_DEBUG, "[ResponseTranslator]["+systemName+"]Response Code : "+responseCode, loggerCategory);
						if (responseCode.equalsIgnoreCase("200")) {
							responseCode = "0";
						} else {
							responseCode = "1";
						}
					}
					
				}
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[ResponseTranslator][handleCommandResponse]Values of response code and response message have been retrieved", loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]TOM response code :" + responseCode, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]TOM response description :" + responseDescription, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Interface name :" + interfaceName, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Response code :" + responseCode + " , Response message :" + responseDescription, loggerCategory);

				List<Map<String, Object>> orderXml = jdbcDatabaseDAO.getOrderXml(cmdRowId);
				
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[ResponseTranslator][handleCommandResponse] Source " + source , loggerCategory);
				
				//Sudharsan : 25/06/2013 :RIM SIM Exchange error handling-Start
				String isSplCmd= jdbcDatabaseDAO.isSplCmd(srcTransId, transId, cmdRowId, cmdRefId ,loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]isSplCmd : "+isSplCmd, loggerCategory);
				//Sudharsan : 25/06/2013 :RIM SIM Exchange error handling-End
				if (responseCode.equals(ApplicationConstants.TOM_RESPONSE_SUCCESS) ) {
					taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Command is success", loggerCategory);
					int commands = 0;

					int commandCount = jdbcDatabaseDAO.getNoOfCommands(
							commands, srcTransId,transId,cmdRowId,loggerCategory);
					taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Remaining commands :" + commandCount, loggerCategory);
					if (commandCount == 0) {
						if(!cmdRefId.contains("CMDUPDATECRM")){
							taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Updating command status" , loggerCategory);
							jdbcDatabaseDAO.updateStatus(cmdRowId, responseCode, responseDescription,isSplCmd);
							jdbcDatabaseDAO.updateParentCmdStatusComplete(srcTransId, transId, cmdRowId, loggerCategory);
							taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Updating order status" , loggerCategory);
							jdbcDatabaseDAO.finalSuccessUpdate(cmdRowId);
							taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Checking interface name to create TOM Response" , loggerCategory);
							if(interfaceName!=null) /*&& (interfaceName.equals(ApplicationConstants.CRM_TASK))*/ {
								//Sudharsan: 20130423: Bug#1981: Added 'responseCode' parameter  - Start
								tomResponse = splCommonComponent.updateCommandTransDtls(responseDocument, cmdRowId, interfaceName,responseCode);
								//Sudharsan: 20130423: Bug#1981: Added 'responseCode' parameter  - End
							}
							taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]TOM Response :" + tomResponse, loggerCategory);
							taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[ResponseTranslator][handleCommandResponse]Posting TOM Response message" , loggerCategory);
							if(ApplicationConstants.SOURCE_OSM.equalsIgnoreCase(source)) {
								orderTransactionSender.postMessage(tomResponse, corrId, "TOMOrderProv");
							}
						}
					}

					else if(interfaceName!=null && (interfaceName.equals(ApplicationConstants.CRM_TASK)) ) {
						taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Checking interface name to update Status and Error task" , loggerCategory);
						taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Interface name :"+interfaceName , loggerCategory);
						taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Updating command status" , loggerCategory);
						jdbcDatabaseDAO.updateStatus(cmdRowId, responseCode, responseDescription,isSplCmd);
						taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Updating order status" , loggerCategory);
						jdbcDatabaseDAO.updateerrortask(cmdRowId);
						taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Creating TOM Response", loggerCategory);
						//Sudharsan: 20130423: Bug#1981: Added 'responseCode' parameter  - Start
						tomResponse = splCommonComponent.updateCommandTransDtls(responseDocument, cmdRowId, interfaceName,responseCode);
						//Sudharsan: 20130423: Bug#1981: Added 'responseCode' parameter  - End
						taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Posting message", loggerCategory);
						if(ApplicationConstants.SOURCE_OSM.equalsIgnoreCase(source)) {
							orderTransactionSender.postMessage(tomResponse, corrId, "TOMOrderProv");
						}
					}else {
						
						

						for (Object xml : orderXml) {
							@SuppressWarnings("rawtypes")
							Map map1 = (Map) xml;
							String order_Xml = (String) map1.get("ORDER_XML");
							orderTransRowId = "" + (BigDecimal) map1.get("ROW_ID");
							String trans_ID = (String) map1.get("TRANS_ID");
							taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]Command reference id :" + cmdRefId, loggerCategory);

							jdbcDatabaseDAO.updateStatus(cmdRowId, responseCode, responseDescription,isSplCmd);

							List<Map<String, Object>> cmdRefIds = jdbcDatabaseDAO.getNewCmdRefId(trans_ID,cmdRefId, orderTransRowId, srcTransId, loggerCategory);
							Map<String, Object> map = cmdRefIds.get(0);
							String cmdRefIdList = (String) map.get("CMD_REF_ID");
							cmdRefId = cmdRefIdList;
							String parRowId = "" + (BigDecimal) map.get("ROW_ID");
							cmdRowId = parRowId;
							taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]New Command reference id :" + cmdRefIdList, loggerCategory);

							CommandParameters parameters = new CommandParameters();
							CommandParamRefDtls paramRefDtls = new CommandParamRefDtls();
							List<HandlerVariables> cmdParametersList = parameters.getParameters(srcTransId, order_Xml, cmdRefIdList, transId, parRowId, orderTransRowId, jdbcDatabaseDAO, true);

							InputStream is = new ByteArrayInputStream(order_Xml.getBytes());
							DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
							domFactory.setNamespaceAware(false);
							DocumentBuilder builder = domFactory.newDocumentBuilder();
							Document doc = builder.parse(is);
							XPathFactory factory = XPathFactory.newInstance();
							XPath xpath = factory.newXPath();

							List<HandlerVariables> cmdTransDtlsList = paramRefDtls.getParamValues(cmdRefIdList, cmdParametersList, xpath, doc, jdbcDatabaseDAO);
							taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Saving details in command trans detail table", loggerCategory);
							SPLCommonComponent.saveCmdTransDetails(cmdTransDtlsList, srcTransId, transId, loggerCategory, jdbcDatabaseDAO);

							
						}
					
						
						}

				} else {
					//Sudharsan : 25/06/2013 :RIM SIM Exchange error handling-Start
					String ErrorCode="";
					String splFlag="";
					if(listCodes != null && listCodes.size()>0 ){
					Map<String, Object> map= listCodes.get(0);
					ErrorCode = (String) map.get("error_code");
					splFlag=(String) map.get("spl_case");
					}
					String rowId= "" + (BigDecimal) orderXml.get(0).get("ROW_ID");
					String OrderXml=(String) orderXml.get(0).get("ORDER_XML");
					
					taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]CmdRefId : "+cmdRefId +" ErrorCode : "+ErrorCode + " SplFlag : "+ splFlag, loggerCategory);
					taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Checking response code ..ResponseCode : "+responseCode, loggerCategory);
					taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]OrderXml list size : "+orderXml.size(), loggerCategory);
					
					Boolean isError= jdbcDatabaseDAO.isResponseCodeMatch(srcTransId, transId, cmdRowId, cmdRefId, ErrorCode, responseCode, loggerCategory);
					
					if(responseCode!=""){
						
						//Check for specific error code //
						String splTransId ="";
						if (isError)//responseCode.equalsIgnoreCase(ErrorCode))
						{
							jdbcDatabaseDAO.orderTransDetailsErrorUpdate(rowId);
							ResponseHandler responseHandlerSplCase = new ResponseHandler();
							responseHandlerSplCase.responseHandlerSplCase(jdbcDatabaseDAO,orderXml,OrderXml,cmdRefId, srcTransId,transId,cmdRowId, rowId,corrId,responseCode, responseDescription,splFlag,loggerCategory);
							
							
						}
						
						//Sudharsan : 25/06/2013 :RIM SIM Exchange error handling-End
						else
						{
							taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Checking the size of Order Xml ", loggerCategory);
							if(orderXml.size()!=0) {
								Map<String, Object> cmdTransMap = (Map<String, Object>) orderXml.get(0);
								orderTransRowId = "" + (BigDecimal) cmdTransMap.get("ROW_ID");
								taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[ResponseTranslator][handleCommandResponse]TOM order trans row id :" + orderTransRowId, loggerCategory);
							}
							//timeout for HTTP/HTTPS request

						if (responseCode.equals("408")) {
							jdbcDatabaseDAO.statusRetryUpdate(cmdRowId, responseCode,responseDescription);
						}

						else {
							jdbcDatabaseDAO.instructorErrorUpdate(cmdRowId, responseCode,responseDescription,loggerCategory);
							jdbcDatabaseDAO.orderTransDetailsErrorUpdate(orderTransRowId);

						}
						taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Checking interface name to create TOM Response" , loggerCategory);
						if(interfaceName!=null)/* && (interfaceName.equals(ApplicationConstants.CRM_TASK)))*/ {
							//Sudharsan: 20130423: Bug#1981: Added 'responseCode' parameter  - Start
							tomResponse = splCommonComponent.updateCommandTransDtls(responseDocument, cmdRowId, interfaceName,responseCode);
							//Sudharsan: 20130423: Bug#1981: Added 'responseCode' parameter  - End
						}
						taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[ResponseTranslator][handleCommandResponse]TOM Response :" + tomResponse, loggerCategory);
						taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseTranslator][handleCommandResponse]Posting message " , loggerCategory);
						if(ApplicationConstants.SOURCE_OSM.equalsIgnoreCase(source)) {
							orderTransactionSender.postMessage(tomResponse, corrId, "TOMOrderProv");
						}
						}
					}
				}

			//}
		}	catch (Exception e) {

			taLogger.log(srcTransId,transId, cmdRowId, ApplicationConstants.LOG_ERROR,"[ResponseTranslator][handleCommandResponse][catch block]"+ "Error in " + this.getClass(), loggerCategory, e);

			// Added to send error response

			jdbcDatabaseDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "Instructor Handler", "ERR_CODE", SPLCommonComponent.getStackTrace(e));

			if (cmdRowId != null) {

				//update error status in t_om_cmd_trans table
				jdbcDatabaseDAO.instructorErrorUpdate(cmdRowId, "1", SPLCommonComponent.getStackTrace(e),loggerCategory);
			}

			//update error status in t_om_order_trans_dtls table
			String orderRowId = "";
			orderRowId = jdbcDatabaseDAO.getOrderRowId(srcTransId, transId, cmdRowId, loggerCategory);
			jdbcDatabaseDAO.orderTransDetailsErrorUpdate(orderRowId);

			if(ApplicationConstants.SOURCE_OSM.equalsIgnoreCase(source)) {
				try {
					tomResponse = splCommonComponent.createTomResponse(cmdRowId);
				} catch (Exception exception) {
				}
	
				taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_ERROR,"[ResponseTranslator][handleCommandResponse]Tom response :"+ tomResponse, loggerCategory);
	
				OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
				orderTransactionSender.postMessage(tomResponse, corrId, "TOMOrderProv");
			}

		}
	}

}
