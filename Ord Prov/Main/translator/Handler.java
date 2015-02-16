package com.m1.bcc.spl.translator;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.xml.sax.SAXException;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.model.OrderTransactonDetail;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.sender.OrderTransactionSender;
import com.m1.bcc.spl.tcpadapter.SocketAdapter;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.ThreadLocalInstance;

import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 15/04/2013					Sudharsan				Changes made for logger category
 * 23/04/2013					Ravikumar G				Bug#1976: Changed to set correct correlation id in JMS Header
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 17/05/2013					Sudharsan				Implemented Engineering Authentication server
 * 21/05/2013					Ravikumar G				Bug#2184 : Update the STUB flag in cmd_trans table
 * 22/05/2013					Sudharsan				Implemented Radius server
 * 05/06/2013   				Sudharsan				Implemented SAS
 * 17/06/2013					Ravikumar G				Bug#2346: update cmd trans status to Received
 * 03/10/2013					Ravikumar G				get interface name from cmd_dest_ref table and pass to IL and U2KOPCO
 * 12/11/2013					Ravikumar G				Bug#20421 - to implement multithreading using thread poll task executor
 * 17/04/2013					Ravikumar G				Bug#20538- Separate timeout setting for all interfaces
 * 16/01/2014					Ravikumar G				Bug#23590 - to update request message in cmd_trans table
 * 17/01/2014					Ravikumar G				Bug#23710 - Updated cmd_trans column row_id type to number
 * 27/01/2014					Ravikumar G				Bug#23086 - added condition if source=OSM then send tomresponse
 ******************************************************************************/

public class Handler {

	TALogger taLogger = TALogger.getTALogger();
	private DatabaseDAO commandHandlerDAO;
	private ThreadPoolTaskExecutor taskExecutor;
	
	JdbcDatabaseDAO jdbcDatabaseDAO;

	public void setJdbcDatabaseDAO(JdbcDatabaseDAO jdbcDatabaseDAO)
	{
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}
	
	public void setCommandHandlerDAO(DatabaseDAO commandHandlerDAO) {
		this.commandHandlerDAO = commandHandlerDAO;
	}
	
	DatabaseDAO databaseDAO;
	

