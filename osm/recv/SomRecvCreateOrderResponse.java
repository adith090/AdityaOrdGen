package com.m1.sg.osm.recv;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.sg.osm.database.dao.DaoFactory;
import com.m1.sg.osm.database.dao.common.CommonDao;
import com.m1.sg.osm.database.entity.common.ErrorLoggingEntity;
import com.m1.sg.osm.util.DOMUtils;
import com.m1.sg.osm.util.DateTimeUtil;
import com.mslv.oms.automation.AutomationContext;
import com.mslv.oms.automation.AutomationException;
import com.mslv.oms.automation.TaskContext;
import com.mslv.oms.automation.plugin.AbstractAutomator;
import common.util.TALogger;

/******************************************************************************* 
 * MODIFICATION HISTORY
 ******************************************************************************* 
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 ******************************************************************************* 
 * 31/10/2012					Yohan					Created
 * 02/04/2013					Yohan					Exception handling
 ******************************************************************************/

public class SomRecvCreateOrderResponse extends AbstractAutomator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void run(String inputData, AutomationContext context)
			throws AutomationException {
		// TODO Auto-generated method stub
				
		String crmOrderId = "";
		TALogger taLogger = TALogger.getTALogger();
		TaskContext taskContext = (TaskContext) context;
		
		try {			
			Document inputOrderDocument = DOMUtils.createDOMDocument(taskContext.getOrder());
			crmOrderId = inputOrderDocument.getElementsByTagName("OrderID").item(0).getTextContent();
			
			if(!inputData.contains("<n1:Id>")){
				String errMessage = "SOM failed to create TOM";
				insertErrorException(crmOrderId, inputData, taskContext, errMessage);
			}
			
		} catch (Exception e){
						
			try {
				if (crmOrderId.equals(""))
					crmOrderId = "unidentified";
				String exception = exceptionToString(e);
				String errMessage = "SOM request to TOM";
				insertErrorException(crmOrderId, exception, taskContext, errMessage);
				
			} catch (IOException ex){
				ex.printStackTrace();
			}
			taLogger.log(crmOrderId, String.valueOf(taskContext.getOrderId()), 01, "ERROR", "osmsomlogging", e);
			
		}
		
	}

	private static String exceptionToString(Throwable e) throws IOException{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String pwException = sw.toString();
		pw.close();
		sw.close();
		return pwException;
	}
	
	private static void insertErrorException(String crmOrderId, String exception, TaskContext taskContext, String errMessage){
		
		CommonDao commonDao = DaoFactory.getDao(CommonDao.class);
		TALogger taLogger = TALogger.getTALogger();
		
		try {
			
			ErrorLoggingEntity errorLogging = new ErrorLoggingEntity();
			
			errorLogging.setTransactionId(String.valueOf(taskContext.getOrderId()));
			errorLogging.setSourceTransactionId(crmOrderId);
			errorLogging.setOrderHistId(String.valueOf(taskContext.getOrderHistoryId()));
			errorLogging.setTaskName(taskContext.getTaskMnemonic());
			errorLogging.setCartridgeName(taskContext.getNamespace());
			errorLogging.setTransactionType("SOM");
			
			if(exception.length() > 4000){
				errorLogging.setStacktrace(exception.substring(0,4000));
			} else {
				errorLogging.setStacktrace(exception);
			}
			
			errorLogging.setPollStatus("N");
			errorLogging.setErrorId(errMessage);
			errorLogging.setInsertDateTime(DateTimeUtil.format(new Date(), DateTimeUtil.DATABASE_FORMAT_DATE));
			errorLogging.setInsertBy(taskContext.getNamespace());
			errorLogging.setUpdateDateTime(DateTimeUtil.format(new Date(), DateTimeUtil.DATABASE_FORMAT_DATE));
			errorLogging.setUpdateBy(taskContext.getNamespace());
		
			commonDao.create(errorLogging, ErrorLoggingEntity.SQL_INSERT_CREATE_ORDER_ERROR_TRANS);
		
		} catch (Exception e){
			
			taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "ERROR", "osmcomlogging", e);
		
		} finally {
			
			if(commonDao != null){
				commonDao.commit();
				commonDao.close();
			}
			
		}
	}

}
