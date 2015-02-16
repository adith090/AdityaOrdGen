package com.m1.bcc.spl.translator;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.w3c.dom.Document;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.model.OrderTransactonDetail;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.sender.OrderTransactionSender;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.ThreadLocalInstance;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 22/05/2013					Sudharsan				Implemented Radius Server 
 * 27/05/2013   				Sudharsan				Implemented ADS 
 * 12/09/2013					Sudharsan				Saving response in t_om_cmd_response table
 * 08/10/2013					Ravikumar.G				updated for replace decrypted password in request message
 * 31/10/2013					Kalyan			        added logic ads interface to automatically contruct http request
 * 12/11/2013					Ravikumar G				Bug#20421 - to implement multithreading using thread poll task executor
 * 20/11/2013					Ravikumar G				Bug#20538 - Added timeout and read timeout
 * 09/12/2013					Ravikumar G				Bug#21454 - updated stub column in cmd_trans and exception handling 
 * 16/01/2014					Ravikumar G				Bug#23590 - to update request message in cmd_trans table
 * 17/01/2014					Ravikumar G				Bug#23710 - Updated cmd_trans column row_id type to number
 * 27/01/2014					Ravikumar G				Bug#23086 - added condition if source=OSM then send tomresponse
 * 03/02/2014					Ravikumar G				Bug#24615 - Updated Http interface to append param with location
 ******************************************************************************/
public class HttpHandler {

	TALogger taLogger;
	private SPLCommonComponent splCommonComponent ;
	String loggerCategory="";
	private Document requestDocument = null;
	String appendDotRes="";


	@Async
	public void processHttpCmd(Map<String, Object> commandDetails, JdbcDatabaseDAO jdbcDatabaseDAO, DatabaseDAO databaseDAO) {

		String cmdRefId="";
		String rowId="";
		String transId="";
		String srcTransId="";
		String interfaceName="";
		String responseMessage="";
		String orderRowId = "";

		taLogger = TALogger.getTALogger();

		boolean isStub = false;
		try {
			splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
			Properties properties = SPLCommonComponent.getSystemStubProperty();
			if(commandDetails!=null) {
				isStub = false;

				cmdRefId = (String)commandDetails.get(ApplicationConstants.COLUMN_CMD_REF_ID);
				CommandTransDetails cmdSystemDetails = databaseDAO.getSystemDetails(cmdRefId);

				String systemName = cmdSystemDetails.getSystemName();
				loggerCategory =systemName.toUpperCase()+"logging";
				splCommonComponent.setLoggerCategory(loggerCategory);
				rowId = "" + (BigDecimal)commandDetails.get(ApplicationConstants.COLUMN_ROW_ID);
				transId = (String)commandDetails.get(ApplicationConstants.COLUMN_TRANS_ID);
				srcTransId = (String)commandDetails.get(ApplicationConstants.COLUMN_SRC_TRANS_ID);
				orderRowId = (String)commandDetails.get("ORDER_ROW_ID");

				databaseDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_RECEIVED);

				splCommonComponent.setLogSrcTransId(srcTransId);
				splCommonComponent.setLogTransId(transId);
				splCommonComponent.setLogCmdRowId(rowId);

				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]Command reference id :" + cmdRefId, loggerCategory);
				String status = (String)commandDetails.get(ApplicationConstants.COLUMN_STATUS);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]Status of command :" + status, loggerCategory);

				String systemTechMethod = cmdSystemDetails.getTechMethod();

