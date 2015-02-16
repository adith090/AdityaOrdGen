package com.m1.bcc.spl.instructor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.SPLCommonComponent;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 22/02/2013					Ravikumar G				Modified for logging cleanup
 * 11/04/2013					Billy Lim				Bug 1774. If param DEFAULT_VALUE is "#ForceGetXpathValue#", set paramValue with Xpath result even if Xpath does not return a value
 ******************************************************************************/

public class CommandParametersBill {


	TALogger taLogger = TALogger.getTALogger();
	String loggercategory = ApplicationConstants.LOGGER_CMD_PARAMETER;



	String xPathVariable;
	String paramName;
	String paramValue;
	String defaultValue;
	String lookupRefId;
	String paramSub;
	String parRowId;
	BigDecimal seqNo;
	String xmlEleType;
	String xmlEleId;
	String xmlEleName;
	String xmlElePrevValue;
	String xmlEleCurrValue;
	String append;
	String prepend;
	String srcTransId;
	String trans_Id;
	String row_ID;
	

	//gets the parameter values from c_om_cmd_param_map and evaluates the paramValue
	public List<HandlerVariables> getParameters(String srcTransId, String order_Xml, String comndRefId, String trans_Id, String row_ID,
			String orderTransRowId, JdbcDatabaseDAO jdbcDatabaseDAO, Boolean flag, String compLineItemId) throws SQLException{

		
		HandlerVariables handlerVar;
		CommandParamRefDtls concateValues = new CommandParamRefDtls();
		CommandTransDetails commandTransDetails = jdbcDatabaseDAO.getCmdDestRefDtls(comndRefId);

		List<HandlerVariables> paramList = new ArrayList<HandlerVariables>();

		try{

			taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "Inside insertParameters method", loggercategory);


			InputStream is = new ByteArrayInputStream(order_Xml.getBytes());
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(is);

			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();

			taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters]Inside method", loggercategory);
			// Passing srcTransId, transId and rowId for logging
			
			List<Map<String, Object>> xmlPath = jdbcDatabaseDAO.getCmdParameters(comndRefId, srcTransId, trans_Id, row_ID,loggercategory);
			