	public void setDatabaseDAO(DatabaseDAO databaseDAO) {
		this.databaseDAO = databaseDAO;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	
	@Async
	@ServiceActivator
	public void handleMessage(Message<?> message) throws IOException,XPathExpressionException, ParserConfigurationException,SAXException, SQLException, Exception {
		ArrayList<Map<String, Object>> commandDetailsList = (ArrayList<Map<String, Object>>) message.getPayload();
		for(final Map<String, Object> commandDetailsMap : commandDetailsList) {
			String cmdRefId = (String) commandDetailsMap.get("CMD_REF_ID");
			final CommandTransDetails commandTransDetails = jdbcDatabaseDAO.getCmdDestRefDtls(cmdRefId);
			String systemTechMethod = commandTransDetails.getTechMethod();
			String systemName = commandTransDetails.getSystemName();

			if(systemTechMethod.equalsIgnoreCase(ApplicationConstants.SYSTEM_TECH_METHOD_HTTP) && !systemName.equalsIgnoreCase(ApplicationConstants.SYSTEM_MMSC)) {
				final HttpHandler httpHandler = new HttpHandler();
				taskExecutor.submit(new Runnable() {
					
					@Override
					public void run() {
						httpHandler.processHttpCmd(commandDetailsMap, jdbcDatabaseDAO, databaseDAO);
					}
				});
			}else {
				taskExecutor.submit(new Runnable() {
					
					@Override
					public void run() {
						processCommand(commandDetailsMap, commandTransDetails);
					}
				});
			}
			
		}
	}

	@Async
	private void processCommand(Map<String, Object> map, CommandTransDetails commandTransDetails) {
		String systemname = null;
		String userName = null;
		String password = null;
		String cmdRefId = null;
		String srcTransId = null;
		String cmdRefIdErr=null;
	    String transactionId=null;
	    String parRowId=null;
	    String orderRowId=null;
	    String techMethod=null;
	    String interfaceName=null;
	    SocketAdapter socketAdapter = null;
	    SocketAdapter iLReqSocketAdapter = null;

	    //Sudharsan : 20130415 : Bug 1894 : Moved class level variables inside this method : Start
	    String loggercategory = "adapterlogging";
		String u2kopcoLoggerCategory="U2KOPCOlogging";
		String ilLoggerCategory = "ILrequestadaptorlogging";
		String sasLoggerCategory = "SASlogging";
		//Sudharsan : 20130415 : Bug 1894 : Moved class level variables inside this method : End

		try {
		    Properties propertiesKey = (Properties) BeanFactory.getBean("properties");
			final String Key = propertiesKey.getProperty("KEY");
	
			List<Map<String, Object>> list = null;
	
			List<List<Map<String, Object>>> totalMsg_IL = new ArrayList<List<Map<String, Object>>>();
	
			int commandCount = 0;
			int commandILCount = 1;
			ILMessage iLMessage = null;
			U2KMessage u2KMessage = null;
			Properties properties = SPLCommonComponent.getSystemStubProperty();
	
			parRowId = "" + (BigDecimal) map.get("ROW_ID");
	
			cmdRefId = (String) map.get("CMD_REF_ID");
	
			systemname = commandTransDetails.getSystemName();
			//databaseDAO.setSysName(systemname);
			String systemTechMethod = commandTransDetails.getTechMethod();
	
			// Ravi: 20130617: Bug#2346: to update cmd trans status to Received - Start
			if(!ApplicationConstants.SYSTEM_TECH_METHOD.equals(systemTechMethod)) {
				taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]systemTechMethod is : " + systemTechMethod, loggercategory);
				commandHandlerDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_RECEIVED);
			}
			// Ravi: 20130617: Bug#2346: to update cmd trans status to Received - End
	
			taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Systemname is : " + systemname,loggercategory);
			
			cmdRefIdErr = (String) map.get("CMD_REF_ID");
			srcTransId = (String) map.get("SRC_TRANS_ID");
			transactionId= (String) map.get("TRANS_ID");
			orderRowId = (String)map.get("ORDER_ROW_ID");
			techMethod = commandTransDetails.getTechMethod();
			interfaceName=commandTransDetails.getInterfaceName();
			String requestMsgFlag = commandTransDetails.getRequestMsgFlag();
			//databaseDAO.setIntfName(interfaceName);
			
			String retTime = "";
			Date d = new Date();
			SimpleDateFormat timeStampFormatter = new SimpleDateFormat("dd-MMM-yyyy-hh-mm-ss");
			retTime = timeStampFormatter.format(d);
			taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Getting command parameter values for row_id :"+parRowId,loggercategory);
			list = jdbcDatabaseDAO.getCmdParamValues(parRowId);
			boolean isStub = SPLCommonComponent.getStubbing(properties, systemname);
			taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Stub is "+isStub+" for "+systemname,loggercategory);
			// Ravi : 20130521 : Bug 2184 : Update the STUB flag in cmd_trans : Start
			//commandHandlerDAO.updateStubbingStatus(srcTransId, transactionId, parRowId, isStub, loggercategory);
			/*if(ApplicationConstants.SYSTEM_TECH_METHOD_SOCKET.equalsIgnoreCase(systemTechMethod) && ApplicationConstants.SYSTEM_MMSC.equalsIgnoreCase(systemname))
			commandHandlerDAO.updateCmdTrans(srcTransId,  transactionId, parRowId, requestMsgFlag, requestMessage, isStub, loggercategory);*/
			// Ravi : 20130521 : Bug 2184 : Update the STUB flag in cmd_trans : End
			
			taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Tech method : "+techMethod,loggercategory);
	
			SPLCommonComponent splCommonComponent=new SPLCommonComponent(taLogger,loggercategory);
			//Ravi: Modified to implement Socket
			if (systemname.equalsIgnoreCase("IL")||systemname.equalsIgnoreCase("SAS")) {
	
				iLMessage = new ILMessage(jdbcDatabaseDAO);
				String loginResponse = "";
				if(systemname.equalsIgnoreCase("IL"))
					loggercategory=ilLoggerCategory;
				if(systemname.equalsIgnoreCase("SAS"))
					loggercategory=sasLoggerCategory;
				
					userName = commandTransDetails.getUserName();
					password = commandTransDetails.getPwd();
					String accountType = commandTransDetails.getAccountType();
	
					taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG,"[Handler][handleMessage]UserName for "+systemname+" :"+userName,loggercategory);
					taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG,"[Handler][handleMessage]Password for "+systemname+" :"+password,loggercategory);
					taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG,"[Handler][handleMessage]Stub is "+isStub+" for "+systemname,loggercategory);
					String decryptedKey="";
					if(password!=null && !password.trim().equals("")){
					decryptedKey = SPLCommonComponent.Crypt(0, Key, password);
					}
					//Comment next line
					//taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG,"[Handler][handleMessage]DecryptedKey :"+decryptedKey,loggercategory);
					String loginRequest = iLMessage.getLoginRequestMessage(userName, decryptedKey);
					if(isStub) {
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Logging in ",loggercategory);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[Handler][handleMessage]Request message for login is :" + loginRequest,loggercategory);
						splCommonComponent.setLoggerCategory(loggercategory);
						String response =splCommonComponent.getMessage("conf/spl/adaptors/"+systemname+"/response/LoginResponse.txt");
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[Handler][handleMessage]Login response is :" + response,loggercategory);
	
						if(iLMessage.isILRequestSuccessful(srcTransId,transactionId,parRowId, response, "LOGIN",loggercategory)) {
							taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Login is successful", loggercategory);
							taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Getting request message..", loggercategory);
	
							String reqMessage = iLMessage.getRequestMessage(list, cmdRefId, parRowId, accountType,loggercategory,srcTransId,transactionId,parRowId);
	
							taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Sending request  message.. ", loggercategory);
							taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Request message :"+reqMessage, loggercategory);
	
								taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Forming register message.." , loggercategory);
								String regMessage = iLMessage.getRegisterAck(accountType);
								commandHandlerDAO.updateCmdTrans(srcTransId,  transactionId, parRowId, requestMsgFlag, reqMessage, isStub, loggercategory);
								taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Register message is :"+regMessage, loggercategory);
	
								response =splCommonComponent.getMessage("conf/spl/adaptors/"+systemname+"/response/ResponseMessage.txt");
								taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Getting response message..", loggercategory);
								taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Response message is :"+response, loggercategory);
	
								jdbcDatabaseDAO.saveCommandResponse(parRowId,cmdRefId, transactionId, response , systemname);
	
						}else {
							jdbcDatabaseDAO.saveCommandResponse(parRowId,cmdRefId, transactionId, response , systemname);
							taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Login is not successful" ,loggercategory);
						}
	
					}else {
						// Ravi: get interface name from cmd_dest_ref
						iLReqSocketAdapter = SocketAdapter.initSocketAdapter(interfaceName, loggercategory, srcTransId, transactionId, parRowId, systemname);
						iLReqSocketAdapter.setLogCategory(loggercategory);
						iLReqSocketAdapter.setSrcTransId(srcTransId);
						iLReqSocketAdapter.setTransactionId(transactionId);
						iLReqSocketAdapter.setParRowId(parRowId);
	
						iLReqSocketAdapter.setEOF("$END$");
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Logging in" + loginRequest,loggercategory);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Request message for login :" + loginRequest,loggercategory);
						loginResponse = iLReqSocketAdapter.sendAndReceive(loginRequest, "LOGIN");
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Receiving login response",loggercategory);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[Handler][handleMessage]Login Response :" + loginResponse,loggercategory);
						if(iLMessage.isILRequestSuccessful(srcTransId,transactionId,parRowId, loginResponse, "LOGIN",loggercategory)) {
							taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Login is successful", loggercategory);
							taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Getting request message..", loggercategory);
	
							String reqMessage = iLMessage.getRequestMessage(list, cmdRefId, parRowId, accountType,loggercategory,srcTransId,transactionId,parRowId);
	
							commandHandlerDAO.updateCmdTrans(srcTransId,  transactionId, parRowId, requestMsgFlag, reqMessage, isStub, loggercategory);
							
							String requestAck = iLReqSocketAdapter.sendAndReceive(reqMessage, "REQUEST");
	
							jdbcDatabaseDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_SENT);
	
							taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Request Ack is:" + requestAck, loggercategory);
	
							// insert response payload in command response table
							if(!iLMessage.isILRequestSuccessful(srcTransId,transactionId,parRowId, requestAck, "ACK",loggercategory)) {
								jdbcDatabaseDAO.saveCommandResponse(parRowId,cmdRefId, transactionId, requestAck , systemname);
							}
	
						}else {
							// insert login error response in command response table
							jdbcDatabaseDAO.saveCommandResponse(parRowId,cmdRefId, transactionId, loginResponse , systemname);
							taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Login is not successful.." ,loggercategory);
						}
						commandILCount = commandILCount + 1;
	
						// Logout the request socket adapter
						String logoutRequest = "SAS3\n" + "0\n" + "SAS_ACTION LOGOUT\n" + "$END$";
						iLReqSocketAdapter.logout(iLReqSocketAdapter, logoutRequest);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Logged out", loggercategory);
					}
	//			}
	
			} else if (systemname.equalsIgnoreCase("U2KOPCO")) {
	
				u2KMessage = new U2KMessage(jdbcDatabaseDAO);
				userName=commandTransDetails.getUserName();
				password=commandTransDetails.getPwd();
				loggercategory=u2kopcoLoggerCategory;
				//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[Handler][handleMessage]Username for U2KOPCO : "+userName+" , Password : "+password ,u2kopcoLoggerCategory);
				//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[Handler][handleMessage]Stub is "+isStub+" for "+systemname,u2kopcoLoggerCategory);
	
				String decryptedKey="";
				if(password!=null && !password.trim().equals("")){
				decryptedKey = SPLCommonComponent.Crypt(0, Key, password);
				}
				//Comment next line
				//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[Handler][handleMessage]DecryptedKey :"+decryptedKey,u2kopcoLoggerCategory);
	
				if(isStub) {
					String LoginMessage = "LOGIN" + ":::" + "123" + "::" + "UN="+ userName + "," + "PWD=" + decryptedKey + ";";
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Logging in..",u2kopcoLoggerCategory);
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[Handler][handleMessage]Request message for login is :" + LoginMessage,u2kopcoLoggerCategory);
					splCommonComponent.setLoggerCategory(u2kopcoLoggerCategory);
					String response =splCommonComponent.getMessage("conf/spl/adaptors/U2K/response/LoginResponse.txt");
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[Handler][handleMessage]Login response is :" + response,u2kopcoLoggerCategory);
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Getting request message..", u2kopcoLoggerCategory);
	
					String reqMessage = u2KMessage.getRequestMessage(list, cmdRefId,srcTransId,transactionId,parRowId,u2kopcoLoggerCategory);
					commandHandlerDAO.updateCmdTrans(srcTransId,  transactionId, parRowId, requestMsgFlag, reqMessage, isStub, loggercategory);
					
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Request message sending ..", u2kopcoLoggerCategory);
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[Handler][handleMessage]Request message is :"+reqMessage, u2kopcoLoggerCategory);
	
					response =splCommonComponent.getMessage("conf/spl/adaptors/U2K/response/ResponseMessage.txt");
	
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Response message :"+response, u2kopcoLoggerCategory);
	
					jdbcDatabaseDAO.saveCommandResponse(parRowId,cmdRefId, transactionId, response , systemname);
	
				}else {
					//16012013 Yohan implement socket
					// Ravi: get interface name from cmd_dest_ref
					socketAdapter = SocketAdapter.initSocketAdapter(interfaceName, u2kopcoLoggerCategory, srcTransId, transactionId, parRowId, systemname);
					socketAdapter.setLogCategory(u2kopcoLoggerCategory);
					socketAdapter.setSrcTransId(srcTransId);
					socketAdapter.setTransactionId(transactionId);
					socketAdapter.setParRowId(parRowId);
	
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Socket opened",u2kopcoLoggerCategory);
					socketAdapter.setEOF(";");
					//String LoginMessage = "LOGIN" + ":::" + "123" + "::" + "UN="+ userName + "," + "PWD=" + decryptedKey + ";";
					
					String LoginMessage = "LOGIN" + ":::" + "123" + "::" + "UN="+ userName + "," + "PWD=#password#;";
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Login message :"+LoginMessage,u2kopcoLoggerCategory);
					LoginMessage = LoginMessage.replace("#password#", decryptedKey);
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Logging in ",u2kopcoLoggerCategory);
					//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]DecryptedKey :"+decryptedKey,u2kopcoLoggerCategory);
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Sending Login message..",u2kopcoLoggerCategory);
					//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Login message :"+LoginMessage,u2kopcoLoggerCategory);
					String responseMessage = socketAdapter.sendAndReceive(LoginMessage);
					taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Response for login request is :"+responseMessage , u2kopcoLoggerCategory);
	
					if((u2KMessage.U2KOPCOrequestIsSuccessful(responseMessage,srcTransId,transactionId,parRowId,u2kopcoLoggerCategory))) {
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Login Successful",u2kopcoLoggerCategory);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Getting request message..",u2kopcoLoggerCategory);
						String reqMessage = "";
	
						// Ravi: get request message using U2KMessage.getRequestMessage
						reqMessage = u2KMessage.getRequestMessage(list, cmdRefId,srcTransId,transactionId,parRowId,u2kopcoLoggerCategory);
						commandHandlerDAO.updateCmdTrans(srcTransId,  transactionId, parRowId, requestMsgFlag, reqMessage, isStub, loggercategory);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Sending request message..", u2kopcoLoggerCategory);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[Handler][handleMessage]Request message :"+reqMessage, u2kopcoLoggerCategory);
	
						responseMessage = socketAdapter.sendAndReceive(reqMessage);
	
						// Ravi: 20130423: Bug#1976: Added for set Status to Sent - Start
						jdbcDatabaseDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_SENT);
						// Ravi: 20130423: Bug#1976: Added for set Status to Sent - End
	
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Receiving payload response ..", u2kopcoLoggerCategory);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[Handler][handleMessage]Payload response message is :"+responseMessage, u2kopcoLoggerCategory);
	
						// insert response payload in command response table
						jdbcDatabaseDAO.saveCommandResponse(parRowId,cmdRefId, transactionId, responseMessage , systemname);
	
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Logging out", u2kopcoLoggerCategory);
						String LogoutMessage = "LOGOUT" + ":::" + "8" + "::" + ";";
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Logout message :"+LogoutMessage, u2kopcoLoggerCategory);
	
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,  "[Handler][handleMessage]Logging out..", u2kopcoLoggerCategory);
						String logoutResponse = socketAdapter.sendAndReceive(LogoutMessage);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Logout response message is :"+logoutResponse, u2kopcoLoggerCategory);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,  "[Handler][handleMessage]Socket Closing..", u2kopcoLoggerCategory);
						socketAdapter.closeSocket(socketAdapter.getUniqueKey());
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,  "[Handler][handleMessage]Socket Closed", u2kopcoLoggerCategory);
						//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Logout response message :"+responseMessage, u2kopcoLoggerCategory);
					}else {
						// insert login error response in command response table
						jdbcDatabaseDAO.saveCommandResponse(parRowId,cmdRefId, transactionId, responseMessage , systemname);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[Handler][handleMessage]Login is not successful" ,u2kopcoLoggerCategory);
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Socket closing..",u2kopcoLoggerCategory);
						socketAdapter.closeSocket(socketAdapter.getUniqueKey());
						taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[Handler][handleMessage]Socket closed",u2kopcoLoggerCategory);
					}
	
				}
			}
			// HTTP Adapter
	
			else if (systemname.equalsIgnoreCase("MMSC")) {
				loggercategory="MMSClogging";
				//databaseDAO.setSysName(systemname);
				//databaseDAO.setIntfName(interfaceName);
				HttpMessage httpMessage = new HttpMessage();
	            userName = commandTransDetails.getUserName();
	            password = commandTransDetails.getPwd();
	            CommandTransDetails threadVariable = new CommandTransDetails();
				threadVariable.setSystemName(systemname);
				threadVariable.setInterfaceName(interfaceName);
				ThreadLocalInstance.set(threadVariable);
				threadVariable.setTimeOut(commandTransDetails.getTimeOut());
				threadVariable.setReadTimeOut(commandTransDetails.getReadTimeOut());
				commandHandlerDAO.updateStubbingStatus(srcTransId, transactionId, parRowId, isStub, loggercategory);
	            httpMessage.loadStub(userName, password, parRowId, cmdRefId, transactionId, retTime, srcTransId,jdbcDatabaseDAO,systemname);
	            ThreadLocalInstance.remove();
	
			}
			/*else if (techMethod.equalsIgnoreCase(ApplicationConstants.SYSTEM_TECH_METHOD_HTTP)) {
				
				HttpHandler httpHandler = new HttpHandler();
				httpHandler.loadHttpSystem(message,jdbcDatabaseDAO,databaseDAO,systemname);
			}*/
			
			 
			//taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_INFO, "[Handler][handleMessage]The command values are :"+ mess.toString(),loggercategory);
		}catch (Exception e) {
			taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_ERROR,"[Handler][handleMessage]Exception occured ",loggercategory, e);
			jdbcDatabaseDAO.insertError("SPL", srcTransId, cmdRefId,"Translator Handler", "ERR_CODE", e.getMessage());
			jdbcDatabaseDAO.instructorErrorUpdate(parRowId,"1",e.getMessage(),loggercategory);
			jdbcDatabaseDAO.orderTransDetailsErrorUpdate(orderRowId);

			OrderTransactonDetail orderTransDtls = commandHandlerDAO.getOrderTransDtlsByRowId(parRowId, transactionId, srcTransId, orderRowId, loggercategory);
			String source = orderTransDtls.getSource();

			if(ApplicationConstants.SOURCE_OSM.equalsIgnoreCase(source)) {
				SPLCommonComponent splCommonComponent = null; 
				String tomResponse = "";
				try {
					splCommonComponent = new SPLCommonComponent(taLogger,loggercategory);
					tomResponse = splCommonComponent.createTomResponse(parRowId);
				} catch (Exception tomException) {
				}
	
				taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_ERROR,"[Handler][handleMessage]TomResponse : "+ tomResponse, loggercategory);
	
				OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
				// Ravi: 20130423: Bug#1976: Added for set correct correlation id in JMS Header - Start
				//String corrId = jdbcDatabaseDAO.getCorrId(orderRowId);
				String corrId = orderTransDtls.getCorrId();
				taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_ERROR,"[Handler][handleMessage]Correlation Id : "+ corrId, loggercategory);
				//orderTransactionSender.postMessage(tomResponse, parRowId);
				orderTransactionSender.postMessage(tomResponse, corrId, "TOMOrderProv");
				// Ravi: 20130423: Bug#1976: Added for set correct correlation id in JMS Header - End
			}

		}finally {
			if(systemname!=null && (systemname.equalsIgnoreCase("IL") || systemname.equalsIgnoreCase("U2KOPCO"))) {
				if(socketAdapter!=null)
					socketAdapter.closeSocket(socketAdapter.getUniqueKey());
				if(iLReqSocketAdapter!=null)
					iLReqSocketAdapter.closeSocket(iLReqSocketAdapter.getUniqueKey());
			}
			ThreadLocalInstance.remove();
		}

	}

}
