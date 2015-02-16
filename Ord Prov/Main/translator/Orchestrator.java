package com.m1.bcc.spl.translator;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import com.m1.bcc.spl.sender.OrderTransactionSender;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 12/09/2013					Ravikumar G				added new column crm_svc_id in bill_order_trans and updated
 * 13/09/2013					Ravikumar G				implemented logic for fire order by minimum sequence no
 * 12/11/2013					Ravikumar G				Bug#20421 - to implement multithreading using thread poll task executor
 * 18/11/2013					Ravikumar G				Bug#20568: changed method for billing to post message in different JMS type
 ******************************************************************************/

public class Orchestrator {
	
	JdbcDatabaseDAO jdbcDatabaseDAO;
	private ThreadPoolTaskExecutor taskExecutor;

	TALogger taLogger = TALogger.getTALogger();
	String loggercategory = ApplicationConstants.LOGGER_CMD_PARAMETER;

	//For logging purposes
	//String inputIdentifier = "";


	public void setJdbcDatabaseDAO(JdbcDatabaseDAO jdbcDatabaseDAO)
	{
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}
	
	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	
	@Async
	public ArrayList<?> handleMessage(Message<ArrayList<Map<String, Object>>> message) throws TransformerConfigurationException, ParserConfigurationException, TransformerException {
		
		ArrayList<Map<String, Object>> messages = (ArrayList<Map<String, Object>>) message.getPayload();
		System.out.println("orderList " + messages.size());
		for(final Map<String, Object> orderMap : messages) {
			taskExecutor.submit(new Runnable() {
				
				@Override
				public void run() {
					processBillingCmd(orderMap);
				}
			});
		}
		return null;
	}
	
