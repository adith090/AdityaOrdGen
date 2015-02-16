package com.m1.bcc.spl.translator;


import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.BeanFactory;


public class HTTPLogin {/*
	
	
	String HTTPLogin="";
	String modsub_mmsc="";
	String loginResponse="";
	Object sessionid="";
	Object uniqueidentifier="";
	String featurename;
	
			
	public String LoginMessage(String userName, String password,String parRowId,String cmdRefId,String transId,String srcTransId) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		
			  HTTPLogin     ="POST /upower/ProcessLayer/processuPowerRequest.uone HTTP/1.1\n"
							+"Content-Type:application/x-www-form-urlencoded\n"
							+"User-Agent: Microsoft (R) BizTalk (R) Server 2006 3.0.1.0\n"
							+"Host:172.16.17.130:280\n"
							+"Content-Length: 95\n"
							+"Expect: 100-continue\n"
							+"Connection: Keep-Alive\n"+"\n"
							+"HTTP/1.1 100 Continue\n"+"\n"
							+"xml_data=<REQUEST>\n"
							+"<LOGIN userid=\""+userName+"\""+" "+ "passwd=\"" +password+"\""+" "  
							+"AdminLogin="+"\"\""+"/>\n" 
							+"</REQUEST>"; 
		
		//System.out.println("HTTPLogin Requests:\n"+HTTPLogin);
		
		
		//Added to load the Http_Login_Success.xml 
		
		ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
      	RequestGateway requestGateway = context.getBean("requestGateway",RequestGateway.class);
		Object reply = requestGateway.echo(HTTPLogin);
		//System.out.println("HTTPLogin response xml \n"+reply);
	
		
		//Added to load the Http_Login_Success.xml 
		
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(false); 
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		Document doc = builder.parse((InputStream) reply);
		
		//Read the message code in the xml & if the login is success , take the session ID

		XPath xPath = XPathFactory.newInstance().newXPath();
		Object msgcode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);
		//System.out.println("msg code:\n"+msgcode);
  
		if(msgcode.equals("uPowerErr002"))
		{
			//System.out.println("Login Success");
			sessionid=xPath.evaluate("/RESPONSE/DATA/RECORDS/RECORD/ATTRIBUTE/ATTR_NAME[@name='sessionid']/ATTR_VALUE/@value", doc, XPathConstants.STRING); 
			//System.out.println("SessionId:\n"+sessionid);
			modsub_mmsc(userName,password,parRowId,cmdRefId,transId);
	  	}
  
		else
		{
			reqAttributeMissing(userName,password,parRowId,cmdRefId,transId); 
		}
  
	      
	      return HTTPLogin;
	}

		//Method to form transaction request & to search 
	
		public String modsub_mmsc(String UserName, String Password,String parRowId,String cmdRefId,String transId) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException 
			
				{
			
				JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("databaseDAO");
			
				String uid =jdbcDatabaseDAO.getUID();
				String featureName=jdbcDatabaseDAO.getFeatureName();
				String modsub_mmsc ="POST /upower/ProcessLayer/processuPowerRequest.uone HTTP/1.1\n"
										 +"Content-Type:application/x-www-form-urlencoded\n"
										 +"User-Agent: Microsoft (R) BizTalk (R) Server 2006 3.0.1.0\n"
										 +"Host:172.16.17.130:280\n"
										 +"Content-Length: 95\n"
										 +"Expect: 100-continue\n"
										 +"Connection: Keep-Alive\n"+"\n"
										 +"HTTP/1.1 100 Continue\n"+"\n"
										 +"xml_data="
										 +"<REQUEST "+"sessionid="+"\""+sessionid+"\""+">\n"
										 +"<SEARCHENTRY featurename="+"\""+"modsub_mmsc"+"\"" +" "+"searchfilter="+"\""+"uid="+uid+"\""+" "+"searchbase="+"\""+"\""+" "+"specialfilter="+"\""+"\">"+"\n"
										 +"<SHOWATTRIBUTE name="+"\""+"uniqueidentifier"+"\""+"/>\n"+"</SEARCHENTRY>\n"
										 +"</REQUEST>\n";
				
			
				//int cmd_values = jdbcDatabaseDAO.updateTable(parRowId,cmdRefId,transId,responsexml);
				
			
				
				ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
		      	RequestGateway requestGateway = context.getBean("requestGateway",RequestGateway.class);
				Object reply = requestGateway.echo(modsub_mmsc);
				//System.out.println("HTTPSearch response xml \n"+reply);
			
				DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
				domFactory.setNamespaceAware(false); 
				DocumentBuilder builder = domFactory.newDocumentBuilder();
				Document doc = builder.parse((InputStream) reply);
				
						  
						  XPath xPath = XPathFactory.newInstance().newXPath();
					      Object msgcode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);
					    //  System.out.println("msgcode:\n"+msgcode);
					      
					      if(msgcode.equals("uPowerErr052"))
					      {
					    	
					    //	System.out.println("Search successful.");
					    	Object uid1=xPath.evaluate("/RESPONSE/DATA/RECORDS/RECORD/ATTRIBUTE/ATTR_NAME[@name='uid']/ATTR_VALUE/@value", doc, XPathConstants.STRING); 
					    //	System.out.println("uid:\n"+uid1);
					    	Object uniqueidentifier=xPath.evaluate("/RESPONSE/DATA/RECORDS/RECORD/ATTRIBUTE/ATTR_NAME[@name='uniqueidentifier']/ATTR_VALUE/@value", doc, XPathConstants.STRING); 
					    //	System.out.println("uniqueidentifier:\n"+uniqueidentifier);
					    	
					    	if(featureName.equals("delsub_common"))
					    	 {
					    		delsub_common(UserName, Password, parRowId, cmdRefId, transId);
					    	 }
					    	else if(featurename.equals("addsub_common"))
				    	     {
				    		 addsub_common(UserName, Password, parRowId, cmdRefId, transId);
				    	     }
				    	   else 
				    	   	{
				    	 	addsub_mmsc(UserName, Password, parRowId, cmdRefId, transId);
				    	    }
				    	   	}
					      
					      else 
					      {
					    	
					    	//System.out.println("No matches were found for the search criteria you entered.  Please try again.");
					    	
					    	
					    	  reqAttributeMissing(UserName,Password,parRowId,cmdRefId,transId); 
					        
					   	}
				return modsub_mmsc;
				
			}
		
		
		public String delsub_common(String UserName, String Password,String parRowId,String cmdRefId,String transId) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException 

		  {

			JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("databaseDAO");

			String delsub_common = "POST /upower/ProcessLayer/processuPowerRequest.uone HTTP/1.1\n"
					 			 +"Content-Type:application/x-www-form-urlencoded\n"
					 			 +"User-Agent: Microsoft (R) BizTalk (R) Server 2006 3.0.1.0\n"
					 			 +"Host:172.16.17.130:280\n"
					 			 +"Content-Length: 95\n"
					 			 +"Expect: 100-continue\n"
					 			 +"Connection: Keep-Alive\n"+"\n"
					 			 +"HTTP/1.1 100 Continue\n"+"\n"
					 			 +"xml_data="
					 			 +"<REQUEST "+"sessionid="+"\""+sessionid+"\">\n"
					 			 +"<DELETEENTRY featurename="+"\""+"delsub_common"+"\""+" "+"base_record="+"\""+"Operator Community of Interest"+"\""+" "
					 			 +"record_identifier="+"\""+"uniqueidentifier="+uniqueidentifier+"\">\n"
					 			 + "<ATTRIBUTE>\n"
					 			 +"</ATTRIBUTE>\n"
					 			 +"</DELETEENTRY>\n"
					 			 +"</REQUEST>";
			//System.out.println("delsub_common request\n"+delsub_common);
			
			
			ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
	      	RequestGateway requestGateway = context.getBean("requestGateway",RequestGateway.class);
	      	
			Object reply = requestGateway.echo(delsub_common);
			//System.out.println("delsub_common response xml \n"+reply);
		
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(false); 
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse((InputStream) reply);
			  
			  XPath xPath = XPathFactory.newInstance().newXPath();
			  Object msgcode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);
		      //System.out.println("msgcode:\n"+msgcode);
		      
		      if(msgcode.equals("UM_1150"))
		      {
		    	  //System.out.println("Delete Subscriber Successful.");
		      }
		      
		      else
		      {
		    	  reqAttributeMissing(UserName,Password,parRowId,cmdRefId,transId);
		      }
		      
		      
		      return delsub_common ;

			}
		
		
		public String addsub_common(String UserName, String Password,String parRowId,String cmdRefId,String transId) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException 
		
		{

			JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("databaseDAO");

			String addsub_common ="POST /upower/ProcessLayer/processuPowerRequest.uone HTTP/1.1\n"
								 +"Content-Type:application/x-www-form-urlencoded\n"
								 +"User-Agent: Microsoft (R) BizTalk (R) Server 2006 3.0.1.0\n"
								 +"Host:172.16.17.130:280\n"
								 +"Content-Length: 95\n"
								 +"Expect: 100-continue\n"
								 +"Connection: Keep-Alive\n"+"\n"
								 +"HTTP/1.1 100 Continue\n"+"\n"
								 +"xml_data="+"<REQUEST "+"sessionid="+"\""+sessionid+"\">\n"
								 +"<ADDENTRY featurename="+"\""+"addsub_common"+"\""+" "
								 +"base_record="+"\""+"Operator Community of Interest"+"\">"
								 +"\n<ATTRIBUTE>" 
								 +"\n<ATTR_NAME name="+"\""+"\">"
								 +"\n<ATTR_VALUE value="+"\""+"\""+"/>\n"
								 + "</ATTR_NAME>\n"
								 +"</ATTRIBUTE>\n"
								 +"</ADDENTRY>\n"
								 +"</REQUEST>";
			
			//System.out.println("addsub_common request:\n"+addsub_common);
			
			ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
	      	RequestGateway requestGateway = context.getBean("requestGateway",RequestGateway.class);
			Object reply = requestGateway.echo(addsub_common);
			//System.out.println("addsub_common response xml \n"+reply);
		
			
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(false); 
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse((InputStream) reply);
			  
			  XPath xPath = XPathFactory.newInstance().newXPath();
			  Object msgcode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);
		      
			  
		      if(msgcode.equals("UM_1048"))
		      {
		    	 //System.out.println("The common subscriber data were added successfully : Full Name=Autosubscription User");
		    	 //System.out.println("Add main line item is success and the msg code is :\n"+msgcode);
		    	  
		      }
		      
		      else
		      {
		    	  reqAttributeMissing(UserName,Password,parRowId,cmdRefId,transId);
		      }
			
			
			return addsub_common;
		
		}


		public String addsub_mmsc(String UserName, String Password,String parRowId,String cmdRefId,String transId) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException 

		{

		JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("databaseDAO");

		
		String addsub_mmsc ="POST /upower/ProcessLayer/processuPowerRequest.uone HTTP/1.1\n"
						   +"Content-Type:application/x-www-form-urlencoded\n"
						   +"User-Agent: Microsoft (R) BizTalk (R) Server 2006 3.0.1.0\n"
						   +"Host:172.16.17.130:280\n"
						   +"Content-Length: 95\n"
						   +"Expect: 100-continue\n"
						   +"Connection: Keep-Alive\n"+"\n"
						   +"HTTP/1.1 100 Continue\n"+"\n"
						   +"xml_data="
						   +"<REQUEST "
						   +"sessionid="+"\""+sessionid+"\">\n"
						   +"<ADDENTRY featurename="+"\""+"addsub_mmsc"+"\""+" "
						   +"base_record="+"\""+"Operator Community of Interest"+"\">"
						   +"\n<ATTRIBUTE>" 
						   +"\n<ATTR_NAME name="+"\""+"uniqueidentifier"+"\""+" "+"required="+"\""+"1"+"\">"
						   +"\n<ATTR_VALUE value="+"\""+uniqueidentifier+"\""+"/>"
						   +"\n</ATTR_NAME>"
						   +"\n<ATTR_NAME name="+"\""+""+"\">"
						   +"\n<ATTR_VALUE value="+"\""+""+"\"/>\n"
						   +"</ATTR_NAME>\n"
						   +"</ATTRIBUTE>\n"
						   +"</ADDENTRY>\n"
						   +"</REQUEST>\n";

		//System.out.println("addsub_mmsc request:\n"+addsub_mmsc);
		
		ApplicationContext context = new ClassPathXmlApplicationContext("Http_adapter.xml");
      	RequestGateway requestGateway = context.getBean("requestGateway",RequestGateway.class);
		Object reply = requestGateway.echo(addsub_mmsc);
		//System.out.println("addsub_mmsc response xml \n"+reply);
	
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(false); 
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		Document doc = builder.parse((InputStream) reply);
		
		  XPath xPath = XPathFactory.newInstance().newXPath();
		  Object msgcode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);
	      
	      if(msgcode.equals("UM_1048"))
	      {
	    	  //System.out.println("The common subscriber data were added successfully : Full Name=Autosubscription User");
	    	  //System.out.println("Add vas is success and the msg code is : \n"+msgcode);
	      }
	      else
	      {
	    	  reqAttributeMissing(UserName,Password,parRowId,cmdRefId,transId);
	      }
		
	      return addsub_mmsc;

		}
		
		public String reqAttributeMissing(String UserName, String Password,String parRowId,String cmdRefId,String transId) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException 
		{
			
			  DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			  domFactory.setNamespaceAware(false); 
			  DocumentBuilder builder = domFactory.newDocumentBuilder();
			  Document doc = builder.parse("C:/Velu/WorkSpaces/Log4j/Http_Translator/WebContent/WEB-INF/resources/Http_Required_Attributes_Missing.xml");
			  
			  XPath xPath = XPathFactory.newInstance().newXPath();
			  
			  Object msgcode = xPath.evaluate("/RESPONSE/MESSAGE/@msgcode", doc, XPathConstants.STRING);
			 // System.out.println("msg code is"+msgcode);
		      
		      Object msgtext= xPath.evaluate("/RESPONSE/MESSAGE/@msgtext", doc, XPathConstants.STRING);
		     // System.out.println("msg text is"+msgtext);
		     
		      return loginResponse;
			
		}
		
*/}