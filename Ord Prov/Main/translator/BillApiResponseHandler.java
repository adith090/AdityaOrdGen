package com.m1.bcc.spl.translator;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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
import com.m1.bcc.spl.sender.OrderTransactionSender;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 22/07/2013					Sudharsan				Created the class for handling API responses
 * 02/09/2013					Sudharsan				Implemented C1 Order Status Poller
 * 11/09/2013					Ravikumar G		  		Removed unused imports
 * 11/09/2013					Ravikumar G		  		changed method updateNextC1Order, add new method getNoOfBillCommands to get remaining commands and moved c1 order status check as its not applicable for error case 
 * 12/09/2013					Ravikumar G				moved update bill_cmd_response table logic as we have to update only when order is completed in c1 and add new method getBillCmdTrans to get src trans id and crm svc id
 * 20/09/2013					Ravikumar G				defect#188:added logic for c1 command sequence
 * 27/09/2013					Ravikumar G				defect#203:order trans dtls stuck in Received status
 * 08/11/2013					Ravikumar G				Bug#20459 - [internal] SPL - New OM .Net Module to send back billing info back to CRM
 * 11/11/2013					Ravikumar G				Bug#20465 - Status not updated to order_trans_dtls table 
 * 12/11/2013					Ravikumar G				Bug#20459 - to update c1 order status id and poll_status
 * 12/11/2013					Ravikumar G				Bug#20421 - to implement multithreading using thread poll task executor
 * 18/11/2013					Ravikumar G				Bug#20568: changed method for billing to post message in different JMS type
 * 18/11/2013					Ravikumar G				Bug 20601 - Added service internal id resets to update back CRM
 * 20/11/2013					Ravikumar G				Bug#20645 - changed soap action and interface name
 * 05/12/2013					Ravikumar G				Bug#21274 - Added condition c1 status committed 
 ******************************************************************************/


public class BillApiResponseHandler {
	
	private JdbcDatabaseDAO jdbcDatabaseDAO;
	private TALogger taLogger;
	private ThreadPoolTaskExecutor taskExecutor;
	
	String loggerCategory = ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING;
	
