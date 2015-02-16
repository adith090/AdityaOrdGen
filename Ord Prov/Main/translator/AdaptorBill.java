package com.m1.bcc.spl.translator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.springframework.integration.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.SPLCommonComponent;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 12/09/2013					Ravikumar G				changed error handling logic
 * 12/11/2013					Ravikumar G				Bug#20421 - to implement multithreading using thread poll task executor
 ******************************************************************************/

public class AdaptorBill {

	JdbcDatabaseDAO jdbcDatabaseDAO;
	private ThreadPoolTaskExecutor taskExecutor;

	TALogger taLogger = TALogger.getTALogger();
	String loggercategory = ApplicationConstants.LOGGER_CMD_PARAMETER;



	public void setJdbcDatabaseDAO(JdbcDatabaseDAO jdbcDatabaseDAO)
	{
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	
	@Async	
	@SuppressWarnings("rawtypes")
	public ArrayList<?> handleMessage(Message<ArrayList<Map<String, Object>>> message) throws TransformerConfigurationException, ParserConfigurationException, TransformerException {
		ArrayList<Map<String, Object>> messages = (ArrayList<Map<String, Object>>) message.getPayload();
		System.out.println("orderList " + messages.size());
		for(final Map<String, Object> orderMap : messages) {
			taskExecutor.submit(new Runnable() {
				
				@Override
				public void run() {
					processAdaptorCmd(orderMap);
				}
			});
		}
		return null;
	}

	private void processAdaptorCmd(Map<String, Object> map) {
		String cmdRefId="";
		String rowId="";
		String transId="";
		String srcTransId="";
		String status;
		String billOrderId=""; 
		String funcRefId="";
		String billCmdTransRowId="";
		String corrId="";
		String opgRefId="";
			
		//ArrayList<?> messages = (ArrayList<?>) message.getPayload();
	
		//for (Object mess : messages) {
			
			try {
				
				System.out.println("inside AdaptorBill");
				
				rowId = (String) map.get("ROW_ID");				
				cmdRefId = (String) map.get("CMD_REF_ID");				
				transId = (String) map.get("TRANS_ID");	
				srcTransId=(String) map.get("SRC_TRANS_ID");
				billOrderId = (String) map.get("BILL_ORDER_ID");
				
				taLogger.log(srcTransId, transId, cmdRefId, ApplicationConstants.LOG_DEBUG,"[AdaptorBill][handleMessage] rowId =" + rowId + "cmdRefId=" +cmdRefId + "transId=" +transId + "srcTransId=" +srcTransId,loggercategory);
				
				jdbcDatabaseDAO.updateOrderTransBill(rowId, srcTransId, transId);
				
				List<Map<String, Object>> paramList=jdbcDatabaseDAO.getBillCmdTransDtlsValues(transId, srcTransId, rowId);
				
				//rowId - send cmd row id to api calls
				BillApiHandler billApiHandler = new BillApiHandler();
				Object ApiCallObject = billApiHandler.callBillAPI(paramList, rowId,transId, srcTransId, cmdRefId,jdbcDatabaseDAO);
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[AdaptorBill][handleMessage] API call has been completed", loggercategory);
			}catch(Exception e){
				
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[AdaptorBill][handleMessage] Inside Catch", loggercategory, e);
				e.printStackTrace();
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[AdaptorBill][handleMessage] Error Inside Adaptor Bill method", loggercategory, e);
				jdbcDatabaseDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "Adaptor Bill", "ERR_CODE", SPLCommonComponent.getStackTrace(e));
				
				// Ravi: changed logic for error handling
				List<Map<String, Object>> rowIdList = jdbcDatabaseDAO.getBillCmdTransRowId(rowId, transId, srcTransId);
				String orderTransRowId="";

				for(Object list:rowIdList){
					Map row = (Map) list;
					billCmdTransRowId = (String) row.get("ROW_ID");
				}
				try {
					jdbcDatabaseDAO.saveBillCommandResponse("", "","", rowId, cmdRefId, transId, billCmdTransRowId, "",srcTransId, ApplicationConstants.TOM_RESPONSE_ERROR, e.getCause().getLocalizedMessage(),"");
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[AdaptorBill][handleMessage] Inside Catch Sent Fail response back to OSM", loggercategory, e);
			}
							
		//}
	}
}
