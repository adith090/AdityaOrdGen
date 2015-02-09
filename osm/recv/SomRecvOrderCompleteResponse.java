package com.m1.sg.osm.recv;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.sg.osm.database.dao.DaoFactory;
import com.m1.sg.osm.database.dao.common.CommonDao;
import com.m1.sg.osm.database.entity.common.ErrorLoggingEntity;
import com.m1.sg.osm.database.entity.common.OSMTransactionEntity;
import com.m1.sg.osm.util.DOMUtils;
import com.m1.sg.osm.util.DateTimeUtil;
import com.m1.sg.osm.util.SOAPUtil;
import com.m1.sg.osm.util.XqueryUtil;
import com.mslv.oms.automation.AutomationContext;
import com.mslv.oms.automation.AutomationException;
import com.mslv.oms.automation.OrderUpdateException;
import com.mslv.oms.automation.TaskContext;
import com.mslv.oms.automation.plugin.AbstractAutomator;
import common.util.TALogger;

/*
 * The SomRecvOrderCompleteResponse class : 
 * 1. Merges original message from COM and response message from TOM,
 * 2. Prepares the order completion message to COM, and
 */

/******************************************************************************* 
 * MODIFICATION HISTORY
 ******************************************************************************* 
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 ******************************************************************************* 
 * 12/06/2012					Aditya					Created
 ******************************************************************************/
