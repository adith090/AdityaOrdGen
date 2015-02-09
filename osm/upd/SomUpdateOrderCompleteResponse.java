package com.m1.sg.osm.upd;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.m1.sg.osm.database.dao.DaoFactory;
import com.m1.sg.osm.database.dao.common.CommonDao;
import com.m1.sg.osm.database.entity.common.ErrorLoggingEntity;
import com.m1.sg.osm.database.entity.common.OSMTransactionEntity;
import com.m1.sg.osm.database.util.UpdateUtil;
import com.m1.sg.osm.util.DateTimeUtil;
import com.m1.sg.osm.util.XMLAPIUtil;
import com.m1.sg.osm.util.XMLParserUtilNonIndex;
import com.m1.sg.osm.util.ConnectionUtil;
import com.m1.sg.osm.util.DOMUtils;
import com.m1.sg.osm.util.SOAPUtil;
import com.mslv.oms.automation.AutomationContext;
import com.mslv.oms.automation.AutomationException;
import com.mslv.oms.automation.OrderNotificationContext;
import com.mslv.oms.automation.plugin.AbstractAutomator;
import common.util.TALogger;

/*
 * SomUpdateOrderCompleteResponse class will update the SOM order status to "Completed" and send a complete response to COM
 * */

/******************************************************************************* 
 * MODIFICATION HISTORY
 ******************************************************************************* 
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 ******************************************************************************* 
 * 20/08/2013					Aditya					Created
 * 
 ******************************************************************************/

public class SomUpdateOrderCompleteResponse extends AbstractAutomator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void run(String inputData, AutomationContext context) throws AutomationException {
		// TODO Auto-generated method stub
		
		String crmOrderId = "";
		TALogger taLogger = TALogger.getTALogger();
		OrderNotificationContext orderContext = (OrderNotificationContext) context;
		
