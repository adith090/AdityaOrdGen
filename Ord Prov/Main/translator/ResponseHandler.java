package com.m1.bcc.spl.translator;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import common.util.TALogger;


/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 25/06/2013					Sudharsan  				Created the class for Handling special case response 
 *******************************************************************************/


public class ResponseHandler {

	private TALogger taLogger;
	
	
	public void responseHandlerSplCase(JdbcDatabaseDAO jdbcDatabaseDAO,List<Map<String, Object>> orderXml,String OrderXml,String cmdRefId,String  srcTransId,String transId,String cmdRowId,String rowId,String corrId,String responseCode, String responseDescription,String splFlag,String loggerCategory) throws Exception
	{
	taLogger = TALogger.getTALogger();
	
	List<Map<String, Object>>  newCommands =jdbcDatabaseDAO.getSplHandlingCommands(cmdRefId);
	taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseHandler][responseHandlerSplCase][SplHandling] New Commands list size :"+ newCommands.size(), loggerCategory);
	HashMap <String,Object>  hMap=jdbcDatabaseDAO.getOrderXmlIds(OrderXml);
	String MoliId=(String) hMap.get("MOLI");
	String OliId=(String) hMap.get("OLI");
	
	Iterator<Map<String, Object>> newCmdItz = newCommands.iterator();
	taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseHandler][responseHandlerSplCase][SplHandling]Iterator values : "+newCmdItz.toString(), loggerCategory);
	while (newCmdItz.hasNext())
	{
	
	Map<String, Object>	newCmdMap = newCmdItz.next();
	String newCmdRefId =(String) newCmdMap.get("NEW_CMD_REF_ID");
	BigDecimal seqNo =(BigDecimal) newCmdMap.get("SEQ_NO");
	taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseHandler][responseHandlerSplCase][SplHandling]SeqNo : "+seqNo  , loggerCategory);
	taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseHandler][responseHandlerSplCase][SplHandling]NewCmdRefId : "+newCmdRefId +" SeqNo : "+seqNo  , loggerCategory);
	{

		jdbcDatabaseDAO.insertTransValues(rowId, newCmdRefId, transId, srcTransId, MoliId, OliId, seqNo, corrId,new Date(), "DBASLAPP", new Date(), "DBASLAPP",rowId, loggerCategory, false);
	}
	}
	taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseHandler][responseHandlerSplCase][SplHandling]Get Start New Command Start", loggerCategory);
	jdbcDatabaseDAO.getStartNewCommand(orderXml, srcTransId, transId, cmdRowId, responseCode, responseDescription, cmdRefId, loggerCategory,splFlag);
	taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[ResponseHandler][responseHandlerSplCase][SplHandling]Get Start New Command End", loggerCategory);
	}
	
}
