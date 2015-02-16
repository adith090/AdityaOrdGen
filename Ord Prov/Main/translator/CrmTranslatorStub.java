package com.m1.bcc.spl.translator;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;
import org.springframework.integration.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;

import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 08/06/2013					Ravikumar G				Bug#2291 : Added OMDnetCall for TechRequest to status update
 ******************************************************************************/

public class CrmTranslatorStub {
	
	private SPLCommonComponent splCommonComponent;
	String soapAction = "http://tempuri.org/IOrderHandlingBFC/InvokeOMDnetModule";
	private String loggerCategory = ApplicationConstants.LOG_CATEGORY_CRM_LOGGING;
	
	JdbcDatabaseDAO jdbcDatabaseDAO;
	
	public void setJdbcDatabaseDAO(JdbcDatabaseDAO jdbcDatabaseDAO) {
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}
		

	/**
	 * 
	 * @param message_CRM
	 */
	
	@Async
	public void createCrmRequestMessage(Message<?> message_CRM) {
		
		TALogger taLogger = TALogger.getTALogger();
		
		String order_id="";
		String trans_id="";
		
		try {
			Properties properties = SPLCommonComponent.getSystemStubProperty();
			boolean isStub = SPLCommonComponent.getStubbing(properties, ApplicationConstants.SYSTEM_CRM);
			splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
			ArrayList<?> messages_CRM = (ArrayList<?>) message_CRM.getPayload();
			
			Properties propertiesKey = (Properties) BeanFactory.getBean("properties");
			String Key = propertiesKey.getProperty("KEY");
            
            for (Object mess : messages_CRM) {
            	
            
            	
			Map<String, Object> map = (Map<String, Object>) mess;
			String corr_id = (String) map.get("ROW_ID");
           	order_id = (String) map.get("SRC_TRANS_ID");
            String curr_val = (String) map.get("STATUS");
            String moli_id = (String) map.get("MOLI_ID");
            trans_id = (String) map.get("TRANS_ID");
            String trans_type = (String) map.get("TRANS_TYPE");
            taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Retrieved order to pass for order update", loggerCategory);
            taLogger.log(order_id,trans_id, ApplicationConstants.LOG_DEBUG,"[CRM Order Status Update][createCrmRequestMessage]CORR ID:"+corr_id+" , ORDER ID:"+order_id +" , CURRENT VALUE:"+curr_val+" , MOLI ID:"+ moli_id+" , TRANS ID:"+ trans_id +" , TRANS TYPE:"+trans_type ,loggerCategory);
            taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Creating CRM request message ", loggerCategory);
            taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Setting CRM attributes ", loggerCategory);
			Document doc = splCommonComponent.getDocument();
			Element dnetommoduleinput = doc.createElement("tem:InvokeOMDnetModule");
            dnetommoduleinput.setAttribute("xmlns:tem", "http://tempuri.org/");
			dnetommoduleinput.setAttribute("xmlns:crm", "http://schemas.datacontract.org/2004/07/CRM.OrderCapture.OrderHandlingCommon");
			doc.appendChild(dnetommoduleinput);
			taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Creating XML to CRM ", loggerCategory);
			Element inputheader = doc.createElement("tem:input");
			dnetommoduleinput.appendChild(inputheader);
			
			Element messageField=doc.createElement("crm:messageNameField");
			inputheader.appendChild(messageField);
			messageField.appendChild(doc.createTextNode("InvokeOMDnetModule"));
			
			Element messageBody=doc.createElement("crm:msgBodyField");
			inputheader.appendChild(messageBody);
			
			taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Adding values to the XML ", loggerCategory);
				
			Element array = SPLCommonComponent.addElement(doc, messageBody, "crm:arrayOfFuncParamsSetField", "");
			
			Element funcparamsset = SPLCommonComponent.addElement(doc, array, "crm:ParamInfoType", "");
			
		    SPLCommonComponent.addElement(doc, funcparamsset, "crm:currValueField", curr_val);
			
			if(trans_type.equals("ORDER")) {
				taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Add elements for ORDER ", loggerCategory);
				 SPLCommonComponent.addElement(doc, funcparamsset, "crm:paramIDField", order_id);
				 SPLCommonComponent.addElement(doc, funcparamsset, "crm:paramNameField", "ORDER_STATUS");
				 SPLCommonComponent.addElement(doc, funcparamsset, "crm:paramTypeField", "T_ORDER_HEADER");
			 }else if(trans_type.equals("MOLI")) {
				 taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Add elements for MOLI ", loggerCategory);
				 SPLCommonComponent.addElement(doc, funcparamsset, "crm:paramIDField", moli_id);
				 SPLCommonComponent.addElement(doc, funcparamsset, "crm:paramNameField", "STATUS");
				 SPLCommonComponent.addElement(doc, funcparamsset, "crm:paramTypeField", "T_ORDER_LINE_ITEM");
			 }else if(trans_type.equals("TOMTECH")) {
				// Ravi : 20130608 : Bug#2291 : Added OMDnetCall for TechRequest to status update : Start
				 taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Add elements for TOMTECH ", loggerCategory);
				 SPLCommonComponent.addElement(doc, funcparamsset, "crm:paramIDField", order_id);
				 SPLCommonComponent.addElement(doc, funcparamsset, "crm:paramNameField", "TECH_REQUEST_STATUS");
				 SPLCommonComponent.addElement(doc, funcparamsset, "crm:paramTypeField", "T_TECH_REQUEST_STAGING");
				// Ravi : 20130608 : Bug#2291 : Added OMDnetCall for TechRequest to status update : End
			 }
			 SPLCommonComponent.addElement(doc, funcparamsset, "crm:prevValueField", "");

			
			 Element authenticationElement = SPLCommonComponent.addElement(doc, messageBody, "crm:authenticationInfoField", "");

			 CommandTransDetails commandTransDetails = jdbcDatabaseDAO.getCommandTransDetails("CRM_002");
			 	//Add element in AuthenticationInfo Element
				SPLCommonComponent.addElement(doc, authenticationElement, "crm:acctTypeField", commandTransDetails.getAccountType());
				String password=commandTransDetails.getPwd();
				taLogger.log(order_id,trans_id, ApplicationConstants.LOG_DEBUG,"[CRM Order Status Update][createCrmRequestMessage]Password from DB :"+password, loggerCategory);
				String decryptedKey="";
				if(password!=null && !password.trim().equals("")){
									decryptedKey=SPLCommonComponent.Crypt(0, Key, password);
									}
				taLogger.log(order_id,trans_id, ApplicationConstants.LOG_DEBUG,"[CRM Order Status Update][createCrmRequestMessage]DecryptedKey :"+decryptedKey, loggerCategory);
				SPLCommonComponent.addElement(doc, authenticationElement, "crm:passwordField", decryptedKey);
				SPLCommonComponent.addElement(doc, authenticationElement, "crm:userNameField", commandTransDetails.getUserName());
			 
				// Ravi : 20130608 : Bug#2291 : Added OMDnetCall for TechRequest to status update
				if(trans_type.equals("TOMTECH")) {
					SPLCommonComponent.addElement(doc, messageBody, "crm:operationField", "UpdateTechRequestStatus");
				}else {
					SPLCommonComponent.addElement(doc, messageBody, "crm:operationField", "UpdateOrderStatus");
				}
				SPLCommonComponent.addElement(doc, messageBody, "crm:operationStatusField", "");
				SPLCommonComponent.addElement(doc, messageBody, "crm:orderIDField", order_id);

				Element msgHeaderElement = SPLCommonComponent.addElement(doc, inputheader, "crm:msgHeaderField", "");

				SPLCommonComponent.addElement(doc, msgHeaderElement, "crm:correlationIDField", corr_id);
				SPLCommonComponent.addElement(doc, msgHeaderElement, "crm:returnCodeField", "");
				SPLCommonComponent.addElement(doc, msgHeaderElement, "crm:returnMessageField", "");
				SPLCommonComponent.addElement(doc, msgHeaderElement, "crm:tranStatusField", ApplicationConstants.STATUS_NEW);
				SPLCommonComponent.addElement(doc, msgHeaderElement, "crm:versionField", "Version1");
				 
			taLogger.log(order_id,trans_id, ApplicationConstants.LOG_DEBUG,"[CRM Order Status Update][createCrmRequestMessage]Final CRM request document :" + doc, loggerCategory);
			taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Converting document to string ..", loggerCategory);
			String requestMessage = splCommonComponent.convertDocumentToString(doc);
			taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO,"[CRM Order Status Update][createCrmRequestMessage]CRM Request Message :" + requestMessage, loggerCategory);
			taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Checking stub value..", loggerCategory);
			taLogger.log(order_id,trans_id, ApplicationConstants.LOG_DEBUG,"[CRM Order Status Update][createCrmRequestMessage]Stub is :"+isStub, loggerCategory);
			if(isStub) {
				splCommonComponent.saveMessage(doc, "UpdateOrderStatus", corr_id, trans_id, ApplicationConstants.SYSTEM_APPEND_REQ_DOTXML);
			}else {
				String wcfRequestMessage = splCommonComponent.convertDocumentToString(doc);
				taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Initializing CRM webservice ", loggerCategory);
				
				StreamSource request = new StreamSource(new StringReader(wcfRequestMessage));
				taLogger.log(order_id,trans_id, ApplicationConstants.LOG_DEBUG, "[CRM Order Status Update][createCrmRequestMessage]Stream source Request :" + request, loggerCategory);
				StreamResult response = new StreamResult(new StringWriter());
				WebServiceTemplate template = new WebServiceTemplate();
				String location = commandTransDetails.getLocation();
				taLogger.log(order_id,trans_id, ApplicationConstants.LOG_DEBUG, "[CRM Order Status Update][createCrmRequestMessage]Location :" + location, loggerCategory);
				SoapActionCallback actionCallBack = new SoapActionCallback(location) {
			            public void doWithMessage(WebServiceMessage msg) {
			                SoapMessage smsg = (SoapMessage)msg;
			                smsg.setSoapAction(soapAction);
			            }
		        };
		        taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]CRM Request message :" + wcfRequestMessage, loggerCategory);
		        taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Sending message to CRM.. ", loggerCategory);
		        template.sendSourceAndReceiveToResult(location, request, actionCallBack, response);
				String responseMessage = response.getWriter().toString();
			
				taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]Received CRM response ", loggerCategory);
		        taLogger.log(order_id,trans_id, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][createCrmRequestMessage]CRM Response Message :" + responseMessage, loggerCategory);
		       // wcfTranslatorDAO.saveCommandResponse(cmdTransId, cmdRefId, transId, responseMessage, interfaceName);
			}
			
           }
		} 
		catch(Exception e)
		{
			taLogger.log(order_id,trans_id,ApplicationConstants.LOG_ERROR,"[CRM Order Status Update][createCrmRequestMessage]Exception occured",loggerCategory, e);
			
			jdbcDatabaseDAO.insertError(ApplicationConstants.APPLICATION_NAME, "ORDERID", "CMD_REF_ID", "createCrmRequestMessage","ERR_CODE",SPLCommonComponent.getStackTrace(e));
			
		}	
	}

	}