	public void setJdbcDatabaseDAO(JdbcDatabaseDAO jdbcDatabaseDAO) {
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}
	
	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Async
	@ServiceActivator
	public void processBillApiResponse(org.springframework.integration.Message<ArrayList<Map<String, Object>>> responseMessageBill)throws Exception{
		
		taLogger = TALogger.getTALogger();
		String c1OrderId="";
		String transId="";
		String cmdRowId="";
		String offerInstId="";
		String serviceIntId="";
		String errCode="";
		String srcTransId="";
		boolean isFirstRecord = true;
		String lineItemId = "";
		String serviceIntIdResets = "";

		//final String soapAction = "http://tempuri.org/IOrderHandlingBFC/InvokeOMDnetModule";
		Properties propertiesKey = (Properties) BeanFactory.getBean("properties");
		String Key = propertiesKey.getProperty("KEY");
		Properties properties = SPLCommonComponent.getSystemStubProperty();
		boolean isStub = SPLCommonComponent.getStubbing(properties, ApplicationConstants.SYSTEM_CRM);

		ArrayList<Map<String, Object>> responseMessageList = responseMessageBill.getPayload();
		Element arrayOfFuncParamSetField = null;
		Document requestDocument = null;
		SPLCommonComponent splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
		final String soapAction =  splCommonComponent.getWSSoapAction(ApplicationConstants.CRM_OMDNETMODULE);
		CommandTransDetails commandTransDetails = null;
		
		for(final Map<String, Object> responseDetails : responseMessageList) {
			
			try {
				
				transId = (String)responseDetails.get(ApplicationConstants.COLUMN_TRANS_ID);
				cmdRowId=(String)responseDetails.get(ApplicationConstants.CMD_ROW_ID);
				offerInstId=(String)responseDetails.get(ApplicationConstants.OFFER_INSTANCE_ID);
				serviceIntId=(String)responseDetails.get(ApplicationConstants.SERVICE_INTERNAL_ID);
				serviceIntIdResets=(String)responseDetails.get(ApplicationConstants.SERVICE_INTERNAL_ID_RESETS);
				errCode=(String)responseDetails.get(ApplicationConstants.ERROR_CODE);
				lineItemId = (String)responseDetails.get("LINE_ITEM_ID");
				if(errCode.equals(ApplicationConstants.TOM_RESPONSE_SUCCESS)) {
					c1OrderId= (String)responseDetails.get(ApplicationConstants.COLUMN_C1ORDER_ID);
					String c1OrderStatus=jdbcDatabaseDAO.getC1OrderStatus(c1OrderId,loggerCategory);
					if (c1OrderStatus!=null && (c1OrderStatus.equalsIgnoreCase(ApplicationConstants.C1ORDER_STATUS) 
							|| c1OrderStatus.equalsIgnoreCase(ApplicationConstants.C1ORDER_STATUS_COMMITTED))) {
						if(isFirstRecord) {
				            String filePath = splCommonComponent.getFilePath(ApplicationConstants.SYSTEM_CRM, ApplicationConstants.CRM_OMDNETMODULE, ApplicationConstants.APPEND_REQUEST, ApplicationConstants.SYSTEM_APPEND_REQ_DOTXML);
							requestDocument = splCommonComponent.getDocument(filePath);
	
							commandTransDetails = jdbcDatabaseDAO.getCommandTransDetails("CRM_002");
							
							setNodeValue(requestDocument, "correlationID", cmdRowId);
							setNodeValue(requestDocument, "acctType", commandTransDetails.getAccountType());
							String password=commandTransDetails.getPwd();
							taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[CRM Billing Order Status Update]Password from DB :"+password, loggerCategory);
							String decryptedKey="";
							if(password!=null && !password.trim().equals("")) {
								decryptedKey = SPLCommonComponent.Crypt(0, Key, password);
							}
							setNodeValue(requestDocument, "password", decryptedKey);
							setNodeValue(requestDocument, "userName", commandTransDetails.getUserName());
							setNodeValue(requestDocument, "operation", "UpdateBillingInfo");
							
							NodeList nodeList = requestDocument.getElementsByTagName("arrayOfParamInfoType");
							arrayOfFuncParamSetField = (Element) nodeList.item(0);
						}

						if(arrayOfFuncParamSetField!=null) {
							if(offerInstId!=null) {
								Element funcParamsSetElement = SPLCommonComponent.addElement(requestDocument, arrayOfFuncParamSetField, "paramInfoType", "");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramType", "LINEITEM");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramID", lineItemId);
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramName", "OFFER_INSTANCE_ID");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramColumn", "BILLING_OFFER_INSTANCE_ID");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "currValue", offerInstId);
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "prevValue", "-");
							}
							if(serviceIntId!=null) {
								Element funcParamsSetElement = SPLCommonComponent.addElement(requestDocument, arrayOfFuncParamSetField, "paramInfoType", "");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramType", "LINEITEM");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramID", lineItemId);
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramName", "SUBSCRIBER_NO");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramColumn", "BILLING_SUBSCRIBER_NO");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "currValue", serviceIntId);
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "prevValue", "-");
							}
							if(serviceIntIdResets!=null) {
								Element funcParamsSetElement = SPLCommonComponent.addElement(requestDocument, arrayOfFuncParamSetField, "paramInfoType", "");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramType", "LINEITEM");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramID", lineItemId);
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramName", "SUBSCRIBER_NO_RESET");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "paramColumn", "BILLING_SUBSCRIBER_NO_RESET");
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "currValue", serviceIntIdResets);
								SPLCommonComponent.addElement(requestDocument, funcParamsSetElement, "prevValue", "-");
							}
							isFirstRecord = false;
						}
					}
				}
				
				taskExecutor.submit(new Runnable() {
					
					@Override
					public void run() {
						processBillCmdResponse(responseDetails);
					}
				});
			}catch(Exception e) {
				//System.err.println(e);
				e.printStackTrace();
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[BillApiResponseHandler][processBillApiResponse]Exception="+e,loggerCategory);
			}
		}
		
		if(requestDocument!=null) {
			String requestMessage = splCommonComponent.convertDocumentToString(requestDocument);
			taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[CRM Billing Order Status Update][updateOrderStatus]CRM Request Message :" + requestMessage, loggerCategory);
			taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[CRM Billing Order Status Update][updateOrderStatus]Stub is :"+isStub, loggerCategory);
			if(isStub) {
				splCommonComponent.saveMessage(requestDocument, "UpdateBillingOrderStatus", cmdRowId, transId, ApplicationConstants.SYSTEM_APPEND_REQ_DOTXML);
			}else {
				String wcfRequestMessage = splCommonComponent.convertDocumentToString(requestDocument);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[CRM Billing Order Status Update][updateOrderStatus]Initializing CRM webservice ", loggerCategory);
				
				StreamSource request = new StreamSource(new StringReader(wcfRequestMessage));
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG, "[CRM Billing Order Status Update][updateOrderStatus]Stream source Request :" + request, loggerCategory);
				StreamResult response = new StreamResult(new StringWriter());
				WebServiceTemplate template = new WebServiceTemplate();
				String location = commandTransDetails.getLocation();
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG, "[CRM Billing Order Status Update][updateOrderStatus]Location :" + location, loggerCategory);
				SoapActionCallback actionCallBack = new SoapActionCallback(location) {
			            public void doWithMessage(WebServiceMessage msg) {
			                SoapMessage smsg = (SoapMessage)msg;
			                smsg.setSoapAction(soapAction);
			            }
		        };
		        taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[CRM Billing Order Status Update][updateOrderStatus]CRM Request message :" + wcfRequestMessage, loggerCategory);
		        taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[CRM Billing Order Status Update][updateOrderStatus]Sending message to CRM.. ", loggerCategory);
		        template.sendSourceAndReceiveToResult(location, request, actionCallBack, response);
				String responseMessage = response.getWriter().toString();
			
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[CRM Billing Order Status Update][updateOrderStatus]Received CRM response ", loggerCategory);
		        taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[CRM Billing Order Status Update][updateOrderStatus]CRM Response Message :" + responseMessage, loggerCategory);
			}
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
	
	private void processBillCmdResponse(Map<String, Object> responseDetails) {
		taLogger = TALogger.getTALogger();
		String c1OrderId="";
		String rowId="";
		String billOrderRowId="";
		String transId="";
		String cmdRefId="";
		String cmdRowId="";
		String offerInstId="";
		String serviceIntId="";
		String errCode="";
		String errMsg="";
		String status="";
		String interfaceName="";
		String srcTransId="";
		String crmSvcId = "";
		boolean isFirstRecord = true;
		String lineItemId = "";

		//ArrayList<Map<String, Object>> responseMessageList = responseMessageBill.getPayload();
		Element arrayOfFuncParamSetField = null;
		Document requestDocument = null;
		CommandTransDetails commandTransDetails = null;
		
		//for(Map<String, Object> responseDetails : responseMessageList) {
			
			try {
				SPLCommonComponent splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
				
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[BillApiResponseHandler][processBillApiResponse]"+"rowId="+rowId+" srcTransId ="+srcTransId+" "+"transId="+transId+" billOrderRowId="+billOrderRowId+ " cmdRowId="+cmdRowId+" cmdRefId="+cmdRefId+" offerInstId="+offerInstId+" serviceIntId="+serviceIntId+" errCode="+errCode+" errMsg="+errMsg+" status="+status+" interfaceName="+interfaceName,loggerCategory);
				rowId= (String)responseDetails.get(ApplicationConstants.COLUMN_ROW_ID);
				
				billOrderRowId=(String)responseDetails.get(ApplicationConstants.COLUMN_BILL_ORDER_ROW_ID);
				transId = (String)responseDetails.get(ApplicationConstants.COLUMN_TRANS_ID);
				cmdRefId=(String)responseDetails.get(ApplicationConstants.COLUMN_CMD_REF_ID);
				cmdRowId=(String)responseDetails.get(ApplicationConstants.CMD_ROW_ID);
				offerInstId=(String)responseDetails.get(ApplicationConstants.OFFER_INSTANCE_ID);
				serviceIntId=(String)responseDetails.get(ApplicationConstants.SERVICE_INTERNAL_ID);
				errCode=(String)responseDetails.get(ApplicationConstants.ERROR_CODE);
				errMsg=(String)responseDetails.get(ApplicationConstants.ERROR_MESSAG);
				status=(String)responseDetails.get(ApplicationConstants.COLUMN_STATUS);
				interfaceName=(String)responseDetails.get(ApplicationConstants.COLUMN_INTERFACE_NAME);
				lineItemId = (String)responseDetails.get("LINE_ITEM_ID");
				String c1OrderStatusIdDB = (String)responseDetails.get("C1_ORDER_STATUS_ID");
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[BillApiResponseHandler][processBillApiResponse]"+"rowId="+rowId+" srcTransId ="+srcTransId+" "+"transId="+transId+" billOrderRowId="+billOrderRowId+ " cmdRowId="+cmdRowId+" cmdRefId="+cmdRefId+" offerInstId="+offerInstId+" serviceIntId="+serviceIntId+" errCode="+errCode+" errMsg="+errMsg+" status="+status+" interfaceName="+interfaceName,loggerCategory);
				// Ravi: removed to get src trans id and crm svc id
				//srcTransId=jdbcDatabaseDAO.getSrcTransIdBill(cmdRowId, transId);
				Map billCmdTrans = jdbcDatabaseDAO.getBillCmdTrans(cmdRowId, transId);
				srcTransId = (String)billCmdTrans.get(ApplicationConstants.COLUMN_SRC_TRANS_ID);
				crmSvcId = (String)billCmdTrans.get(ApplicationConstants.COLUMN_CRM_SVC_ID);
				
				int commands=0;
				OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[BillApiResponseHandler][processBillApiResponse]"+"srcTransId ="+srcTransId+" "+"transId="+transId+ " cmdRowId="+cmdRowId+" cmdRefId="+cmdRefId+" offerInstId="+offerInstId+" serviceIntId="+serviceIntId+" errCode="+errCode+" errMsg="+errMsg+" status="+status+" interfaceName="+interfaceName,loggerCategory);
				String orderRowId = jdbcDatabaseDAO.getOrderRowIdBill(srcTransId, transId, cmdRowId, loggerCategory);
				if(errCode.equals(ApplicationConstants.TOM_RESPONSE_SUCCESS)) {
					// Ravi: moved c1 order status check as its not applicable for error case
					c1OrderId= (String)responseDetails.get(ApplicationConstants.COLUMN_C1ORDER_ID);
					String c1OrderStatus=jdbcDatabaseDAO.getC1OrderStatus(c1OrderId,loggerCategory);
					//System.out.println("c1OrderStatus ::: "+c1OrderStatus);
					if (c1OrderStatus!=null && (c1OrderStatus.equalsIgnoreCase(ApplicationConstants.C1ORDER_STATUS)
							|| c1OrderStatus.equalsIgnoreCase(ApplicationConstants.C1ORDER_STATUS_COMMITTED))) {
						// Ravi: moved update bill_cmd_response table as we have to update only when order is completed in c1
						jdbcDatabaseDAO.updateBillCmdResponse(rowId, c1OrderStatus, loggerCategory);
						jdbcDatabaseDAO.updateOrderTransBillComplete(billOrderRowId, srcTransId, transId);
						jdbcDatabaseDAO.updateCommandTransBillComplete(cmdRowId, srcTransId, transId,billOrderRowId, errCode, errMsg);
						
						// Ravi: changed method to check in bill order trans for remaining commands
						//int commandCount = jdbcDatabaseDAO.getNoOfCommandsBill(commands, cmdRowId,transId,cmdRefId);
						// Ravi: defect#188: updated method getNoOfBillCommands for c1 command sequence
						//int commandCount = jdbcDatabaseDAO.getNoOfBillCommands(srcTransId, transId, cmdRowId, crmSvcId, loggerCategory);
						// Ravi: to check all the commands are completed for TOM
						int commandCount = jdbcDatabaseDAO.getNoOfPendingTOMCommands(srcTransId, transId, cmdRowId, crmSvcId, loggerCategory);
						if(commandCount==0) {
							jdbcDatabaseDAO.finalSuccessUpdateBill(cmdRowId);
							//SPLCommonComponent splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
							String tomResponse = splCommonComponent.createBillTomResponse(cmdRowId);
							String corrId = jdbcDatabaseDAO.getCorrId(orderRowId);
							//orderTransactionSender.postMessage(tomResponse, corrId);
							orderTransactionSender.postBillingResponse(tomResponse, corrId);
						}else {
							// Ravi: changed method to update next c1 order in bill_order_trans table
							//jdbcDatabaseDAO.updateNextC1Order(srcTransId, transId, cmdRowId, crmSvcId, loggerCategory);
							jdbcDatabaseDAO.updateC1CmdStatusToNew(srcTransId, transId, cmdRowId, crmSvcId, loggerCategory);
						}
						// Ravi: to check all the billing commands are completed for the Order
						commandCount = jdbcDatabaseDAO.getNoOfBillCommands(srcTransId, transId, cmdRowId, crmSvcId, loggerCategory);
						if(commandCount>0) {
							jdbcDatabaseDAO.updateC1CmdStatusToNew(srcTransId, transId, cmdRowId, crmSvcId, loggerCategory);
						}
					}else {
						if(c1OrderStatusIdDB !=null && c1OrderStatus!=null && !c1OrderStatus.equals(c1OrderStatusIdDB))
							jdbcDatabaseDAO.updateBillCmdResponse(rowId, c1OrderStatus, loggerCategory);
					}
				}else if(errCode.equals(ApplicationConstants.TOM_RESPONSE_ERROR)) {
					jdbcDatabaseDAO.updateBillCmdResponse(rowId, "-", loggerCategory);
					jdbcDatabaseDAO.updateBillOrderTransError(billOrderRowId, srcTransId, transId);
					// Ravi: updated error code and error msg in bill_cmd_trans table
					//jdbcDatabaseDAO.updateBillCommandTransBillError(cmdRowId, srcTransId, transId,billOrderRowId, errCode,  errMsg);	
					jdbcDatabaseDAO.updateBillCommandTransStatus(cmdRowId, srcTransId, transId, billOrderRowId, errCode,  errMsg, ApplicationConstants.STATUS_ERROR, loggerCategory);
					jdbcDatabaseDAO.orderTransDetailsErrorUpdate(orderRowId);
					String tomResponse = splCommonComponent.createBillTomResponse(cmdRowId);
					String corrId = jdbcDatabaseDAO.getCorrId(orderRowId);
					orderTransactionSender.postBillingResponse(tomResponse, corrId);
				}
			}catch(Exception e) {
				//System.err.println(e);
				e.printStackTrace();
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO, "[BillApiResponseHandler][processBillApiResponse]Exception="+e,loggerCategory);
			}
		//}
	}
	
}


