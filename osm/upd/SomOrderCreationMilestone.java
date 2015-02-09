package com.m1.sg.osm.upd;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import org.w3c.dom.Document;

import com.m1.sg.osm.database.dao.DaoFactory;
import com.m1.sg.osm.database.dao.common.CommonDao;
import com.m1.sg.osm.database.entity.common.ErrorLoggingEntity;
import com.m1.sg.osm.database.entity.common.OSMTransactionEntity;
import com.m1.sg.osm.database.util.InsertUtil;
import com.m1.sg.osm.database.util.UpdateUtil;
import com.m1.sg.osm.util.DOMUtils;
import com.m1.sg.osm.util.DateTimeUtil;
import com.mslv.oms.automation.AutomationContext;
import com.mslv.oms.automation.AutomationException;
import com.mslv.oms.automation.OrderNotificationContext;
import com.mslv.oms.automation.plugin.AbstractAutomator;
import common.util.TALogger;

/*
 * SomOrderCreationMilestone class will insert an entry to the database
 * */

/******************************************************************************* 
 * MODIFICATION HISTORY
 ******************************************************************************* 
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 ******************************************************************************* 
 * 20/08/2013					Aditya					Created
 ******************************************************************************/

public class SomOrderCreationMilestone extends AbstractAutomator {
	//
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static String updateOrderCommand = "<OrderDataUpdate><OrderId>{0}</OrderId><Add path=\"/\"><CreationFinishFlag>Y</CreationFinishFlag></Add></OrderDataUpdate>";
	
