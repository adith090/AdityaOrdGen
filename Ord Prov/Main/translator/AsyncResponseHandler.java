package com.m1.bcc.spl.translator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.scheduling.annotation.Async;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.model.OrderTransactonDetail;
import com.m1.bcc.spl.tcpadapter.SocketAdapter;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;

import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 18/01/2013					Ravikumar G				Created
 * 23/01/2013					Ravikumar G				Modified to update logger and last execution time
 * 17/04/2013					Billy Lim				Bug 1908. Move class variables used by @Async function into local function. This is to prevent class variables from by being overridden when called asynchronously
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 17/04/2013					Ravikumar G				Bug#20538- Separate timeout setting for all interfaces
 ******************************************************************************/

public class AsyncResponseHandler {

	private DatabaseDAO asyncTransactionDAO;
	private String loggerCategory = "ILresponseadaptorlogging";
	private String inputIdentifier="[AsyncResponseHandler][handleAsyncResponse]";
	private TALogger taLogger = null;
	//Bug 1908 Refer to Modification History
	//private String srcTransId = "";
	//private String transId = "";
	//private String cmdRowId = "";

	/**
	 *
	 * @param asyncTransactionDAO
	 */
	public void setAsyncTransactionDAO(DatabaseDAO asyncTransactionDAO) {
		this.asyncTransactionDAO = asyncTransactionDAO;
	}

	@Async
	@ServiceActivator
	public void handleAsyncResponse(Message<ArrayList<Map<String, Object>>> message) {
		handleAsyncResponse();
	}

	@Async
	public void handleAsyncResponse() {
		SocketAdapter iLResSocketAdapter = null;
		String logoutRequest = "";
		String uniqueKey = "";

		Properties propertiesKey = (Properties) BeanFactory.getBean("properties");
		final String Key = propertiesKey.getProperty("KEY");
		//Bug 1908 Refer to Modification History
		String cmdRowId = "";
		String transId = "";
		String srcTransId = "";
		try {
			Properties properties = SPLCommonComponent.getSystemStubProperty();
			boolean isStub = SPLCommonComponent.getStubbing(properties, "IL");
			taLogger = TALogger.getTALogger();
			taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Getting stub value..Stub is "+ isStub, loggerCategory);
			if(!isStub) {
				//taLogger = TALogger.getTALogger();
				//updateLastExecutionPollTime("ILResponseScheduler");
				taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Getting credentials..", loggerCategory);

				// Ravi: Changed to get account type for Resp queue Id
				CommandTransDetails commandTransDetails = asyncTransactionDAO.getCmdDestRefDtls("INSTANTLINK_002");
				String userName = commandTransDetails.getUserName();
				String password = commandTransDetails.getPwd();
				String accountType = commandTransDetails.getAccountType();

				taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG, "UserName for IL is :" + userName, loggerCategory);
				taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG, "Password for IL is :" + password, loggerCategory);
				taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG, "Account type :" + accountType, loggerCategory);


