package com.m1.bcc.spl.sender;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.util.BeanFactory;

import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/11/2012					Ravikumar G				Created
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 18/11/2013					Ravikumar G				Bug#20568: Added method postBillingResponse
 ******************************************************************************/

public class OrderTransactionSender {
	private JmsTemplate jmsTemplate;
	TALogger taLogger = TALogger.getTALogger();
	

	public OrderTransactionSender() {
	}

	/**
	 *
	 * @param jmsTemplate
	 */
	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	/** postMesage - used to post message in osm out queue
	 *
	 * @param response
	 */
	public void postMessage(final String response, final String corrId, final String jmsType) {
		MessageCreator messageCreator = new MessageCreator() {

			@Override
			public Message createMessage(Session session) throws JMSException {
				taLogger.log("OrderTransactionSender", ApplicationConstants.LOG_DEBUG,
						"INSIDE createMessage","adapterlogging");	
				
				TextMessage splToTomMessage = session.createTextMessage(response);
				taLogger.log("OrderTransactionSender", ApplicationConstants.LOG_DEBUG,
						"Response="+response,"adapterlogging");	
				splToTomMessage.setJMSCorrelationID(corrId);
				taLogger.log("OrderTransactionSender", ApplicationConstants.LOG_DEBUG,
						"CorrId="+corrId,"adapterlogging");	
				//splToTomMessage.setJMSType("ATask_RetrieveSysResp");
				splToTomMessage.setJMSType(jmsType);
				return splToTomMessage;
			}
		};
		
		if(this.jmsTemplate == null){this.jmsTemplate = (JmsTemplate) BeanFactory.getBean("jmsQueueTemplate");}
		
		jmsTemplate.send("jmsqueue/om/osm/SPLResponse", messageCreator);
	}
	
	public void postBillingResponse(final String response, final String corrId) {
		MessageCreator messageCreator = new MessageCreator() {

			@Override
			public Message createMessage(Session session) throws JMSException {
				taLogger.log("OrderTransactionSender", ApplicationConstants.LOG_DEBUG, "INSIDE createMessage","adapterlogging");	
				TextMessage splToTomMessage = session.createTextMessage(response);
				taLogger.log("OrderTransactionSender", ApplicationConstants.LOG_DEBUG, "Response="+response,"adapterlogging");	
				splToTomMessage.setJMSCorrelationID(corrId);
				taLogger.log("OrderTransactionSender", ApplicationConstants.LOG_DEBUG, "CorrId="+corrId,"adapterlogging");	
				splToTomMessage.setJMSType("TOMOrderProvBill");
				return splToTomMessage;
			}
		};
		jmsTemplate.send("jmsqueue/om/osm/SPLResponse", messageCreator);
	}
	/**
	 * 
	 * @param response
	 * @param corrId
	 */
	public void postMessageAck(final String response, final String corrId) {
		MessageCreator messageCreator = new MessageCreator() {

			@Override
			public Message createMessage(Session session) throws JMSException {
				taLogger.log("[OrderTransactionSender][postMessageAck]", ApplicationConstants.LOG_DEBUG,"INSIDE createMessage","adapterlogging");	
				TextMessage splToOsmAckMessage = session.createTextMessage(response);
				taLogger.log("[OrderTransactionSender][postMessageAck]", ApplicationConstants.LOG_DEBUG,"Response="+response,"adapterlogging");	
				splToOsmAckMessage.setJMSCorrelationID(corrId);
				taLogger.log("[OrderTransactionSender][postMessageAck]", ApplicationConstants.LOG_DEBUG,"CorrId="+corrId,"adapterlogging");	
				splToOsmAckMessage.setJMSType("TOMOrderBillingNotificationSIProv");
				taLogger.log("[OrderTransactionSender][postMessageAck]", ApplicationConstants.LOG_DEBUG,"JMSType="+splToOsmAckMessage.getJMSType(),"adapterlogging");
				return splToOsmAckMessage;
			}
		};
		jmsTemplate.send("jmsqueue/om/osm/SPLResponse", messageCreator);
	}
}
