package com.m1.bcc.spl.translator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.TransformerException;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import com.m1.bcc.spl.constants.ApplicationConstants;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 08/01/2013					Ravikumar G				Created
 ******************************************************************************/

public class WSSEHeaderWebServiceMessageCallback implements WebServiceMessageCallback {

    public static final String WSS_10_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    public final static String WSSE_Security_Elem = "Security";

    public final static String WSSE_Security_prefix = "wsse";

    public final static String WSSE_UsernameToken_Elem = "UsernameToken";

    public final static String WSSE_Username_Elem = "Username";

    public final static String WSSE_Password_Elem = "Password";

    private String username;
    private String password;
    private String soapAction;
    private TALogger taLogger;

    private String loggerCategory = ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING;


    public WSSEHeaderWebServiceMessageCallback(String username, String password, String soapAction) {
    	taLogger = TALogger.getTALogger();
    	taLogger.log("[WSSEHeaderWebServiceMessageCallback]", ApplicationConstants.LOG_INFO,"username : "+ username, loggerCategory);
        this.username = username;
        this.password = password;
        this.soapAction = soapAction;
    }

    /**
     * @see org.springframework.ws.client.core.WebServiceMessageCallback#doWithMessage(org.springframework.ws.WebServiceMessage)
     */
    public void doWithMessage(WebServiceMessage message) throws IOException, TransformerException {
        try {

        	taLogger.log("[WSSEHeaderWebServiceMessageCallback][doWithMessage]", ApplicationConstants.LOG_INFO," WebServiceMessage : "+ message, loggerCategory);

            // you have to use the default SAAJWebMessageFactory
            SaajSoapMessage saajSoapMessage = (SaajSoapMessage) message;
            saajSoapMessage.setSoapAction(soapAction);
            SOAPMessage soapMessage = saajSoapMessage.getSaajMessage();

            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
            SOAPHeader soapHeader = soapEnvelope.getHeader();

            taLogger.log("[WSSEHeaderWebServiceMessageCallback][doWithMessage]", ApplicationConstants.LOG_INFO," soapHeader : "+ soapHeader, loggerCategory);

            // ... add the WS-Security Header Element
            Name headerElementName = soapEnvelope.createName(WSSE_Security_Elem, WSSE_Security_prefix, WSS_10_NAMESPACE);
            SOAPHeaderElement soapHeaderElement = soapHeader.addHeaderElement(headerElementName);

            // otherwise a RST without appliesTo fails
            soapHeaderElement.setMustUnderstand(true);

            // add the usernameToken to "Security" soapHeaderElement
            SOAPElement usernameTokenSOAPElement = soapHeaderElement.addChildElement(WSSE_UsernameToken_Elem);

            taLogger.log("[WSSEHeaderWebServiceMessageCallback][doWithMessage]", ApplicationConstants.LOG_INFO," usernameTokenSOAPElement : "+ usernameTokenSOAPElement, loggerCategory);

            // add the username to usernameToken
            SOAPElement userNameSOAPElement = usernameTokenSOAPElement.addChildElement(WSSE_Username_Elem);
            userNameSOAPElement.addTextNode(username);

            taLogger.log("[WSSEHeaderWebServiceMessageCallback][doWithMessage]", ApplicationConstants.LOG_INFO," userNameSOAPElement : "+ userNameSOAPElement.getTextContent(), loggerCategory);

            // add the password to usernameToken
            SOAPElement passwordSOAPElement = usernameTokenSOAPElement.addChildElement(WSSE_Password_Elem);
            passwordSOAPElement.addTextNode(password);
            taLogger.log("[WSSEHeaderWebServiceMessageCallback][doWithMessage]", ApplicationConstants.LOG_INFO," passwordSOAPElement : "+ passwordSOAPElement.getTextContent(), loggerCategory);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		    soapMessage.writeTo(byteArrayOutputStream);
		    String credentialMessage = new String(byteArrayOutputStream.toByteArray());
		    taLogger.log("[WSSEHeaderWebServiceMessageCallback][doWithMessage]", ApplicationConstants.LOG_INFO, "Message in Request CallBack = " + credentialMessage, loggerCategory);

        } catch (SOAPException soapException) {
            throw new RuntimeException("WSSEHeaderWebServiceMessageCallback", soapException);
        }
    }
}