	@Override
	public void run(String inputData, AutomationContext context)
			throws AutomationException {
		// TODO Auto-generated method stub
		
		String crmOrderId = "";
		TALogger taLogger = TALogger.getTALogger();
		OrderNotificationContext orderContext = (OrderNotificationContext) context;
	
		try {
			String orderErase = removeNamespaceAndPreamble(orderContext.getOrder());
			Document inputDocument = DOMUtils.createDOMDocument(orderErase);			
			crmOrderId = DOMUtils.somepathOfDoc(inputDocument, "OrderItem").getElementsByTagName("OrderID").item(0).getTextContent();
			
			taLogger.log(crmOrderId, String.valueOf(orderContext.getOrderId()), 01, "============== Som BillingSI Order Creation Milestone ===============", "osmsomlogging");
//			taLogger.log(crmOrderId, String.valueOf(orderContext.getOrderId()), 01, inputData, "osmsomlogging");			

			/* 
			 * Insert order entry into the database 
			*/
			Document orderItemDocument = DOMUtils.somepathOfDoc(inputDocument, "OrderItem");			
			OSMTransactionEntity osmEntity = new OSMTransactionEntity();
			String orderState = DOMUtils.retrieveValuePath(inputDocument, "GetOrder.Response/OrderState");

			// original order
//			if (DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemXMLData/LineItem/OrderHeader/RevisionNumber").equals("1"))
			if (!orderState.equals("closed.completed"))
			{
				// Yohan : 20131105 : Bug 20386 : Additional OpCo Flag to support MOLI status update
				String opCoFlag = "N";
				String opCoProvider = DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemXMLData/LineItem/OrderHeader/OpCoProvider");
				if (opCoProvider != null && (opCoProvider.equalsIgnoreCase("M1") || opCoProvider.equalsIgnoreCase("NC")))
					opCoFlag = "Y";
				
				osmEntity.setOpCoFlag(opCoFlag);
				
				// end Bug 20386
				
				String crmOrderHeaderId = DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemXMLData/LineItem/OrderHeader/CRMOrderID");
				osmEntity.setCrmOrderId(crmOrderHeaderId);
				
				setEntryElements(osmEntity, orderItemDocument, orderContext);
				osmEntity.setParentTransactionId(DOMUtils.retrieveValuePath(inputDocument, "GetOrder.Response/_root/corrID").split("_")[0]);
	//			InsertUtil.printLogInsert(osmEntity, orderContext, taLogger, "osmsomlogging");
				InsertUtil.insertOSMTransaction(osmEntity, OSMTransactionEntity.SQL_INSERT_OSM_TRANS);
				
				orderContext.updateOrderData(MessageFormat.format(updateOrderCommand, String.valueOf(orderContext.getOrderId())));
				
			}
			else { // revision order
				/* Update SOM revision number*/
				osmEntity.setSourceTransactionId(DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemXMLData/LineItem/OrderHeader/OrderID"));
				osmEntity.setTransactionType("SOM");		
//				osmEntity.setOrderLineItemId(DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemID"));
//				String originalOrderId = RetrieveOriginalOrderId(osmEntity, orderContext);
//				osmEntity.setOrderId(originalOrderId);
				String cartridgeName = orderContext.getNamespace();
				osmEntity.setInsertBy(cartridgeName);
				osmEntity.setRevision(DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemXMLData/LineItem/OrderHeader/RevisionNumber"));
				osmEntity.setUpdateDateTime(DateTimeUtil.format(new Date(), DateTimeUtil.DATABASE_FORMAT_DATE));
				UpdateUtil.updateOSMTransaction(osmEntity, OSMTransactionEntity.SQL_UPDATE_REVISION_OSM_TRANS);
			}

			taLogger.log(crmOrderId, String.valueOf(orderContext.getOrderId()), 01, "END SomBillingSIOrderCreationMilestone", "osmsomlogging");

		} catch (Exception e){
			
			try {	
				if (crmOrderId.equals(""))
					crmOrderId = "unidentified";
				String exception = exceptionToString(e);
				String errMessage = "Order Creation Milestone";
				insertErrorException(crmOrderId, exception, orderContext, errMessage);
				
			} catch (IOException ex){
				ex.printStackTrace();
			}
			taLogger.log(crmOrderId, String.valueOf(orderContext.getOrderId()), 01, "ERROR", "osmsomlogging", e);
		} 
	}
	
	private static String removeNamespaceAndPreamble(String xmlString) {
		// Original  
//		return xmlString.replaceAll("(<\\?[^<]*\\?>)?", ""). /* remove preamble */
//		  replaceAll(" xmlns.*?(\"|\').*?(\"|\')", "")  /* remove xmlns declaration */
//		  .replaceAll("(<)(\\w+:)(.*?>)", "$1$3").replaceAll("im:", "") /* remove opening tag prefix */
//		  .replaceAll("(</)(\\w+:)(.*?>)", "$1$3"); /* remove closing tags prefix */
		
		// Tuned
		return xmlString.replaceAll("<\\?.+?>", "") /* remove preamble */
			   .replaceAll(" xmlns.+?(\"|\').+?(\"|\')", "") /* remove xmlns declaration */
			   .replaceAll("(<)(\\w+:)(.+?>)", "$1$3").replaceAll("im:", "") /* remove opening tag prefix */
			   .replaceAll("(</)(\\w+:)(.+?>)", "$1$3"); /* remove closing tags prefix */
	}

	private static void setEntryElements (OSMTransactionEntity osmEntity, Document orderItemDocument, OrderNotificationContext orderContext) throws Exception {
		
		osmEntity.setOrderId(String.valueOf(orderContext.getOrderId()));
		osmEntity.setSourceTransactionId(DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemXMLData/LineItem/OrderHeader/OrderID"));
		osmEntity.setTransactionType("SOM");
		osmEntity.setPollStatus("N");
	
		osmEntity.setRevision(DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemXMLData/LineItem/OrderHeader/RevisionNumber"));
		osmEntity.setOrderLineItemId(DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemID"));
		osmEntity.setMainOrderLineItemId(DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/ParentLineItemID"));
		osmEntity.setStatus("Created");
		osmEntity.setInsertDateTime(DateTimeUtil.format(new Date(), DateTimeUtil.DATABASE_FORMAT_DATE));
		osmEntity.setInsertBy(orderContext.getNamespace());
		osmEntity.setUpdateDateTime(DateTimeUtil.format(new Date(), DateTimeUtil.DATABASE_FORMAT_DATE));
		osmEntity.setUpdateBy(orderContext.getNamespace());
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