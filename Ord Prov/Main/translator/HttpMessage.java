package com.m1.bcc.spl.translator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 17/05/2013					Sudharsan				Implemented Engineering Authentication server
 * 17/06/2013					Ravikumar G				Bug#2346: to update cmd trans status to sent
 * 16/01/2014					Ravikumar G				Bug#23590 - to update request message in cmd_trans table
 ******************************************************************************/

public class HttpMessage {

	String HTTPLogin="";
	String modsub_mmsc="";
	String loginResponse="";
	Object sessionid="";
	Object uniqueidentifier="";
	String featurename;
	TALogger taLogger = TALogger.getTALogger();
	SPLCommonComponent splCommonComponent;

	Properties propertiesKey = (Properties) BeanFactory.getBean("properties");
	String Key = propertiesKey.getProperty("KEY");

	String mmscLoggerCategory="MMSClogging";

public void loadStub(String userName, String password,String parRowId,String cmdRefId,String transId,String retTime,String srcTransId,JdbcDatabaseDAO jdbcDatabaseDAO,String systemname) throws Exception
{
		splCommonComponent = new SPLCommonComponent(taLogger,mmscLoggerCategory);
	    {
		 HTTPLoginStub(userName,password,parRowId,cmdRefId,transId,retTime,srcTransId,jdbcDatabaseDAO,systemname);
	    }
}

public String HTTPLoginStub(String userName, String password,String parRowId,String cmdRefId,String transId,String retTime,String srcTransId,JdbcDatabaseDAO jdbcDatabaseDAO,String systemname) throws Exception 
{

	 
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][HTTPLoginStub]Logging in mmsc..", mmscLoggerCategory);
			String adminLogin=jdbcDatabaseDAO.getAdminLogin(srcTransId,transId,parRowId,mmscLoggerCategory);
			Properties properties = SPLCommonComponent.getSystemStubProperty();
			boolean isStub = SPLCommonComponent.getStubbing(properties, systemname);
			Document doc=null;
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			doc = builder.parse("conf/spl/adaptors/MMSC/request/Http_Login_Request_MMSC.xml");
			