		try {
			String orderErase = removeNamespaceAndPreamble(orderContext.getOrder());
			Document inputDocument = DOMUtils.createDOMDocument(orderErase);
			crmOrderId = DOMUtils.retrieveValuePath(DOMUtils.somepathOfDoc(inputDocument, "OrderItem"), "OrderItem/LineItemXMLData/LineItem/OrderHeader/OrderID");
			
			taLogger.log(crmOrderId, String.valueOf(orderContext.getOrderId()), 01, "============== Som BillingSI Order Complete Milestone ===============", "osmsomlogging");

//			Document controlDataDocument = XMLParserUtil.parseOSMControlData(orderErase);
			Document orderDocument = XMLParserUtilNonIndex.parseOSMControlData(orderErase);
			Document orderItemDocument = DOMUtils.somepathOfDoc(inputDocument, "OrderItem");

//			Document responseMessage = DOMUtils.createDOMDocument(SOAPUtil.extractSOAPResponse(inputData));
						
			// handle no action, indicated by the absence of SOMOrderID (created when receiving TOM complete response), can also check productspec
			if (orderDocument.getElementsByTagName("SOMOrderID").getLength() == 0) { 
				Document orderDocumentCopy = DOMUtils.createNewDOMDocument();
				DOMUtils.createElementUnderParent(orderDocumentCopy, "ControlData", "", "./");
				NodeList lineItemNodeList = orderDocument.getElementsByTagName("OrderItem");
				ArrayList<Document> listOfOrderItemDocument = generateDocumentOrderItem(orderDocument, lineItemNodeList);

				for(Document eachOfOrderLineItemDoc : listOfOrderItemDocument) {
//					String osmIndexOfOrderItem = eachOfOrderLineItemDoc.getElementsByTagName("OSMIndexOfOrderItem").item(0).getTextContent();
//					String osmReferenceIndex = eachOfOrderLineItemDoc.getElementsByTagName("OSMReferenceIndex").item(0).getTextContent();
//					DOMUtils.setTextNodeInElement(eachOfOrderLineItemDoc, "OrderItem/LineItemXMLData/LineItem/OSMIndexOfOrderItem", osmIndexOfOrderItem + "|" + osmIndexOfOrderItem);
//					DOMUtils.setTextNodeInElement(eachOfOrderLineItemDoc, "OrderItem/LineItemXMLData/LineItem/OSMReferenceIndex", osmReferenceIndex + "|" + osmReferenceIndex);
					DOMUtils.createElementUnderParent(eachOfOrderLineItemDoc, "SOMOrderID", String.valueOf(orderContext.getOrderId()), "OrderItem/LineItemXMLData/LineItem");
					DOMUtils.createStructureUnderElement(orderDocumentCopy, orderDocumentCopy.getElementsByTagName("ControlData").item(0), eachOfOrderLineItemDoc);
				}
				orderDocument.removeChild(orderDocument.getElementsByTagName("ControlData").item(0));
				orderDocument = DOMUtils.createDOMDocument(DOMUtils.convertDocumenttoString(orderDocumentCopy));
			}
			
			String responseMessageToCom = SOAPUtil.createSOAPRequest(DOMUtils.convertDocumenttoString(orderDocument));
			
			/* Send complete order response to COM */
			ConnectionUtil.SendMessageToJMSQueue("jmsqueue/om/osm/OrderCompleteResponse", 
			responseMessageToCom
			, inputDocument.getElementsByTagName("corrID").item(0).getChildNodes().item(0).getNodeValue()
			, inputDocument.getElementsByTagName("JMSType").item(0).getChildNodes().item(0).getNodeValue());
			
			/* Update the status to completed */
			OSMTransactionEntity osmEntity = new OSMTransactionEntity();
			osmEntity.setTransactionType("SOM");
			osmEntity.setPollStatus("N");
			osmEntity.setStatus("Completed");
			osmEntity.setOrderId(String.valueOf(orderContext.getOrderId()));
			osmEntity.setSourceTransactionId(DOMUtils.retrieveValuePath(orderItemDocument, "OrderItem/LineItemXMLData/LineItem/OrderHeader/OrderID"));
			osmEntity.setUpdateDateTime(DateTimeUtil.format(new Date(), DateTimeUtil.DATABASE_FORMAT_DATE));
			UpdateUtil.updateOSMTransaction(osmEntity, OSMTransactionEntity.SQL_UPDATE_STATUS_OSM_TRANS);

			XMLAPIUtil.addSaveResponseMessage(responseMessageToCom, orderContext);
			
		} catch (Exception e){
			
			try {	
				if (crmOrderId.equals(""))
					crmOrderId = "unidentified";
				String exception = exceptionToString(e);
				String errMessage = "Order Completion Milestone";
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
//		  .replaceAll("(<)(\\w+:)(.*?>)", "$1$3") /* remove opening tag prefix */
//		  .replaceAll("(</)(\\w+:)(.*?>)", "$1$3"); /* remove closing tags prefix */
		
		// Tuned
		return xmlString.replaceAll("<\\?.+?>", "") /* remove preamble */
			   .replaceAll(" xmlns.+?(\"|\').+?(\"|\')", "") /* remove xmlns declaration */
			   .replaceAll("(<)(\\w+:)(.+?>)", "$1$3") /* remove opening tag prefix */
			   .replaceAll("(</)(\\w+:)(.+?>)", "$1$3"); /* remove closing tags prefix */
	}

	private static ArrayList<Document> generateDocumentOrderItem(Document controlDataDocument, NodeList orderItemList) throws ParserConfigurationException {
		ArrayList<Document> listOfOrderItemDocument = new ArrayList<Document>();
		
		for(int i=0; i < orderItemList.getLength(); i++){
			listOfOrderItemDocument.add(DOMUtils.somepathOfDoc(controlDataDocument, orderItemList.item(i)));
		}
		
		return listOfOrderItemDocument;
		
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