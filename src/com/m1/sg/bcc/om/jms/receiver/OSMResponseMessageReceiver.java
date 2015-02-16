package com.m1.sg.bcc.om.jms.receiver;

import java.text.MessageFormat;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.jdbc.core.JdbcTemplate;

import com.m1.sg.bcc.om.config.OMPollerConfigurationLoader;
import com.m1.sg.bcc.om.constant.OMPollerLoggerConstant;
import com.m1.sg.bcc.om.constant.SQLStatementPool;
import com.m1.sg.bcc.om.database.dao.OMJDBCDao;
import com.m1.sg.bcc.om.logger.OMPollerLogger;

public class OSMResponseMessageReceiver implements MessageListener {

	@Override
	public void onMessage(Message message) {
		// TODO Auto-generated method stub
		
		TextMessage textMessage = (TextMessage) message;
		
		try {
		
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.RESPONSEMESSAGELOGGER.getValue()
					, message.getJMSCorrelationID(), new String[]{textMessage.getText()});
			
			String[] correlationIdContext = textMessage.getJMSCorrelationID().split("_");
			
			if(textMessage.getText().contains("<n1:Id>") && textMessage.getText().contains("</n1:Id>")){
//				updatePollerStatusOrderHeader(correlationIdContext[0], 
//						MessageFormat.format(SQLStatementPool.SQL_UPDATE_COMPLETE_STATUS_CRM, 
//								OMPollerConfigurationLoader.getOrderTypeTableMapping().get(correlationIdContext[4]))
//								);
				
			} else {
				
				String updateSQL = MessageFormat.format(SQLStatementPool.SQL_UPDATE_ERROR_STATUS_CRM, 
						new Object[]{OMPollerConfigurationLoader.getOrderTypeTableMapping().get(correlationIdContext[4])
						, OMPollerConfigurationLoader.getOrderStatus().get(1)});
				
				OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
						, OMPollerLoggerConstant.LoggerName.RESPONSEMESSAGELOGGER.getValue()
						, message.getJMSCorrelationID(), new String[]{updateSQL});
				
				updatePollerStatusOrderHeader(correlationIdContext[0], updateSQL);
			}
		} catch (Exception e){
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.ERROR.getSeverity()
					, OMPollerLoggerConstant.LoggerName.RESPONSEMESSAGELOGGER.getValue()
					, "ResponseMessageException", "", e);
			
		}
		
	}
	
	private void updatePollerStatusOrderHeader(Object orderId, String sqlId){
		OMJDBCDao omJDBCDao = new OMJDBCDao();
		JdbcTemplate crmTemplate = omJDBCDao.getCRMJDBCTemplate();
		Object[] paramsMap = new Object[]{orderId};
		crmTemplate.update(sqlId
				, paramsMap, OMJDBCDao.jdbcTypeMapping(paramsMap));
	}

}
