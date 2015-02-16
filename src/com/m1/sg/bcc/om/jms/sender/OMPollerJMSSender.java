package com.m1.sg.bcc.om.jms.sender;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Random;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import com.m1.sg.bcc.om.config.OMPollerConfigurationLoader;
import com.m1.sg.bcc.om.constant.OMPollerConstant;
import com.m1.sg.bcc.om.constant.SQLStatementPool;
import com.m1.sg.bcc.om.database.dao.OMJDBCDao;
import com.m1.sg.bcc.om.util.OMBeanFactory;

/************************************************************************************************************************************
 * 
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.database.dao
 * Description: This class created for service JMS Protocol framework base on spring framework properties the application use this
 * 				class for sending the JMS message to weblogic queue 
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMPollerJMSSender {

	private JmsTemplate jmsTemplate; 
	
	/**
	 *	Constructor create JMSTemplate Beans 
	 */
	public OMPollerJMSSender(){
		this.jmsTemplate = (JmsTemplate) OMBeanFactory.getBean("jmsQueueTemplate");
	}
	
	/**
	 * getter method for JMSTemplate Object
	 * 
	 * @return	JMSTemplate Object
	 */
	public JmsTemplate getJMSTemplate(){
		return this.jmsTemplate;
	}
	
	/**
	 * this method use for sending the message to JMS Queue the JMS properties configure
	 * support create order format to OSM request queue
	 * 
	 * @param queueName		Queue JNDIName you need to send message to this destination
	 * @param payLoad		Message send to queue
	 */
	public void sendMessageToJMSQueue(String queueName, final String payLoad, final Object pollMessageMap, final String orderType){
		
		MessageCreator msgCreator = new MessageCreator() {	
			@Override
			public Message createMessage(Session session) throws JMSException {
				// TODO Auto-generated method stub
				
				Destination replyQueue = jmsTemplate.getDestinationResolver()
						.resolveDestinationName(session, "oracle/communications/ordermanagement/WebServiceResponseQueue", true);
				
				TextMessage textMessage = session.createTextMessage();

				textMessage.setJMSType(OMPollerConstant.JMSProperties.JMS_TYPE.getValue());
				textMessage.setJMSPriority(Integer.valueOf(OMPollerConstant.JMSProperties.PRIORITY.getValue()));
				textMessage.setJMSRedelivered(Boolean.valueOf(OMPollerConstant.JMSProperties.LIMIT.getValue()));
				
				textMessage.setJMSReplyTo(replyQueue);
				textMessage.setJMSCorrelationID(generateCorrelationId(pollMessageMap, orderType));
				
				textMessage.setStringProperty("URI", "/osm/wsapi");
				textMessage.setStringProperty("_wls_mimehdrContent_Type", "text/xml; charset=utf-8");
				
				textMessage.setText(payLoad);
				
				return textMessage;
			}
		};
		
		jmsTemplate.send(queueName, msgCreator);
		
		/* update status in CRM Table to completed */
//		updatePollerStatusOrderHeader(pollMessageMap, orderType);
	}
	
	private String generateCorrelationId(Object pollMessageMap, String orderType){
		Map<?, ?> pollMessageMapStub = (Map<?, ?>) pollMessageMap;
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(pollMessageMapStub.get(OMPollerConstant.OrderHeaderColumnName.ORDER_ID.getValue()));
		strBuffer.append("_");
		strBuffer.append(pollMessageMapStub.get(OMPollerConstant.OrderHeaderColumnName.PRIORITY.getValue()));
		strBuffer.append("_");
		strBuffer.append(pollMessageMapStub.get(OMPollerConstant.OrderHeaderColumnName.REVISION_NUMBER.getValue()));
		strBuffer.append("_");
		strBuffer.append(generateRandomNumber(100000000, 999999999));
		strBuffer.append("_");
		strBuffer.append(orderType);
		return strBuffer.toString();
	}
	
	@SuppressWarnings("unused")
	private void updatePollerStatusOrderHeader(Object pollMessageMap, String orderType){
		
		String tableName = OMPollerConfigurationLoader.getOrderTypeTableMapping().get(orderType);
		
		Map<?, ?> pollMessageMapStub = (Map<?, ?>) pollMessageMap;
		OMJDBCDao omJDBCDao = new OMJDBCDao();
		JdbcTemplate crmTemplate = omJDBCDao.getCRMJDBCTemplate();
		Object[] paramsMap = new Object[]{pollMessageMapStub.get(OMPollerConstant.OrderHeaderColumnName.ORDER_ID.getValue())};
		crmTemplate.update(MessageFormat.format(SQLStatementPool.SQL_UPDATE_CREATION_STATUS_CRM, tableName)
				, paramsMap, OMJDBCDao.jdbcTypeMapping(paramsMap));
	}
	
	private Integer generateRandomNumber(Integer min, Integer max){
		Random randomObj = new Random();
		return randomObj.nextInt(max - min) + min;
	}
	
}
