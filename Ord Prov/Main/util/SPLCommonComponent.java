package com.m1.bcc.spl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.instructor.HandlerVariables;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.model.OrderTransactonDetail;
import com.m1.bcc.spl.model.OrderTransactonDetailBill;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;

import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 22/02/2013					Ravikumar G				Modified for logging cleanup
 * 23/04/2013					Sudharsan				Added 'responseCode' parameter 
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 22/05/2013					Sudharsan				Implemented Radius server
 * 27/05/2013   				Sudharsan				Implemented ADS
 * 25/06/2013					Sudharsan				Implemented RIM SIM Exchange error handling logic
 * 30/08/2013					Sudharsan				Changed statuscode,errorcode,taskresponse in TOM response that we send while acknowledgement
 * 20/09/2013					Ravikumar G				removed system.out 
 * 06/11/2013					Ravikumar G				Bug#20397: to update xml_par_ele_id in cmd_trans_dtls table 
 * 29/11/2013					Ravikumar G				Bug#20592- Changed for DB Adaptor design change
 * 06/12/2013					Ravikumar G				Bug#21337 - updated method to update task response from CRM
 * 16/01/2014					Ravikumar G				Bug#23590 - removed column task_response from cmd_trans table
 * 03/02/2014					Ravikumar G				Bug#24615 - Updated method getStatusCodeAndMsg to get return code and message for ADS
 ******************************************************************************/

public class SPLCommonComponent {

	private TALogger taLogger;
	private String loggerCategory;
	private Properties soapActionProps;
	private String logSrcTransId;
	private String logTransId;
	private String logCmdRowId;
    private static final String[] timeFormats = {"HH:mm:ss","HH:mm", "HHmmss", "hhmmss"};
    private static final String[] dateSeparators = {"/","-"," ", ""};

    private static final String DMY_FORMAT = "dd{sep}MM{sep}yyyy";
    private static final String YMD_FORMAT = "yyyy{sep}MM{sep}dd";
    private static final String DMMMY_FORMAT = "dd{sep}MMM{sep}yyyy";

    private static final String ymd_template = "\\d{4}{sep}\\d{2}{sep}\\d{2}.*";
    private static final String dmy_template = "\\d{2}{sep}\\d{2}{sep}\\d{4}.*";
    private static final String dmmmy_template = "\\d{2}{sep}\\w{3}{sep}\\d{4}.*";

	//public SPLCommonComponent() {}

	public SPLCommonComponent(TALogger taLogger, String loggerCategory) throws IOException {
		this.taLogger = taLogger;
		this.loggerCategory = loggerCategory;
		soapActionProps = new Properties();
		InputStream inputStream = new FileInputStream(ApplicationConstants.WSSOAPACTION_FILENAME);
		soapActionProps.load(inputStream);
	}


	public String getLoggerCategory() {
		return loggerCategory;
	}

	public void setLoggerCategory(String loggerCategory) {
		this.loggerCategory = loggerCategory;
	}

	public String getLogSrcTransId() {
		return logSrcTransId;
	}

	public void setLogSrcTransId(String logSrcTransId) {
		this.logSrcTransId = logSrcTransId;
	}

	public String getLogTransId() {
		return logTransId;
	}

	public void setLogTransId(String logTransId) {
		this.logTransId = logTransId;
	}

	public String getLogCmdRowId() {
		return logCmdRowId;
	}

	public void setLogCmdRowId(String logCmdRowId) {
		this.logCmdRowId = logCmdRowId;
	}


	/**
	 *
	 * @return
	 * @throws IOException
	 */
	public static boolean getStubProperty() throws IOException {
		boolean isStub = false;
		String stubFlag = "N";
		Properties properties = new Properties();
		//InputStream inputStream = getClass().getClassLoader().getResourceAsStream(ApplicationConstants.STUB_FILENAME);
		InputStream inputStream = new FileInputStream(ApplicationConstants.STUB_FILENAME);
		properties.load(inputStream);
        stubFlag = (String) properties.getProperty(ApplicationConstants.STUB_PROPERTY);
		if(ApplicationConstants.STUB_YES.equalsIgnoreCase(stubFlag))
			isStub = true;
		return isStub;
	}

	/**
	 *
	 * @return
	 * @throws IOException
	 */
	public static Properties getSystemStubProperty() throws IOException {
		Properties properties = new Properties();
		InputStream inputStream = new FileInputStream(ApplicationConstants.STUBBING_FILENAME);
		properties.load(inputStream);
		return properties;
	}

	public static Properties getU2KRSPProperty() throws IOException {
		Properties properties = new Properties();
		InputStream inputStream = new FileInputStream(ApplicationConstants.U2KRSP_FILENAME);
		properties.load(inputStream);
		return properties;
	}

	/**
	 *
	 * @param properties
	 * @param systemName
	 * @return
	 * @throws IOException
	 */
	public static boolean getStubbing(Properties properties, String systemName) throws IOException {
		boolean isStub = false;
		String stubFlag = "N";
        stubFlag = (String) properties.getProperty(systemName);
		if(ApplicationConstants.STUB_YES.equalsIgnoreCase(stubFlag))
			isStub = true;
		return isStub;
	}

	public String getWSSoapAction(String interfaceName) throws IOException {
		String soapAction = (String) soapActionProps.getProperty(interfaceName);
		if(soapAction==null)
			soapAction = "";
		return soapAction;
	}

	public static Properties loadProperty(String fileName) throws IOException {
		Properties properties = new Properties();
		InputStream inputStream = new FileInputStream(fileName);
		properties.load(inputStream);
		return properties;
	}

	/**
	 *
	 * @param date
	 * @return
	 */
	public static String formatDateToString(Date date) {
		DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");
		return dateFormat.format(date);
	}

	/**
	 *
	 * @return
	 */
	public static String formatCurrentDateToDataTime() {
		String dateTimeFormat = "";
	    SimpleDateFormat timeStampFormatter = new SimpleDateFormat("dd-MMM-yyyy-hh-mm-ss");
	    dateTimeFormat = timeStampFormatter.format(new Date());
	    return dateTimeFormat;
	}

	public static String formatSysDateToText() {
		String dateTimeFormat = "";
		//System.out.println("inside formatSysDateToText");
	    SimpleDateFormat timeStampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
	    dateTimeFormat = timeStampFormatter.format(new Date());
	    //System.out.println("in formatSysDateToText date: " + dateTimeFormat);
	    return dateTimeFormat;
	}