			//for a single parameter for a single cmd_Ref_Id
			for(Object path : xmlPath){

				handlerVar = new HandlerVariables();

				Map map = (Map) path;
				xPathVariable = (String) map.get("XPATH");
				
				if (xPathVariable.contains("|"))
				{
					String xPathTemp = xPathVariable;
					String[] splitString = xPathTemp.split("\\|", -1);
					xPathVariable = splitString[0] + compLineItemId +  splitString[1];
					
				}
				
			
				String newXpath = "Envelope/Body/ProvRequest/ListOfRef/"	+ xPathVariable;
				handlerVar.setxPathVariable(newXpath);
				taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] new xpath is:" + newXpath, loggercategory);

				
				
				
				paramName = (String) map.get("PARAM_NAME");
				handlerVar.setParamName(paramName);
				taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] param name is:" + paramName, loggercategory);

				
				paramSub = (String) map.get("PARAM_SUB");
				handlerVar.setParamSub(paramSub);
				taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] paramSub is:" + paramSub, loggercategory);

				handlerVar.setRow_ID(row_ID);

				handlerVar.setTrans_Id(trans_Id);

				handlerVar.setSrcTransId(srcTransId);

				handlerVar.setCommndRefId(comndRefId);
				taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] cmdRefId is:" + comndRefId, loggercategory);


				handlerVar.setParRowId(orderTransRowId);
				taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] orderTransRowId is:" + orderTransRowId, loggercategory);

				seqNo = (BigDecimal) map.get("SEQ_NO");
				handlerVar.setSeqNo(seqNo);
				taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] seqNo is:" + seqNo, loggercategory);

				xmlEleType = (String) map.get("XML_ELE_TYPE");
				//handlerVar.setXmlEleType(xmlEleType);
				taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] xmlEleType is:" + xmlEleType, loggercategory);

				append = (String) map.get("APPEND");
				handlerVar.setAppend(append);

				prepend = (String) map.get("PREPEND");
				handlerVar.setPrepend(prepend);
				taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] append and prepend is:" + append + prepend, loggercategory);

				defaultValue = (String) map.get("DEFAULT_VALUE");
				handlerVar.setDefaultValue(defaultValue);
				taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] defaultValue is:" + defaultValue, loggercategory);

				lookupRefId = (String) map.get("LOOKUP_REF_ID");
				handlerVar.setLookupRefId(lookupRefId);
				taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] lookup ref id is:" + lookupRefId, loggercategory);

				//evaluates the param value if default value is null
				boolean XpathRescon = true;
				if(!xPathVariable.equals("-") && defaultValue==null){

					XPathExpression expr1 = xpath.compile(newXpath);
					XpathRescon = (Boolean) (expr1.evaluate(doc, XPathConstants.BOOLEAN));
					if(XpathRescon) {
						paramValue = (String) expr1.evaluate(doc, XPathConstants.STRING);
					}

					taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters]1. param value if xpath is not null:" + paramValue, loggercategory);
					
				}

				//evaluates the param value if default value is not null
				else if(defaultValue!=null){

					paramValue = defaultValue;
					taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters]2. param value if default value is not null:" + paramValue, loggercategory);
				}
				else{
					
					paramValue = defaultValue;
					taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters]3. param value if default value is not null:" + paramValue+" XpathRescon= "+XpathRescon, loggercategory);
				
				}


				if(XpathRescon) {
					//evaluates the param value if lookup Ref Id is not null
					if(lookupRefId!=null){

						// Passing srcTransId, transId and rowId for logging
						paramValue = jdbcDatabaseDAO.getNewParamValue(lookupRefId,paramValue, srcTransId, trans_Id, row_ID,loggercategory);
						taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] param value if lookup ref id is not null:" + paramValue, loggercategory);
					}

					if(append!=null){
						paramValue = paramValue+append;
					}

					if(prepend!=null){
						paramValue = prepend+paramValue;
					}
					if(paramValue!=null){
						
					
					if(paramValue.equalsIgnoreCase("#sysdate#"))
					{
						paramValue=SPLCommonComponent.formatSysDateToText();
					}

					else if (paramValue.equalsIgnoreCase("#transid#"))
					{
						paramValue=row_ID;
					}

					// Added as part of Bug#306
					else if (paramValue.equalsIgnoreCase("#username#"))
					{
						paramValue=commandTransDetails.getUserName();
						System.out.println("param value as username"+paramValue);
					}
					//Added for Bug#1774
					else if (paramValue.equalsIgnoreCase("#ForceGetXpathValue#"))
					{
						XPathExpression expr2 = xpath.compile(newXpath);
						paramValue = (String) expr2.evaluate(doc, XPathConstants.STRING);
					}					

					
					}
					handlerVar.setParamValue(paramValue);
					taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] paramValue is:" + paramValue, loggercategory);
					

					handlerVar.setXmlEleType(xmlEleType);
					handlerVar.setXmlEleId(xmlEleId);
					handlerVar.setXmlEleName(xmlEleName);
					handlerVar.setXmlEleCurrValue(xmlEleCurrValue);
					handlerVar.setXmlElePrevValue(xmlElePrevValue);

					taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] xml ele id is::" + xmlEleId, loggercategory);

					taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] xml ele name is:" + xmlEleName, loggercategory);

					taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] xml ele curr value is:" + xmlEleCurrValue, loggercategory);

					taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][getParameters] xml ele prev value is:" + xmlElePrevValue, loggercategory);



					paramList.add(handlerVar);
				}

				// Ravi: resetting the below values to null
				xmlEleId = null;
				xmlEleName = null;
				xmlEleCurrValue = null;
				xmlElePrevValue = null;
				
							

			}// end for loop


		}catch(Exception e){


			taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][try catch block] inside catch block:" + e.getMessage(), loggercategory, e);
			e.printStackTrace();
			
		}


		return paramList;

	}

	//evaluates the param value for a given xpath
	public String evaluateXPath(XPath xpath, Document doc, String expr)
			throws XPathExpressionException {

		XPathExpression exp = xpath.compile(expr);
		String xmlDetails = (String) exp.evaluate(doc, XPathConstants.STRING);
		taLogger.log(srcTransId, trans_Id, row_ID , ApplicationConstants.LOG_INFO, "[CommandParameters][evaluateXPath] param value for xpath:" + xmlDetails, loggercategory);
		return xmlDetails;


	}
}
