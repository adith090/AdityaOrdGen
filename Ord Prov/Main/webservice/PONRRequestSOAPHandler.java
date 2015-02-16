package com.m1.bcc.spl.webservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ws.soap.SoapElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.BeanFactory;
import common.util.TALogger;

public class PONRRequestSOAPHandler implements SOAPHandler<SOAPMessageContext> {

	private Map<String, Object> ponrStatus;
	String status;
	Integer errorCode;
	Integer rowCount;
	
	DataSource dataSource = (DataSource) BeanFactory.getBean("dataSource");
	JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

	TALogger taLogger = null;

	
	@Override
	public boolean handleMessage(SOAPMessageContext context) {
		/*
		 * try { taLogger = TALogger.getTALogger(); } catch (IOException
		 * ioException) { // TODO Auto-generated catch block
		 * ioException.printStackTrace(); }
		 */


		Boolean isRequest = (Boolean) context
				.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		taLogger = (TALogger) BeanFactory.getBean("taLogger");
		taLogger.log("WS", ApplicationConstants.LOG_INFO, "isRequest "
				+ isRequest,
				ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		// System.out.println(isRequests);
		// taLogger.log("RequestType", 01, ""+isRequest, "splwscrmlogging");

		// for response message only, true for outbound messages, false for
		// inbound
		SOAPMessage soapMessage = null;
		try {
			soapMessage = context.getMessage();
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPHeader soapHeader = soapEnvelope.getHeader();
			SOAPBody soapBody = soapMessage.getSOAPBody();
			
			if (soapHeader == null) {
				soapHeader = soapEnvelope.addHeader();
				generateSOAPErrMessage(soapMessage, "No SOAP header.");
			}
			if (!isRequest) {

				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				soapMessage.writeTo(byteArrayOutputStream);

				taLogger.log(
						"WS",
						ApplicationConstants.LOG_INFO,
						"before getting all the values",
						ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);

				NodeList name = soapBody.getElementsByTagName("TransId");
				NodeList type = soapBody.getElementsByTagName("TransType");				
				NodeList rev = soapBody.getElementsByTagName("Revision");
				
				Node IdNode = name.item(0);				
				Node TypeNode = type.item(0);				
				Node revisionNode = rev.item(0);

				String transId = IdNode.getTextContent();	
				
				String transType = TypeNode.getTextContent();
				
				Integer revision = Integer.parseInt(revisionNode
						.getTextContent());
				
				
				taLogger.log("WS", ApplicationConstants.LOG_INFO,transId + transType + revision , ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);

				ponrStatus = getValuesFromStoredProcedure(transType, transId,
						revision);

				Map<String, Object> map = ponrStatus;

				status = (String) map.get("ALLOW_CANCEL");
				
				taLogger.log("WS", ApplicationConstants.LOG_INFO, "ALLOW_CANCEL:" + status , ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
				
				/*errorCode = (Integer) map.get("error_code");		
				rowCount = (Integer) map.get("rowcount");*/

				//String message = new String(byteArrayOutputStream.toByteArray());

			} else {

				taLogger.log("WS", ApplicationConstants.LOG_INFO, "inside else loop" , ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				soapMessage.writeTo(byteArrayOutputStream);
				String responseMessage = new String(
						byteArrayOutputStream.toByteArray());
				
				//System.out.println("response message");

				//System.out.println("response msg is:" + responseMessage);
				/*
				 * NodeList allowcncel = (NodeList) soapBody
				 * .addChildElement("ALLOW_CANCEL");
				 * soapBody.setNodeValue(status); System.out.println(soapBody);
				 */

				/*Iterator iterator = soapBody.getChildElements();

				Node node = (Node) iterator.next();
				NodeList nodelist = node.getChildNodes();
				System.out.println("FINAL:" + nodelist.item(0));
				System.out.println("FINAL2:" + nodelist.item(1));
				System.out.println("FINAL3:" + nodelist.item(2));
				System.out.println("FINAL name:"
						+ nodelist.item(0).getLocalName());
				// node.getChildNodes()
				final Node statusNode=nodelist.item(0);
				statusNode.setNodeValue(status);*/
				/*nodelist.item(0).setNodeValue(status);
				nodelist.item(1).setNodeValue(""+errorCode);
				nodelist.item(2).setNodeValue(""+rowCount);
				*/
				/*final String localName = soapBody.getFirstChild()
						.getLocalName();*/
				final Node responseNode = soapBody.getFirstChild();
				final Node returnNode = responseNode.getFirstChild();
				final Node statusNode = returnNode.getFirstChild();
				statusNode.setNodeValue(status);
				
				taLogger.log("WS", ApplicationConstants.LOG_INFO, "status node value after setting the value" +statusNode.getNodeValue() , ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);

				/*
				 * NodeList allowcncel =
				 * soapBody.getElementsByTagName("ALLOW_CANCEL");
				 * System.out.println("fstatus" + allowcncel);
				 * 
				 * NodeList errorcode = soapBody
				 * .getElementsByTagName("error_code");
				 * System.out.println("errorcode node:" + errorcode);
				 * 
				 * NodeList rowcount =
				 * soapBody.getElementsByTagName("rowcount");
				 * System.out.println("rowcount node:" + rowcount);
				 * 
				 * Node allowNode = allowcncel.item(0);
				 * System.out.println("node:" + allowNode.getLocalName());
				 * 
				 * Node errorNode = errorcode.item(0);
				 * System.out.println("node2:" + errorNode.getLocalName()); Node
				 * countNode = rowcount.item(0); System.out.println("node3:" +
				 * countNode.getLocalName());
				 * 
				 * allowNode.setNodeValue("Test"); // System.out.println("7");
				 * System.out.println("ponr status is:" + status);
				 * 
				 * errorNode.setNodeValue(""+errorCode);
				 * 
				 * // System.out.println("8");
				 * System.out.println("Node Value is"
				 * +allowNode.getTextContent());
				 * System.out.println("error code is:" + errorCode);
				 * countNode.setNodeValue("" + rowCount); //
				 * System.out.println("9"); System.out.println("rowcount is:" +
				 * rowCount); soapMessage.saveChanges();
				 */
				soapMessage.writeTo(byteArrayOutputStream);
				responseMessage = new String(
						byteArrayOutputStream.toByteArray());
				
				taLogger.log("WS", ApplicationConstants.LOG_INFO, "response msg is:" + responseMessage , ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
			}
			
		} catch (Exception e) {
			generateSOAPErrMessage(soapMessage, e.getMessage());
		}
		return true;
	}

	public Map<String, Object> getValuesFromStoredProcedure(String transType,
			String transId, Integer revision) throws Exception {

		taLogger.log("WS", ApplicationConstants.LOG_INFO,
				"inside calling stored procedure method",
				ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);

		MyStoredProcedure sp = new MyStoredProcedure(this.jdbcTemplate);

		Map<String, Object> result = new HashMap<String, Object>();

		try {
			result = sp.execute(transType, transId, revision);
		}

		catch (DataAccessException dae) {

		}
		
		return result;

	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close(MessageContext context) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<QName> getHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	private void generateSOAPErrMessage(SOAPMessage msg, String reason) {
		try {
			SOAPBody soapBody = msg.getSOAPPart().getEnvelope().getBody();
			SOAPFault soapFault = soapBody.addFault();
			soapFault.setFaultString(reason);
			SOAPFaultException soapFaultException = new SOAPFaultException(
					soapFault);
			// taLogger.log("Fault", 02, soapFaultException.getMessage(),
			// "splwslogging", soapFaultException);
			// throw soapFaultException;
		} catch (SOAPException soapException) {
			soapException.printStackTrace();
			// taLogger.log("generateSOAPErrMessage", 02,
			// soapException.getMessage(), "splwslogging", soapException);
		}
	}

}