	public static String formatSysDateToDate(String paramValue, String dateFormat, String interfaceName) {

		String dateTimeFormat = "";

		if(interfaceName.equalsIgnoreCase("ARBOR_001") || interfaceName.equalsIgnoreCase("ARBOR_002")){
		Date date = null;
		//System.out.println("date format inside formate date is: " + dateFormat);
		SimpleDateFormat sdf  = new SimpleDateFormat(ApplicationConstants.DATE_FORMAT_ARBOR);//yyyy-MM-DD'T'hh:mm:ss");
		try {
			date = sdf.parse(paramValue);
			String a=date.toString();


		} catch (ParseException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}



	    SimpleDateFormat timeStampFormatter = new SimpleDateFormat(dateFormat);
	    dateTimeFormat = timeStampFormatter.format(date);//new Date());
		}

		else if(interfaceName.equalsIgnoreCase("BBRR_001")){

			dateTimeFormat = "";
			Date date = null;
			SimpleDateFormat sdf  = new SimpleDateFormat(ApplicationConstants.DATE_FORMAT_BBRR);//yyyy-MM-DD'T'hh:mm:ss");
			try {
				date = sdf.parse(paramValue);
				String a=date.toString();



			} catch (ParseException e) {
				// TODO Auto-generated catch block

				e.printStackTrace();
			}



			SimpleDateFormat timeStampFormatter = new SimpleDateFormat(dateFormat);
		    //SimpleDateFormat timeStampFormatter = new SimpleDateFormat("dd-MM-yyyy");
		    dateTimeFormat = timeStampFormatter.format(date);//new Date());

		}else if(interfaceName.equalsIgnoreCase("CRM_001") || interfaceName.equalsIgnoreCase("CRM_002")){

			dateTimeFormat = "";
			Date date = null;
			SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd");
			try {
				date = sdf.parse(paramValue);
				String a=date.toString();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			SimpleDateFormat timeStampFormatter = new SimpleDateFormat(dateFormat);
		    dateTimeFormat = timeStampFormatter.format(date);
		}

	    return dateTimeFormat;
	}

	/**
	 *
	 * @param throwable
	 * @return
	 */
	public static String getStackTrace(Throwable throwable) {
		Writer errMessage = new StringWriter();
		PrintWriter errWriter = new PrintWriter(errMessage);
		throwable.printStackTrace(errWriter);
		return errMessage.toString();
	}

	/**
	 *
	 * @param xmlString
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document convertStringToXmlDocument(String xmlString) throws ParserConfigurationException, SAXException, IOException {
		Document xmlDocument = null;
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(false);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		xmlDocument = documentBuilder.parse(new ByteArrayInputStream(xmlString.getBytes()));
		return xmlDocument;
	}

	/**
	 *
	 * @param document
	 * @return
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
	public String convertDocumentToString(Document document) throws TransformerConfigurationException, TransformerException {
		String message = "";
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(document);
		StreamResult output = new StreamResult(new StringWriter());
		transformer.transform(source, output);
		message = output.getWriter().toString();
		return message;
	}


	public Document createDOMDocument(String inputXML) throws ParserConfigurationException, SAXException, IOException {

		InputSource in = new InputSource();
		StringReader strReader = new StringReader(inputXML);
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(false);
		DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
		in.setCharacterStream(strReader);
		return domBuilder.parse(in);
		}



	/**
	 *
	 * @param document
	 * @param expression
	 * @param returnType
	 * @return
	 * @throws XPathExpressionException
	 */
	public Object getXpathValue(Document document, String expression, QName returnType) throws XPathExpressionException {
		taLogger.log(logSrcTransId, logTransId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getXpathValue]Xpath " + expression, loggerCategory);
		//XPathExpression expr = xPath.compile(expression);
		XPath xPath = XPathFactory.newInstance().newXPath();
		Object commandParameterValue = xPath.evaluate(expression, document, XPathConstants.STRING);
		taLogger.log(logSrcTransId, logTransId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getXpathValue]XpathValue " + commandParameterValue, loggerCategory);
		return commandParameterValue;
	}

	/**
	 *
	 * @return
	 * @throws ParserConfigurationException
	 */
	public Document getDocument() throws ParserConfigurationException {
		taLogger.log("[SPLCommonComponent][getDocument]", ApplicationConstants.LOG_INFO, "getDocument ", loggerCategory);
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document document = docBuilder.newDocument();
		document.setXmlStandalone(true);
		return document;
	}

	/**
	 *
	 * @param filePath
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document getDocument(String filePath) throws ParserConfigurationException, SAXException, IOException {
		taLogger.log(logSrcTransId, logTransId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getDocument] filePath " + filePath, loggerCategory);
		String message = getMessage(filePath);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(false);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(new ByteArrayInputStream(message.getBytes()));
		document.setXmlStandalone(true);
		taLogger.log(logSrcTransId, logTransId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getDocument] [End] " + filePath, loggerCategory);
		return document;
	}

	/**
	 *
	 * @param document
	 * @param rootElement
	 * @param paramName
	 * @param paramValue
	 * @return
	 */
	public static Element addElement(Document document, Element rootElement, String paramName, String paramValue) {
		Element element = document.createElement(paramName);
		element.appendChild(document.createTextNode(paramValue));
		rootElement.appendChild(element);
		return element;
	}

	/**
	 *
	 * @param document
	 * @param element
	 * @param attributeName
	 * @param attributeValue
	 */
	public static void addAttribute(Document document, Element element, String attributeName, String attributeValue) {
		Attr attr = document.createAttribute(attributeName);
		attr.setValue(attributeValue);
		element.setAttributeNode(attr);
	}

	/**
	 *
	 * @param document
	 * @param operationName
	 * @param cmdRowId
	 * @param transId
	 * @param messageType
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
	public void saveMessage(Document document, String operationName, String cmdRowId, String transId, String appendDotXml) throws TransformerConfigurationException, TransformerException {
		taLogger.log(logSrcTransId, logTransId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][saveMessage]saveMessage " + operationName, loggerCategory);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(document);
		StreamResult output = new StreamResult(new File(ApplicationConstants.FOLDER_ADAPTORS + ApplicationConstants.FOLDER_STUBS + operationName + "_" + cmdRowId + "_" + transId + appendDotXml));
		transformer.transform(source, output);
	}
	/**
	 * 
	 * @param MessageString
	 * @param interfaceName
	 * @param cmdRowId
	 * @param transId
	 * @param appendDotXml
	 * @throws IOException
	 */
	public void saveMessage(String MessageString,String interfaceName,String cmdRowId,String transId,String appendDotXml) throws IOException{
	FileWriter fileWriter = new FileWriter(ApplicationConstants.FOLDER_ADAPTORS + ApplicationConstants.FOLDER_STUBS + interfaceName + "_" + cmdRowId + "_" + transId + appendDotXml);
	BufferedWriter out = new BufferedWriter(fileWriter);
	out.write(MessageString);
	out.close();
	}
	/**
	 *
	 * @param filePath
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public String getMessage(String filePath) throws FileNotFoundException, IOException {
		taLogger.log(logSrcTransId, logTransId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getMessage]filePath " + filePath, loggerCategory);
		String message = "";
		DataInputStream inputStream = new DataInputStream(new FileInputStream(filePath));
		byte[] datainBytes = new byte[inputStream.available()];
		inputStream.readFully(datainBytes);
		inputStream.close();
		//content = new String(datainBytes, 0, datainBytes.length);
		message = new String(datainBytes);
		return message;
	}

	/** Method getFilePath
	 *
	 * @param systemName
	 * @param interfaceName
	 * @param reqOrResFolder
	 * @param appendDotXml
	 * @return
	 */
	public String getFilePath(String systemName, String interfaceName, String reqOrResFolder, String appendDotXml) {
		// conf/spl/adaptors/ + System Name + request + interfacename.xml
		taLogger.log(logSrcTransId, logTransId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getFilePath]filePath for " + interfaceName, loggerCategory);
		String filePath = ApplicationConstants.FOLDER_ADAPTORS + systemName + reqOrResFolder + interfaceName + appendDotXml;
		taLogger.log(logSrcTransId, logTransId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getFilePath]filePath " + filePath, loggerCategory);
		return filePath;
	}

	public String createTomResponse(String responseCode,String responseDescription,String corrId,String transId,String opgRefId,String funcRefId,String cmdRefId) throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
		String loggerCategory="osmlogging";
		taLogger.log("[SPLCommonComponent][createTomResponse]", ApplicationConstants.LOG_INFO, "transId= "+transId, loggerCategory);
		Document tomDocument = getDocument();
		Element soapEnvelope = null;
		soapEnvelope = tomDocument.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soapenv:Envelope");
		tomDocument.appendChild(soapEnvelope);
		Element soapHeader = SPLCommonComponent.addElement(tomDocument, soapEnvelope, "soapenv:Header", "");
		Element soapBody = SPLCommonComponent.addElement(tomDocument, soapEnvelope, "soapenv:Body", "");

		Element rootElement = tomDocument.createElementNS(ApplicationConstants.TOM_RES_NS, ApplicationConstants.TOM_RES_PROVRESPONSE);
		soapBody.appendChild(rootElement);

		if(ApplicationConstants.TOM_RESPONSE_SUCCESS.equals(responseCode)) {
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_STATUSCODE, responseCode);
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORCODE, "");
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORMESSAGE, "");
		}else {
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_STATUSCODE, ApplicationConstants.TOM_RESPONSE_ERROR);
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORMESSAGE, responseDescription);
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORCODE, responseCode);
		}

		SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_CORRELATIONID, corrId);
		SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_TXNSTARTTIME, "");
		Element listOfRefElement = SPLCommonComponent.addElement(tomDocument, rootElement, ApplicationConstants.TOM_RES_LISTOFREF, "");
		SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_TOMORDERID, transId);
		SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_OPGREFID, opgRefId);
		SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_FUNCREFID, funcRefId);
		SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_CMDREFID, cmdRefId);
		SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_TASKRESPONSE, "-");
		Element functionParamInfoElement = SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_FUNCPARAMINFO, "");
		SPLCommonComponent.addElement(tomDocument, functionParamInfoElement, ApplicationConstants.TOM_RES_PARAMINFO, "");

	    String tomResponse = convertDocumentToString(tomDocument);

	    taLogger.log("[SPLCommonComponent][createTomResponse]", ApplicationConstants.LOG_INFO, "tomResponse= "+tomResponse, loggerCategory);

	    return tomResponse;
	}

	//public String createTomResponse(OrderTransactonDetail orderTransactonDetail) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, SQLException {
	public String createTomResponse(String cmdTransId) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, SQLException {
		String loggerCategory="osmlogging";
		DatabaseDAO databaseDAO = (DatabaseDAO) BeanFactory.getBean(ApplicationConstants.BEAN_DATABASEDAO);
		OrderTransactonDetail orderTransactonDetail = databaseDAO.getOrderTransDetails(cmdTransId);

		String funcRefId = orderTransactonDetail.getFuncRefId();
		String opgRefId = orderTransactonDetail.getOpgRefId();
		String corrId = orderTransactonDetail.getCorrId();
		String responseCode = orderTransactonDetail.getReturnCode();
		String responseDescription = orderTransactonDetail.getReturnMsg();
		String transId = orderTransactonDetail.getTransId();
		String cmdRefId = orderTransactonDetail.getCmdRefId();
		String transStatus = orderTransactonDetail.getStatus();

		taLogger.log("[SPLCommonComponent][createTomResponse]", ApplicationConstants.LOG_INFO, "transId= "+transId, loggerCategory);

		Document tomDocument = getDocument();
		Element soapEnvelope = null;
		soapEnvelope = tomDocument.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soapenv:Envelope");
		tomDocument.appendChild(soapEnvelope);
		Element soapHeader = SPLCommonComponent.addElement(tomDocument, soapEnvelope, "soapenv:Header", "");
		Element soapBody = SPLCommonComponent.addElement(tomDocument, soapEnvelope, "soapenv:Body", "");

		Element rootElement = tomDocument.createElementNS(ApplicationConstants.TOM_RES_NS, ApplicationConstants.TOM_RES_PROVRESPONSE);
		soapBody.appendChild(rootElement);

		if(ApplicationConstants.STATUS_COMPLETED.equals(transStatus)) {
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_STATUSCODE, responseCode);
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORCODE, "");
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORMESSAGE, "");
		}else {
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_STATUSCODE, ApplicationConstants.TOM_RESPONSE_ERROR);
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORMESSAGE, responseDescription);
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORCODE, responseCode);
		}

		SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_CORRELATIONID, corrId);
		SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_TXNSTARTTIME, "");
		Element listOfRefElement = SPLCommonComponent.addElement(tomDocument, rootElement, ApplicationConstants.TOM_RES_LISTOFREF, "");
		SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_TOMORDERID, transId);
		SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_OPGREFID, opgRefId);
		SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_FUNCREFID, funcRefId);
		SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_CMDREFID, cmdRefId);
		SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_TASKRESPONSE, "-");
		Element functionParamInfoElement = SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_FUNCPARAMINFO, "");
		SPLCommonComponent.addElement(tomDocument, functionParamInfoElement, ApplicationConstants.TOM_RES_PARAMINFO, "");

	    String tomResponse = convertDocumentToString(tomDocument);

	    taLogger.log("[SPLCommonComponent][createTomResponse]", ApplicationConstants.LOG_INFO, "tomResponse= "+tomResponse, loggerCategory);

	    return tomResponse;
	}
	
	//public String createTomResponse(OrderTransactonDetail orderTransactonDetail) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, SQLException {
		public String createBillTomResponse(String cmdTransId) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, SQLException {
			String loggerCategory="osmlogging";
			DatabaseDAO databaseDAO = (DatabaseDAO) BeanFactory.getBean(ApplicationConstants.BEAN_DATABASEDAO);
			taLogger.log("[SPLCommonComponent][createBillTomResponse]", ApplicationConstants.LOG_INFO, "cmdTransId= "+cmdTransId, loggerCategory);
			OrderTransactonDetailBill orderTransactonDetail = databaseDAO.getOrderTransDetailsBill(cmdTransId);

			String funcRefId = orderTransactonDetail.getFuncRefId();
			String opgRefId = orderTransactonDetail.getOpgRefId();
			String corrId = orderTransactonDetail.getCorrId();
			String responseCode = orderTransactonDetail.getReturnCode();
			String responseDescription = orderTransactonDetail.getReturnMsg();
			String transId = orderTransactonDetail.getTransId();
			String cmdRefId = orderTransactonDetail.getCmdRefId();
			String transStatus = orderTransactonDetail.getStatus();

			taLogger.log("[SPLCommonComponent][createBillTomResponse]", ApplicationConstants.LOG_INFO, "transId= "+transId, loggerCategory);

			Document tomDocument = getDocument();
			Element soapEnvelope = null;
			soapEnvelope = tomDocument.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soapenv:Envelope");
			tomDocument.appendChild(soapEnvelope);
			Element soapHeader = SPLCommonComponent.addElement(tomDocument, soapEnvelope, "soapenv:Header", "");
			Element soapBody = SPLCommonComponent.addElement(tomDocument, soapEnvelope, "soapenv:Body", "");

			Element rootElement = tomDocument.createElementNS(ApplicationConstants.TOM_RES_NS, ApplicationConstants.TOM_RES_PROVRESPONSE);
			soapBody.appendChild(rootElement);

			if(ApplicationConstants.STATUS_COMPLETED.equals(transStatus)) {
				SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_STATUSCODE, responseCode);
				SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORCODE, "");
				SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORMESSAGE, "");
			}else {
				SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_STATUSCODE, ApplicationConstants.TOM_RESPONSE_ERROR);
				SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORMESSAGE, responseDescription);
				SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORCODE, responseCode);
			}

			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_CORRELATIONID, corrId);
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_TXNSTARTTIME, "");
			Element listOfRefElement = SPLCommonComponent.addElement(tomDocument, rootElement, ApplicationConstants.TOM_RES_LISTOFREF, "");
			SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_TOMORDERID, transId);
			SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_OPGREFID, opgRefId);
			SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_FUNCREFID, funcRefId);
			SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_CMDREFID, cmdRefId);
			SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_TASKRESPONSE, "-");
			Element functionParamInfoElement = SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_FUNCPARAMINFO, "");
			SPLCommonComponent.addElement(tomDocument, functionParamInfoElement, ApplicationConstants.TOM_RES_PARAMINFO, "");

		    String tomResponse = convertDocumentToString(tomDocument);

		    taLogger.log("[SPLCommonComponent][createBillTomResponse]", ApplicationConstants.LOG_INFO, "tomResponse= "+tomResponse, loggerCategory);
		    //System.out.println( "[SPLCommonComponent][createBillTomResponse]TomResponse= "+tomResponse);
		    return tomResponse;
		}
		
	//Sudharsan - Create ack tom response for c1 adaptor -Start
		public String createAckTomResponseBill(String cmdTransId,String funcRefId) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, SQLException {
			String loggerCategory="osmlogging";
			DatabaseDAO databaseDAO = (DatabaseDAO) BeanFactory.getBean(ApplicationConstants.BEAN_DATABASEDAO);
			taLogger.log("[SPLCommonComponent][createAckTomResponseBill]", ApplicationConstants.LOG_INFO, "cmdTransId= "+cmdTransId, loggerCategory);
			/*OrderTransactonDetailBill orderTransactonDetail = databaseDAO.getOrderTransDetailsBill(cmdTransId);

			String funcRefId = orderTransactonDetail.getFuncRefId();
			String opgRefId = orderTransactonDetail.getOpgRefId();
			String corrId = orderTransactonDetail.getCorrId();
			String responseCode = orderTransactonDetail.getReturnCode();
			String responseDescription = orderTransactonDetail.getReturnMsg();
			String transId = orderTransactonDetail.getTransId();
			String cmdRefId = orderTransactonDetail.getCmdRefId();
			String transStatus = orderTransactonDetail.getStatus();*/
			
			//taLogger.log("[SPLCommonComponent][createBillTomResponse]", ApplicationConstants.LOG_INFO, "funcRefId= "+funcRefId+" opgRefId="+opgRefId+" corrId="+corrId+" responseCode="+responseCode +" responseDescription="+responseDescription+" transId="+transId+" cmdRefId="+cmdRefId+" transStatus ="+transStatus , loggerCategory);
			taLogger.log("[SPLCommonComponent][createAckTomResponseBill]", ApplicationConstants.LOG_INFO, "transId= "+cmdTransId, loggerCategory);

			Document tomDocument = getDocument();
			Element soapEnvelope = null;
			soapEnvelope = tomDocument.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soapenv:Envelope");
			tomDocument.appendChild(soapEnvelope);
			Element soapHeader = SPLCommonComponent.addElement(tomDocument, soapEnvelope, "soapenv:Header", "");
			Element soapBody = SPLCommonComponent.addElement(tomDocument, soapEnvelope, "soapenv:Body", "");

			Element rootElement = tomDocument.createElementNS(ApplicationConstants.TOM_RES_NS, ApplicationConstants.TOM_RES_PROVRESPONSE);
			soapBody.appendChild(rootElement);

			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_STATUSCODE, "0");
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORCODE, "0");
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_ERRORMESSAGE, "");
			SPLCommonComponent.addAttribute(tomDocument, rootElement, ApplicationConstants.TOM_RES_TXNSTARTTIME, "");

			Element listOfRefElement = SPLCommonComponent.addElement(tomDocument, rootElement, ApplicationConstants.TOM_RES_LISTOFREF, "");
			SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_TOMORDERID, cmdTransId);
			SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_FUNCREFID, funcRefId);
			SPLCommonComponent.addElement(tomDocument, listOfRefElement, ApplicationConstants.TOM_RES_TASKRESPONSE, "success");
			
		    String tomResponse = convertDocumentToString(tomDocument);

		    taLogger.log("[SPLCommonComponent][createAckTomResponseBill]", ApplicationConstants.LOG_INFO, "TomResponse = "+tomResponse, loggerCategory);
		    //System.out.println("[SPLCommonComponent][createAckTomResponseBill]TomResponse = "+tomResponse);
		    return tomResponse;
		}	
	//Sudharsan - Create ack tom response for c1 adaptor -End	
	/**
	 * 
	 * @param responseDocument
	 * @param cmdTransId
	 * @param interfaceName
	 * @param responseCode
	 * @return
	 * @throws SQLException
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 * @throws XPathExpressionException
	 * @throws IOException
	 */
	//Sudharsan: 20130423: Bug#1981: Added 'responseCode' parameter 
	public String updateCommandTransDtls(Document responseDocument, String cmdTransId, String interfaceName,String responseCode) throws SQLException, TransformerConfigurationException, TransformerException, XPathExpressionException, IOException {

		String tomResponse="";
		String transId="";
		String srcTransId="";
		String rowId="";
		String sqlQuery;
		String loggerCategory="osmlogging";
		try{
		JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean(ApplicationConstants.BEAN_JDBCDATABASEDAO);
		DatabaseDAO databaseDAO = (DatabaseDAO) BeanFactory.getBean(ApplicationConstants.BEAN_DATABASEDAO);
		OrderTransactonDetail orderTransactonDetail = databaseDAO.getOrderTransDetails(cmdTransId);


		rowId = orderTransactonDetail.getRowId();
		srcTransId = orderTransactonDetail.getSrcTransId();
		transId = orderTransactonDetail.getTransId();


		if(interfaceName.equalsIgnoreCase(ApplicationConstants.CRM_TASK)){

			/*String taskResponse = (String) getXpathValue(responseDocument, "Envelope/Body/ProvResponse/ListOfRef/TaskResponse", XPathConstants.STRING);

			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "taskResponse is: " + taskResponse, loggerCategory);
			sqlQuery = "UPDATE T_OM_CMD_TRANS SET TASK_RESPONSE=? WHERE upper(ROW_ID)= ?";
			JdbcTemplate jdbcTemplate = databaseDAO.getJdbcTemplate();
			jdbcTemplate.update(sqlQuery, new Object[] { taskResponse, cmdTransId });*/

			//List<CommandTransDetails> cmdTransDtlsList = getCommandTransDetails(responseDocument, transId);
			List<CommandTransDetails> cmdTransDtlsList = getCommandTransDtls(responseDocument, transId);
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "List Size in updateCommandTransDtls " + cmdTransDtlsList.size(), loggerCategory);
			databaseDAO.updateTaskResponseDetails(cmdTransDtlsList, transId, rowId, srcTransId);

		}

		if(interfaceName.equalsIgnoreCase(ApplicationConstants.CRM_OMDNETMODULE)){
			//Sudharsan: 20130423: Bug#1981:Verifying response code- Start
			if(responseCode.equalsIgnoreCase(ApplicationConstants.TOM_RESPONSE_SUCCESS))
			//Sudharsan: 20130423: Bug#1981:Verifying response code- End	
			{
				//List<CommandTransDetails> cmdTransDtlsList = getCommandTransDetailsforOMDNETMODULE(responseDocument, transId);
				List<CommandTransDetails> cmdTransDtlsList = getCommandTransDetailsforCrmModule(responseDocument, transId);
				taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "List Size in updateCommandTransDtls " + cmdTransDtlsList.size(), loggerCategory);
				databaseDAO.updateTaskResponseDetails(cmdTransDtlsList, transId, rowId, srcTransId);
			}
		}


		tomResponse = createGenericTomResponse(cmdTransId);
		taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "tomResponse: " + tomResponse, loggerCategory);

		}catch(Exception e){
			// TODO Auto-generated catch block
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_ERROR, "error in updateMethod is:" + e.getMessage(), loggerCategory, e);
		}

		return tomResponse;
	}



	public String createGenericTomResponse(String parRowId){

		String tomResponse="";
		String loggerCategory="osmlogging";
		String srcTransId = "";
		String transId = "";
		String rowId = "";

		try{

			JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean(ApplicationConstants.BEAN_JDBCDATABASEDAO);
			DatabaseDAO databaseDAO = (DatabaseDAO) BeanFactory.getBean(ApplicationConstants.BEAN_DATABASEDAO);
			OrderTransactonDetail orderTransactonDetail = null;
		
			orderTransactonDetail = databaseDAO.getOrderTransDetails(parRowId);
			

			String trxnTime="";
			String responseCode = orderTransactonDetail.getReturnCode();
			String responseDescription = orderTransactonDetail.getReturnMsg();
			rowId = orderTransactonDetail.getRowId();
			srcTransId = orderTransactonDetail.getSrcTransId();
			String funcRefId = orderTransactonDetail.getFuncRefId();
			String opgRefId = orderTransactonDetail.getOpgRefId();
			String corrId = orderTransactonDetail.getCorrId();
			String cmdRefId = orderTransactonDetail.getCmdRefId();
			transId = orderTransactonDetail.getTransId();
			String transStatus = orderTransactonDetail.getStatus();


			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "funcRefId in TOM Response is:" + funcRefId, loggerCategory);
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "opgRefId in TOM Response is:" + opgRefId, loggerCategory);
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "corrId in TOM Response is:" + corrId, loggerCategory);
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "cmdRefId in TOM Response is:" + cmdRefId, loggerCategory);
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "errCode in TOM Response is:" + responseCode, loggerCategory);
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "errMsg in TOM Response is:" + responseDescription, loggerCategory);



			List<Map<String, Object>> xmlElementValues = jdbcDatabaseDAO.getXmlEleValues(parRowId);
			//String taskResponse = jdbcDatabaseDAO.getTaskResponse(parRowId);
			String taskResponse = "-";
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "size of xmlElementValues list:" + xmlElementValues.size(), loggerCategory);

			/*if(taskResponse==null){
				taskResponse="-";
			}*/

			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "taskResponse is:" + taskResponse, loggerCategory);

		Document doc = getDocument();

		Element soapEnvelope = null;
		soapEnvelope = doc.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soapenv:Envelope");
		doc.appendChild(soapEnvelope);

		Element Header = doc.createElement("soapenv:Header");
		soapEnvelope.appendChild(Header);

		Element Body = doc.createElement("soapenv:Body");
		soapEnvelope.appendChild(Body);

		Element ProvResponse = doc.createElement("ProvResponse");

		SPLCommonComponent.addAttribute(doc, ProvResponse, ApplicationConstants.TOM_RES_CORRELATIONID, corrId);
		if(ApplicationConstants.STATUS_COMPLETED.equals(transStatus)) {
			SPLCommonComponent.addAttribute(doc, ProvResponse, ApplicationConstants.TOM_RES_STATUSCODE, responseCode);
			SPLCommonComponent.addAttribute(doc, ProvResponse, ApplicationConstants.TOM_RES_ERRORCODE, "");
			SPLCommonComponent.addAttribute(doc, ProvResponse, ApplicationConstants.TOM_RES_ERRORMESSAGE, "");
		}else {
			SPLCommonComponent.addAttribute(doc, ProvResponse, ApplicationConstants.TOM_RES_STATUSCODE, ApplicationConstants.TOM_RESPONSE_ERROR);
			SPLCommonComponent.addAttribute(doc, ProvResponse, ApplicationConstants.TOM_RES_ERRORMESSAGE, responseDescription);
			SPLCommonComponent.addAttribute(doc, ProvResponse, ApplicationConstants.TOM_RES_ERRORCODE, responseCode);
		}
		SPLCommonComponent.addAttribute(doc, ProvResponse, ApplicationConstants.TOM_RES_TXNSTARTTIME, trxnTime);

		Body.appendChild(ProvResponse);


		Element ListOfRef = doc.createElement("ListOfRef");
		ProvResponse.appendChild(ListOfRef);

		 SPLCommonComponent.addElement(doc, ListOfRef, "TomOrderID", transId);
		 SPLCommonComponent.addElement(doc, ListOfRef, "OPGRefID", opgRefId);
		 SPLCommonComponent.addElement(doc, ListOfRef, "FuncRefID", funcRefId);
		 SPLCommonComponent.addElement(doc, ListOfRef, "CmdRefID", cmdRefId);
		 SPLCommonComponent.addElement(doc, ListOfRef, "TaskResponse", taskResponse);

		Element FuncParamInfo = doc.createElement("FuncParamInfo");
		ListOfRef.appendChild(FuncParamInfo);


		for(Object list:xmlElementValues){

			Element ParamInfo = doc.createElement("ParamInfo");
			FuncParamInfo.appendChild(ParamInfo);
			Map map = (Map) list;

			String xmlEleType = (String) map.get("XML_ELE_TYPE");
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "xmlEleType:" + xmlEleType, loggerCategory);
			String xmlEleId = (String) map.get("XML_ELE_ID");
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "xmlEleId:" + xmlEleId, loggerCategory);
			String xmlEleName = (String) map.get("XML_ELE_NAME");
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "xmlEleName:" + xmlEleName, loggerCategory);
			String xmlEleCurrValue = (String) map.get("XML_ELE_CURR_VALUE");
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "xmlEleCurrValue:" + xmlEleCurrValue, loggerCategory);
			String xmlElePrevValue = (String) map.get("XML_ELE_PREV_VALUE");
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "xmlElePrevValue:" + xmlElePrevValue, loggerCategory);

			if(xmlElePrevValue==null){
				xmlElePrevValue = "-";
			}

		 SPLCommonComponent.addElement(doc, ParamInfo, "ParamType", xmlEleType);
		 SPLCommonComponent.addElement(doc, ParamInfo, "ParamID", xmlEleId);
		 SPLCommonComponent.addElement(doc, ParamInfo, "ParamName", xmlEleName);
		 SPLCommonComponent.addElement(doc, ParamInfo, "CurrentValue", xmlEleCurrValue);
		 SPLCommonComponent.addElement(doc, ParamInfo, "PreviousValue", xmlElePrevValue);

		}


		tomResponse = convertDocumentToString(doc);

		}catch(Exception e){
			taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_ERROR, "Inside Catch createGenericTomResponse", loggerCategory, e);
		}

		return tomResponse;
	}

	public Element createElement(Document document, Node listOfRefNode, String elementName, String elementValue) {
		Element element = document.createElement(elementName);
		element.setTextContent(elementValue);
		listOfRefNode.appendChild(element);
		return element;
	}

	private List<CommandTransDetails> getCommandTransDetails(Document taskResponse, String transId) throws XPathExpressionException, IOException {
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]Inside Method", loggerCategory);
		Properties properties = loadProperty(ApplicationConstants.OSMTRANSLATION_FILENAME);
		XPath xPath = XPathFactory.newInstance().newXPath();
		String parRowId = (String) getXpathValue(taskResponse, "Envelope/Body/ProvResponse/@CorrelationId", XPathConstants.STRING);
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails] parRowId " + parRowId, loggerCategory);
		NodeList paramInfoList = (NodeList) xPath.evaluate("Envelope/Body/ProvResponse/ListOfRef/FuncParamInfo/ParamInfo", taskResponse, XPathConstants.NODESET);
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]Size of paramInfo List " + paramInfoList.getLength(), loggerCategory);
		CommandTransDetails commandTransDetails = null;
		List<CommandTransDetails> list = new ArrayList<CommandTransDetails>();
		for(int paramInfoIndex=0;paramInfoIndex<paramInfoList.getLength();paramInfoIndex++) {
			Node paramInfoNode = paramInfoList.item(paramInfoIndex);
			taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]paramInfoNode " + paramInfoNode, loggerCategory);
			if(paramInfoNode!=null && paramInfoNode.getNodeType()==Node.ELEMENT_NODE) {
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]paramInfoNode Name " + paramInfoNode.getNodeName(), loggerCategory);
				NodeList paramInfoChilds = paramInfoNode.getChildNodes();
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]paramInfoChilds Size " + paramInfoChilds.getLength(), loggerCategory);
				commandTransDetails = new CommandTransDetails();
				commandTransDetails.setParRowId(parRowId);
				for(int paramInfoChildIndex=0;paramInfoChildIndex<paramInfoChilds.getLength();paramInfoChildIndex++) {
					Node childNode = paramInfoChilds.item(paramInfoChildIndex);
					taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]childNode " + childNode, loggerCategory);
					if(childNode!=null && !childNode.getTextContent().equals("") && childNode.getNodeType()==Node.ELEMENT_NODE) {
						String nodeName = childNode.getNodeName();
						taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]childNode Name " + nodeName, loggerCategory);
						String nodeValue = childNode.getTextContent().trim();
						taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]childNode Value " + nodeValue, loggerCategory);
						if(nodeName.equalsIgnoreCase("ParamType"))
							commandTransDetails.setXmlEleType(nodeValue);
						if(nodeName.equalsIgnoreCase("ParamID"))
							commandTransDetails.setXmlEleId(nodeValue);
						if(nodeName.equalsIgnoreCase("ParamName")) {
							if(properties.containsKey(nodeValue)) {
								nodeValue = properties.getProperty(nodeValue);
							}
							childNode.setTextContent(nodeValue);
							commandTransDetails.setXmlEleName(nodeValue);
						}
						if(nodeName.equalsIgnoreCase("CurrentValue"))
							commandTransDetails.setXmlEleCurrValue(nodeValue);
					}
				}
				list.add(commandTransDetails);
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]List Size Inside For Loop " + list.size(), loggerCategory);
			}
		}
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]List Size " + list.size(), loggerCategory);
		try {
			String tomResponse = convertDocumentToString(taskResponse);
			taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]tomResponse " + tomResponse, loggerCategory);
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}


	private List<CommandTransDetails> getCommandTransDetailsforOMDNETMODULE(Document taskResponse, String transId) throws XPathExpressionException, IOException {

		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetails]Inside Method", loggerCategory);
		Properties properties = loadProperty(ApplicationConstants.OSMTRANSLATION_FILENAME);
		XPath xPath = XPathFactory.newInstance().newXPath();
		String parRowId = (String) getXpathValue(taskResponse, "InvokeOMDnetModuleResponse/InvokeOMDnetModuleResult/msgHeaderField/correlationIDField", XPathConstants.STRING);
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE] parRowId " + parRowId, loggerCategory);
		NodeList paramInfoList = (NodeList) xPath.evaluate("InvokeOMDnetModuleResponse/InvokeOMDnetModuleResult/msgBodyField/arrayOfFuncParamsSetField/ParamInfoType", taskResponse, XPathConstants.NODESET);
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE]Size of paramInfo List " + paramInfoList.getLength(), loggerCategory);
		CommandTransDetails commandTransDetails = null;
		List<CommandTransDetails> list = new ArrayList<CommandTransDetails>();
		for(int paramInfoIndex=0;paramInfoIndex<paramInfoList.getLength();paramInfoIndex++) {
			Node paramInfoNode = paramInfoList.item(paramInfoIndex);
			taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE]paramInfoNode " + paramInfoNode, loggerCategory);
			if(paramInfoNode!=null && paramInfoNode.getNodeType()==Node.ELEMENT_NODE) {
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE]paramInfoNode Name " + paramInfoNode.getNodeName(), loggerCategory);
				NodeList paramInfoChilds = paramInfoNode.getChildNodes();
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE]paramInfoChilds Size " + paramInfoChilds.getLength(), loggerCategory);
				commandTransDetails = new CommandTransDetails();
				commandTransDetails.setParRowId(parRowId);
				for(int paramInfoChildIndex=0;paramInfoChildIndex<paramInfoChilds.getLength();paramInfoChildIndex++) {
					Node childNode = paramInfoChilds.item(paramInfoChildIndex);
					taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE]childNode " + childNode, loggerCategory);
					if(childNode!=null && !childNode.getTextContent().equals("") && childNode.getNodeType()==Node.ELEMENT_NODE) {
						String nodeName = childNode.getNodeName();
						taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE]childNode Name " + nodeName, loggerCategory);
						String nodeValue = childNode.getTextContent().trim();
						taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE]childNode Value " + nodeValue, loggerCategory);
						if(nodeName.equalsIgnoreCase("a:paramTypeField"))
							commandTransDetails.setXmlEleType(nodeValue);
						if(nodeName.equalsIgnoreCase("a:paramIDField"))
							commandTransDetails.setXmlEleId(nodeValue);
						if(nodeName.equalsIgnoreCase("a:paramNameField")) {
							if(properties.containsKey(nodeValue)) {
								nodeValue = properties.getProperty(nodeValue);
							}
							childNode.setTextContent(nodeValue);
							commandTransDetails.setXmlEleName(nodeValue);
						}
						if(nodeName.equalsIgnoreCase("a:currValueField"))
							commandTransDetails.setXmlEleCurrValue(nodeValue);
					}
				}
				list.add(commandTransDetails);
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE]List Size Inside For Loop " + list.size(), loggerCategory);
			}
		}
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE]List Size " + list.size(), loggerCategory);
		try {
			String tomResponse = convertDocumentToString(taskResponse);
			taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforOMDNETMODULE]tomResponse " + tomResponse, loggerCategory);
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	private void updateDocument(Document taskResponse, String transId) throws IOException {
		Properties properties = loadProperty(ApplicationConstants.OSMTRANSLATION_FILENAME);
		NodeList paramInfoList = taskResponse.getElementsByTagName("ParamInfo");
		for(int paramInfoIndex=0;paramInfoIndex<paramInfoList.getLength();paramInfoIndex++) {
			Node paramInfoNode = paramInfoList.item(paramInfoIndex);
			taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "paramInfoNode " + paramInfoNode, loggerCategory);
			if(paramInfoNode!=null && paramInfoNode.getNodeType()==Node.ELEMENT_NODE) {
				taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "paramInfoNode Name " + paramInfoNode.getNodeName(), loggerCategory);
				NodeList paramInfoChilds = paramInfoNode.getChildNodes();
				taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "paramInfoChilds Size " + paramInfoChilds.getLength(), loggerCategory);
				for(int paramInfoChildIndex=0;paramInfoChildIndex<paramInfoChilds.getLength();paramInfoChildIndex++) {
					Node childNode = paramInfoChilds.item(paramInfoChildIndex);
					taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "childNode " + childNode, loggerCategory);
					if(childNode!=null && !childNode.getTextContent().equals("") && childNode.getNodeType()==Node.ELEMENT_NODE) {
						String nodeName = childNode.getNodeName();
						taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "childNode Name " + nodeName, loggerCategory);
						String nodeValue = childNode.getTextContent().trim();
						taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "childNode Value " + nodeValue, loggerCategory);
						if(nodeName.equalsIgnoreCase("ParamName")) {
							if(properties.containsKey(nodeValue)) {
								nodeValue = properties.getProperty(nodeValue);
							}
							childNode.setTextContent(nodeValue);
						}
					}
				}
			}
		}
		try {
			String tomResponse = convertDocumentToString(taskResponse);
			taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "tomResponse List Size " + tomResponse, loggerCategory);
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String getProprtyTranslation(String xmlEleName) {
		// Load the properties from the spring bean - configured in applicationcontext-datasource.xml
		Properties properties = (Properties) BeanFactory.getBean("properties");
		//properties.values();
		Map<Object, Object> propMap = new HashMap<Object, Object>();
		properties.putAll(propMap);
		Set<Map.Entry<Object, Object>> setMap = properties.entrySet();
		Iterator<Map.Entry<Object, Object>> mapIterator = setMap.iterator();
		while (mapIterator.hasNext()) {
			Map.Entry<Object, Object> mapProperty = mapIterator.next();
			String value = (String) mapProperty.getValue();
			if(value!=null) {
				xmlEleName = (String)mapProperty.getKey();
			}
		}
		return xmlEleName;
	}