	private void processBillingCmd(Map<String, Object> map) {
		String cmdRefId="";
		String rowId="";
		String transId="";
		String srcTransId="";
		String status=ApplicationConstants.STATUS_NOT_STARTED;
		String pollStatus = "N";
		String returnCode="";
		String returnMsg="";
		Date effectiveDt;
		String crmSvcId = "";
		String billOrderId="";
		Date insertDt=new Date();
		String insertBy="";
		String updatedBy="";
		Date updatedDt=new Date();
		String effectiveDate; 
		String funcRefId="";
		String opgRefId="";
		String corrId="";
		String BillCmdTransRowId="";
		BigDecimal seqNo=null;
		String orderRowId="";
		
		//ArrayList<?> messages = (ArrayList<?>) message.getPayload();
		
			//for (Object mess : messages) {
				
			try {
					
				taLogger.log("", "", "", ApplicationConstants.LOG_DEBUG,"[Orchestrator][handleMessage] Inside Orchestrator" ,loggercategory);
				//Map map = (Map) mess;
				
				orderRowId = (String) map.get("ORDER_ROW_ID");	
				//BillCmdTransRowId= (String) map.get("ROW_ID");
				cmdRefId = (String) map.get("CMD_REF_ID");				
				transId = (String) map.get("TRANS_ID");	
				crmSvcId =(String) map.get("CRM_SVC_ID");					
				srcTransId=(String) map.get("SRC_TRANS_ID");
				taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_DEBUG,"[Orchestrator][handleMessage] BillCmdTransRowId="+BillCmdTransRowId+" cmdRefId="+cmdRefId+" transId="+transId+" crmSvcId=" + crmSvcId+" srcTransId="+srcTransId ,loggercategory);
				effectiveDt = (Date) map.get("EFFECTIVE_DT");
				
				SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy");
				effectiveDate = formatter.format(effectiveDt);
				taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_DEBUG,"[Orchestrator][handleMessage] = effectiveDt" + effectiveDt ,loggercategory);
				taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_DEBUG,"[Orchestrator][handleMessage] = EffectiveDate" + effectiveDate ,loggercategory);
				
				List<Map<String, Object>> list=  jdbcDatabaseDAO.getBillCmdTransValues(cmdRefId, transId, crmSvcId, effectiveDate);
				
				for(Object id:list){
					
					Map map1 = (Map) id;
					rowId=(String) map1.get("ROW_ID");
					srcTransId=(String) map1.get("SRC_TRANS_ID");
					seqNo=(BigDecimal) map1.get("SEQ_NO");
				}							

				jdbcDatabaseDAO.updateCommandTransBill(cmdRefId, transId, crmSvcId, effectiveDate,srcTransId);
				
				taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_DEBUG,"[Orchestrator][handleMessage]ROW_ID="+rowId+", cmdRefId="+cmdRefId+", transId="+transId+", srcTransId="+srcTransId+", status="+status+", pollStatus="+pollStatus+
						", returnCode="+returnCode+", returnMsg="+returnMsg+", billOrderId="+billOrderId+", INSERT_DT="+insertDt+
						", INSERT_BY="+insertBy+", UPDATED_DT="+updatedDt+", UPDATED_BY="+updatedBy ,loggercategory);
				
				
				// Ravi: added new column crm_svc_id and updated
				String billOrderTransRowId = jdbcDatabaseDAO.insertOrderTransBill("" , cmdRefId, transId, srcTransId,seqNo, billOrderId, status, pollStatus, returnCode, "", insertDt, insertBy, updatedDt, updatedBy, crmSvcId, loggercategory);
				taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_DEBUG,"[Orchestrator][handleMessage] OrderTransRowId=" + billOrderTransRowId ,loggercategory);
				
				
				jdbcDatabaseDAO.updateBillOrderTransRowId(billOrderTransRowId, transId, cmdRefId, crmSvcId, effectiveDate, srcTransId);
				
				
				/*Ravi: added to check, any c1 order status in error for same srcTransId and crmSvcId. 
				if any c1 order in error then wont update another c1 order to New for same srcTransId and crmSvcId */
				int statusCount = jdbcDatabaseDAO.checkC1OrderForError(srcTransId, transId, rowId, crmSvcId, loggercategory);
				if(statusCount==0)
					jdbcDatabaseDAO.updateC1OrderStatusToNew(srcTransId,transId,rowId, crmSvcId, loggercategory);

			}catch(Exception e){
					
					taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[Orchestrator][handleMessage] Inside Catch", loggercategory, e);
					e.printStackTrace();
					taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[Orchestrator][handleMessage] Error Inside Instructor Handler method", loggercategory, e);

					jdbcDatabaseDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "Orchestrator", "ERR_CODE", SPLCommonComponent.getStackTrace(e));

					if (rowId != null) {

						//update error status in t_om_cmd_trans table
						jdbcDatabaseDAO.updateBillcmdTransError(rowId, "1", e.getMessage(),loggercategory);
					}

					//update error status in t_om_order_trans_dtls table
					String orderTransRowId = jdbcDatabaseDAO.getBillCmdTransOrderRowId(cmdRefId, transId, srcTransId, rowId);
					jdbcDatabaseDAO.orderTransDetailsErrorUpdate(orderTransRowId);


					SPLCommonComponent splCommonComponent = null;
					try {
						splCommonComponent = new SPLCommonComponent(taLogger, loggercategory);
					} catch (IOException ioException) {
						taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[Orchestrator][handleMessage] Error instantiating SPLCommonComponent", loggercategory, ioException);
					}
					
					List<Map<String,Object>> orderList = jdbcDatabaseDAO.getOrderTransDtlsValues(orderTransRowId, transId, srcTransId);
					
					for(Object list:orderList){

						Map funcMap = (Map) list;
						funcRefId=(String) funcMap.get("FUNC_REF_ID");
						opgRefId=(String) funcMap.get("OPG_REF_ID");
						corrId= (String) funcMap.get("CORR_ID");
					}
					String tomResponse = "";
					try {
						tomResponse = splCommonComponent.createTomResponse("responseCode",""+e.getMessage(),corrId,transId,opgRefId,funcRefId,cmdRefId);
					} catch (Exception e1) {
						e1.printStackTrace();
					}

					OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
					//orderTransactionSender.postMessage(tomResponse, corrId);
					orderTransactionSender.postBillingResponse(tomResponse, corrId);

					taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[Orchestrator][handleMessage] Inside Catch Sent Fail response back to OSM", loggercategory, e);
			}
				
				
			//}
	}
}
