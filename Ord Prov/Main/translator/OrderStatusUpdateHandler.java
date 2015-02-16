package com.m1.bcc.spl.translator;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 08/06/2013					Ravikumar G				Bug#2291 : Added OMDnetCall for TechRequest to status update
 * 06/11/2013					Ravikumar G				Bug#20397: updated moli status update and moved the request message format to request template CRM_order_status_request.xml
 * 12/11/2013					Ravikumar G				Bug#20421 - to implement multithreading using thread poll task executor
 * 20/11/2013					Ravikumar G				Bug#20645 - updated paramColumn for moli update to STATUS_CD
 * 10/12/2013					Ravikumar G				Bug#21455 - updated New OM .Net Module call to update Tech Request Status 
 ******************************************************************************/

public class OrderStatusUpdateHandler {
	
	private JdbcDatabaseDAO jdbcDatabaseDAO;
	private ThreadPoolTaskExecutor taskExecutor;
	
	public void setJdbcDatabaseDAO(JdbcDatabaseDAO jdbcDatabaseDAO) {
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}
	
	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	
	@ServiceActivator
	@Async
	public void updateOrderStatus(Message<ArrayList<Map<String, Object>>> message) {
	
		ArrayList<Map<String, Object>> orderTransList = (ArrayList<Map<String, Object>>) message.getPayload();
		
        for(final Map<String, Object> orderTrans : orderTransList) {
        	taskExecutor.submit(new Runnable() {
				
				@Override
				public void run() {
					processOrderStatusUpdate(orderTrans);
				}
			});
		}	
	}
	
