package com.m1.sg.bcc.om.config;

import org.w3c.dom.Document;

import com.m1.sg.bcc.om.util.AESEncryption;
import com.m1.sg.bcc.om.util.OMPollerXMLUtil;

public class OMPollerConfigurationCredential {
	
	private static String password;
	private static String username;
	private String jmsPasswordEncrypt;
	
	static {
		try {
		
			String xmlCredential = OMPollerXMLUtil.readXMLFile("./conf/osmapp_secure.xml");
			Document credentialDoc = OMPollerXMLUtil.createDOMDocument(xmlCredential);
			
			System.out.println("Load Create Order Credential ....");
			
			password = credentialDoc.getElementsByTagName("Password").item(0).getChildNodes().item(0).getNodeValue();
			username = credentialDoc.getElementsByTagName("Username").item(0).getChildNodes().item(0).getNodeValue();
			
			System.out.println("Decrypt Create Order Credential ...");
			
			password = AESEncryption.aesDecrypt(password);
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static String getPassword(){
		return password;
	}
	
	public static String getUsername(){
		return username;
	}
	
	public void setPasswordEncrypt(String jmsPasswordEncrypt){
		this.jmsPasswordEncrypt = jmsPasswordEncrypt;
	}
	
	public String getJMSCredential() throws Exception{
		System.out.println("Decrypt JMS Credential ...");
		return AESEncryption.aesDecrypt(this.jmsPasswordEncrypt);
	}
	
}
