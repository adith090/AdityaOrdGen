package com.m1.bcc.spl.listener;

import java.util.Properties;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.xpath.XPathConstants;

import org.springframework.jndi.JndiTemplate;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.model.OrderTransactonDetail;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.sender.OrderTransactionSender;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.XPathReader;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/11/2012					Ravikumar G				Created for listener
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 13/05/2013					Ravikumar G				Bug#2116: updated exception handling to update cmd and order as Error and send back TOMResponse. 
 * 														Updated logging and removed System.out
 * 16/05/2013					Billy Lim				Bug 2142 [Internal] For RETRY TaskRequest, return TOM Success Response when Order is in Completed status in T_OM_ORDER_TRANS_DTLS
 * 17/05/2013					Ravikumar G				Bug#2170: for JMS Authentication load context and set the user and password
 * 31/12/2013					Ravikumar G				Bug#22498 - Updated priority in order_trans_dtls table
 * 10/01/2014					Ravikumar G				Bug#23067 - Convert into single Listerner for both Billing and Non Billing
 * 27/01/2014					Ravikumar G				Bug#23086 - updated source in order trans details and added condition if source=OSM then send tomresponse
 ******************************************************************************/

public class OrderTransactionListener implements MessageListener {

	private DatabaseDAO orderTransactionDAO;
	TALogger taLogger = TALogger.getTALogger();
	private String loggerCategory="osmlogging";
	JdbcDatabaseDAO jdbcDatabaseDAO;

	public OrderTransactionListener() {
		// Ravi: 20130517: Bug#2170: Added for JMS authentication - Start
		JndiTemplate jndiTemplate = (JndiTemplate) BeanFactory.getBean("jndiTemplate");
		try {
			Properties jmsProperties = SPLCommonComponent.loadProperty(ApplicationConstants.SPLAPP_FILENAME);
			Properties envProperties = jndiTemplate.getEnvironment();
			envProperties.put("java.naming.security.principal", jmsProperties.get("jms_user"));
			taLogger.log("", ApplicationConstants.LOG_DEBUG, "[JMS Authentication]JMS User: " + jmsProperties.get("jms_user"), loggerCategory);
			String password = (String) jmsProperties.get("jms_pwd");
			taLogger.log("", ApplicationConstants.LOG_DEBUG, "[JMS Authentication]password: " + password, loggerCategory);
			String key = (String) jmsProperties.get("KEY");
			String decryptedPwd = "";
			if(password!=null && !password.trim().equals("")) {
				decryptedPwd = SPLCommonComponent.Crypt(0, key, password);
			}
			taLogger.log("", ApplicationConstants.LOG_DEBUG, "[JMS Authentication]decryptedPwd: " + decryptedPwd, loggerCategory);
			envProperties.put("java.naming.security.credentials", decryptedPwd);
			jndiTemplate.setEnvironment(envProperties);
			jndiTemplate.getContext();
		} catch (Exception exception) {
		}
		// Ravi: 20130517: Bug#2170: Added for JMS authentication - End
	}
	
