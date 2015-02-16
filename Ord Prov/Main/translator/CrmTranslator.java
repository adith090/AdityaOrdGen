package com.m1.bcc.spl.translator;


import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.w3c.dom.Document;
import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.SPLCommonComponent;


import common.util.TALogger;

public class CrmTranslator {
	
JdbcDatabaseDAO jdbcDatabaseDAO;
	
	public void setJdbcDatabaseDAO(JdbcDatabaseDAO jdbcDatabaseDAO) {
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}
	
	public void CreateCrmRequestXml(Message<?> message_CRM) {
		
		TALogger taLogger = TALogger.getTALogger();
		
		try {
			
				taLogger.log("systemName", ApplicationConstants.LOG_DEBUG, "Inside CRM Translator CreateCrmRequestXml method", "adapterlogging");
			
			    ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
		      	RequestGateway requestGateway = context.getBean("requestGateway",RequestGateway.class);
				Object reply = requestGateway.echo("Request");
				
			    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
				domFactory.setNamespaceAware(false); 
				DocumentBuilder builder = domFactory.newDocumentBuilder();
				Document doc = builder.parse((InputStream) reply);
			
            } 
		catch(Exception e)
		{
		jdbcDatabaseDAO.insertError(ApplicationConstants.APPLICATION_NAME, "ORDERID", "CMD_REF_ID", "CreateCrmRequestXml","ERR_CODE", SPLCommonComponent.getStackTrace(e));
		
			taLogger.log("systemName",ApplicationConstants.LOG_ERROR,"Inside CRM Translator CreateCrmRequestXml method","adapterlogging", e);
		}	
	
	}
}