			String docMessage=splCommonComponent.convertDocumentToString(doc);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]Forming HTTPLogin request message..",mmscLoggerCategory);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]HTTPLogin request as doc Message is :"+docMessage,mmscLoggerCategory);
		    
		    NodeList nodeList = doc.getElementsByTagName("LOGIN");
	        for(int x=0,size= nodeList.getLength(); x<size; x++) 
	        {
	        	nodeList.item(x).getAttributes().getNamedItem("userid").setNodeValue(userName);
	        	nodeList.item(x).getAttributes().getNamedItem("passwd").setNodeValue("#password#");
	        	nodeList.item(x).getAttributes().getNamedItem("AdminLogin").setNodeValue(adminLogin);
	        }

		
		    // Convert Http_Login_Request_MMSC from doc type to string 
	        taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]Converting document to string..",mmscLoggerCategory);
		     String httploginStr=splCommonComponent.convertDocumentToString(doc);
		    
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]HTTPLogin request message is :"+httploginStr,mmscLoggerCategory);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]Validating stub value..",mmscLoggerCategory);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][HTTPLoginStub]Stub is :"+isStub,mmscLoggerCategory);
		    if (isStub) 
			{
				
				FileWriter fileWriter = new FileWriter("conf/spl/adaptors/MMSC/request/Http_Login_Request_"+ transId + "_" + retTime + ".xml");
				BufferedWriter out = new BufferedWriter(fileWriter);
				out.write(docMessage);
				out.close();
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]HTTPLogin request created successfully.",mmscLoggerCategory);

				domFactory = DocumentBuilderFactory.newInstance();
				domFactory.setNamespaceAware(false);
				builder = domFactory.newDocumentBuilder();
				doc = builder.parse("conf/spl/adaptors/MMSC/response/Http_Login_Success.xml");

				CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
				String systemName = cmdSystemDetails.getSystemName();
			}

			else if (!isStub) 
			{
					
					taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"HTTPLogin request before replacing the  decryptedKey is "+httploginStr , mmscLoggerCategory);
					taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"Key :"+Key+"Password :"+password, mmscLoggerCategory);
				    String decryptedKey="";
				    if(password!=null && !password.trim().equals(""))
				    {
				    	decryptedKey=SPLCommonComponent.Crypt(0, Key, password);
				    }
				    if(httploginStr.contains("#password#")){
				    httploginStr=httploginStr.replaceAll("#password#", decryptedKey);
				    }
				    //Comment the next two lines
				    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"DecryptedKey :"+decryptedKey , mmscLoggerCategory);
		        	taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"HTTPLogin request after replacing the  decryptedKey is "+httploginStr , mmscLoggerCategory);

					 
				    ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
			      	RequestGateway requestGateway = context.getBean("requestGateway",RequestGateway.class);
			      	taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]Sending message through gateway..",mmscLoggerCategory);
			      	Object httploginreply = requestGateway.echo(httploginStr);
			      	taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]Getting echo message..",mmscLoggerCategory);
					taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][HTTPLoginStub]Httplogin message echoed back  is :"+httploginreply,mmscLoggerCategory);
				
					// parse the reply as doc type 
					taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]Creating DOM document ",mmscLoggerCategory);
					doc = splCommonComponent.createDOMDocument(httploginreply.toString());
					taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][HTTPLoginStub]Httplogin reply response xml :"+doc,mmscLoggerCategory);
					
					}

			String responseMessageLogin = splCommonComponent.convertDocumentToString(doc);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]Response message login in string format :"+responseMessageLogin,mmscLoggerCategory);
			XPath xPath = XPathFactory.newInstance().newXPath();
			Object msgcode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]Retrieving msg code read from Xpath ..", mmscLoggerCategory);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][HTTPLoginStub]msg code read from Xpath is :"+msgcode, mmscLoggerCategory);
		
		if(msgcode.equals("uPowerErr002"))
		{

			sessionid=xPath.evaluate("/RESPONSE/DATA/RECORDS/RECORD/ATTRIBUTE/ATTR_NAME[@name='sessionid']/ATTR_VALUE/@value", doc, XPathConstants.STRING);
			List<Map<String, Object>> list =jdbcDatabaseDAO.getUID(parRowId);
			String uid="";
			String searchbase="";
			String specialfilter="";
			//String featurename="";

			for (Object cRefId : list) 
			{
				Map newMap = (Map) cRefId;
			    String paramName = (String) newMap.get("PARAM_NAME");
			    if(paramName.equals("uid"))
			        uid = (String) newMap.get("PARAM_VALUE");
			    
			    if(paramName.equals("featurename"))
			    	featurename = (String) newMap.get("PARAM_VALUE");
			 }
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][HTTPLoginStub]featurename="+featurename +" & sessionid="+sessionid+"  & uniqueidentifier="+uniqueidentifier , mmscLoggerCategory);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]Validating feature name..", mmscLoggerCategory);
			if(featurename.equals("addsub_common"))
			{

				 addsub_common(userName, password, parRowId, cmdRefId, transId, retTime, srcTransId, jdbcDatabaseDAO, isStub,featurename,uid );
			}
			else if (featurename.equals("addsub_mmsc"))
			{
				modsub_mmsc(userName, password, parRowId, cmdRefId, transId, retTime, srcTransId, jdbcDatabaseDAO,isStub );
				
			}
			else if (featurename.equals("delsub_common"))
			{

				modsub_mmsc(userName, password, parRowId, cmdRefId, transId, retTime, srcTransId, jdbcDatabaseDAO,isStub );
			}
	  	}

		else
		{

			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][HTTPLoginStub]Required attribute is missing in login message" , mmscLoggerCategory);

	        jdbcDatabaseDAO.saveCommandResponse(parRowId, cmdRefId, transId, responseMessageLogin, systemname);

		}

	  

		

		return HTTPLogin;

	}


	//Method to form transaction request & to search

		public String modsub_mmsc(String userName, String password,String parRowId,String cmdRefId,String transId,String retTime,String srcTransId,JdbcDatabaseDAO jdbcDatabaseDAO,boolean isStub ) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerConfigurationException, TransformerException, SQLException, SPLExceptionHandler
		{
			
			  
				Document doc=null;
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][modsub_mmsc]Processing modsub_mmsc command", mmscLoggerCategory);

				List<Map<String, Object>> list =jdbcDatabaseDAO.getUID(parRowId);
				DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
				domFactory.setNamespaceAware(false);
				DocumentBuilder builder = domFactory.newDocumentBuilder();
				doc = builder.parse("conf/spl/adaptors/MMSC/request/HTTP_Search_Request_MMSC.xml");
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][modsub_mmsc]modsub_mmsc request as doc message is :"+doc,mmscLoggerCategory);
			    
			    
			    NodeList nodeList = doc.getElementsByTagName("REQUEST");
		        for(int x=0,size= nodeList.getLength(); x<size; x++) 
		        {
		        	nodeList.item(x).getAttributes().getNamedItem("sessionid").setNodeValue((String) sessionid);
		        	
		        }
			    
			    /*NodeList nodeList1 = doc.getElementsByTagName("SEARCHENTRY");
		        for(int x=0,size= nodeList1.getLength(); x<size; x++) 
		        {
		        	nodeList1.item(x).getAttributes().getNamedItem("searchfilter").setNodeValue("uid="+uid);
		        	nodeList1.item(x).getAttributes().getNamedItem("searchbase").setNodeValue(searchbase);
		        	nodeList1.item(x).getAttributes().getNamedItem("specialfilter").setNodeValue(specialfilter);
		        }*/
			    
			    // Convert modsub_mmsc_request from doc type to string 
			    String modsubmmscStr=splCommonComponent.convertDocumentToString(doc);
			    /*for (Object cRefId : list) 
				{
					Map newMap = (Map) cRefId;
				    paramName = (String) newMap.get("PARAM_NAME");
				    String paramValue = (String) newMap.get("PARAM_VALUE");
					modsubmmscStr = modsubmmscStr.replace("#" + paramName +"#", paramValue);
				}*/
			    modsubmmscStr = replaceParamName(modsubmmscStr, list,srcTransId,transId,parRowId);
			    
			    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][modsub_mmsc]modsub_mmsc request message is :"+modsubmmscStr,mmscLoggerCategory);
			    
				String uid="";
				String searchbase="";
				String specialfilter="";
				String paramName = "";
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][modsub_mmsc]Validating stub value..",mmscLoggerCategory);
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][modsub_mmsc]Stub is :"+isStub,mmscLoggerCategory);
				CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
				String systemName = cmdSystemDetails.getSystemName();
				String requestMsgFlag = cmdSystemDetails.getRequestMsgFlag();
				taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[HttpMessage][modsub_mmsc]requestMsgFlag :"+ requestMsgFlag, mmscLoggerCategory);
				jdbcDatabaseDAO.updateCmdTrans(srcTransId,  transId, parRowId, requestMsgFlag, modsubmmscStr, mmscLoggerCategory);
				if(isStub)
				{
					
				
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][modsub_mmsc]modsub_mmsc message is :"+modsubmmscStr,mmscLoggerCategory);
				
				FileWriter fileWriter = new FileWriter("conf/spl/adaptors/MMSC/request/HTTP_Search_Request_"+transId+"_"+retTime+".xml");
				BufferedWriter out = new BufferedWriter(fileWriter);
				out.write(modsubmmscStr);
				out.close();

				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][modsub_mmsc]modsub_mmsc file created successfully.", mmscLoggerCategory);
				
					domFactory = DocumentBuilderFactory.newInstance();
					domFactory.setNamespaceAware(false);
					builder = domFactory.newDocumentBuilder();
					doc = builder.parse("conf/spl/adaptors/MMSC/response/Http_Search.xml");
					taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG, "[HttpMessage][modsub_mmsc]Document created :"+doc, mmscLoggerCategory);
					
				}
				else if (!isStub)
				{
					
				    //Send the request and echo back
					ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
					RequestGateway requestGateway = context.getBean("requestGateway", RequestGateway.class);
					// Ravi: 20130617: Bug#2346: to update cmd trans status to "sent" - Start 
					jdbcDatabaseDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_SENT);
					// Ravi: 20130617: Bug#2346: to update cmd trans status to "sent" - End
					Object modsubmmscreply = requestGateway.echo(modsubmmscStr);
					taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][modsub_mmsc]Sending modsubmmsc message through gateway..",mmscLoggerCategory);
					taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][modsub_mmsc]modsubmmsc message echoed back  is :"+modsubmmscreply,mmscLoggerCategory);
					
					// parse the reply as doc type 
					taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][modsub_mmsc]Converting string to document..",mmscLoggerCategory);
					doc = splCommonComponent.createDOMDocument(modsubmmscreply.toString());
					//System.out.println("modsubmmscreply response xml as doc type : \n"+doc);
					taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][modsub_mmsc]modsubmmscreply response xml as doc type :"+doc,mmscLoggerCategory);

				}
				
				/*CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
				String systemName = cmdSystemDetails.getSystemName();*/

					 XPath xPath = XPathFactory.newInstance().newXPath();
					 Object returnval = xPath.evaluate("/RESPONSE/RESULT/@returnval", doc, XPathConstants.STRING);
					 taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][modsub_mmsc]Retrieving 'returnval' value from Xpath", mmscLoggerCategory);
					 taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][modsub_mmsc]'returnval' value retrieved from Xpath :"+returnval, mmscLoggerCategory);
					 
					 
					 Object msgCode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);
					 taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][modsub_mmsc]Retrieving 'msgCode' value from Xpath", mmscLoggerCategory);
					 taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][modsub_mmsc]msgCode value retrieved from Xpath :"+msgCode, mmscLoggerCategory);
					 
					 Object uniqueidentifierFromSearch = xPath.evaluate("/RESPONSE/DATA/RECORDS/RECORD/ATTRIBUTE/ATTR_NAME[@name='uniqueidentifier']/ATTR_VALUE/@value", doc, XPathConstants.STRING);
					 taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][modsub_mmsc]Validating 'returnval' parameter..", mmscLoggerCategory);    
					 	if ((returnval.equals("1")) &&(msgCode.equals("uPowerErr051")) )
					      {
					        
					        NodeList nodeList1 = doc.getElementsByTagName("RESULT");
					        for(int x=0,size= nodeList1.getLength(); x<size; x++) 
					        {
					        	taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][modsub_mmsc]Changing node value..", mmscLoggerCategory);
					        	nodeList1.item(x).getAttributes().getNamedItem("returnval").setNodeValue("0");
					        	String afterChanging=nodeList1.item(x).getAttributes().getNamedItem("returnval").getNodeValue();
					        	//System.out.println("afterChanging NodeValue= "+afterChanging);
					        	taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG, "[HttpMessage][modsub_mmsc]Changed Node value is :"+afterChanging, mmscLoggerCategory);
					        }
					        taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][modsub_mmsc]Converting document to string..", mmscLoggerCategory);
					        String responseMessageHttpSearch=splCommonComponent.convertDocumentToString(doc);
					        
						    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][modsub_mmsc]modsub_mmsc response message :"+responseMessageHttpSearch, mmscLoggerCategory);
							//jdbcDatabaseDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_SENT);
					    	jdbcDatabaseDAO.saveCommandResponse(parRowId, cmdRefId, transId, responseMessageHttpSearch, systemName);
					      }
					 	
					      else if((returnval.equals("1")) &&(msgCode.equals("uPowerErr052")) )
					      {
					    	  taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG, "[HttpMessage][modsub_mmsc]featurename="+featurename+" , returnval="+returnval+" , msgCode="+msgCode, mmscLoggerCategory);
					    	  if(featurename.equals("addsub_mmsc"))
					    	 
					    	  {
					    		  addsub_mmsc(userName,password,parRowId,cmdRefId,transId,retTime,srcTransId,jdbcDatabaseDAO,isStub,featurename,uid,uniqueidentifierFromSearch );
					    	  }
					    	  else if (featurename.equals("delsub_common"))
					    	  {
					    		  delsub_common(userName, password, parRowId, cmdRefId, transId,retTime,srcTransId,jdbcDatabaseDAO,isStub,uniqueidentifierFromSearch);
					    	  }
					    	 
					      }

					      else if(!returnval.equals("1"))
					      {
					    	  taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][modsub_mmsc]returnval parameter is missing", mmscLoggerCategory);
					    	  reqAttributeMissing(userName,password,parRowId,cmdRefId,transId,retTime,srcTransId,jdbcDatabaseDAO,isStub);
					      }

			

			return modsub_mmsc;

			}


		public String delsub_common(String userName, String password,String parRowId,String cmdRefId,String transId,String retTime,String srcTransId,JdbcDatabaseDAO jdbcDatabaseDAO,boolean isStub,Object uniqueidentifierFromSearch) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerConfigurationException, TransformerException, SQLException, SPLExceptionHandler

		  {

			Document doc=null;
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][delsub_common]Processing delsub_common command..", mmscLoggerCategory);
			List<Map<String, Object>> list =jdbcDatabaseDAO.getUID(parRowId);
			String searchbase="";
			String specialfilter="";
			String featurename="";
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			doc = builder.parse("conf/spl/adaptors/MMSC/request/delsub_common_Request_MMSC.xml");
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][delsub_common]delsub_common request as doc Message is :"+doc,mmscLoggerCategory);
		    //System.out.println("delsub_common request as doc Message is:\n"+doc);
		    
		    NodeList nodeList = doc.getElementsByTagName("REQUEST");
	        for(int x=0,size= nodeList.getLength(); x<size; x++) 
	        {
	        	nodeList.item(x).getAttributes().getNamedItem("sessionid").setNodeValue((String) sessionid);
	        	
	        }
		    
		    NodeList nodeList1 = doc.getElementsByTagName("DELETEENTRY");
	        for(int x=0,size= nodeList.getLength(); x<size; x++) 
	        {
	        	nodeList1.item(x).getAttributes().getNamedItem("record_identifier").setNodeValue("uniqueidentifier="+uniqueidentifierFromSearch);
	        }
		    
		    // Convert delsub_common_request from doc type to string 
	        taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][delsub_common]Converting document to String..",mmscLoggerCategory);
		    String delsubcommonStr=splCommonComponent.convertDocumentToString(doc);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][delsub_common]delsub_common request message is :"+delsubcommonStr,mmscLoggerCategory);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][delsub_common]Validating stub value..",mmscLoggerCategory);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][delsub_common]Stub is :"+isStub,mmscLoggerCategory);
		    CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
			String systemName = cmdSystemDetails.getSystemName();
			String requestMsgFlag = cmdSystemDetails.getRequestMsgFlag();
			taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[HttpMessage][modsub_mmsc]requestMsgFlag :"+ requestMsgFlag, mmscLoggerCategory);
			jdbcDatabaseDAO.updateCmdTrans(srcTransId,  transId, parRowId, requestMsgFlag, delsubcommonStr, mmscLoggerCategory);
			if(isStub)
			{
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][delsub_common]delsub_common message is :"+delsubcommonStr,mmscLoggerCategory);

			FileWriter fileWriter = new FileWriter("conf/spl/adaptors/MMSC/request/delsub_common_Request_"+ transId + "_" + retTime + ".xml");
			BufferedWriter out = new BufferedWriter(fileWriter);
			out.write(delsubcommonStr);
			out.close();
		
			domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(false);
			builder = domFactory.newDocumentBuilder();
			 doc = builder.parse("conf/spl/adaptors/MMSC/response/Http_Delete_Subscriber_Successful.xml");
			 }
			
			else if(!isStub)
			{
				
			    //Send the request and echo back
				ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
				RequestGateway requestGateway = context.getBean("requestGateway", RequestGateway.class);
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][delsub_common]Sending delsubcommon message through gateway..",mmscLoggerCategory);
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][delsub_common]Echoeing back..",mmscLoggerCategory);
				// Ravi: 20130617: Bug#2346: to update cmd trans status to "sent" - Start 
				jdbcDatabaseDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_SENT);
				// Ravi: 20130617: Bug#2346: to update cmd trans status to "sent" - End
				Object delsubcommonreply = requestGateway.echo(delsubcommonStr);
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][delsub_common]delsubcommon message echoed back  is :"+ delsubcommonreply,mmscLoggerCategory);
				
				// parse the reply as doc type 

				doc = splCommonComponent.createDOMDocument(delsubcommonreply.toString());
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][delsub_common]delsubcommonreply response xml as doc type :"+doc,mmscLoggerCategory);
				
			}
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][delsub_common]Converting document to string..",mmscLoggerCategory);
			String responseMessageHttpDelete = splCommonComponent.convertDocumentToString(doc);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][delsub_common]delsub_common response in string :"+responseMessageHttpDelete, mmscLoggerCategory);
			/*CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
			String systemName = cmdSystemDetails.getSystemName();*/
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			Object returnval = xPath.evaluate("/RESPONSE/RESULT/@resultval", doc,XPathConstants.STRING);
			// System.out.println("msgcode:\n"+msgcode);


			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][delsub_common]Saving response..", mmscLoggerCategory);
			//jdbcDatabaseDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_SENT);
			jdbcDatabaseDAO.saveCommandResponse(parRowId, cmdRefId, transId,responseMessageHttpDelete, systemName);


			return null ;

			}


		public String addsub_common(String userName, String password,String parRowId,String cmdRefId,String transId,String retTime, String srcTransId,JdbcDatabaseDAO jdbcDatabaseDAO,boolean isStub,String featurename,String uid ) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerConfigurationException, TransformerException, SQLException, SPLExceptionHandler
		{
			Document doc=null;
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]Processing addsub_common command..", mmscLoggerCategory);
			List<Map<String, Object>> list =jdbcDatabaseDAO.getUID(parRowId);
			
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			doc = builder.parse("conf/spl/adaptors/MMSC/request/addsub_common_Request_MMSC.xml");
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][addsub_common]addsub_common request as doc Message is :"+doc,mmscLoggerCategory);
			NodeList nodeList = doc.getElementsByTagName("REQUEST");
			for(int x=0,size= nodeList.getLength(); x<size; x++) 
	        {
	        	nodeList.item(x).getAttributes().getNamedItem("sessionid").setNodeValue((String) sessionid);
	        }
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]Converting document to string..",mmscLoggerCategory);
		    String addsubCommonStr=splCommonComponent.convertDocumentToString(doc);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]addsubCommon request as string Message is:"+addsubCommonStr,mmscLoggerCategory);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]Replacing param name..",mmscLoggerCategory);
		    addsubCommonStr = replaceParamName(addsubCommonStr, list,srcTransId,transId,parRowId);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]addsubCommon request after replacing param names :"+addsubCommonStr,mmscLoggerCategory);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]Validating stub value..",mmscLoggerCategory);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][addsub_common]Stub is :"+isStub,mmscLoggerCategory);
		    CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
			String systemName = cmdSystemDetails.getSystemName();
			String requestMsgFlag = cmdSystemDetails.getRequestMsgFlag();
			taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[HttpMessage][addsub_common]requestMsgFlag  :"+ requestMsgFlag, mmscLoggerCategory);
		    jdbcDatabaseDAO.updateCmdTrans(srcTransId,  transId, parRowId, requestMsgFlag, addsubCommonStr, mmscLoggerCategory);
			if(isStub)
			{
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]addsub_common message is :"+addsubCommonStr,mmscLoggerCategory);

			FileWriter fileWriter = new FileWriter("conf/spl/adaptors/MMSC/request/addsub_common_Request_"+transId+"_"+retTime+".xml");
			BufferedWriter out = new BufferedWriter(fileWriter);
			out.write(addsubCommonStr);
			out.close();
			//System.out.println("addsub_common_Request file created successfully.");
			  domFactory = DocumentBuilderFactory.newInstance();
			  domFactory.setNamespaceAware(false);
			  builder = domFactory.newDocumentBuilder();
			  doc = builder.parse("conf/spl/adaptors/MMSC/response/Http_Add_Subscriber_Successful.xml");

			 }
			else if (!isStub)
			{
			    
			    //Send the request and echo back
				ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
				RequestGateway requestGateway = context.getBean("requestGateway", RequestGateway.class);
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]Sending addsubcommon message through gateway..",mmscLoggerCategory);
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]addsubcommonreply message echoeing back..",mmscLoggerCategory);
				// Ravi: 20130617: Bug#2346: to update cmd trans status to "sent" - Start 
				jdbcDatabaseDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_SENT);
				// Ravi: 20130617: Bug#2346: to update cmd trans status to "sent" - End
				Object addsubcommonreply = requestGateway.echo(addsubCommonStr);
				//System.out.println("addsubcommonreply echo back  is: \n"+addsubcommonreply);
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][addsub_common]addsubcommonreply echoed back  is :"+addsubcommonreply,mmscLoggerCategory);
				
				// parse the reply as doc type 
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]Creating DOM document..",mmscLoggerCategory);
				doc = splCommonComponent.createDOMDocument(addsubcommonreply.toString());
				//System.out.println("addsubcommonreply response xml as doc type : /n"+doc);
				taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][addsub_common]addsubcommonreply response xml as doc type : /n"+doc,mmscLoggerCategory);
			}
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]Converting document to string..",mmscLoggerCategory);
			String addsubCommon=splCommonComponent.convertDocumentToString(doc);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_common]addsubCommon message in string :"+addsubCommon,mmscLoggerCategory);
			/*CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
			String systemName = cmdSystemDetails.getSystemName();*/


			  XPath xPath = XPathFactory.newInstance().newXPath();
			  Object msgcode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);

			  //jdbcDatabaseDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_SENT);
		      jdbcDatabaseDAO.saveCommandResponse(parRowId, cmdRefId, transId, addsubCommon, systemName);

		      return null;

		}


		public String addsub_mmsc(String userName, String password,String parRowId,String cmdRefId,String transId,String retTime,String srcTransId,JdbcDatabaseDAO jdbcDatabaseDAO,boolean isStub ,String featurename,String uid, Object uniqueidentifierFromSearch) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerConfigurationException, TransformerException, SQLException, SPLExceptionHandler

		{
			Document doc=null;
			//System.out.println("Inside addsub_mmsc method");
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][addsub_mmsc]Processing addsub_mmsc command..", mmscLoggerCategory);
			List<Map<String, Object>> list =jdbcDatabaseDAO.getUID(parRowId);
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			doc = builder.parse("conf/spl/adaptors/MMSC/request/addsub_mmsc_Request_MMSC.xml");
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][addsub_mmsc]addsub_mmsc request as doc Message is :"+doc,mmscLoggerCategory);
		    
		    NodeList nodeList = doc.getElementsByTagName("REQUEST");
	        for(int x=0,size= nodeList.getLength(); x<size; x++) 
	        {
	        	nodeList.item(x).getAttributes().getNamedItem("sessionid").setNodeValue((String) sessionid);
	        }
	       
	       /* NodeList nodeList1 = doc.getElementsByTagName("ATTR_VALUE");
	        for(int x=0,size= nodeList1.getLength(); x<size; x++) 
	        {
	        	nodeList1.item(x).getAttributes().getNamedItem("value").setNodeValue(uid);
	        }*/
	       
	        // Convert addsub_mmsc_Request_MMSC from doc type to string
	        taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_mmsc]Converting document to string..",mmscLoggerCategory);
		    String addsubmmscstr=splCommonComponent.convertDocumentToString(doc);
		    addsubmmscstr = replaceParamName(addsubmmscstr, list,srcTransId,transId,parRowId);
		    String uniqueid = uniqueidentifierFromSearch.toString();
		    addsubmmscstr = addsubmmscstr.replaceAll("unid",uniqueid);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_mmsc]addsubmmsc request message is:"+addsubmmscstr,mmscLoggerCategory);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_mmsc]Validating stub value..",mmscLoggerCategory);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][addsub_mmsc]Stub is :"+isStub,mmscLoggerCategory);
		    CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
			String systemName = cmdSystemDetails.getSystemName();
			String requestMsgFlag = cmdSystemDetails.getRequestMsgFlag();
			taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[HttpMessage][modsub_mmsc]requestMsgFlag :"+ requestMsgFlag, mmscLoggerCategory);
			jdbcDatabaseDAO.updateCmdTrans(srcTransId,  transId, parRowId, requestMsgFlag, addsubmmscstr, mmscLoggerCategory);
		if(isStub)
		{
		taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_mmsc]addsub_mmsc request is :"+addsubmmscstr,mmscLoggerCategory);
		
		FileWriter fileWriter = new FileWriter("conf/spl/adaptors/MMSC/request/addsub_mmsc_Request_"+transId+"_"+retTime+".xml");
		BufferedWriter out = new BufferedWriter(fileWriter);
		out.write(addsubmmscstr);
		out.close();
		//System.out.println("addsub_mmsc_Request file created successfully.");

		  domFactory = DocumentBuilderFactory.newInstance();
		  domFactory.setNamespaceAware(false);
		  builder = domFactory.newDocumentBuilder();
		  doc = builder.parse("conf/spl/adaptors/MMSC/response/Http_Add_Subscriber_Successful.xml");
		}
		else if (!isStub)
		{
					    
		    //Send the request and echo back
			ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
			RequestGateway requestGateway = context.getBean("requestGateway", RequestGateway.class);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_mmsc]Sending addsubmmsc message throught gateway..",mmscLoggerCategory);
			// Ravi: 20130617: Bug#2346: to update cmd trans status to "sent" - Start 
			jdbcDatabaseDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_SENT);
			// Ravi: 20130617: Bug#2346: to update cmd trans status to "sent" - End
			Object addsubmmscreply = requestGateway.echo(addsubmmscstr);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_mmsc]Echoeing back addsubmmsc message..",mmscLoggerCategory);
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][addsub_mmsc]addsubmmsc echoed back  is :"+addsubmmscreply,mmscLoggerCategory);
			
			// parse the reply as doc type 
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_mmsc]Creating DOM document..",mmscLoggerCategory);
			doc = splCommonComponent.createDOMDocument(addsubmmscreply.toString());
			taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][addsub_mmsc]addsubmmscreply response xml as doc type :"+doc,mmscLoggerCategory);
		}
		taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_mmsc]Converting document to string..",mmscLoggerCategory);
		    String addsubMmsc=splCommonComponent.convertDocumentToString(doc);
		    taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][addsub_mmsc]addsub_mmsc message is :"+addsubMmsc,mmscLoggerCategory);
			/*CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
			String systemName = cmdSystemDetails.getSystemName();*/
	
		  XPath xPath = XPathFactory.newInstance().newXPath();
		  Object msgcode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);

		
	      //jdbcDatabaseDAO.updateCommandTransactionStatus(parRowId, ApplicationConstants.STATUS_SENT);
	      jdbcDatabaseDAO.saveCommandResponse(parRowId, cmdRefId, transId, addsubMmsc, systemName);

	      return null;

		}

		public String reqAttributeMissing(String userName, String password,String parRowId,String cmdRefId,String transId,String retTime,String srcTransId,JdbcDatabaseDAO jdbcDatabaseDAO,boolean isStub ) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerConfigurationException, TransformerException, SQLException, SPLExceptionHandler
		 {
			  taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][reqAttributeMissing]Required attribute missing ..", mmscLoggerCategory);
			  DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			  domFactory.setNamespaceAware(false);
			  DocumentBuilder builder = domFactory.newDocumentBuilder();
			  Document doc = builder.parse("conf/spl/adaptors/MMSC/response/Http_Required_Attributes_Missing.xml");
			  taLogger.log(srcTransId,transId,parRowId,ApplicationConstants.LOG_INFO, "[HttpMessage][reqAttributeMissing]Converting document to string..", mmscLoggerCategory);
			  String responseMessage=splCommonComponent.convertDocumentToString(doc);

			  CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
			  String systemName = cmdSystemDetails.getSystemName();
			  jdbcDatabaseDAO.saveCommandResponse(parRowId, cmdRefId, transId, responseMessage, systemName);

			  XPath xPath = XPathFactory.newInstance().newXPath();

			  Object msgcode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);
			  //System.out.println("msgcode is :\n"+msgcode);

			  Object msgtext = xPath.evaluate("/RESPONSE/MESSAGE/@msgtext", doc, XPathConstants.STRING);
			 // System.out.println("msgtext is :\n"+msgtext);

		      return loginResponse;

		}
		
		private String replaceParamName(String requestMessage, List list,String srcTransId,String transactionId, String parRowId)
		{
			for (Object cRefId : list) 
			{
				Map newMap = (Map) cRefId;
				//taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_INFO,"[HttpMessage][replaceParamName]RequestMessage=  "+requestMessage+" \n  newMap="+newMap,mmscLoggerCategory);
			    String paramName = (String) newMap.get("PARAM_NAME");
			    
			    String paramValue = (String) newMap.get("PARAM_VALUE");
			    
			    if ((paramValue == null) || (paramValue == "")) {
			    	paramValue = "";
			    }
			    	//System.out.println("#" + paramName +"#"   +" is replaced by  "+paramValue);
			    	taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][replaceParamName] #" + paramName +"#"   +" is replaced by  "+paramValue,mmscLoggerCategory);
			    	requestMessage = requestMessage.replaceAll("#" + paramName +"#", paramValue);
			   
			}
			taLogger.log(srcTransId,transactionId,parRowId,ApplicationConstants.LOG_DEBUG,"[HttpMessage][replaceParamName]After replacing request message formed is :"+requestMessage,mmscLoggerCategory);
			return requestMessage;
		}

}
