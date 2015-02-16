package com.m1.sg.bcc.om.util;

/************************************************************************************************************************************
 *  
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.util
 * Description: This utility class created for handle the request message in protocol Web Service and JMS Message 
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMPollerSOAPUtil {
	
	/**
	 * Create SOAP Header, SOAP Body and Web Service authentication XML
	 * 
	 * @param requestMessage	Payload message
	 * @param username			Username for authentication
	 * @param password			Password for authentication
	 * @return					Payload insert SOAP request
	 */
	public static String createSOAPRequest(String requestMessage, String username, String password){
		StringBuffer sb  = new StringBuffer();
		sb.append("<soapenv:Envelope xmlns:ord=\"http://xmlns.oracle.com/communications/ordermanagement\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">");
		sb.append("<soapenv:Header>");
		sb.append("<wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
		sb.append("<wsse:UsernameToken wsu:Id=\"UsernameToken-1\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">");
		sb.append("<wsse:Username>");
	    sb.append(username);
		sb.append("</wsse:Username>");
		sb.append("<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">");
		sb.append(password);
		sb.append("</wsse:Password>");
		sb.append("</wsse:UsernameToken>");
		sb.append("</wsse:Security>");
		sb.append("</soapenv:Header>");
		sb.append("<soapenv:Body>");
		sb.append("<ord:CreateOrder>");
		sb.append(requestMessage);
		sb.append("</ord:CreateOrder>");
		sb.append("</soapenv:Body>"); 
		sb.append("</soapenv:Envelope>");
		String soapRequest = sb.toString();
		sb=null;		
		
		return  soapRequest;
	}
	
}
