package com.m1.bcc.spl.webservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLExceptionHandler;

import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 18/11/2012					Ravikumar G				Created
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 06/11/2013					Ravikumar G				Bug#20397: modifed for new request format
 * 18/11/2013					Ravikumar G				Bug 20601 - service internal id resets to update back CRM
 * 27/12/2013					Yohan					Bug 22336 - Modify CRM MANUALTASK response to CRM
 * 17/01/2014					Ravikumar G				Bug#23710 - Updated cmd_trans column row_id type to number
 ******************************************************************************/

public class RequestSOAPHandler implements SOAPHandler<SOAPMessageContext>{

	private String statusCode;
	private String correlationId;
	private boolean isError = false;
	TALogger taLogger = null;
	private String loggerCategory = ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING;

	@Override
	public boolean handleMessage(SOAPMessageContext context) {
		Boolean isRequest = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		taLogger = (TALogger) BeanFactory.getBean("taLogger");
		taLogger.log("", "", "", ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]isRequest " + isRequest, loggerCategory);

		//for response message only, true for outbound messages, false for inbound
		SOAPMessage soapMessage = null;
		try {
		    soapMessage = context.getMessage();
		    taLogger.log("", "", "", ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]Initializing Soap Envelope " , loggerCategory);
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPHeader soapHeader = soapEnvelope.getHeader();
			SOAPBody soapBody = soapMessage.getSOAPBody();
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			soapMessage.writeTo(byteArrayOutputStream);
		    String requestMessage = new String(byteArrayOutputStream.toByteArray());
		    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		    taLogger.log("", "", "", ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]set Namespaceaware true" , loggerCategory);
			domFactory.setNamespaceAware(false);
			DocumentBuilder documentBuilder = domFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(new ByteArrayInputStream(requestMessage.getBytes()));

			if (soapHeader == null) {
				taLogger.log("", "", "", ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]inside if soap header null " , loggerCategory);
				soapHeader = soapEnvelope.addHeader();
				generateSOAPErrMessage(soapMessage, "No SOAP header.");
			}
			
			if(!isRequest){
				taLogger.log("", "", "", ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]Inside if isRequest false" , loggerCategory);
			    
				NodeList nodeList = document.getElementsByTagName("arrayOfParamInfoType");
				taLogger.log("", "", "", ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]get arrayOfParamInfoType Nodelist : " + nodeList , loggerCategory);

				correlationId = (String) getXpathValue(document, "Envelope/Body/input/msgHeader/correlationID", XPathConstants.STRING);
				taLogger.log("", "", correlationId, ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]cmd Row Id : " + correlationId , loggerCategory);
				
				if(correlationId!=null && !correlationId.equals("")) {
					DatabaseDAO databaseDAO = (DatabaseDAO) BeanFactory.getBean(ApplicationConstants.BEAN_DATABASEDAO);

					Map<String, Object> cmdTransDetailsMap = databaseDAO.getCmdOrderTransactionDetails(correlationId);
					taLogger.log("", "", correlationId, ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]cmdTransDetailsMap : " + cmdTransDetailsMap, loggerCategory);
					if(cmdTransDetailsMap==null || cmdTransDetailsMap.isEmpty()) {
						generateSOAPErrMessage(soapMessage, "Invalid Correlation Id in request message");
					}

					String cmdRowId = "" + (BigDecimal) cmdTransDetailsMap.get("ROW_ID");
					String cmdRefId = (String) cmdTransDetailsMap.get("CMD_REF_ID");
					String funcRefId = (String) cmdTransDetailsMap.get("FUNC_REF_ID");
					String opgRefId = (String) cmdTransDetailsMap.get("OPG_REF_ID");
					String osmCorrelationId = (String) cmdTransDetailsMap.get("CORR_ID");
					String transId = (String) cmdTransDetailsMap.get("TRANS_ID");

					taLogger.log("", transId, correlationId, ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]osmCorrelationId : " + osmCorrelationId, loggerCategory);

					for (int element = 0; element < nodeList.getLength(); element++) {
						Node listOfRefNode = nodeList.item(element);
						createElement(document, listOfRefNode, "statusCode", "0");
						createElement(document, listOfRefNode, ApplicationConstants.MESSAGE_TOMORDERID, transId);
						createElement(document, listOfRefNode, ApplicationConstants.MESSAGE_OPGREFID, opgRefId);
						createElement(document, listOfRefNode, ApplicationConstants.MESSAGE_FUNCREFID, funcRefId);
						createElement(document, listOfRefNode, ApplicationConstants.MESSAGE_CMDREFID, cmdRefId);
					}
					
					taLogger.log("", transId, correlationId, ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]cmdRefId : " + cmdRefId, loggerCategory);

					saveChanges(soapMessage, document);

					byteArrayOutputStream.reset();
					soapMessage.writeTo(byteArrayOutputStream);
					//String responseMessage = new String(byteArrayOutputStream.toByteArray());
					taLogger.log("", transId, correlationId, ApplicationConstants.LOG_INFO, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]Message : " + requestMessage, loggerCategory);
					//databaseDAO.updateCommandTransactionStatus(correlationId);
					try {
						taLogger.log("", transId, correlationId, ApplicationConstants.LOG_DEBUG, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]Saving command response", loggerCategory);
						databaseDAO.saveCommandResponse(cmdRowId, cmdRefId, transId, requestMessage, ApplicationConstants.CRM_TASK, false);
					} catch (SPLExceptionHandler sqlExceptionHandler) {
						taLogger.log("", transId, correlationId, ApplicationConstants.LOG_ERROR, "[RequestSOAPHandler][handleMessage][CRMTaskResponse]Status Node Name: ", loggerCategory, sqlExceptionHandler);
					}


				}else {
					generateSOAPErrMessage(soapMessage, "Correlation Id is empty in request message");
				}

				//OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
				//orderTransactionSender.postMessage(new String(byteArrayOutputStream.toByteArray()));
				//System.out.println(orderTransactionSender);
			}else {
				// Yohan : 20131227 : Bug 22336 : Modify CRM MANUALTASK response to CRM - start
				document.renameNode(document.getElementsByTagName("processRequestResponse").item(0), "", "processRequest");
				Node inputNode = document.getElementsByTagName("processRequest").item(0);
				Node msgHeaderNode = createElement(document, inputNode, "msgResHeader", "");
				Element t = (Element) msgHeaderNode;
				t.setAttribute("xmlns", "");
				
				createElement(document, msgHeaderNode, "correlationID", correlationId);
				if(!isError) {
					createElement(document, msgHeaderNode, "returnCode", "0");
					createElement(document, msgHeaderNode, "returnMessage", "Success");
				}
				else {
					createElement(document, msgHeaderNode, "returnCode", "1");
					createElement(document, msgHeaderNode, "returnMessage", "Error");
				}
				
				Node msgBodyNode = createElement(document, inputNode, "msgResBody", "");
				saveChanges(soapMessage, document);
				
				// Yohan : 20131227 : Bug 22336 : Modify CRM MANUALTASK response to CRM - end
			}
		}catch (Exception exception) {
			generateSOAPErrMessage(soapMessage, exception.getMessage());
		}
		return true;
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
			isError = true;
			/*SOAPBody soapBody = msg.getSOAPPart().getEnvelope().getBody();
			SOAPFault soapFault = soapBody.addFault();
			soapFault.setFaultString(reason);
			SOAPFaultException soapFaultException = new SOAPFaultException(soapFault);
			taLogger = (TALogger) BeanFactory.getBean("taLogger");
			taLogger.log("", "", "", ApplicationConstants.LOG_INFO, "[RequestSOAPHandler][handleMessage][CRMTask]Fault : " , loggerCategory, soapFaultException);*/
			/*Document document = getDocument(msg);
			Node inputNode = document.getElementsByTagName("processRequestResponse").item(0);
			Node msgHeaderNode = createElement(document, inputNode, "msgHeader", "");
			createElement(document, msgHeaderNode, "correlationID", correlationId);
			Node msgBodyNode = createElement(document, inputNode, "msgBody", "");
			createElement(document, msgBodyNode, "returnCode", "1");
			createElement(document, msgBodyNode, "returnMessage", reason);
			saveChanges(msg, document);*/
		} catch (Exception exception) {
			taLogger.log("", "", "", ApplicationConstants.LOG_ERROR, "[RequestSOAPHandler][handleMessage][CRMTask]Fault : " , loggerCategory, exception);
		}
	}

	private Element createElement(Document document, Node listOfRefNode, String elementName, String elementValue) {
		Element element = document.createElement(elementName);
		element.setTextContent(elementValue);
		listOfRefNode.appendChild(element);
		return element;
	}
	
	public Object getXpathValue(Document document, String expression, QName returnType) throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		Object commandParameterValue = xPath.evaluate(expression, document, XPathConstants.STRING);
		return commandParameterValue;
	}
	
	private Document getDocument(SOAPMessage soapMessage) throws SOAPException, IOException, ParserConfigurationException, SAXException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		soapMessage.writeTo(byteArrayOutputStream);
	    String requestMessage = new String(byteArrayOutputStream.toByteArray());
	    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(false);
		DocumentBuilder documentBuilder = domFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(new ByteArrayInputStream(requestMessage.getBytes()));
		return document;
	}
	
	private void saveChanges(SOAPMessage soapMessage, Document document) throws TransformerException, SOAPException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(document);
		StreamResult output = new StreamResult(new StringWriter());
		transformer.transform(source, output);
		StreamSource streamSource = new StreamSource(new ByteArrayInputStream(output.getWriter().toString().getBytes()));
		soapMessage.getSOAPPart().setContent(streamSource);
		soapMessage.saveChanges();
	}

}