	/**
	 *
	 */
	@Override
	public void onMessage(Message message) {
		
		String tomOrderId = "";
		OrderTransactonDetail orderTransactonDetail = null;
		String crmOrderId = "";
		
		try {

			String correlationID = message.getJMSCorrelationID();
			taLogger.log("CorrelationID="+correlationID, ApplicationConstants.LOG_DEBUG, "Inside OrderTransactionListener- onMessage",loggerCategory);
			String requestXml = ((TextMessage) message).getText();
			taLogger.log("CorrelationID="+correlationID, ApplicationConstants.LOG_DEBUG, "Received requestXml="+requestXml,loggerCategory);
			XPathReader xPathReader = new XPathReader(requestXml);

			tomOrderId = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_TOM_ORDER_ID, XPathConstants.STRING);
			taLogger.log("", tomOrderId, ApplicationConstants.LOG_INFO, "tomOrderId " + tomOrderId,loggerCategory);
			String funcRefId = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_FUN_REF_ID, XPathConstants.STRING);
			taLogger.log("", tomOrderId, ApplicationConstants.LOG_INFO, "funcRefId " + funcRefId,loggerCategory);
			String opgRefId = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_OPG_REF_ID, XPathConstants.STRING);
			taLogger.log("", tomOrderId, ApplicationConstants.LOG_INFO, "opgRefId " + opgRefId,loggerCategory);
			String taskRequest = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_TASK, XPathConstants.STRING);
			taLogger.log("", tomOrderId, ApplicationConstants.LOG_INFO, "taskRequest " + taskRequest,loggerCategory);
			//String cOneFlag = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_C1_FLAG, XPathConstants.STRING);
				

			String cOneFlag = ApplicationConstants.C1_FLAG_NO;
			if(xPathReader.isValidPath(ApplicationConstants.XPATH_C1_FLAG))
				cOneFlag = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_C1_FLAG, XPathConstants.STRING);
			
			taLogger.log("[OrderTransactionListenerBill][onMessage]cOneFlag="+cOneFlag, ApplicationConstants.LOG_DEBUG,"cOneFlag " + cOneFlag,loggerCategory);
			
			String crmSvcId = "";
			if(ApplicationConstants.C1_FLAG_YES.equals(cOneFlag)) {
				crmOrderId = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_CRM_ORD_BILL, XPathConstants.STRING);
				crmSvcId = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_CRM_SVC_ID, XPathConstants.STRING);
				taLogger.log("[OrderTransactionListenerBill][onMessage]cOneFlag="+cOneFlag, ApplicationConstants.LOG_DEBUG,"crmSvcId " + crmSvcId,loggerCategory);
			}else {
			
				if(opgRefId.equals("-")){
					crmOrderId = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_TECHREQUEST_CRM_ORDER_ID, XPathConstants.STRING);
				}
				else{
					crmOrderId = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_CRM_ORDER_ID, XPathConstants.STRING);
				}
			}
	    
		    String priority = null;
		    if(xPathReader.isValidPath(ApplicationConstants.XPATH_PRIORITY))
		    	priority = (String) xPathReader.getXpathValue(ApplicationConstants.XPATH_PRIORITY, XPathConstants.STRING);
			taLogger.log(crmOrderId, tomOrderId, ApplicationConstants.LOG_INFO, "priority " + priority, loggerCategory);
		    
		    taLogger.log(crmOrderId, tomOrderId, ApplicationConstants.LOG_DEBUG, "crmOrderId " + crmOrderId,loggerCategory);
		    
			orderTransactonDetail = new OrderTransactonDetail();
			orderTransactonDetail.setOrderId(tomOrderId);
			orderTransactonDetail.setCrmOrderId(crmOrderId);
			orderTransactonDetail.setOrderXml(requestXml);
			orderTransactonDetail.setFuncRefId(funcRefId);
			orderTransactonDetail.setOpgRefId(opgRefId);
			orderTransactonDetail.setCorrId(correlationID);
			orderTransactonDetail.setStatus(ApplicationConstants.STATUS_NEW);
			orderTransactonDetail.setPollStatus("N");
			orderTransactonDetail.setTaskRequest(taskRequest);
			orderTransactonDetail.setCOneFlag(cOneFlag);
			orderTransactonDetail.setUpdatedBy("admin");
			orderTransactonDetail.setSource(ApplicationConstants.SOURCE_OSM);
			
			if(ApplicationConstants.C1_FLAG_YES.equals(cOneFlag)) {
				orderTransactonDetail.setCOneFlag(cOneFlag);
				orderTransactonDetail.setCrmSvcId(crmSvcId);
			}else {
				//String priorityFlag = orderTransactionDAO.getPriorityFlag(priority, loggerCategory);
				orderTransactonDetail.setPriority(priority);
			}
			if (ApplicationConstants.STATUS_NEW.equalsIgnoreCase(taskRequest)) {
				orderTransactionDAO.saveOrderTransactionDetails(orderTransactonDetail);
			}
			else if (ApplicationConstants.STATUS_ERROR_TASK.equalsIgnoreCase(taskRequest)) {
				orderTransactionDAO.saveOrderTransactionDetailsErrorTask(orderTransactonDetail);
			}
			else if(ApplicationConstants.STATUS_CANCELLED.equalsIgnoreCase(taskRequest) || ApplicationConstants.STATUS_RETRY.equalsIgnoreCase(taskRequest)
					|| ApplicationConstants.STATUS_SKIP.equalsIgnoreCase(taskRequest)) {
				taLogger.log(crmOrderId, tomOrderId, ApplicationConstants.LOG_DEBUG,"taskRequest="+taskRequest,loggerCategory);
				//Start by Billy Lim Added in Bug 2142. Refer to modification history
				//orderTransactionDAO.updateOrderTransactionDetails(orderTransactonDetail);
				if(ApplicationConstants.STATUS_RETRY.equalsIgnoreCase(taskRequest)){
					OrderTransactonDetail orderTransactonDetailByTransId = orderTransactionDAO.getOrderTransDetailsByOrderTransId(orderTransactonDetail.getOrderId(), orderTransactonDetail.getCrmOrderId(), loggerCategory);
					if(ApplicationConstants.STATUS_COMPLETED.equalsIgnoreCase(orderTransactonDetailByTransId.getStatus()))
					{							
						OrderTransactonDetail orderCmdTrans =  orderTransactionDAO.getLastCompletedCmdRefId(orderTransactonDetail.getOrderId(), orderTransactonDetail.getCrmOrderId(), loggerCategory);
						taLogger.log(orderTransactonDetailByTransId.getSrcTransId(), orderTransactonDetailByTransId.getTransId(), orderTransactonDetailByTransId.getCmdRowId(), ApplicationConstants.LOG_INFO, "Last cmd_Ref_Id for transId="+orderTransactonDetail.getOrderId() + ", last CmdRowId="+orderCmdTrans.getCmdRowId(), loggerCategory);
						taLogger.log(orderTransactonDetailByTransId.getSrcTransId(), orderTransactonDetailByTransId.getTransId(), orderTransactonDetailByTransId.getCmdRowId(), ApplicationConstants.LOG_INFO, "Updating new correlation ID for transId="+orderTransactonDetail.getOrderId() + ", correlationID="+correlationID, loggerCategory);
						orderTransactionDAO.updateOrderTransactonCorrID(orderTransactonDetail, loggerCategory);
						String source = orderTransactonDetailByTransId.getSource();
						if(ApplicationConstants.SOURCE_OSM.equalsIgnoreCase(source)) {
							OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
							SPLCommonComponent splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
							String tomResponse = splCommonComponent.createGenericTomResponse(orderCmdTrans.getCmdRowId());
							taLogger.log(orderTransactonDetailByTransId.getSrcTransId(), orderTransactonDetailByTransId.getTransId(), orderTransactonDetailByTransId.getCmdRowId(), ApplicationConstants.LOG_INFO, "[OrderTransactionListener][onMessage]Tom response :"+ tomResponse, loggerCategory);
							orderTransactionSender.postMessage(tomResponse, orderTransactonDetail.getCorrId(), "TOMOrderProv");
						}
					}
					else
						orderTransactionDAO.updateOrderTransactionDetails(orderTransactonDetail);
				}
			else
				orderTransactionDAO.updateOrderTransactionDetails(orderTransactonDetail);
			//End by Billy Lim Added in Bug 2142. Refer to modification history
			}
			// Ravi: 20130507: Bug#2116: updated exception handling to update cmd and order status to Error and send tomresponse - Start
		
		} catch (Exception exception) {
			taLogger.log(crmOrderId, tomOrderId, ApplicationConstants.LOG_ERROR, "Inside Catch Exception: ",loggerCategory, exception);
			orderTransactionDAO.insertError(ApplicationConstants.APPLICATION_NAME, crmOrderId, "cmdRefId=-1", "onMessage", "OrderTransactionListener", SPLCommonComponent.getStackTrace(exception));
			
			if(orderTransactonDetail!=null) {
				String cmdRowId = orderTransactonDetail.getCmdRowId();
				if(cmdRowId!=null && !cmdRowId.equals("")) {
					String orderRowId = "";
					orderRowId = orderTransactionDAO.getOrderRowId(crmOrderId, tomOrderId, cmdRowId, loggerCategory);
					//update error status in t_om_cmd_trans table and t_om_order_trans_dtls table
					orderTransactionDAO.updateCommandTransactionStatus(cmdRowId, orderRowId, "1", SPLCommonComponent.getStackTrace(exception));

					String tomResponse = "";
					try {
						SPLCommonComponent splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
						tomResponse = splCommonComponent.createTomResponse(cmdRowId);
					} catch (Exception eException) {
					}

					taLogger.log(crmOrderId, tomOrderId, cmdRowId, ApplicationConstants.LOG_ERROR, "[OrderTransactionListener][onMessage]Tom response :"+ tomResponse, loggerCategory);

					OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
					orderTransactionSender.postMessage(tomResponse, orderTransactonDetail.getCorrId(), "TOMOrderProv");
				}
			}
		}
		// Ravi: 20130507: Bug#2116: updated exception handling to update cmd and order status to Error and send tomresponse - End

	}

	/**
	 *
	 * @param orderTransactionDAO
	 */
	public void setOrderTransactionDAO(DatabaseDAO orderTransactionDAO) {
		this.orderTransactionDAO = orderTransactionDAO;
	}

}