/*	private CommandTransDetails getCommandTransDetails(NodeList rootNode, String transId) {
		taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "Inside getCommandTransDetails", loggerCategory);
		CommandTransDetails commandTransDetails = new CommandTransDetails();
		for(int index = 0; index < rootNode.getLength(); index ++){
			Node aNode = rootNode.item(index);
			taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "Node Name " + aNode.getNodeName(), loggerCategory);
			if (aNode.getNodeType() == Node.ELEMENT_NODE){
				NodeList childNodes = aNode.getChildNodes();
				System.out.println("Node Length " + childNodes.getLength());
				if (childNodes.getLength() > 0){
					if(aNode!=null && !aNode.getNodeName().equals("ParamInfo") && aNode.getNodeType()==Node.ELEMENT_NODE) {
						String nodeName = aNode.getNodeName();
						taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "childNode Name " + nodeName, loggerCategory);
						String nodeValue = aNode.getTextContent().trim();
						taLogger.log("", transId, "", ApplicationConstants.LOG_INFO, "childNode Value " + nodeValue, loggerCategory);
						if(nodeName.equalsIgnoreCase("ParamType"))
							commandTransDetails.setXmlEleType(nodeValue);
						if(nodeName.equalsIgnoreCase("ParamID"))
							commandTransDetails.setXmlEleType(nodeValue);
						if(nodeName.equalsIgnoreCase("ParamName"))
							commandTransDetails.setXmlEleType(nodeValue);
						if(nodeName.equalsIgnoreCase("CurrentValue"))
							commandTransDetails.setXmlEleType(nodeValue);
					}
				}
				getCommandTransDetails(childNodes, transId);
			}
		}
		return commandTransDetails;
	}
*/
	/**
	 * Method to convert Node to String
	 * @param nodeName
	 * @return
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */

	public String nodeToString(Document document, String nodeName) throws TransformerFactoryConfigurationError, TransformerException {
		NodeList nodeList = document.getElementsByTagName(nodeName);
		String inputXml = "";
		if(nodeName!=null && nodeList.getLength()>0) {
		Node node = nodeList.item(0);
		if(node!=null) {
		StringWriter stringWriter = new StringWriter();
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
		inputXml = stringWriter.toString();
		inputXml = inputXml.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\r", "");
		// inputXml = StringEscapeUtils.escapeXml(inputXml);
		}
		}
		return inputXml;
		}

	public String DateConvert(String date) throws Exception
    {
		taLogger.log(" !!!!!! inside date convert",ApplicationConstants.LOG_DEBUG, "date is" + date, "instrutorlogging");
           date = date.substring(0, 10);
           taLogger.log("inside date convert",ApplicationConstants.LOG_DEBUG, "date.substring(0, 10)" + date, "instrutorlogging");
           SimpleDateFormat parse = new SimpleDateFormat("yyyy-MM-dd");
           taLogger.log("inside date convert",ApplicationConstants.LOG_DEBUG, "date is" + date, "instrutorlogging");
           SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
        java.util.Date parsed =parse.parse(date);
        date = formatter.format(parsed);
        taLogger.log(" !!!!!!!!!!!!!!! inside date convert",ApplicationConstants.LOG_DEBUG, "Returned date is" + date, "instrutorlogging");
        return date;
 }

	// 12012013 Yohan
	/**
	 * Method to replace namespace marked by namespaceMarker with newHostName
	 */
	public Document UpdateSystemNamespace(Document docToUpdate, String namespaceMarker, String newHostName) {
		Document updatedDoc = docToUpdate;
		try {
			String req = convertDocumentToString(docToUpdate);
//			taLogger.log("Before Replace", ApplicationConstants.LOG_DEBUG, req, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
			req = req.replaceAll(namespaceMarker, newHostName);
			updatedDoc = createDOMDocument(req);
//			taLogger.log("After Replace", ApplicationConstants.LOG_DEBUG, req, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		} catch (Exception e){
			e.printStackTrace();
		}
		return updatedDoc;
	}

	//14012013 Yohan
    public static boolean isXML (String input) {
        /**
         * return true if the String passed in is something like XML
         *
         *
         * @param inString a string that might be XML
         * @return true of the string is XML, false otherwise
         */

            boolean retBool = false;
            Pattern pattern;
            Matcher matcher;

            // REGULAR EXPRESSION TO SEE IF IT AT LEAST STARTS AND ENDS
            // WITH THE SAME ELEMENT
            final String XML_PATTERN_STR = "<(\\S+?)(.*?)>(.*?)</\\1>";



            // IF WE HAVE A STRING
            if (input != null && input.trim().length() > 0) {

                // IF WE EVEN RESEMBLE XML
                if (input.trim().startsWith("<")) {

                    pattern = Pattern.compile(XML_PATTERN_STR,
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

                    // RETURN TRUE IF IT HAS PASSED BOTH TESTS
                    matcher = pattern.matcher(input);
                    retBool = matcher.matches();
                }
            // ELSE WE ARE FALSE
            }
            return retBool;
    }


    /**
     * Formats dateValue to specified date format
     * @param dateValue
     * @param dateFormat
     * @return
     */
	public static String formatDate(String dateValue, String dateFormat) {

		//taLogger.log(" Inside formatDate method ",ApplicationConstants.LOG_DEBUG, "dateValue is" + dateValue +"dateFormat is"+dateFormat, "instrutorlogging");
		try{

		Date date = null;
		String datePattern = getDatePattern(dateValue);
		//System.out.println("dateFormat " + datePattern);
		if (datePattern == null) {
			throw new IllegalArgumentException("Date is not in an accepted format " + dateValue);
		}

		for (String sep : dateSeparators) {
			String actualDateFormat = datePattern.replace("{sep}", sep);
			//System.out.println("actualDateFormat " + actualDateFormat);
			// try first with the time
			for (String time : timeFormats) {
				//System.out.println("timeFormats " + time);
				try {
					date = new SimpleDateFormat(actualDateFormat + " " + time).parse(dateValue);
					//System.out.println("Date is"+date);
				} catch (ParseException parseException) {
					// if no space between date and time then parse again without space between date and time
					try {
						date = new SimpleDateFormat(actualDateFormat + "" + time).parse(dateValue);
					}catch(ParseException exception) {}
				}
				if (date != null) {
					SimpleDateFormat timeStampFormatter = new SimpleDateFormat(dateFormat);
				    dateValue = timeStampFormatter.format(date);
					return dateValue;
				}
			}
			// didn't work, try without the time formats
			try {
				date = new SimpleDateFormat(actualDateFormat).parse(dateValue);
			} catch (ParseException parseException) {
				parseException.printStackTrace();
			}
			if (date != null) {
				SimpleDateFormat timeStampFormatter = new SimpleDateFormat(dateFormat);
			    dateValue = timeStampFormatter.format(date);
				return dateValue;
			}
		}
		}catch(Exception e){
			dateValue = "-";
		}
		//System.out.println("Final Format dateValue " + dateValue);
		return dateValue;
	}

	/**
	 * get the pattern of the Date
	 * @param date
	 * @return
	 */
	private static String getDatePattern(String date) {
		for (String sep : dateSeparators) {
			//System.out.println("sep " + sep);
			String ymdPattern = ymd_template.replace("{sep}", sep);
			//System.out.println("ymdPattern " + ymdPattern);
			String dmyPattern = dmy_template.replace("{sep}", sep);
			//System.out.println("dmyPattern " + dmyPattern);
			String dmmmyPattern = dmmmy_template.replace("{sep}", sep);
			//System.out.println("dmmmyPattern " + dmmmyPattern);
			if (date.matches(ymdPattern)) {
				return YMD_FORMAT;
			}
			if (date.matches(dmyPattern)) {
				return DMY_FORMAT;
			}
			if (date.matches(dmmmyPattern)) {
				return DMMMY_FORMAT;
			}
		}
		return null;
	}

	public static void saveCmdTransDetails(List cmdTransDtlsList, String srcTransId, String transId, String loggerCategory, JdbcDatabaseDAO jdbcDatabaseDAO) throws Exception {
		TALogger taLogger = TALogger.getTALogger();
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[SPLCommonComponent][saveCmdTransDetails]Inside Method", loggerCategory);
		int count = 0;
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[SPLCommonComponent][saveCmdTransDetails]CmdTransDtlsList Size " +cmdTransDtlsList.size(), loggerCategory);
		
		for(Object list:cmdTransDtlsList){
			HandlerVariables var = (HandlerVariables) list;

			String parRowId = var.getRow_ID();
			Date insertDt=new Date();
			String insertBy="DBASLAPP";
			String updatedBy="DBASLAPP";
			Date updatedDt=new Date();
			//String parRowId = row_ID;
			String paramName = var.getParamName();
			String paramValue = var.getParamValue();
			String paramSub = var.getParamSub();
			String xmlEleType = var.getXmlEleType();
			String xmlEleId = var.getXmlEleId();
			String xmlEleCurrValue = var.getXmlEleCurrValue();
			String xmlElePrevValue = var.getXmlElePrevValue();
			String xmlEleName = var.getXmlEleName();
			BigDecimal seqNo = var.getSeqNo();
			String lkupRefId = var.getLookupRefId();
			String xmlParEleId = var.getXmlParEleId();

			//jdbcDatabaseDAO.insertTransDetails(parRowId, parRowId, paramName, paramValue, paramSub, xmlEleType, xmlEleId, xmlEleName, xmlEleCurrValue, xmlElePrevValue, seqNo, lkupRefId, insertDt, insertBy, updatedDt, updatedBy, srcTransId, transId,loggerCategory);
			jdbcDatabaseDAO.insertCmdTransDetails(var, srcTransId, transId,loggerCategory);

			// Ravi: Added to update cmd trans status as 'New'
			taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[SPLCommonComponent][saveCmdTransDetails]xmlParEleId :"+xmlParEleId, loggerCategory);
			taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[SPLCommonComponent][saveCmdTransDetails]Par Row Id :"+parRowId, loggerCategory);
			taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[SPLCommonComponent][saveCmdTransDetails]Cmd Trans Details List Size = " + cmdTransDtlsList.size(), loggerCategory);
			taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[SPLCommonComponent][saveCmdTransDetails]Cmd Trans Details List  = " + cmdTransDtlsList, loggerCategory);
			count = count + 1;
			taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[SPLCommonComponent][saveCmdTransDetails]Cmd Trans Details Count = " + count, loggerCategory);
			if(count==cmdTransDtlsList.size())
				
				jdbcDatabaseDAO.updateCmdTransStatus(parRowId);
				
		}
	}
	public static String Crypt(Integer key, String s, String s1) throws Exception 
	    {
		    int i = key.intValue();
			int ai[] = new int[64];
			int ai1[] = new int[64];
			int ai2[] = new int[64];
			String s2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz-_";
	        String s3 = "";
	        String s4 = "";
	        String s5 = "";
	        String s6 = "";
	        boolean flag1 = false;
	        Exception exception = null;

	        try
	        {
	            for(int l = 0; l < 64; l++)
	                ai[l] = -1;

	            int k2 = s.length();
	            int j2 = 0;
	            for(int i1 = 0; i1 < k2; i1++)
	            {
	                char c = s.charAt(i1);
	                if(!Character.isLetterOrDigit(c))
	                    switch(c)
	                    {
	                    default:
	                        continue;

	                    case 45: // '-'
	                    case 95: // '_'
	                        break;
	                    }
	                int i4 = s2.indexOf(c, 0);
	                if(ai[i4] == -1)
	                {
	                    ai[i4] = j2;
	                    s3 = s3 + s2.charAt(i4);
	                    j2++;
	                }
	            }

	            for(int j1 = 0; j1 < 64; j1++)
	                if(ai[j1] == -1)
	                {
	                    ai[j1] = j2;
	                    s3 = s3 + s2.charAt(j1);
	                    j2++;
	                }

	            for(int k1 = 0; k1 < 64; k1++)
	            {
	                ai1[k1] = ai[k1] / 8;
	                ai2[k1] = ai[k1] % 8;
	            }

	            j2 = 0;
	            int l2 = s1.length();
	            for(int l1 = 0; l1 < l2; l1++)
	            {
	                if(!Character.isLetterOrDigit(s1.charAt(l1)))
	                    switch(s1.charAt(l1))
	                    {
	                    default:
	                        continue;

	                    case 45: // '-'
	                    case 95: // '_'
	                        break;
	                    }
	                s4 = s4 + s1.charAt(l1);
	                j2++;
	            }

	            boolean flag11 = false;
	            if(l2 % 2 == 1)
	            {
	                s4 = s4 + s4.charAt(l2 - 1);
	                l2++;
	                flag11 = true;
	            }
	            for(int i2 = 0; i2 < l2; i2 += 2)
	            {
	                int j = s2.indexOf(s4.charAt(i2), 0);
	                int k = s2.indexOf(s4.charAt(i2 + 1), 0);
	                int k3 = ai2[j];
	                int l3 = ai2[k];
	                int i3 = ai1[j];
	                int j3 = ai1[k];
	                if(j == k)
	                {
	                    if(i > 0)
	                    {
	                        s5 = s5 + s3.charAt((k3 + 8 * i3 + 1) % 64);
	                        s5 = s5 + s3.charAt((k3 + 8 * i3 + 1) % 64);
	                    } else
	                    {
	                        s5 = s5 + s3.charAt((k3 + 8 * i3 + 63) % 64);
	                        s5 = s5 + s3.charAt((k3 + 8 * i3 + 63) % 64);
	                    }
	                } else
	                if(i3 == j3)
	                {
	                    if(i > 0)
	                    {
	                        s5 = s5 + s3.charAt((k3 + 1) % 8 + 8 * i3);
	                        s5 = s5 + s3.charAt((l3 + 1) % 8 + 8 * j3);
	                    } else
	                    {
	                        s5 = s5 + s3.charAt((k3 + 7) % 8 + 8 * i3);
	                        s5 = s5 + s3.charAt((l3 + 7) % 8 + 8 * j3);
	                    }
	                } else
	                if(k3 == l3)
	                {
	                    if(i > 0)
	                    {
	                        s5 = s5 + s3.charAt(k3 + 8 * ((i3 + 1) % 8));
	                        s5 = s5 + s3.charAt(l3 + 8 * ((j3 + 1) % 8));
	                    } else
	                    {
	                        s5 = s5 + s3.charAt(k3 + 8 * ((i3 + 7) % 8));
	                        s5 = s5 + s3.charAt(l3 + 8 * ((j3 + 7) % 8));
	                    }
	                } else
	                {
	                    s5 = s5 + s3.charAt(l3 + 8 * i3);
	                    s5 = s5 + s3.charAt(k3 + 8 * j3);
	                }
	            }

	            if(flag11)
	            {
	                l2--;
	                s6 = s5.substring(0, l2);
	                //System.out.println("Encrypted/decrypted value is "+s6);
	            } else
	            {
	                s6 = s5;
	                //System.out.println("Encrypted/decrypted value is "+s6);
	            }
	        }
	        catch(Exception exception1)
	        {
	            s6 = "";
	           // System.out.println("Exception is " + exception1);
	           // System.out.println("Input Value is iOption=[" + i + "] sUserId=[" + s + "] sPassword=[" + s1 + "]");
	            flag1 = true;
	            exception = exception1;

	        }
	        finally { }
	        if(flag1)
	            throw exception;
	        else
	            return s6;
	    }
	
	//For reading ADS response code 
	public static CommandTransDetails getStatusCodeAndMsg(String responseHeader) throws Exception {
		/*String responseHeader = "HTTP/1.1 200 OK\n" +
							"Connection: close\n"+
							"Date: Tue, 18 Dec 2012 09:39:54 GMT\n"+
							"Content-Length: 0\n"+
							"X-Powered-By: Servlet/2.4 JSP/2.0\n";*/
		
		// convert String into InputStream
		InputStream is = new ByteArrayInputStream(responseHeader.getBytes());

		// read it with BufferedReader
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		CommandTransDetails cmdTransDetails = new CommandTransDetails();
		String line = "";
		String statuscode = "";
		String statusMsg = "";
		if ((line = br.readLine()) != null) {

			String msg = line;
			if (msg.contains("HTTP") || msg.contains("OK")) {
				String[] values = msg.split(" ");
				for (int j = 0; j < values.length; j = j + 1) {
					statuscode = values[1];
					statusMsg = values[2];
				}

			} else {
				String[] values = msg.split(":");
				statuscode = values[0];
				statusMsg = values[1];
				// System.out.println(val1 +" is " + val2.trim());
			}

		}

		cmdTransDetails.setReturnCode(statuscode);
		cmdTransDetails.setReturnMsg(statusMsg);
		br.close();

		// return statuscode;
		return cmdTransDetails;
	}

/*M.Rahman: New method to handle billing related transactions: Begin*/
	
	public static void saveCmdTransDetailsBill(List cmdTransDtlsList, String srcTransId, String transId, String loggerCategory, JdbcDatabaseDAO jdbcDatabaseDAO) throws Exception {
		TALogger taLogger = TALogger.getTALogger();
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[SPLCommonComponent][saveCmdTransDetails]Inside Method", loggerCategory);
		int count = 0;
		for(Object list:cmdTransDtlsList){
			HandlerVariables var = (HandlerVariables) list;

			String parRowId = var.getRow_ID();
			Date insertDt=new Date();
			String insertBy="DBASLAPP";
			String updatedBy="DBASLAPP";
			Date updatedDt=new Date();
			//String parRowId = row_ID;
			String paramName = var.getParamName();
			String paramValue = var.getParamValue();
			String paramSub = var.getParamSub();
			String xmlEleType = var.getXmlEleType();
			String xmlEleId = var.getXmlEleId();
			String xmlEleCurrValue = var.getXmlEleCurrValue();
			String xmlElePrevValue = var.getXmlElePrevValue();
			String xmlEleName = var.getXmlEleName();
			BigDecimal seqNo = var.getSeqNo();
			String lkupRefId = var.getLookupRefId();

			jdbcDatabaseDAO.insertTransDetailsBill(parRowId, parRowId, paramName, paramValue, paramSub, xmlEleType, xmlEleId, xmlEleName, xmlEleCurrValue, xmlElePrevValue, seqNo, lkupRefId, insertDt, insertBy, updatedDt, updatedBy, srcTransId, transId,loggerCategory);

			// Ravi: Added to update cmd trans status as 'New'
			taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[SPLCommonComponent][saveCmdTransDetails]Cmd Trans Details List Size = " + cmdTransDtlsList.size(), loggerCategory);
			count = count + 1;
			taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[SPLCommonComponent][saveCmdTransDetails]Cmd Trans Details Count = " + count, loggerCategory);
			/*if(count==cmdTransDtlsList.size())
				jdbcDatabaseDAO.updateBillCmdTransStatus(parRowId);*/

		}
	}	
	
/*M.Rahman: New method to handle billing related transactions: End*/

	public static Date parseDate(String strDate) throws ParseException {
		DateFormat formatter = new SimpleDateFormat(ApplicationConstants.DATE_FORMAT_YMD);
		Date objDate = (Date)formatter.parse(strDate);
		return objDate;
	}
	
	/**
	 * for Bug#21337
	 * @param docResponse
	 * @param transId
	 * @return
	 * @throws XPathExpressionException
	 * @throws Exception
	 */
	private List<CommandTransDetails> getCommandTransDtls(Document docResponse, String transId) throws XPathExpressionException, Exception {
		String response = convertDocumentToString(docResponse);
		InputStream inputStream = new ByteArrayInputStream(response.getBytes());
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(false);
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		Document taskResponse = builder.parse(inputStream);
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]Inside Method", loggerCategory);
		Properties properties = loadProperty(ApplicationConstants.OSMTRANSLATION_FILENAME);
		XPath xPath = XPathFactory.newInstance().newXPath();
		String parRowId = (String) getXpathValue(taskResponse, "Envelope/Body/input/msgHeader/correlationID", XPathConstants.STRING);
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls] parRowId " + parRowId, loggerCategory);
		NodeList paramInfoList = (NodeList) xPath.evaluate("Envelope/Body/input/msgBody/arrayOfParamInfoType/paramInfoType", taskResponse, XPathConstants.NODESET);
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]Size of paramInfo List " + paramInfoList.getLength(), loggerCategory);
		CommandTransDetails commandTransDetails = null;
		List<CommandTransDetails> list = new ArrayList<CommandTransDetails>();
		for(int paramInfoIndex=0;paramInfoIndex<paramInfoList.getLength();paramInfoIndex++) {
			Node paramInfoNode = paramInfoList.item(paramInfoIndex);
			taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]paramInfoNode " + paramInfoNode, loggerCategory);
			if(paramInfoNode!=null && paramInfoNode.getNodeType()==Node.ELEMENT_NODE) {
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]paramInfoNode Name " + paramInfoNode.getNodeName(), loggerCategory);
				NodeList paramInfoChilds = paramInfoNode.getChildNodes();
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]paramInfoChilds Size " + paramInfoChilds.getLength(), loggerCategory);
				commandTransDetails = new CommandTransDetails();
				commandTransDetails.setParRowId(parRowId);
				for(int paramInfoChildIndex=0;paramInfoChildIndex<paramInfoChilds.getLength();paramInfoChildIndex++) {
					Node childNode = paramInfoChilds.item(paramInfoChildIndex);
					taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]childNode " + childNode, loggerCategory);
					if(childNode!=null && !childNode.getTextContent().equals("") && childNode.getNodeType()==Node.ELEMENT_NODE) {
						String nodeName = childNode.getNodeName();
						taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]childNode Name " + nodeName, loggerCategory);
						String nodeValue = childNode.getTextContent().trim();
						taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]childNode Value " + nodeValue, loggerCategory);
						if(nodeName.equalsIgnoreCase("paramType"))
							commandTransDetails.setXmlEleType(nodeValue);
						if(nodeName.equalsIgnoreCase("paramID"))
							commandTransDetails.setXmlEleId(nodeValue);
						if(nodeName.equalsIgnoreCase("paramName")) {
							if(properties.containsKey(nodeValue)) {
								nodeValue = properties.getProperty(nodeValue);
							}
							childNode.setTextContent(nodeValue);
							commandTransDetails.setXmlEleName(nodeValue);
						}
						if(nodeName.equalsIgnoreCase("currValue"))
							commandTransDetails.setXmlEleCurrValue(nodeValue);
					}
				}
				list.add(commandTransDetails);
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]List Size Inside For Loop " + list.size(), loggerCategory);
			}
		}
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]List Size " + list.size(), loggerCategory);
		try {
			String tomResponse = convertDocumentToString(taskResponse);
			taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDtls]tomResponse " + tomResponse, loggerCategory);
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * for Bug#21337
	 * @param taskResponse
	 * @param transId
	 * @return
	 * @throws XPathExpressionException
	 * @throws IOException
	 */
	private List<CommandTransDetails> getCommandTransDetailsforCrmModule(Document taskResponse, String transId) throws XPathExpressionException, IOException {

		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]Inside Method", loggerCategory);
		Properties properties = loadProperty(ApplicationConstants.OSMTRANSLATION_FILENAME);
		XPath xPath = XPathFactory.newInstance().newXPath();
		String parRowId = (String) getXpathValue(taskResponse, "InvokeOMDnetModuleResponse/msgHeader/correlationID", XPathConstants.STRING);
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule] parRowId " + parRowId, loggerCategory);
		NodeList paramInfoList = (NodeList) xPath.evaluate("InvokeOMDnetModuleResponse/msgBody/arrayOfParamInfoType/paramInfoType", taskResponse, XPathConstants.NODESET);
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]Size of paramInfo List " + paramInfoList.getLength(), loggerCategory);
		CommandTransDetails commandTransDetails = null;
		List<CommandTransDetails> list = new ArrayList<CommandTransDetails>();
		for(int paramInfoIndex=0;paramInfoIndex<paramInfoList.getLength();paramInfoIndex++) {
			Node paramInfoNode = paramInfoList.item(paramInfoIndex);
			taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]paramInfoNode " + paramInfoNode, loggerCategory);
			if(paramInfoNode!=null && paramInfoNode.getNodeType()==Node.ELEMENT_NODE) {
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]paramInfoNode Name " + paramInfoNode.getNodeName(), loggerCategory);
				NodeList paramInfoChilds = paramInfoNode.getChildNodes();
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]paramInfoChilds Size " + paramInfoChilds.getLength(), loggerCategory);
				commandTransDetails = new CommandTransDetails();
				commandTransDetails.setParRowId(parRowId);
				for(int paramInfoChildIndex=0;paramInfoChildIndex<paramInfoChilds.getLength();paramInfoChildIndex++) {
					Node childNode = paramInfoChilds.item(paramInfoChildIndex);
					taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]childNode " + childNode, loggerCategory);
					if(childNode!=null && !childNode.getTextContent().equals("") && childNode.getNodeType()==Node.ELEMENT_NODE) {
						String nodeName = childNode.getNodeName();
						taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]childNode Name " + nodeName, loggerCategory);
						String nodeValue = childNode.getTextContent().trim();
						taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]childNode Value " + nodeValue, loggerCategory);
						if(nodeName.equalsIgnoreCase("paramType"))
							commandTransDetails.setXmlEleType(nodeValue);
						if(nodeName.equalsIgnoreCase("paramID"))
							commandTransDetails.setXmlEleId(nodeValue);
						if(nodeName.equalsIgnoreCase("paramName")) {
							if(properties.containsKey(nodeValue)) {
								nodeValue = properties.getProperty(nodeValue);
							}
							childNode.setTextContent(nodeValue);
							commandTransDetails.setXmlEleName(nodeValue);
						}
						if(nodeName.equalsIgnoreCase("currValue"))
							commandTransDetails.setXmlEleCurrValue(nodeValue);
					}
				}
				list.add(commandTransDetails);
				taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]List Size Inside For Loop " + list.size(), loggerCategory);
			}
		}
		taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]List Size " + list.size(), loggerCategory);
		try {
			String tomResponse = convertDocumentToString(taskResponse);
			taLogger.log(logSrcTransId, transId, logCmdRowId, ApplicationConstants.LOG_INFO, "[SPLCommonComponent][getCommandTransDetailsforCrmModule]tomResponse " + tomResponse, loggerCategory);
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
}