public class SomRecvOrderCompleteResponse extends AbstractAutomator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void run(String inputData, AutomationContext context)
			throws AutomationException {
		// TODO Auto-generated method stub
		
		String crmOrderId = "";
		String exitStatus = "";
		String functionName = "";
		TALogger taLogger = TALogger.getTALogger();
		TaskContext taskContext = (TaskContext) context;
		
		try {
			String orderErase = removeNamespaceAndPreamble(taskContext.getOrder());
			Document getOrderDocument = DOMUtils.createDOMDocument(orderErase);
			Document orderItemRefDocument = DOMUtils.somepathOfDoc(getOrderDocument, "orderItemRef");
			crmOrderId = DOMUtils.retrieveValuePath(orderItemRefDocument, "orderItemRef/LineItemXMLData/LineItem/OrderHeader/OrderID");
			
			taLogger.log(crmOrderId, String.valueOf(taskContext.getOrderId()), 01, "Enter SomFibreBillingSIOrderCompleteResponse", "osmsomlogging");
//			taLogger.log(crmOrderId, String.valueOf(taskContext.getOrderId()), 01, inputData, "osmsomlogging");			
						
//			Document responseMessage = DOMUtils.createDOMDocument(removeIndex(SOAPUtil.extractSOAPResponse(inputData)));
//						
//			functionName = getFunctionName(getOrderDocument);								
//			
//			String osmIndexOfOrderItemText = responseMessage.getElementsByTagName("OSMIndexOfOrderItem").item(0).getChildNodes().item(0).getNodeValue();
//			String osmReferenceIndex = responseMessage.getElementsByTagName("OSMReferenceIndex").item(0).getChildNodes().item(0).getNodeValue();
////			String indexOfOrderItem = osmIndexOfOrderItemText.split("\\|")[osmIndexOfOrderItemText.split("\\|").length-1];
//						
//			DOMUtils.createElementUnderParent(responseMessage, "OSMIndexOfOrderItem", osmIndexOfOrderItemText, "ProvRequest/ListOfRef/LineItemXML/LineItem");
//			DOMUtils.createElementUnderParent(responseMessage, "OSMReferenceIndex", osmReferenceIndex, "ProvRequest/ListOfRef/LineItemXML/LineItem");
//			DOMUtils.createElementUnderParent(responseMessage, "SOMOrderID", String.valueOf(taskContext.getOrderId()), "ProvRequest/ListOfRef/LineItemXML/LineItem");
//			
			
			/* Data synchronization Mr.Gun 19/03/2013 */
//			Document getOrderExtendDocument = DOMUtils.createDOMDocument(orderErase);
//			DOMUtils.createElementUnderParent(getOrderExtendDocument, "TomResponseMessage", "", "GetOrder.Response/_root");
//			DOMUtils.createStructureUnderElement(getOrderExtendDocument, getOrderExtendDocument.getElementsByTagName("TomResponseMessage").item(0), responseMessage);
//			
//			Element provRequest = (Element) getOrderExtendDocument.getElementsByTagName("ProvRequest").item(0);
//			provRequest.removeAttribute("xmlns");
//			
//			String inputExtend = DOMUtils.convertDocumenttoString(getOrderExtendDocument);		
//			String inputNonstatic = new String(inputExtend);
//			
//			taLogger.log(crmOrderId, String.valueOf(taskContext.getOrderId()), 01, "Sync input\n" + inputNonstatic, "osmsomlogging");
//			
//			String resultXquery = XqueryUtil.executeXQuery(inputNonstatic, "script/xquery/common/SomFibreRSPServiceProv_SyncField.xqy");
			
//			taLogger.log(crmOrderId, String.valueOf(taskContext.getOrderId()), 01, "Sync result\n" + resultXquery, "osmsomlogging");
//			deletePendingOrderId(getOrderDocument, responseMessage, taskContext);

//			taskContext.updateOrderData(resultXquery);
			
			updateParRecvFlag(crmOrderId, DOMUtils.createDOMDocument(inputData).getElementsByTagName("TOMOrderId").item(0).getChildNodes().item(0).getNodeValue(), taskContext);

			exitStatus = "success";
			taLogger.log(crmOrderId, String.valueOf(taskContext.getOrderId()), 01, "End SomBillingOrderCompleteResponse", "osmsomlogging");	

		} catch (Exception e){
			
			System.out.println("========================== Get The Exception ========================");
			
			try {
				if (crmOrderId.equals(""))
					crmOrderId = "unidentified";
				String exception = exceptionToString(e);
				String errMessage = "TOM response to SOM";
				insertErrorException(crmOrderId, exception, taskContext, errMessage);
				
			} catch (IOException ex){
				ex.printStackTrace();
			}
			taLogger.log(crmOrderId, String.valueOf(taskContext.getOrderId()), 01, "ERROR", "osmsomlogging", e);
			
		} finally {
			if(taskContext != null && !exitStatus.equals("")){
				taskContext.completeTaskOnExit(exitStatus);
			}
		}				
	}

	
	private static String removeNamespaceAndPreamble(String xmlString) {
		// Original  
//		return xmlString.replaceAll("(<\\?[^<]*\\?>)?", ""). /* remove preamble */
//		  replaceAll(" xmlns.*?(\"|\').*?(\"|\')", "")  /* remove xmlns declaration */
//		  .replaceAll("(<)(\\w+:)(.*?>)", "$1$3") /* remove opening tag prefix */
//		  .replaceAll("(</)(\\w+:)(.*?>)", "$1$3"); /* remove closing tags prefix */
		
		// Tuned
		return xmlString.replaceAll("<\\?.+?>", "") /* remove preamble */
			   .replaceAll(" xmlns.+?(\"|\').+?(\"|\')", "") /* remove xmlns declaration */
			   .replaceAll("(<)(\\w+:)(.+?>)", "$1$3") /* remove opening tag prefix */
			   .replaceAll("(</)(\\w+:)(.+?>)", "$1$3"); /* remove closing tags prefix */
	}
	
	private static void updateParRecvFlag(String crmOrderId, String tomOrderId, TaskContext taskContext){
		
		CommonDao commonDao = DaoFactory.getDao(CommonDao.class);
		TALogger taLogger = TALogger.getTALogger();
		try {
			
			OSMTransactionEntity ote = new OSMTransactionEntity();
			
			ote.setOrderId(String.valueOf(tomOrderId));
			ote.setSourceTransactionId(crmOrderId);
			ote.setTransactionType("TOM");
			
			commonDao.create(ote, OSMTransactionEntity.SQL_UPDATE_PAR_RECV_FLAG_TRANS);
			
		} catch (Exception e){
			taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "ERROR", "osmsomlogging", e);
		} finally {
			if(commonDao != null){
				commonDao.close();
			}
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