package com.m1.sg.osm.upd;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import org.w3c.dom.Document;

import com.m1.sg.osm.util.DOMUtils;
import com.m1.sg.osm.util.DateTimeUtil;
import com.m1.sg.osm.database.dao.DaoFactory;
import com.m1.sg.osm.database.dao.common.CommonDao;
import com.m1.sg.osm.database.entity.common.ErrorLoggingEntity;
import com.m1.sg.osm.database.entity.common.OSMTransactionEntity;
import com.m1.sg.osm.database.util.UpdateUtil;
import com.mslv.oms.automation.AutomationContext;
import com.mslv.oms.automation.AutomationException;
import com.mslv.oms.automation.OrderNotificationContext;
import com.mslv.oms.automation.plugin.AbstractAutomator;
import common.util.TALogger;

/*
 * The SomUpdateStatus class handles "In Progress" or "Cancelled" status change
 */

/******************************************************************************* 
 * MODIFICATION HISTORY
 ******************************************************************************* 
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 ******************************************************************************* 
 * 31/10/2012					Yohan					Created
 * 02/04/2013					Yohan					Exception handling
 * 05/09/2013					Yohan					Update to Cancelled when Aborted (Bug 2689)
 ******************************************************************************/

public class SomUpdateStatus extends AbstractAutomator {
	
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
		OrderNotificationContext orderContext = (OrderNotificationContext) context;		
	
		try {						
			OSMTransactionEntity osmEntity = new OSMTransactionEntity();			
			Document inputDocument = DOMUtils.createDOMDocument(orderContext.getOrder());
			String orderState = DOMUtils.retrieveValuePath(inputDocument, "GetOrder.Response/OrderState");

			/*remove by Mr.Gun Because I need to move the logic to ComUpdateOrderStatusInprograss objective is remove thread sleep*/
			
//			if (orderState.equals("open.running.in_progress")) {
//				Thread.sleep(2000); // To be updated
//
//				String orderErase = removeNamespaceAndPreamble(orderContext.getOrder());
//				inputDocument = DOMUtils.createDOMDocument(orderErase);
//				Document orderItemDocument = DOMUtils.somepathOfDoc(inputDocument, "OrderItem");
//				crmOrderId = DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemXMLData/LineItem/OrderHeader/OrderID");
//	
//				/* Update the status to "In Progress" */
//				osmEntity.setTransactionType("SOM");
//				osmEntity.setPollStatus("N");
//				osmEntity.setStatus("In Progress");
//				osmEntity.setOrderId(String.valueOf(orderContext.getOrderId()));
//				osmEntity.setSourceTransactionId(crmOrderId);
//				osmEntity.setUpdateDateTime(DateTimeUtil.format(new Date(), DateTimeUtil.DATABASE_FORMAT_DATE));
//				UpdateUtil.updateOSMTransaction(osmEntity, OSMTransactionEntity.SQL_UPDATE_STATUS_OSM_TRANS);
//			}
			
			if (orderState.equals("open.not_running.cancelled") || orderState.equals("closed.aborted")) { // Yohan : 20130905 : Bug 2689 : Update to Cancelled when Aborted
				
				String orderErase = removeNamespaceAndPreamble(orderContext.getOrder());
				inputDocument = DOMUtils.createDOMDocument(orderErase);
				Document orderItemDocument = DOMUtils.somepathOfDoc(inputDocument, "OrderItem");
				crmOrderId = DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemXMLData/LineItem/OrderHeader/OrderID");
				
				/* Update SOM status to cancelled */
				osmEntity.setTransactionType("SOM");
				osmEntity.setPollStatus("N");
				osmEntity.setOrderId(String.valueOf(orderContext.getOrderId()));
				osmEntity.setStatus("Cancelled");
				osmEntity.setSourceTransactionId(crmOrderId);
				osmEntity.setUpdateDateTime(DateTimeUtil.format(new Date(), DateTimeUtil.DATABASE_FORMAT_DATE));
				UpdateUtil.updateOSMTransaction(osmEntity, OSMTransactionEntity.SQL_UPDATE_STATUS_OSM_TRANS);
			}
						
		} catch (Exception e){
			
			try {	
				if (crmOrderId.equals(""))
					crmOrderId = "unidentified";
				String exception = exceptionToString(e);
				String errMessage = "Update Status Milestone";
				insertErrorException(crmOrderId, exception, orderContext, errMessage);
				
			} catch (IOException ex){
				ex.printStackTrace();
			}
			taLogger.log(crmOrderId, String.valueOf(orderContext.getOrderId()), 01, "ERROR", "osmsomlogging", e);
		}
	}
	
	private static String removeNamespaceAndPreamble(String xmlString) {
		// Original
//		  return xmlString.replaceAll("(<\\?[^<]*\\?>)?", ""). /* remove preamble */
//		  replaceAll(" xmlns.*?(\"|\').*?(\"|\')", "")  /* remove xmlns declaration */
//		  .replaceAll("(<)(\\w+:)(.*?>)", "$1$3").replaceAll("im:", "") /* remove opening tag prefix */
//		  .replaceAll("(</)(\\w+:)(.*?>)", "$1$3"); /* remove closing tags prefix */
		
		// Tuned
		return xmlString.replaceAll("<\\?.+?>", "") /* remove preamble */
			   .replaceAll(" xmlns.+?(\"|\').+?(\"|\')", "") /* remove xmlns declaration */
			   .replaceAll("(<)(\\w+:)(.+?>)", "$1$3").replaceAll("im:", "") /* remove opening tag prefix */
			   .replaceAll("(</)(\\w+:)(.+?>)", "$1$3"); /* remove closing tags prefix */
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
	
	private static void insertErrorException(String crmOrderId, String exception, OrderNotificationContext orderContext, String errMessage){
		
		CommonDao commonDao = DaoFactory.getDao(CommonDao.class);
		TALogger taLogger = TALogger.getTALogger();
		
		try {
			
			ErrorLoggingEntity errorLogging = new ErrorLoggingEntity();
			
			errorLogging.setTransactionId(String.valueOf(orderContext.getOrderId()));
			errorLogging.setSourceTransactionId(crmOrderId);
			errorLogging.setOrderHistId(String.valueOf(orderContext.getNotificationHistoryId()));
			errorLogging.setTaskName(orderContext.getNotificationName());
			errorLogging.setCartridgeName(orderContext.getNamespace());
			errorLogging.setTransactionType("SOM");
			
			if(exception.length() > 4000){
				errorLogging.setStacktrace(exception.substring(0,4000));
			} else {
				errorLogging.setStacktrace(exception);
			}
			
			errorLogging.setPollStatus("N");
			errorLogging.setErrorId(errMessage);
			errorLogging.setInsertDateTime(DateTimeUtil.format(new Date(), DateTimeUtil.DATABASE_FORMAT_DATE));
			errorLogging.setInsertBy(orderContext.getNamespace());
			errorLogging.setUpdateDateTime(DateTimeUtil.format(new Date(), DateTimeUtil.DATABASE_FORMAT_DATE));
			errorLogging.setUpdateBy(orderContext.getNamespace());
		
			commonDao.create(errorLogging, ErrorLoggingEntity.SQL_INSERT_CREATE_ORDER_ERROR_TRANS);
		
		} catch (Exception e){
			
			taLogger.log(crmOrderId, String.valueOf(orderContext.getOrderId()), 01, "ERROR", "osmcomlogging", e);
		
		} finally {
			
			if(commonDao != null){
				commonDao.commit();
				commonDao.close();
			}
			
		}
	}
}