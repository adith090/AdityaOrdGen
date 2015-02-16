package com.m1.bcc.spl.tcpadapter;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 17/04/2013					Ravikumar G				Bug#1921- if empty response from IL then send tom response
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 17/04/2013					Ravikumar G				Bug#20538- Separate timeout setting for all interfaces
 ******************************************************************************/

public class SocketAdapter {

	Socket socket;
	InetAddress host;
	int port;
	DataOutputStream outToServer;
	BufferedReader inFromServerBuffer;
	String EOF;
	String srcTransId;
	String transactionId;
	String parRowId;
	String logCategory;
	private static int socketCount = 0;
	private int uniqueKey;
	TALogger taLogger = TALogger.getTALogger();




	public int getUniqueKey() {
		return uniqueKey;
	}

	public void setUniqueKey(int uniqueKey) {
		this.uniqueKey = uniqueKey;
	}

	public String getSrcTransId() {
		return srcTransId;
	}

	public void setSrcTransId(String srcTransId) {
		this.srcTransId = srcTransId;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getParRowId() {
		return parRowId;
	}

	public void setParRowId(String parRowId) {
		this.parRowId = parRowId;
	}

	public String getLogCategory() {
		return logCategory;
	}

	public void setLogCategory(String logCategory) {
		this.logCategory = logCategory;
	}


	public void setEOF (String EOF) {
		this.EOF = EOF;
	}

	public void setAddress (String destAddress, String port) throws UnknownHostException {
			this.host = InetAddress.getByName(destAddress);
			this.port = Integer.parseInt(port);
	}

	public void openSocket () {
	    // Open a network socket connection to the specified host and port
        try{
        	// check if the destination is available
		    socket = new Socket(host, port);

		    // Get input and output streams for the socket
		    outToServer = new DataOutputStream(socket.getOutputStream());
		    inFromServerBuffer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}
        catch (IOException e) {
		        e.printStackTrace();
		}
	}

	public void openSocket (String logCategory, String srcTransId,String transactionId,String parRowId, String systemName, int timeOut, int readTimeOut) throws Exception {
	    // Open a network socket connection to the specified host and port
       	// check if the destination is available
		TALogger taLogger = TALogger.getTALogger();
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][openSocket]Initialize Socket ", logCategory);
       	socket = new Socket();
       	//Properties properties = (Properties) BeanFactory.getBean("properties");
       	//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[SocketAdapter][openSocket]getting Properties", logCategory);
		//int timeOut = Integer.parseInt(properties.getProperty("TIMEOUT"));
		//int readTimeOut = Integer.parseInt(properties.getProperty("READ_TIMEOUT"));
       	//int timeOut = Integer.parseInt(properties.getProperty(systemName + "_TIMEOUT"));
		//int readTimeOut = Integer.parseInt(properties.getProperty(systemName + "_READ_TIMEOUT"));
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][openSocket]TIME OUT : " + timeOut, logCategory);
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][openSocket]READ TIME OUT : " + readTimeOut, logCategory);

		socket.connect(new InetSocketAddress(host, port), timeOut);
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][openSocket]Connected : " + timeOut, logCategory);
		socket.setSoTimeout(readTimeOut);

		// Get input and output streams for the socket
		outToServer = new DataOutputStream(socket.getOutputStream());
		inFromServerBuffer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	public void closeSocket (int uniqueKey) {
		try {
			TALogger taLogger = TALogger.getTALogger();
			outToServer.close();
			inFromServerBuffer.close();
			socket.close();
			taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][closeSocket]Socket Closed for host " + InetAddress.getLocalHost().getHostAddress() + " with Unique Key: " + uniqueKey, logCategory);
		}
		catch (IOException e) {
			taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[SocketAdapter][closeSocket]Exception is : "+e, logCategory);
		}
	}

	public String sendAndReceive (String requestMessage) throws Exception {
        TALogger taLogger = TALogger.getTALogger();

		String receivedMsg = "";
        char[] responseChar = new char[1];
        boolean reachedValidChar = false;
        byte[] buffer = new byte[5012];
        int bytes = 0;
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]Sending to Server... ", logCategory);

            outToServer.writeBytes(requestMessage); // Send the response message
            taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]Writing into out stream... ", logCategory);
		    outToServer.writeBytes("\r\n");
		    outToServer.writeBytes("\r\n");
		    outToServer.write(buffer, 0, bytes);
		    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]Writing to out buffer... ", logCategory);
		    outToServer.flush();
		    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]flush the out buffer... ", logCategory);
	        while(true) {
	        	//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]inside while to read data... ", logCategory);
		        inFromServerBuffer.read(responseChar);
		        //taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]inside while after read data... " + responseChar[0], logCategory);
		        	if(responseChar[0] > 32)
		        		reachedValidChar = true;
		        	if(reachedValidChar)
		        		receivedMsg = receivedMsg + String.valueOf(responseChar);
		        	if(responseChar[0] == ';')
		        		reachedValidChar = false;

		        if (receivedMsg.contains(EOF)) {
		        	receivedMsg = receivedMsg.substring(0, receivedMsg.indexOf(EOF)+EOF.length());
		        	break;
		        }
		        //taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]receivedMsg " + receivedMsg, logCategory);
	        }
	        taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[SocketAdapter][sendAndReceive]Received message :" + receivedMsg , logCategory);


		return receivedMsg;
	}

	public String sendAndReceive (String requestMessage, String indentifier) throws Exception {
        TALogger taLogger = TALogger.getTALogger();

		String receivedMsg = "";
        char[] responseChar = new char[1];
        boolean reachedValidChar = false;
        byte[] buffer = new byte[5012];
        int bytes = 0;
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"] Sending to Server... ", logCategory);

            outToServer.writeBytes(requestMessage); // Send the response message
            taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Writing into out stream... ", logCategory);
		    outToServer.writeBytes("\r\n");
		    outToServer.writeBytes("\r\n");
		    outToServer.write(buffer, 0, bytes);
		    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Writing to out buffer... ", logCategory);
		    outToServer.flush();
		    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG,"[SocketAdapter][sendAndReceive]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]flush the out buffer... ", logCategory);

		    String responseLine = "";
		    while ((responseLine = inFromServerBuffer.readLine()) != null) {
		    	//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[SocketAdapter][sendAndReceive]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]inside while to read data... ", logCategory);
		    	receivedMsg = receivedMsg + "\n" + responseLine;

		    	if((receivedMsg==null || receivedMsg.trim().equals(""))) {
		    		//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][sendAndReceive]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Received message is empty.. :" + receivedMsg , logCategory);

		    		break;
		        }

		    	if (receivedMsg.contains("$END$")) {
	    			receivedMsg = receivedMsg.substring(0, receivedMsg.indexOf("$END$")+"$END$".length());
	    			taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][sendAndReceive]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Message received Inside EOF :" + receivedMsg , logCategory);
	    			break;
		    	}
			}

		    // ravi: 20130417: Bug 1921: if empty response received from IL then send tom response Start
		    if(receivedMsg==null || receivedMsg.trim().equals(""))
		    	throw new SPLExceptionHandler("Received Empty " + indentifier + " response from IL");
		    // ravi: 20130417: Bug 1921: if empty response received from IL then send tom response End

	        taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[SocketAdapter][sendAndReceive]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Received message :" + receivedMsg , logCategory);

		return receivedMsg;
	}

	/**
	 * Added to initialize the SocketAdapter
	 * @param interfaceName
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static SocketAdapter initSocketAdapter(String interfaceName,String loggingCategory,String srcTransId,String transactionId,String parRowId, String systemName) throws Exception {
		TALogger taLogger = TALogger.getTALogger();
		JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean(ApplicationConstants.BEAN_JDBCDATABASEDAO);
		SocketAdapter socketAdapter = new SocketAdapter();
		CommandTransDetails cmndTransDetails = jdbcDatabaseDAO.getCommandTransDetails(interfaceName);
		String location = cmndTransDetails.getLocation();
		socketAdapter.setAddress(location.split(":")[0], location.split(":")[1]);
		//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][initSocketAdapter]HOST: "+location.split(":")[0], loggingCategory);
		//taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][initSocketAdapter]PORT: "+location.split(":")[1], loggingCategory);
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][initSocketAdapter]Opening Socket..",loggingCategory);
		int timeOut = Integer.parseInt(cmndTransDetails.getTimeOut());
		int readTimeOut = Integer.parseInt(cmndTransDetails.getReadTimeOut());
		socketAdapter.openSocket(loggingCategory, srcTransId, transactionId, parRowId, systemName, timeOut, readTimeOut);
		socketCount = socketCount + 1;
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][initSocketAdapter] Socket Opened for host " + InetAddress.getLocalHost().getHostAddress() +" With Unique Key:" + socketCount,loggingCategory);
		socketAdapter.setUniqueKey(socketCount);
		return socketAdapter;
	}

	/**
	 *
	 * @param socketAdapter
	 * @param logoutRequest
	 * @throws Exception
	 */
	public void logout(SocketAdapter socketAdapter, String logoutRequest) throws Exception {
		TALogger taLogger = TALogger.getTALogger();
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[SocketAdapter][LOGOUT][Unique Key:"+ this.getUniqueKey() +"]Logging out..", logCategory);
		String logoutResponse = socketAdapter.sendAndReceive(logoutRequest, "LOGOUT");
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][LOGOUT][Unique Key:"+ this.getUniqueKey() +"]Logout response message is :"+logoutResponse, logCategory);
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[SocketAdapter][LOGOUT][Unique Key:"+ this.getUniqueKey() +"]Socket Closing..", logCategory);
		socketAdapter.closeSocket(socketAdapter.getUniqueKey());
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[SocketAdapter][LOGOUT][Unique Key:"+ this.getUniqueKey() +"]Socket Closed", logCategory);
	}

	/**
	 * Method to get register ack or response if any response with register ack
	 * @param requestMessage
	 * @return
	 * @throws IOException
	 */
	public String sendAndReceiveResponse (String requestMessage, String indentifier) throws Exception {
        TALogger taLogger = TALogger.getTALogger();

		String receivedMsg = "";
        byte[] buffer = new byte[5012];
        int bytes = 0;
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Sending to Server..." , logCategory);

            outToServer.writeBytes(requestMessage); // Send the response message
            taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Writing into out stream... ", logCategory);
		    outToServer.writeBytes("\r\n");
		    outToServer.writeBytes("\r\n");
		    outToServer.write(buffer, 0, bytes);
		    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Writing to out buffer... ", logCategory);
		    outToServer.flush();
		    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]flush the out buffer... ", logCategory);

		    int count = 1;
		    String regAckResponse = "";
		    String responseLine = "";
		    while ((responseLine = inFromServerBuffer.readLine()) != null) {
		    	taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO,"[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]inside while to read data... ", logCategory);
		    	receivedMsg = receivedMsg + "\n" + responseLine;

		    	if((receivedMsg==null || receivedMsg.trim().equals("")) && count>1) {
		    		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Received message is empty.. :" + receivedMsg , logCategory);
		        	break;
		        }
		    	if (receivedMsg.contains("$END$")) {
		    		if(count==1) {
		        		receivedMsg = receivedMsg.substring(0, receivedMsg.indexOf("$END$")+"$END$".length());
		        		regAckResponse = receivedMsg;
		        		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Acknowledgement received :" + regAckResponse , logCategory);
		        		receivedMsg = "";
			        	count = count + 1;
		    		} else {
		    			receivedMsg = receivedMsg.substring(0, receivedMsg.indexOf("$END$")+"$END$".length());
		    			taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Message received :" + receivedMsg , logCategory);
		    			break;
		    		}
		    	}
			}

		    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Received message :" + receivedMsg , logCategory);
		    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]regAckResponse message :" + regAckResponse , logCategory);

		    if(receivedMsg==null || receivedMsg.trim().equals(""))
		    	receivedMsg = regAckResponse;
		    taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_INFO, "[SocketAdapter][sendAndReceiveResponse]["+ indentifier +"][Unique Key:"+ this.getUniqueKey() +"]Received message :" + receivedMsg , logCategory);

		return receivedMsg;
	}

}