	private void processOrderStatusUpdate(Map<String, Object> orderTrans) {
		SPLCommonComponent splCommonComponent;
		String loggerCategory = ApplicationConstants.LOG_CATEGORY_CRM_LOGGING;
		
		TALogger taLogger = TALogger.getTALogger();
		
		String orderId="";
		String transId="";
		
		try {
			Properties properties = SPLCommonComponent.getSystemStubProperty();
			boolean isStub = SPLCommonComponent.getStubbing(properties, ApplicationConstants.SYSTEM_CRM);
			splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
			final String soapAction =  splCommonComponent.getWSSoapAction(ApplicationConstants.CRM_OMDNETMODULE);
			//ArrayList<Map<String, Object>> orderTransList = (ArrayList<Map<String, Object>>) message.getPayload();
			
			Properties propertiesKey = (Properties) BeanFactory.getBean("properties");
			String Key = propertiesKey.getProperty("KEY");
            
            //for (Map<String, Object> orderTrans : orderTransList) {
            	
				String corrId = (String) orderTrans.get("ROW_ID");
	           	orderId = (String) orderTrans.get("SRC_TRANS_ID");
	            String status = (String) orderTrans.get("STATUS");
	            String currValue = propertiesKey.getProperty(status);
	            String moliId = (String) orderTrans.get("MOLI_ID");
	            transId = (String) orderTrans.get("TRANS_ID");
	            String transType = (String) orderTrans.get("TRANS_TYPE");
	            String opcoFlag = (String) orderTrans.get("OPCO_FLAG");
	            taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]soapAction " + soapAction, loggerCategory);
	            taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]Retrieved order to pass for order update", loggerCategory);
	            taLogger.log(orderId, transId, ApplicationConstants.LOG_DEBUG,"[CRM Order Status Update][updateOrderStatus]CORR ID:"+corrId+" , ORDER ID:"+orderId +" , CURRENT VALUE:"+currValue+" , MOLI ID:"+ moliId+" , TRANS ID:"+ transId +" , TRANS TYPE:"+transType ,loggerCategory);
	            taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]Creating CRM request message ", loggerCategory);
	            taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]Setting CRM attributes ", loggerCategory);
	            
	            String filePath = splCommonComponent.getFilePath(ApplicationConstants.SYSTEM_CRM, ApplicationConstants.CRM_OMDNETMODULE, ApplicationConstants.APPEND_REQUEST, ApplicationConstants.SYSTEM_APPEND_REQ_DOTXML);
				Document requestDocument = splCommonComponent.getDocument(filePath);

				CommandTransDetails commandTransDetails = jdbcDatabaseDAO.getCommandTransDetails(ApplicationConstants.CRM_OMDNETMODULE);
				
				if(transType.equals("TOMTECH"))
					setNodeValue(requestDocument, "correlationID", orderId);
				else
					setNodeValue(requestDocument, "correlationID", transId);
				setNodeValue(requestDocument, "acctType", commandTransDetails.getAccountType());
				String password=commandTransDetails.getPwd();
				taLogger.log(orderId, transId, ApplicationConstants.LOG_DEBUG,"[CRM Order Status Update][updateOrderStatus]Password from DB :"+password, loggerCategory);
				String decryptedKey="";
				if(password!=null && !password.trim().equals("")) {
					decryptedKey = SPLCommonComponent.Crypt(0, Key, password);
				}
				setNodeValue(requestDocument, "password", decryptedKey);
				setNodeValue(requestDocument, "userName", commandTransDetails.getUserName());
				if(ApplicationConstants.OPCO_FLAG.equals(opcoFlag)) {
					setNodeValue(requestDocument, "operation", "UpdateOpCoMoliStatus");
				}else {
					if(transType.equals("TOMTECH")) {
						setNodeValue(requestDocument, "operation", "UpdateTechRequestStatus");
					}else {
						setNodeValue(requestDocument, "operation", "UpdateMoliStatus");
					}
				}
				
				NodeList nodeList = requestDocument.getElementsByTagName("arrayOfParamInfoType");
				Element arrayOfFuncParamSetField = (Element) nodeList.item(0);

				Element funcParamsSetElement = SPLCommonComponent.addElement(requestDocument, arrayOfFuncParamSetField, "paramInfoType", "");

			    if(transType.equals("MOLI")) {
			    	taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]Add elements for MOLI ", loggerCategory);
					SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramType", "LINEITEM");
					SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramID", moliId);
			    }else if(transType.equals("TOMTECH")) {
			    	// Ravi : 20131210 : Bug#21455 : Added OMDnetModule call for TechRequest status update : Start
					taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]Add elements for TOMTECH ", loggerCategory);
					SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramType", "TECHREQHEADER");
					SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramID", orderId);
					// Ravi : 20131210 : Bug#21455 : Added OMDnetModule call for TechRequest status update : End
			    }
				SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramName", "STATUS_CD");
			    SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramColumn", "STATUS_CD");
			    SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "currValue", currValue);
			    SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "prevValue", "");
			    
				taLogger.log(orderId, transId, ApplicationConstants.LOG_DEBUG,"[CRM Order Status Update][updateOrderStatus]Final CRM request document :" + requestDocument, loggerCategory);
				taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]Converting document to string ..", loggerCategory);
				String requestMessage = splCommonComponent.convertDocumentToString(requestDocument);
				taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO,"[CRM Order Status Update][updateOrderStatus]CRM Request Message :" + requestMessage, loggerCategory);
				taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]Checking stub value..", loggerCategory);
				taLogger.log(orderId, transId, ApplicationConstants.LOG_DEBUG,"[CRM Order Status Update][updateOrderStatus]Stub is :"+isStub, loggerCategory);
				if(isStub) {
					splCommonComponent.saveMessage(requestDocument, "UpdateOrderStatus", corrId, transId, ApplicationConstants.SYSTEM_APPEND_REQ_DOTXML);
				}else {
					String wcfRequestMessage = splCommonComponent.convertDocumentToString(requestDocument);
					taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]Initializing CRM webservice ", loggerCategory);
					
					StreamSource request = new StreamSource(new StringReader(wcfRequestMessage));
					taLogger.log(orderId, transId, ApplicationConstants.LOG_DEBUG, "[CRM Order Status Update][updateOrderStatus]Stream source Request :" + request, loggerCategory);
					StreamResult response = new StreamResult(new StringWriter());
					WebServiceTemplate template = new WebServiceTemplate();
					String location = commandTransDetails.getLocation();
					taLogger.log(orderId, transId, ApplicationConstants.LOG_DEBUG, "[CRM Order Status Update][updateOrderStatus]Location :" + location, loggerCategory);
					SoapActionCallback actionCallBack = new SoapActionCallback(location) {
				            public void doWithMessage(WebServiceMessage msg) {
				                SoapMessage smsg = (SoapMessage)msg;
				                smsg.setSoapAction(soapAction);
				            }
			        };
			        taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]CRM Request message :" + wcfRequestMessage, loggerCategory);
			        taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]Sending message to CRM.. ", loggerCategory);
			        template.sendSourceAndReceiveToResult(location, request, actionCallBack, response);
					String responseMessage = response.getWriter().toString();
				
					taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]Received CRM response ", loggerCategory);
			        taLogger.log(orderId, transId, ApplicationConstants.LOG_INFO, "[CRM Order Status Update][updateOrderStatus]CRM Response Message :" + responseMessage, loggerCategory);
			       // wcfTranslatorDAO.saveCommandResponse(cmdTransId, cmdRefId, transId, responseMessage, interfaceName);
				}
			
           //}
		} 
		catch(Exception e)
		{
			taLogger.log(orderId, transId,ApplicationConstants.LOG_ERROR,"[CRM Order Status Update][updateOrderStatus]Exception occured",loggerCategory, e);
			
			jdbcDatabaseDAO.insertError(ApplicationConstants.APPLICATION_NAME, "ORDERID", "CMD_REF_ID", "updateOrderStatus","ERR_CODE",SPLCommonComponent.getStackTrace(e));
			
		}	
	}
	
	private void setNodeValue(Document document, String paramName, String paramValue) throws SPLExceptionHandler {
		NodeList userNameList = document.getElementsByTagName(paramName);
		int index=0;
		while(index <userNameList.getLength()){
	        Node userNameNode = userNameList.item(index);
	        if(userNameNode!=null)
	        	userNameNode.setTextContent(paramValue);
	        else
	        	throw new SPLExceptionHandler("[WcfTranslator-setNodeValue-SPLExceptionHandler]    Element " +  paramName + " not available in XML document");
	        index++;
		}
	}

}