				String decryptedKey="";
				if(password!=null && !password.trim().equals("")) {
					decryptedKey = SPLCommonComponent.Crypt(0, Key, password);
				}
				taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG, "DecryptedKey :"+decryptedKey, loggerCategory);
				// process the response: Start
				ILMessage iLMessage = new ILMessage();
				taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Forming Login Request message..", loggerCategory);
				String loginRequest = iLMessage.getLoginRequestMessage(userName, decryptedKey);
				taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Login Request :"+loginRequest , loggerCategory);
				taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Initializing iLResSocketAdapter parameter..", loggerCategory);
				iLResSocketAdapter = SocketAdapter.initSocketAdapter("INSTANTLINK_002",loggerCategory,"","","", "IL");
				iLResSocketAdapter.setLogCategory(loggerCategory);
				uniqueKey = "" + iLResSocketAdapter.getUniqueKey();
				taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG, "iLResSocketAdapter :"+iLResSocketAdapter, loggerCategory);
				iLResSocketAdapter.setEOF("$END$");
				taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Sending Login Request is :" + loginRequest, loggerCategory);

				String loginResponse = iLResSocketAdapter.sendAndReceive(loginRequest, "LOGIN");
				taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Login Response received is :" + loginResponse, loggerCategory);

				if(iLMessage.isILRequestSuccessful(loginResponse, "LOGIN",loggerCategory)) {

					taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Getting request message..", loggerCategory);

					String registerMessage = iLMessage.getRegisterAck(accountType);

					taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Register message sending is :"+registerMessage, loggerCategory);

					String responseMessage = iLResSocketAdapter.sendAndReceiveResponse(registerMessage, "REGISTER_ACK");

					taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Response message received is :"+responseMessage, loggerCategory);

					String sasAction = iLMessage.getResponseParamValue(responseMessage, "SAS_ACTION",loggerCategory,"","","");
					taLogger.log(inputIdentifier, ApplicationConstants.LOG_INFO, "Retreiving sasAction from Response message.."+sasAction, loggerCategory);
					taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG, "sasAction :"+sasAction, loggerCategory);
					logoutRequest = "SAS3\n" + "0\n" + "SAS_ACTION LOGOUT\n" + "$END$";

					if(sasAction!=null && sasAction.equalsIgnoreCase("REGISTER_ACK")) {
						// Logout the response socket adapter if No immediate response! and response back is only the register ack
						//iLResSocketAdapter.logout(iLResSocketAdapter, logoutRequest);
					}else {
						if(sasAction!=null && sasAction.equalsIgnoreCase("RESPONSE")) {
							//Bug 1908 Refer to Modification History
							//saveCommandResponse(iLMessage, responseMessage);
							Map <String,String>orderDetailMap = saveCommandResponse(iLMessage, responseMessage);
							cmdRowId = orderDetailMap.get("cmdRowId");
							transId = orderDetailMap.get("transId");
							srcTransId = orderDetailMap.get("srcTransId");						
							
							while(true) {
								String responseAck = "SAS3\n" + "0\n" + "SAS_ACTION RESPONSE_ACK\n" + "ACK_STATUS 0\n" + "ACK_SMESSAGE OK\n" + "$END$\n";
								taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, inputIdentifier+"responseAck message sending ..", loggerCategory);
								taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO,inputIdentifier+"responseAck message :"+responseAck, loggerCategory);
								responseMessage = iLResSocketAdapter.sendAndReceive(responseAck, "RESPONSE_ACK");
								taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, inputIdentifier+"Receiving Response message..", loggerCategory);
								taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO,inputIdentifier+"Response message received is :"+responseMessage, loggerCategory);
								sasAction = iLMessage.getResponseParamValue(responseMessage, "ERROR",loggerCategory,srcTransId, transId, cmdRowId);

								taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, inputIdentifier+"sasAction Response message :"+sasAction, loggerCategory);

								// Logout if No immediate response! or ErrorResponse
								if(responseMessage==null || responseMessage.equals("") || (sasAction!=null && sasAction.equalsIgnoreCase("ERROR"))) {
									iLResSocketAdapter.logout(iLResSocketAdapter, logoutRequest);
									break;
								}else {
									saveCommandResponse(iLMessage, responseMessage);
									//break;
								}

							}
						}
					}

				}else {
					taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, inputIdentifier+"Login is not successful" ,loggerCategory);
					iLResSocketAdapter.closeSocket(iLResSocketAdapter.getUniqueKey());
				}
			}
			// Response End
		}catch(IOException ioException) {
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_ERROR, inputIdentifier+"[Unique Key:"+ uniqueKey +"]IOException", loggerCategory, ioException);
		} catch (SQLException sqlException) {
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_ERROR, inputIdentifier+"[Unique Key:"+ uniqueKey +"]SQLException", loggerCategory, sqlException);
		} catch (SPLExceptionHandler splExceptionHandler) {
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_ERROR, inputIdentifier+"[Unique Key:"+ uniqueKey +"]SPLExceptionHandler", loggerCategory, splExceptionHandler);
		}catch (Exception exception) {
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_ERROR, inputIdentifier+"[Unique Key:"+ uniqueKey +"]Exception", loggerCategory, exception);
		}finally {
			if(iLResSocketAdapter!=null)
				iLResSocketAdapter.closeSocket(iLResSocketAdapter.getUniqueKey());
		}
	}

	/**
	 *
	 * @param iLMessage
	 * @param responseMessage
	 * @throws SQLException
	 * @throws SPLExceptionHandler
	 * @throws IOException
	 */
	//Bug 1908 Refer to Modification History
	private Map <String,String> saveCommandResponse(ILMessage iLMessage, String responseMessage) throws SQLException, SPLExceptionHandler, IOException {
	//private void saveCommandResponse(ILMessage iLMessage, String responseMessage) throws SQLException, SPLExceptionHandler, IOException {
		//Bug 1908 Refer to Modification History
		//cmdRowId = iLMessage.getResponseParamValue(responseMessage, "ORDER_NO",loggerCategory,srcTransId, transId, cmdRowId);
		String cmdRowId = iLMessage.getResponseParamValue(responseMessage, "ORDER_NO",loggerCategory,"", "", "");
		String transId = "";
		String srcTransId = "";
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, inputIdentifier+"Processing  response message.. ", loggerCategory);
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, inputIdentifier+"Response message is: "+ responseMessage, loggerCategory);
		OrderTransactonDetail orderTransactonDetail = asyncTransactionDAO.getOrderTransDetails(cmdRowId);
		//if(orderTransactonDetail==null)
		//	new SPLExceptionHandler("Error: No Records found for Cmd Row Id " + cmdRowId);
		if(orderTransactonDetail!=null) {
			transId = orderTransactonDetail.getTransId();
			srcTransId=orderTransactonDetail.getSrcTransId();
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, inputIdentifier+"TransId retrieved is :"+ transId, loggerCategory);
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, inputIdentifier+"SrcTransId retrieved is :"+ srcTransId, loggerCategory);
			//insert response payload in command response table
			asyncTransactionDAO.saveCommandResponse(cmdRowId, orderTransactonDetail.getCmdRefId(), orderTransactonDetail.getTransId(),
					responseMessage , ApplicationConstants.SYSTEM_IL, false);
		}
		//Bug 1908 Refer to Modification History
		Map <String,String>orderDetailMap = new HashMap <String,String>();
		orderDetailMap.put("srcTransId",srcTransId);
		orderDetailMap.put("transId", transId);
		orderDetailMap.put("cmdRowId", cmdRowId);		
		return orderDetailMap;
	}

	/**
	 * function to update last execution time
	 * @param pollerName
	 */
	/*private void updateLastExecutionPollTime(String pollerName) {
		Date date = new Date();
		taLogger.log("[CommandHandler][updateLastExecutionPollTime]", ApplicationConstants.LOG_INFO, "Last Execution time of "+pollerName+" is : "+ date, loggerCategory);
		asyncTransactionDAO.updateLastExecutionTime(date,pollerName);

	}*/
}