				String isAuthTokenRequired = cmdSystemDetails.getAuthTokenRequired();
				interfaceName = cmdSystemDetails.getInterfaceName();
				String wsdlLocation = cmdSystemDetails.getLocation();
				String userName = cmdSystemDetails.getUserName();
				String password = cmdSystemDetails.getPwd();
				String acctType=cmdSystemDetails.getAccountType();
				String searchParamTag = cmdSystemDetails.getSearchParamTag();
				String replaceParamTag = cmdSystemDetails.getReplaceParamTag();
				String requestMsgFlag = cmdSystemDetails.getRequestMsgFlag();
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]Username is: "+userName+" , Password is: "+password, loggerCategory);
				isStub = SPLCommonComponent.getStubbing(properties, systemName);

				// Ravi: added for replace decrypted password in request message
				Properties propertiesKey = (Properties) BeanFactory.getBean("properties");
				final String key = propertiesKey.getProperty("KEY");
				final String decryptedKey = getDecryptedPwd(password,key);

				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]System tech method :" + systemTechMethod, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]System name :" + systemName, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]Is auth token required :" + isAuthTokenRequired, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]Interface name :" + interfaceName, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]Wsdl location :" + wsdlLocation, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]Stub :" + isStub, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]searchParamTag :" + searchParamTag, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]replaceParamTag :" + replaceParamTag, loggerCategory);
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]requestMsgFlag : "+requestMsgFlag, loggerCategory);

				List<CommandTransDetails> cmdTransDetailsList = databaseDAO.getCmdTransDetails(rowId, null, srcTransId, transId,systemName);
				StringBuffer sb=new StringBuffer(wsdlLocation);
				sb.append( "?" );
				Iterator<CommandTransDetails> cmdTransDetails = cmdTransDetailsList.iterator();
				String requestMessage = "";
				while(cmdTransDetails.hasNext()) {
					CommandTransDetails commandTransDetails = (CommandTransDetails) cmdTransDetails.next();
					if(commandTransDetails!=null) {
						String paramName = commandTransDetails.getParamName();
						String paramValue = commandTransDetails.getParamValue();
						taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG, "[HttpHandler][loadHttpSystem]"+"PARAM NAME : " + paramName + " , PARAM VALUE : " + paramValue, loggerCategory);

						if((systemName != null) && (systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_ADS) || systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_EAS) 
								|| systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_RS))){
							sb.append(paramName);
							sb.append( "=" );
							sb.append(paramValue);
							if(cmdTransDetails.hasNext())
							sb.append("&");	
						}
						
					}
					else {
						taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]CommandTransDetails :" + commandTransDetails, loggerCategory);
					}

				}

				if(ApplicationConstants.SYSTEM_EAS.equals(systemName)||ApplicationConstants.SYSTEM_ADS.equals(systemName)) 
				{
					appendDotRes=ApplicationConstants.SYSTEM_APPEND_RES_DOTTXT;
				}
				else if(ApplicationConstants.SYSTEM_RS.equals(systemName)) 
				{
					appendDotRes=ApplicationConstants.SYSTEM_APPEND_RES_DOTXML;
				}

				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]Stub is :"+isStub, loggerCategory);

				requestMessage=sb.toString();
				taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]Final request message after setting values :" + requestMessage, loggerCategory);
				databaseDAO.updateCmdTrans(srcTransId,  transId, rowId, requestMsgFlag, requestMessage, isStub, loggerCategory);
				
				if(isStub) {
					String stubResponsePath = splCommonComponent.getFilePath(systemName, interfaceName, ApplicationConstants.APPEND_RESPONSE,appendDotRes);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]StubResponsePath is :"+stubResponsePath, loggerCategory);
					responseMessage = splCommonComponent.getMessage(stubResponsePath);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_DEBUG,"[HttpHandler][loadHttpSystem]ResponseMessage is :"+responseMessage, loggerCategory);
					taLogger.log(srcTransId,transId,rowId, ApplicationConstants.LOG_INFO,"[HttpHandler][loadHttpSystem]Response message received .. ", loggerCategory);
					splCommonComponent.saveMessage(responseMessage, interfaceName, rowId, srcTransId, appendDotRes);
					databaseDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);

				}else {
					// Ravi: added for replace decrypted password in request message
					requestMessage = requestMessage.replace("#password#", decryptedKey);
					CommandTransDetails threadVariable = new CommandTransDetails();
					threadVariable.setSystemName(systemName);
					threadVariable.setInterfaceName(interfaceName);
					threadVariable.setQueryString(requestMessage);
					if(cmdSystemDetails!=null) {
						threadVariable.setTimeOut(cmdSystemDetails.getTimeOut());
						threadVariable.setReadTimeOut(cmdSystemDetails.getReadTimeOut());
					}
					ThreadLocalInstance.set(threadVariable);
					databaseDAO.updateCommandTransactionStatus(rowId, ApplicationConstants.STATUS_SENT);
					ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
					ThreadLocalInstance.remove();
					RequestGateway requestGateway = context.getBean("requestGateway",RequestGateway.class);
					taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_INFO,"[HttpHandler][loadHttpSystem]["+systemName+"]Sending message through gateway..",loggerCategory);
					Object httploginreply = requestGateway.echo(requestMessage);
					taLogger.log(srcTransId,transId,rowId,ApplicationConstants.LOG_INFO,"[HttpHandler][loadHttpSystem]["+systemName+"]httploginreply="+httploginreply,loggerCategory);
					responseMessage= (String) httploginreply;
				}

				databaseDAO.saveCommandResponse(rowId, cmdRefId, transId, responseMessage, interfaceName, false);
			}


		}
		catch(Exception exception) {

			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_ERROR,"[HttpHandler][loadHttpSystem]Exception is :"+exception, loggerCategory);
			jdbcDatabaseDAO.insertError("SPL", srcTransId, cmdRefId, "Translator Handler", "ERR_CODE", exception.getMessage());
			jdbcDatabaseDAO.instructorErrorUpdate(rowId, "1", exception.getMessage(), loggerCategory);
			jdbcDatabaseDAO.orderTransDetailsErrorUpdate(orderRowId);

			OrderTransactonDetail orderTransDtls = databaseDAO.getOrderTransDtlsByRowId(rowId, transId, srcTransId, orderRowId, loggerCategory);
			String source = orderTransDtls.getSource();
			
			if(ApplicationConstants.SOURCE_OSM.equalsIgnoreCase(source)) {
				SPLCommonComponent splCommonComponent = null; 
				String tomResponse = "";
				try {
					splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
					tomResponse = splCommonComponent.createTomResponse(rowId);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
	
				taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_ERROR,"[Handler][handleMessage]TomResponse : "+ tomResponse, loggerCategory);
	
				OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
				String corrId = orderTransDtls.getCorrId();
				taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_ERROR,"[Handler][handleMessage]Correlation Id : "+ corrId, loggerCategory);
				orderTransactionSender.postMessage(tomResponse, corrId, "TOMOrderProv");
			}

		}finally {
			ThreadLocalInstance.remove();
		}
	}

	private String getDecryptedPwd(String password,String key) throws Exception {
		String decryptedKey="";
		if(password!=null && !password.trim().equals("")) {
			decryptedKey = SPLCommonComponent.Crypt(0, key, password);
		}
		return decryptedKey;
	}

}
