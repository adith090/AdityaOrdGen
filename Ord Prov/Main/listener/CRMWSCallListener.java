package com.m1.bcc.spl.listener;

import java.io.StringReader;
import java.util.Properties;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.jndi.JndiTemplate;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.instructor.Handler;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;

import common.util.TALogger;

public class CRMWSCallListener implements MessageListener {

	private TALogger taLogger = TALogger.getTALogger();
	private String loggerCatalog = "osmlogging";
	
	public CRMWSCallListener(){
		JndiTemplate jndiTemplate = (JndiTemplate) BeanFactory.getBean("jndiTemplate");
		try {
			Properties jmsProperties = SPLCommonComponent.loadProperty(ApplicationConstants.SPLAPP_FILENAME);
			Properties envProperties = jndiTemplate.getEnvironment();
			envProperties.put("java.naming.security.principal", jmsProperties.get("jms_user"));
			taLogger.log("", ApplicationConstants.LOG_DEBUG, "[JMS Authentication]JMS User: " + jmsProperties.get("jms_user"), loggerCatalog);
			String password = (String) jmsProperties.get("jms_pwd");
			taLogger.log("", ApplicationConstants.LOG_DEBUG, "[JMS Authentication]password: " + password, loggerCatalog);
			String key = (String) jmsProperties.get("KEY");
			String decryptedPwd = "";
			if(password!=null && !password.trim().equals("")) {
				decryptedPwd = SPLCommonComponent.Crypt(0, key, password);
			}
			taLogger.log("", ApplicationConstants.LOG_DEBUG, "[JMS Authentication]decryptedPwd: " + decryptedPwd, loggerCatalog);
			envProperties.put("java.naming.security.credentials", decryptedPwd);
			jndiTemplate.setEnvironment(envProperties);
			jndiTemplate.getContext();
		} catch (Exception exception) {
			
		}
	}
	
	@Override
	public void onMessage(Message message) {
		// TODO Auto-generated method stub
		
		TextMessage textMessage = (TextMessage) message;
		
		try {
			
			Handler handlerObj = new Handler();
		
			taLogger.log("CorrelationID=" + textMessage.getJMSCorrelationID(), ApplicationConstants.LOG_DEBUG, "Inside CRMWSCallListenner- onMessage", loggerCatalog);
			taLogger.log("CorrelationID=" + textMessage.getJMSCorrelationID(), ApplicationConstants.LOG_DEBUG, textMessage.getText(), loggerCatalog);
			
			String requestMessage = textMessage.getText();
			Document requestMessageDoc = createDOMDocument(textMessage.getText());
			
			String opgRefId = evaluateXPath(requestMessageDoc, "//ProvRequest/ListOfRef/OPGRefID");
			String funcRefId = evaluateXPath(requestMessageDoc, "//ProvRequest/ListOfRef/FuncRefID");
			String sourceTransId = evaluateXPath(requestMessageDoc, "//OrderID");
			String transId = evaluateXPath(requestMessageDoc, "//ProvRequest/ListOfRef/TransID");
			
			handlerObj.processOrderStatus(opgRefId, funcRefId, sourceTransId, transId, requestMessage, textMessage.getJMSCorrelationID(), requestMessageDoc, getXpathExecutor());
			
		} catch (Exception e){
			
			taLogger.log("[CRMWSCallListenner][onMessage]CRMWSCallListenner Part", ApplicationConstants.LOG_DEBUG,
					"Exception="+ SPLCommonComponent.getStackTrace(e), loggerCatalog);
			
		}
	}
	
//	private String extractSOAPResponse(String inputXML) throws Exception {
//		Document doc = createDOMDocument(inputXML);
//		Node soapBodyNode = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body").item(0);
//		NodeList childNode = soapBodyNode.getChildNodes();
//		Node firstChildElement = null;
//		for(int i=0; i < childNode.getLength(); i++){if(childNode.item(i).getNodeType() == Element.ELEMENT_NODE){firstChildElement = childNode.item(i);break;}}
//		Document newDoc = createNewDOMDocument();
//		Node newNodeinNewDoc = newDoc.importNode(firstChildElement, true);
//		newDoc.appendChild(newNodeinNewDoc);
//		return convertDocumenttoString(newDoc);
//	}
	
	private String evaluateXPath(Document doc, String expr)
			throws XPathExpressionException {

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		
		XPathExpression exp = xpath.compile(expr);
		String xmlDetails = (String) exp.evaluate(doc, XPathConstants.STRING);
		return xmlDetails;

	}
	
	private XPath getXpathExecutor(){
		return XPathFactory.newInstance().newXPath();
	}
	
	private Document createDOMDocument(String inputXML) throws Exception{

		InputSource in = new InputSource();
		StringReader strReader = new StringReader(inputXML);
		
		try {
		
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(true);
			DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
			
			in.setCharacterStream(strReader);
			
			return domBuilder.parse(in);
		
		} catch (Exception e){
			throw e;
		} finally {
			strReader.close();
		}
	}
	
//	private Document createNewDOMDocument() throws ParserConfigurationException{
//		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
//		domFactory.setNamespaceAware(false);
//		DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
//		return domBuilder.newDocument();
//	}
//	
//	private String convertDocumenttoString(Document doc) throws Exception{
//		
//		TransformerFactory factory = TransformerFactory.newInstance();
//		Transformer transformer = factory.newTransformer();
//		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//		StringWriter writer = new StringWriter();
//		Result result = new StreamResult(writer);
//		Source source = new DOMSource(doc);
//		transformer.transform(source, result);
//		writer.close();
//		String finalMessage = writer.toString();
//		
//		return finalMessage;
//		
//	}

}
