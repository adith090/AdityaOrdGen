package com.m1.bcc.spl.dao;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.w3c.dom.Document;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.instructor.CommandParamRefDtls;
import com.m1.bcc.spl.instructor.CommandParameters;
import com.m1.bcc.spl.instructor.HandlerVariables;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.model.OrderTransactonDetail;
import com.m1.bcc.spl.model.OrderTransactonDetailBill;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.sender.OrderTransactionSender;
import com.m1.bcc.spl.translator.DBAdaptorStoredProc;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import com.m1.bcc.spl.util.ThreadLocalInstance;
import common.util.TALogger;


/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/11/2012					Ravikumar G				Created
 * 22/02/2013					Ravikumar G				Modified for logging cleanup
 * 19/04/2013					Billy Lim				Bug 1954. Check variable xmlEleName in function updateTaskResponseDetails(...) for null
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 13/05/2013					Ravikumar G				Bug#2116: moved method updateOrderTransactionDetails exception handling to OrderTransactionListener. 
 * 														Updated logging.
 * 16/05/2013					Billy Lim				Bug 2142 [Internal] For RETRY TaskRequest, return TOM Success Response when Order is in Completed status in T_OM_ORDER_TRANS_DTLS
 * 17/05/2013					Sudharsan				Implemented Engineering Authentication server
 * 21/05/2013					Ravikumar G				Bug#2184 : Update the STUB flag in cmd_trans table
 * 22/05/2013					Sudharsan				Implemented Radius server
 * 27/05/2013   				Sudharsan				Implemented ADS
 * 17/06/2013					Ravikumar G				Bug#2346: Added condition in updateCommandTransactionStatus to check if status is New then update to Received
 * 17/09/2013					Ravikumar G				Implemented skip, retry logic for billing commands
 * 20/09/2013					Ravikumar G				defect#188:commented updateNextC1Order and added new method for sequence
 * 27/09/2013					Ravikumar G				defect#203:order trans dtls stuck in Received status
 * 17/10/2013					Ravikumar G				Added queryString variable for the interface Http GET method
 * 20/11/2013					Ravikumar G				Bug#20538 - modified method location and httpmethod, added method to get timeout and read timeout
 * 29/11/2013					Ravikumar G				Bug#20592- Changed for DB Adaptor design change
 * 04/12/2013					Ravikumar G				Bug#21221 - Removed unused imports, commented codes
 * 12/12/2013					Ravikumar G				Bug#21569 - Updated c_om_cmd_dest_ref to c_om_interface_ref
 * 31/12/2013					Ravikumar G				Bug#22498 - Added priority column in t_om_order_trans_dtls
 * 16/01/2014					Ravikumar G				Bug#23590 - Added method to update request message in cmd_trans table
 * 17/01/2014					Ravikumar G				Bug#23710 - Updated cmd_trans column row_id type to number
 * 23/01/2014					Ravikumar G				Bug#24043 - Updated for column row_id type to number
 * 24/01/2014					Ravikumar G				Bug 24267 - updated the sql for update bill_order_trans Skip functionality in method updateCOneSkipRetry
 * 27/01/2014					Ravikumar G				Bug#23086 - updated transaction for ADS provisioning and added condition if source=OSM then send tomresponse
 * 31/01/2014					Ravikumar G				Bug#24614 - Updated to get OPCO billing datasource and for call opco stored procedure
 * 03/02/2014					Ravikumar G				Bug#21970 - Added method updateStartHubApprovalFlag to update starhub Approval flag current value
 ******************************************************************************/

public class DatabaseDAO {

	private DataSource dataSource;
	private DataSource dbAdaptorDataSource;
	private DataSource opcoBillingDataSource;
	private DataSourceTransactionManager dataSourceTransactionManager;
	private DataSourceTransactionManager dbAdaptorTransactionManager;
	private DataSourceTransactionManager opcoBillingTransactionManager;
	OrderTransactionSender orderTransactionSender;
	TALogger taLogger;
/*	String sysName;
	String intfName;
	private String queryString;

	public synchronized String getSysName() {
		return sysName;
	}

	public synchronized void setSysName(String sysName) {
		this.sysName = sysName;
	}
	
	public synchronized String getIntfName() {
		return intfName;
	}

	public synchronized void setIntfName(String intfName) {
		this.intfName = intfName;
	}

	public synchronized String getQueryString() {
		return queryString;
	}
	
	public synchronized void setQueryString(String queryString) {
		this.queryString = queryString;
	}*/
		/**
	 * SQL Query
	 */

	private static String SQL_PARAM_NAME_COUNT = "SELECT COUNT(*) AS COUNT FROM T_OM_CMD_TRANS_DTLS WHERE PAR_ROW_ID=? AND PARAM_NAME=?";
	//private static String SQL_GET_LOCATION = "SELECT LOCATION FROM C_OM_CMD_DEST_REF WHERE SYSTEM_NAME=?";
	//private static String SQL_CMD_DEST_REF_DTLS = "SELECT USER_NAME, PWD, ACCOUNT_TYPE FROM C_OM_CMD_DEST_REF WHERE INTERFACE_NAME=?";
	private static String SQL_GET_LOCATION = "SELECT LOCATION FROM C_OM_INTERFACE_REF WHERE SYSTEM_NAME=?";
	private static String SQL_CMD_DEST_REF_DTLS = "SELECT USER_NAME, PWD, ACCOUNT_TYPE FROM C_OM_INTERFACE_REF WHERE INTERFACE_NAME=?";

	public DatabaseDAO() {
		taLogger = TALogger.getTALogger();
	}

	/**
	 *
	 * @param dataSourceTransactionManager
	 */
	public void setTransactionManager(DataSourceTransactionManager dataSourceTransactionManager) {
		this.dataSourceTransactionManager = dataSourceTransactionManager;
		dataSource = dataSourceTransactionManager.getDataSource();
	}

	/**
	 * Method added to get CRM Data source - Bug#20592
	 * @param dbAdaptorTransactionManager
	 */
	public void setDbAdaptorTransactionManager(DataSourceTransactionManager dbAdaptorTransactionManager) {
		this.dbAdaptorTransactionManager = dbAdaptorTransactionManager;
		dbAdaptorDataSource = dbAdaptorTransactionManager.getDataSource();
	}
	
	/**
	 * Method added to get opco billing Data source - Bug#24614
	 * @param dbAdaptorTransactionManager
	 */
	public void setOpcoBillingTransactionManager(DataSourceTransactionManager opcoBillingTransactionManager) {
		this.opcoBillingTransactionManager  = opcoBillingTransactionManager;
		opcoBillingDataSource = opcoBillingTransactionManager.getDataSource();
	}

	/**
	 *
	 * @return jdbcTemplate
	 */
	public JdbcTemplate getJdbcTemplate() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		return jdbcTemplate;
	}

	/**
	 *
	 * @param orderTransactonDetail
	 */
	public void saveOrderTransactionDetails(OrderTransactonDetail orderTransactonDetail) {
		try {
			taLogger.log("CorrelationID="+orderTransactonDetail.getCorrId(), ApplicationConstants.LOG_DEBUG,
					"taskRequest=" + orderTransactonDetail.getTaskRequest() + ", tomOrderId=" + orderTransactonDetail.getOrderId() + ", " +
							"crmOrderId=" + orderTransactonDetail.getCrmOrderId(),"osmlogging");
			JdbcTemplate jdbcTemplate = getJdbcTemplate();
			String sqlQuery = "INSERT INTO T_OM_ORDER_TRANS_DTLS (" +
					"TRANS_ID, SRC_TRANS_ID, ORDER_XML, FUNC_REF_ID, OPG_REF_ID, STATUS, POLL_STATUS, CORR_ID, C1_FLAG, PRIORITY, SOURCE, " +
					"INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate,user,sysdate,user)";
			Object sqlParams[] = new Object[] {orderTransactonDetail.getOrderId(), orderTransactonDetail.getCrmOrderId(), orderTransactonDetail.getOrderXml(),
					orderTransactonDetail.getFuncRefId(), orderTransactonDetail.getOpgRefId(), orderTransactonDetail.getStatus(), orderTransactonDetail.getPollStatus(),
					orderTransactonDetail.getCorrId(),orderTransactonDetail.getCOneFlag(), orderTransactonDetail.getPriority(), orderTransactonDetail.getSource()};
			int[] sqlTypes = new int[] {Types.VARCHAR, Types.VARCHAR, Types.CLOB, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};

			jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
		}catch(Exception exception) {
			exception.printStackTrace();
			taLogger.log("Inside saveOrderTransactionDetails Method - Catch ", ApplicationConstants.LOG_ERROR,"","osmlogging",exception);
		}
	}

	/**
	 *
	 * @param orderTransactonDetail
	 */
	public void saveOrderTransactionDetailsErrorTask(OrderTransactonDetail orderTransactonDetail) {
		try {

			JdbcTemplate jdbcTemplate = getJdbcTemplate();
			String sqlQuery = "INSERT INTO T_OM_ORDER_TRANS_DTLS (" +
					"TRANS_ID, SRC_TRANS_ID, ORDER_XML,FUNC_REF_ID, OPG_REF_ID, STATUS, POLL_STATUS, CORR_ID, PRIORITY, SOURCE, " +
					"INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate,user,sysdate,user)";
			Object sqlParams[] = new Object[] {orderTransactonDetail.getOrderId(), orderTransactonDetail.getCrmOrderId(), orderTransactonDetail.getOrderXml(),
					ApplicationConstants.ERROR_TASK_FUNCREFID,ApplicationConstants.ERROR_TASK_OPGREFID, orderTransactonDetail.getStatus(), orderTransactonDetail.getPollStatus(),
					orderTransactonDetail.getCorrId(), orderTransactonDetail.getPriority(), orderTransactonDetail.getSource()};
			int[] sqlTypes = new int[] {Types.VARCHAR, Types.VARCHAR, Types.CLOB, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};

			jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
			taLogger.log("Inside saveOrderTransactionDetailsErrorTask Method - Record inserted ", ApplicationConstants.LOG_DEBUG,"sqlQuery="+sqlQuery,ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);


		} catch (Exception exception) {
			exception.printStackTrace();
			taLogger.log(
					"Inside saveOrderTransactionDetailsErrorTask Method - Catch ",
					ApplicationConstants.LOG_ERROR,
					"",
					ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING,
					exception);
		}
	}

		/**
	 *
	 *
	 * @param orderTransactonDetail
	 */
	public void updateOrderTransactionDetails(OrderTransactonDetail orderTransactonDetail) throws Exception {
		// Bug#2116: commented try and catch, added throws and Updated logging - Start
			JdbcTemplate jdbcTemplate = getJdbcTemplate();
			String taskRequest = orderTransactonDetail.getTaskRequest();
			String corrId = orderTransactonDetail.getCorrId();
			String transid = orderTransactonDetail.getOrderId();
			//get srcTransId for logging
			String srcTransid = orderTransactonDetail.getCrmOrderId();
			String cOneFlag = orderTransactonDetail.getCOneFlag();
			taLogger.log(srcTransid, transid, ApplicationConstants.LOG_DEBUG, "Inside updateOrderTransactionDetails Method", "osmlogging");
			
			// Ravi: Added for billing commands
			if(ApplicationConstants.C1_FLAG_YES.equalsIgnoreCase(cOneFlag)) {
				updateCOneSkipRetry(orderTransactonDetail, jdbcTemplate);
			}else {
				if (taskRequest.equalsIgnoreCase(ApplicationConstants.STATUS_CANCELLED)) {
					jdbcTemplate
							.update("update T_OM_ORDER_TRANS_DTLS set STATUS = '"+ApplicationConstants.STATUS_CANCELLED+"', UPDATED_DT=?, UPDATED_BY=? where TRANS_ID = ? AND SRC_TRANS_ID=?",
									new Object[] {new Date(), orderTransactonDetail.getUpdatedBy(), orderTransactonDetail.getOrderId(), srcTransid});
					jdbcTemplate
							.update("update T_OM_CMD_TRANS set STATUS = '"+ApplicationConstants.STATUS_CANCELLED+"', UPDATED_DT=?, UPDATED_BY=? where TRANS_ID = ? AND SRC_TRANS_ID=? and STATUS not in ('"+ApplicationConstants.STATUS_COMPLETED+"',"+" '"+ApplicationConstants.STATUS_ERROR+"')",
									new Object[] {new Date(), orderTransactonDetail.getUpdatedBy(), orderTransactonDetail.getOrderId(), srcTransid});
	
				} else if (taskRequest.equalsIgnoreCase(ApplicationConstants.STATUS_SKIP)) {
	
					SPLCommonComponent splCommonComponent = new SPLCommonComponent(taLogger, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
	
					taLogger.log(srcTransid, transid, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateOrderTransactionDetails]Inside SKIP block", "osmlogging");
	
					JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("jdbcDatabaseDAO");
					String cmdRowId = jdbcDatabaseDAO.selectRowId(srcTransid, transid);
					taLogger.log(srcTransid, transid, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateOrderTransactionDetails]cmdRowId " + cmdRowId, "osmlogging");
	
					jdbcTemplate.update("update T_OM_CMD_TRANS set STATUS = '"+ApplicationConstants.STATUS_SKIP+"', " +
							"UPDATED_DT=?, UPDATED_BY=?, CORR_ID='"+ corrId + "', RETURN_CODE='0' where TRANS_ID = ? AND SRC_TRANS_ID=? and STATUS = '"+ApplicationConstants.STATUS_ERROR+"'",
							new Object[] {new Date(), orderTransactonDetail.getUpdatedBy(), orderTransactonDetail.getOrderId(), srcTransid});
	
					jdbcTemplate.update("update T_OM_ORDER_TRANS_DTLS set ORDER_XML = ?, CORR_ID='"+ corrId + "' where TRANS_ID = ? AND SRC_TRANS_ID=?",
							new Object[] { orderTransactonDetail.getOrderXml(), orderTransactonDetail.getOrderId(), srcTransid });
	
					taLogger.log(srcTransid, transid, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateOrderTransactionDetails]After SKIP", "osmlogging");
	
					int commands = 0;
	
					taLogger.log(srcTransid, transid, cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO][updateOrderTransactionDetails]jdbcDatabaseDAO=="+jdbcDatabaseDAO, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
	
					int commandCount = jdbcDatabaseDAO.getNoOfCommands(commands,srcTransid, transid,cmdRowId,ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
					taLogger.log(srcTransid, transid, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateOrderTransactionDetails]cmdRowId " + cmdRowId, ApplicationConstants.LOGGER_CMD_PARAMETER);
					taLogger.log(srcTransid, transid, cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO][updateOrderTransactionDetails]commandCount =" + commandCount, ApplicationConstants.LOGGER_CMD_PARAMETER);
	
					if (commandCount != 0) {
						taLogger.log(srcTransid, transid, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateOrderTransactionDetails]Inside command count!=0 : "+commandCount, ApplicationConstants.LOGGER_CMD_PARAMETER);
						List<Map<String, Object>> orderXml = jdbcDatabaseDAO.getOrderXml(cmdRowId);
						String cmdRefId = "";
						for (Object xml : orderXml) {
							@SuppressWarnings("rawtypes")
							Map map1 = (Map) xml;
							String order_Xml = (String) map1.get("ORDER_XML");
							String orderTransRowId = "" + (BigDecimal) map1.get("ROW_ID");
							String trans_ID = (String) map1.get("TRANS_ID");
							//passing srcTransId for logging
							List<Map<String, Object>> cmdRefIds = jdbcDatabaseDAO.getNewCmdRefId(trans_ID, cmdRefId, orderTransRowId, srcTransid, ApplicationConstants.LOGGER_CMD_PARAMETER);
							Map<String, Object> map = cmdRefIds.get(0);
							String cmdRefIdList = (String) map.get("CMD_REF_ID");
							String parRowId = "" + (BigDecimal) map.get("ROW_ID");
							//Bug#2116: Set parRowId in orderTransactionDetails to use in OrderTransactionListener catch - Start
							orderTransactonDetail.setCmdRowId(parRowId);
							// Bug#2116: Set parRowId in orderTransactionDetails to use in OrderTransactionListener catch - End
							taLogger.log(srcTransid, trans_ID, parRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO][updateOrderTransactionDetails]New CMD REF ID  =" + cmdRefIdList, ApplicationConstants.LOGGER_CMD_PARAMETER);
	
							// Added for Bug 1610
							CommandParameters parameters = new CommandParameters();
				            CommandParamRefDtls paramRefDtls = new CommandParamRefDtls();
							List<HandlerVariables> cmdParametersList = parameters.getParameters(srcTransid, order_Xml, cmdRefIdList, trans_ID, parRowId, orderTransRowId, jdbcDatabaseDAO, true);
							taLogger.log(srcTransid, trans_ID, parRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateOrderTransactionDetails] " + "cmdParametersList Size" + " = " + cmdParametersList.size(), ApplicationConstants.LOGGER_CMD_PARAMETER);
	
							InputStream is = new ByteArrayInputStream(order_Xml.getBytes());
							DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
							domFactory.setNamespaceAware(false);
							DocumentBuilder builder = domFactory.newDocumentBuilder();
							Document doc = builder.parse(is);
							XPathFactory factory = XPathFactory.newInstance();
							XPath xpath = factory.newXPath();
	
							List<HandlerVariables> cmdTransDtlsList = paramRefDtls.getParamValues(cmdRefIdList, cmdParametersList, xpath, doc, jdbcDatabaseDAO);
	
							SPLCommonComponent.saveCmdTransDetails(cmdTransDtlsList, srcTransid, trans_ID, ApplicationConstants.LOGGER_CMD_PARAMETER, jdbcDatabaseDAO);
	
						}
					}
	
					else
					{
						taLogger.log(srcTransid, transid, ApplicationConstants.LOG_DEBUG, "transid="+transid, "osmlogging");
						taLogger.log(srcTransid, transid, ApplicationConstants.LOG_DEBUG, "cmdRowId " + cmdRowId, "osmlogging");
						taLogger.log(srcTransid, transid, ApplicationConstants.LOG_INFO, "cmdRowId " + cmdRowId +" Updating final success in Order trans table..", "osmlogging");
						jdbcDatabaseDAO.finalSuccessUpdate(cmdRowId);
						
						OrderTransactonDetail orderTransDtls = getOrderTransDtlsByCmdRowId(cmdRowId, transid, srcTransid, "osmlogging");
						String source = orderTransDtls.getSource();
						
						if(ApplicationConstants.SOURCE_OSM.equalsIgnoreCase(source)) {
							String tomResponse="";
							// changed method with single parameter - passing cmd row id as parameter - Ravi
							taLogger.log(srcTransid, transid, ApplicationConstants.LOG_DEBUG, "cmdRowId " + cmdRowId, "osmlogging");
							tomResponse = splCommonComponent.createTomResponse(cmdRowId);
		
							OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
							orderTransactionSender.postMessage(tomResponse, corrId, "TOMOrderProv");
						}
					}
	
				} else {
	
					jdbcTemplate
					.update("update T_OM_CMD_TRANS set CORR_ID='"+ corrId + "' where TRANS_ID = ? AND SRC_TRANS_ID=?",
							new Object[] { orderTransactonDetail.getOrderId(), srcTransid });
	
					jdbcTemplate
							.update("update T_OM_ORDER_TRANS_DTLS set STATUS = '"+ApplicationConstants.STATUS_RETRY+"', ORDER_XML = ?, " +
									"POLL_STATUS='N', CORR_ID='"+ corrId + "' where TRANS_ID = ? AND SRC_TRANS_ID=? and upper(STATUS)=upper('Error')",
									new Object[] { orderTransactonDetail.getOrderXml(),
											orderTransactonDetail.getOrderId(), srcTransid });
				}
			}
	}

	/**
	 * Method to handle skip, retry and cancelled for billing commands
	 * @param orderTransactonDetail
	 * @param jdbcTemplate
	 * @throws Exception
	 */
	public void updateCOneSkipRetry(OrderTransactonDetail orderTransactonDetail, JdbcTemplate jdbcTemplate) throws Exception {
		String taskRequest = orderTransactonDetail.getTaskRequest();
		String corrId = orderTransactonDetail.getCorrId();
		String transId = orderTransactonDetail.getOrderId();
		String srcTransId = orderTransactonDetail.getCrmOrderId();
		String crmSvcId = orderTransactonDetail.getCrmSvcId();
		String loggerCategory = "osmlogging";
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCOneSkipRetry]", loggerCategory);
		SPLCommonComponent splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);

		if (taskRequest.equalsIgnoreCase(ApplicationConstants.STATUS_CANCELLED)) {
			jdbcTemplate.update("update T_OM_BILL_ORDER_TRANS set STATUS = '"+ApplicationConstants.STATUS_CANCELLED+"', UPDATED_DT=sysdate, UPDATED_BY=user where SRC_TRANS_ID=? and TRANS_ID = ? " +
					"and STATUS not in ('"+ApplicationConstants.STATUS_COMPLETED+"',"+" '"+ApplicationConstants.STATUS_ERROR+"')", new Object[] {srcTransId, transId});

			jdbcTemplate.update("update T_OM_BILL_CMD_TRANS set STATUS = '"+ApplicationConstants.STATUS_CANCELLED+"', UPDATED_DT=sysdate, UPDATED_BY=user where SRC_TRANS_ID=? and TRANS_ID = ? " +
					"and STATUS not in ('"+ApplicationConstants.STATUS_COMPLETED+"',"+" '"+ApplicationConstants.STATUS_ERROR+"')", new Object[] {srcTransId, transId});

			jdbcTemplate.update("update T_OM_ORDER_TRANS_DTLS set STATUS = '"+ApplicationConstants.STATUS_CANCELLED+"', UPDATED_DT=sysdate, UPDATED_BY=user where SRC_TRANS_ID=? and TRANS_ID = ?",
					new Object[] {srcTransId, transId});

		} else if (taskRequest.equalsIgnoreCase(ApplicationConstants.STATUS_SKIP)) {

			taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCOneSkipRetry]Inside SKIP block", loggerCategory);

			JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("jdbcDatabaseDAO");
			String billOrderRowId = jdbcDatabaseDAO.getCOneErrorOrderTransId(srcTransId, transId, "", crmSvcId, loggerCategory);
			String billCmdRowId = getBillCmdRowId(srcTransId, transId, billOrderRowId, loggerCategory);
			taLogger.log(srcTransId, transId, billCmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCOneSkipRetry]billOrderRowId " + billOrderRowId, loggerCategory);

			if (billOrderRowId != null && !billOrderRowId.equalsIgnoreCase("") && !billOrderRowId.equalsIgnoreCase("-")) {
				//Aditya: Bug 21053, added additional condition to update only only those commands in error instead of the entire TOM
				jdbcTemplate.update("update T_OM_BILL_CMD_TRANS set STATUS = '"+ApplicationConstants.STATUS_SKIP+"', UPDATED_DT=sysdate, UPDATED_BY=user, " +
						"CORR_ID='"+ corrId + "', RETURN_CODE='0' where SRC_TRANS_ID=? and TRANS_ID = ? and BILL_ORDER_ROW_ID= ? and STATUS = '"+ApplicationConstants.STATUS_ERROR+"'", new Object[] {srcTransId, transId, billOrderRowId});
			}
			else {
				String sql = "SELECT DISTINCT CMD_REF_ID FROM T_OM_BILL_CMD_TRANS WHERE SRC_TRANS_ID = ? and TRANS_ID = ? and STATUS = '"+ApplicationConstants.STATUS_ERROR+"'";
				String cmdRefID = (String)getJdbcTemplate().queryForObject(
						sql, new Object[] { srcTransId, transId }, String.class);
				jdbcTemplate.update("update T_OM_BILL_CMD_TRANS set STATUS = '"+ApplicationConstants.STATUS_SKIP+"', UPDATED_DT=sysdate, UPDATED_BY=user, " +
						"CORR_ID='"+ corrId + "', RETURN_CODE='0' where SRC_TRANS_ID=? and TRANS_ID = ? and CMD_REF_ID= ? and STATUS = '"+ApplicationConstants.STATUS_ERROR+"'", new Object[] {srcTransId, transId, cmdRefID});
			}

			jdbcTemplate.update("update T_OM_BILL_ORDER_TRANS set STATUS = '"+ApplicationConstants.STATUS_SKIP+"', UPDATED_DT=sysdate, UPDATED_BY=user " +
					"where ROW_ID=? and STATUS = '"+ApplicationConstants.STATUS_ERROR+"'", new Object[] {billOrderRowId});

			jdbcTemplate.update("update T_OM_ORDER_TRANS_DTLS set ORDER_XML = ?, CORR_ID='"+ corrId + "' where SRC_TRANS_ID=? and TRANS_ID = ?",
					new Object[] { orderTransactonDetail.getOrderXml(), srcTransId, transId});

			taLogger.log(srcTransId, transId, billCmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCOneSkipRetry]After SKIP", loggerCategory);

			/*
			 * Logic: Update the next command that is not started and of a higher sequence.
			 * After the command has been updated, check if the command in the TOM order has been completed.
			 * If all the commands in the TOM have been completed then complete the TOM order
			 */

			jdbcDatabaseDAO.updateC1CmdStatusToNew(srcTransId, transId, "", crmSvcId, loggerCategory);

			int commandCount = jdbcDatabaseDAO.getNoOfPendingTOMCommands(srcTransId, transId, billCmdRowId, crmSvcId, loggerCategory);

			System.out.println("\n\n\n/****************************\n updateCOneSkipRetry Pending TOM Commands count = " + commandCount + "\n\n\n /********************\n\n");
			if (commandCount == 0) {
				jdbcTemplate.update("update T_OM_ORDER_TRANS_DTLS set STATUS='"+ ApplicationConstants.STATUS_COMPLETED+ "', UPDATED_DT=sysdate, UPDATED_BY=user where src_trans_id=? and trans_id=?", 
						new Object[] {srcTransId, transId}); 
				String tomResponse = splCommonComponent.createBillTomResponse(billCmdRowId);
				//String corrId = jdbcDatabaseDAO.getCorrId(orderRowId);
				orderTransactionSender.postBillingResponse(tomResponse, corrId);
			}
			else {
				jdbcDatabaseDAO.updateC1CmdStatusToNew(srcTransId, transId, "", crmSvcId, loggerCategory);
			}

			//			//int commandCount = jdbcDatabaseDAO.getNoOfBillCommands(srcTransId, transId, billCmdRowId, crmSvcId, loggerCategory);
			//			// Ravi: to check all the commands are completed for TOM
			//			int commandCount = jdbcDatabaseDAO.getNoOfPendingTOMCommands(srcTransId, transId, billCmdRowId, crmSvcId, loggerCategory);
			//			if(commandCount==0) {
			//				jdbcTemplate.update("update T_OM_ORDER_TRANS_DTLS set STATUS='"+ ApplicationConstants.STATUS_COMPLETED+ "', UPDATED_DT=sysdate, UPDATED_BY=user where src_trans_id=? and trans_id=?", 
			//						new Object[] {srcTransId, transId}); 
			//				String tomResponse = splCommonComponent.createBillTomResponse(billCmdRowId);
			//				//String corrId = jdbcDatabaseDAO.getCorrId(orderRowId);
			//				orderTransactionSender.postBillingResponse(tomResponse, corrId);
			//			}else {
			//				// Ravi: defect#188:commented updateNextC1Order and added new method for sequence
			//				//jdbcDatabaseDAO.updateNextC1Order(srcTransId, transId, billCmdRowId, crmSvcId, loggerCategory);
			//				jdbcDatabaseDAO.updateC1CmdStatusToNew(srcTransId, transId, billCmdRowId, crmSvcId, loggerCategory);
			//			}
			//			// Ravi: to check all the billing commands are completed for the Order
			//			commandCount = jdbcDatabaseDAO.getNoOfBillCommands(srcTransId, transId, billCmdRowId, crmSvcId, loggerCategory);
			//			if(commandCount>0) {
			//				jdbcDatabaseDAO.updateC1CmdStatusToNew(srcTransId, transId, billCmdRowId, crmSvcId, loggerCategory);
			//			}

		} else {

			jdbcTemplate.update("update T_OM_BILL_CMD_TRANS set CORR_ID='"+ corrId + "' where  src_trans_id=? and TRANS_ID = ? and STATUS = ?", new Object[] { srcTransId, transId, ApplicationConstants.STATUS_ERROR });

			jdbcTemplate.update("update T_OM_ORDER_TRANS_DTLS set STATUS = '"+ApplicationConstants.STATUS_RETRY+"', ORDER_XML = ?, " + "POLL_STATUS='N', CORR_ID='"+ corrId + "' " +
					"where src_trans_id=? and TRANS_ID = ? and STATUS= ?", new Object[] { orderTransactonDetail.getOrderXml(), srcTransId, transId, ApplicationConstants.STATUS_ERROR});
		}
	}
	
	private String getBillCmdRowId(String srcTransId, String transId, String billOrderRowId, String loggerCategory) {
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"In getBillCmdRowId ", loggerCategory);
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql = "select ROW_ID from t_om_bill_cmd_trans where BILL_ORDER_ROW_ID=?";
		 taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][getBillCmdRowId]Sql= " + sql, loggerCategory);
		 List<Map<String, Object>> billCmdRowIdList = jdbcTemplate.queryForList(sql, new Object[]{billOrderRowId});
		 String billCmdRowId = "";
		 if(billCmdRowIdList.size()>0) {
			 Map<String, Object> billCmdMap = billCmdRowIdList.get(0);
			 if(billCmdMap!=null)
				 billCmdRowId = (String)billCmdMap.get("ROW_ID");
		 }
		 taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][getBillCmdRowId]billCmdRowId="+ billCmdRowId, loggerCategory);
		 return billCmdRowId;
	}
	
	/**
	 *
	 * @param correlationId
	 * @param status
	 */

	public void updateCommandTransactionStatus(String cmdRowId, String status) {
		taLogger.log("", "", cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO]Inside updateCommandTransactionStatus", ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sql = "update T_OM_CMD_TRANS set STATUS = ? where ROW_ID = ?";
		// Ravi: 20130617: Bug#2346: Added condition to check if status is New then update to Received - Start
		if(ApplicationConstants.STATUS_RECEIVED.equalsIgnoreCase(status)) {
			sql = sql + " AND upper(status)='" + ApplicationConstants.STATUS_NEW.toUpperCase() + "'";
		}
		// Ravi: 20130617: Bug#2346: Added condition to check if status is New then update to Received - End
		
		taLogger.log("", "", cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO]Inside updateCommandTransactionStatus SQL: " + sql + " cmdRowId="+ cmdRowId + " status " + status, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		
		jdbcTemplate.update(sql, new Object[] { status, cmdRowId });
		taLogger.log("", "", cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO]updateCommandTransactionStatus End" , ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
	}

	/**
	 *
	 * @param correlationId
	 * @param status
	 * @param returnCode
	 * @param returnMessage
	 */
	public void updateCommandTransactionStatus(String cmdRowId, String orderRowId, String returnCode, String returnMessage) {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		taLogger.log("Inside updateCommandTransactionStatus Method", ApplicationConstants.LOG_DEBUG,"returnCode="+returnCode+" returnMessage="+returnMessage+" cmdRowID="+cmdRowId, ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
		if(returnMessage!=null && !returnMessage.equals("") && returnMessage.length()>500)
		{
			returnMessage = returnMessage.substring(0, 500);
			taLogger.log("Inside updateCommandTransactionStatus Method", ApplicationConstants.LOG_DEBUG,"returnMessage="+returnMessage, ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
		}
		String sql="update T_OM_CMD_TRANS set status='" + ApplicationConstants.STATUS_ERROR + "',"+ " RETURN_CODE=?," +"RETURN_MSG= ?,"+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user"+ " where ROW_ID=?" ;
		jdbcTemplate.update(sql,new Object[] {returnCode,returnMessage, cmdRowId});
		taLogger.log("Inside updateCommandTransactionStatus Method", ApplicationConstants.LOG_DEBUG,sql, ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
		sql="update T_OM_ORDER_TRANS_DTLS set STATUS='"+ApplicationConstants.STATUS_ERROR+"',"+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user"+ " where ROW_ID='" + orderRowId + "'";
		taLogger.log("Inside updateCommandTransactionStatus Method", ApplicationConstants.LOG_DEBUG,sql, ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
	    jdbcTemplate.update(sql);
	}

	/**
	 *
	 * @param rowId
	 * @return
	 */
	public Map<String, Object> getCmdOrderTransactionDetails(String rowId) {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		Map<String, Object> cmdOrderTransDetails = jdbcTemplate.queryForMap(ApplicationConstants.SQL_GET_CMD_ORDER_TRANS_DETAILS, new Object[] {rowId});
		return cmdOrderTransDetails;
	}

	/**
	 *
	 * @param cmdRefId
	 * @return system name
	 * @throws SQLException
	 */
	public String getSystemName(String cmdRefId) throws SQLException {
		String systemName = "";
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		systemName = jdbcTemplate.queryForObject(ApplicationConstants.SQL_GET_SYSTEMNAME, new Object[]{cmdRefId}, String.class);
		return systemName;
    }

	/**
	 *
	 * @param cmdRefId
	 * @return
	 * @throws SQLException
	 * @throws SPLExceptionHandler
	 */
	public CommandTransDetails getSystemDetails(String cmdRefId) throws SQLException, SPLExceptionHandler {
		CommandTransDetails commandTransDetails = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		List<CommandTransDetails> commandTransList = jdbcTemplate.query(ApplicationConstants.SQL_GET_SYSTEMDETAILS, new Object[] {cmdRefId}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
		if(commandTransList.size()!=0)
			commandTransDetails = commandTransList.get(0);
		else
			throw new SPLExceptionHandler("Exception in getSystemDetails: No System Details found for Command " + cmdRefId);
		return commandTransDetails;
    }

	public CommandTransDetails getAuthTokenParams(String systemName) throws SQLException, SPLExceptionHandler {
		taLogger.log("[DataBaseDAO]=>[getCmdTransDetailsList]", ApplicationConstants.LOG_INFO, "systemName=" + systemName , ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		CommandTransDetails authTokenTransDetails = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		List<CommandTransDetails> commandTransList = jdbcTemplate.query(ApplicationConstants.SQL_GET_AUTH_TOKEN_PARAMS, new Object[] {systemName}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
		taLogger.log("[DataBaseDAO]=>[getCmdTransDetailsList]", ApplicationConstants.LOG_INFO, "SQL_GET_AUTH_TOKEN_PARAMS=" + ApplicationConstants.SQL_GET_AUTH_TOKEN_PARAMS , ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		if(commandTransList.size()!=0)
			authTokenTransDetails = commandTransList.get(0);
		else
			throw new SPLExceptionHandler("Exception in getAuthTokenParams: No Auth Token Params found for system " + systemName);
		return authTokenTransDetails;
    }

	/**
	 *
	 * @param rowId
	 * @return
	 * @throws SQLException
	 */
	public List<Map<String,Object>> getCmdTransDetailsList(String rowId) throws SQLException {
		taLogger.log("DataBaseDAO=>getCmdTransDetailsList", ApplicationConstants.LOG_INFO, "getCmdTransDetailsList=" + rowId , ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		List<Map<String,Object>> commandTransDetailsList = jdbcTemplate.queryForList(ApplicationConstants.SQL_GET_CMD_TRANS_DTLS, new Object[] {rowId});
		taLogger.log("DataBaseDAO=>getCmdTransDetailsList", ApplicationConstants.LOG_INFO, "Command_Parameter_Values: \n"+commandTransDetailsList, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return commandTransDetailsList;
    }


	/**
	 *
	 * @param rowId
	 * @param cmdRefId
	 * @return
	 * @throws SQLException
	 */
	public List<CommandTransDetails> getCmdTransDetails(String rowId, String cmdRefId, String srcTransId, String transId,String systemName) throws SQLException {
		taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getCmdTransDetails]Method Start rowId: " + rowId, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getCmdTransDetails]CmdRefId="+cmdRefId+" SystemName="+systemName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		List<CommandTransDetails> commandTransList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		if(rowId!=null)
			if(ApplicationConstants.SYSTEM_OPCOBILLING.equalsIgnoreCase(systemName) || ApplicationConstants.SYSTEM_PSBILLING.equalsIgnoreCase(systemName)) {
				commandTransList = jdbcTemplate.query(ApplicationConstants.SQL_GET_DB_CMD_TRANS_DTLS, new Object[] {rowId}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
			}
			else{
				commandTransList = jdbcTemplate.query(ApplicationConstants.SQL_GET_CMD_TRANS_DTLS, new Object[] {rowId}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));	
			}
		else if(cmdRefId!=null)
			commandTransList = jdbcTemplate.query(ApplicationConstants.SQL_GET_CMD_AUTH_TOKEN_TRANS_DTLS, new Object[] {cmdRefId}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
		taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getCmdTransDetails]commandTransList: " + commandTransList, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return commandTransList;
    }


	/**
	 *
	 * @param cmdRefId
	 * @param paramName
	 * @return
	 * @throws SQLException
	 */
	public String getAccountType(String cmdRefId, String paramName) throws SQLException {
		taLogger.log("DataBaseDAO=>getAccountType", ApplicationConstants.LOG_INFO, "cmdRefId=" + cmdRefId, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		String accountType = "";
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		accountType = jdbcTemplate.queryForObject(ApplicationConstants.SQL_GET_ACCOUNTTYPE, new Object[]{cmdRefId, paramName}, String.class);
		taLogger.log("DataBaseDAO=>getAccountType", ApplicationConstants.LOG_INFO, "getAccountType=" + accountType, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return accountType;
    }

	/**
	 *
	 * @param cmdRowId
	 * @param cmdRefId
	 * @param transId
	 * @param responseMessage
	 * @throws SQLException
	 * @throws SPLExceptionHandler
	 */
	public void saveCommandResponse(String cmdRowId, String cmdRefId, String transId, String responseMessage, String interfaceName, boolean immediatelyFlag) throws SQLException, SPLExceptionHandler {
		taLogger.log("", transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][saveCommandResponse] Method Start cmdRefId=" + cmdRefId, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sqlQuery = ApplicationConstants.SQL_SAVE_COMMAND_RESPONSE;
		Object sqlParams[] = null;

		taLogger.log("", transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][saveCommandResponse] interfaceName=" + interfaceName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);

		//Added For Bug No:298
		//Ravi: Moved PROXIMITY_002 to SIMA
		if(interfaceName.equalsIgnoreCase(ApplicationConstants.INTERFACE_SIMA_001) || immediatelyFlag) {
			taLogger.log("", transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][saveCommandResponse] Inside If interface equals SIMA_001", ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
			sqlParams = new Object[] {cmdRowId, cmdRefId, transId, responseMessage, interfaceName, ApplicationConstants.STATUS_RECEIVED};
		}
		else {
			taLogger.log("", transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][saveCommandResponse] Inside else if not interface equals SIMA_001", ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
			sqlParams = new Object[] {cmdRowId, cmdRefId, transId, responseMessage, interfaceName, ApplicationConstants.STATUS_NEW};
		}
		int[] sqlTypes = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.CLOB, Types.VARCHAR, Types.VARCHAR};
		jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
		taLogger.log("", transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][saveCommandResponse] [End]", ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
	}


	public OrderTransactonDetail getOrderTransDetails(String transId) throws SQLException {
		taLogger.log("DataBaseDAO=>getOrderTransDetails", ApplicationConstants.LOG_INFO, "CmdTransId=" + transId, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		List<OrderTransactonDetail> orderTransactonDetailsList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		orderTransactonDetailsList = jdbcTemplate.query(ApplicationConstants.SQL_GET_ORDER_TRANS_DETAILS, new Object[] {transId}, new BeanPropertyRowMapper<OrderTransactonDetail>(OrderTransactonDetail.class));
		taLogger.log("DataBaseDAO=>getOrderTransDetails", ApplicationConstants.LOG_INFO, "orderTransactonDetailsList: \n"+orderTransactonDetailsList, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		OrderTransactonDetail orderTransactonDetail = null;
		if(orderTransactonDetailsList.size()!=0)
			orderTransactonDetail = orderTransactonDetailsList.get(0);
		return orderTransactonDetail;
	}
	
	public OrderTransactonDetail getCorrelationID(String transId) throws SQLException {
		OrderTransactonDetail otd = new OrderTransactonDetail();
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		List<Map<String, Object>> result = jdbcTemplate.queryForList("select corr_id from t_om_cmd_trans where row_id = ?", new Object[]{transId});
		if(result.size() != 0){otd.setCorrId(String.valueOf(result.get(0).get("CORR_ID")));}
		return otd;
	}
	
	 /* @param transId
	 * @return
	 * @throws SQLException
	 */
	public OrderTransactonDetailBill getOrderTransDetailsBill(String transId) throws SQLException {
		taLogger.log("getOrderTransDetailsBill", ApplicationConstants.LOG_INFO, "CmdTransId=" + transId, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		List<OrderTransactonDetailBill> orderTransactonDetailsListBill = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		orderTransactonDetailsListBill = jdbcTemplate.query(ApplicationConstants.SQL_GET_ORDER_TRANS_DETAILS_BILL, new Object[] {transId}, new BeanPropertyRowMapper<OrderTransactonDetailBill>(OrderTransactonDetailBill.class));
		taLogger.log("getOrderTransDetailsBill", ApplicationConstants.LOG_INFO, "orderTransactonDetailsListBill: \n"+orderTransactonDetailsListBill, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		OrderTransactonDetailBill orderTransactonDetail = null;
		if(orderTransactonDetailsListBill.size()!=0)
			orderTransactonDetail = orderTransactonDetailsListBill.get(0);
		return orderTransactonDetail;
	}

	
	/**
	 * @param orderTransId
	 * @return
	 * @throws SQLException
	 * Added by Billy Lim in Bug 2142. Refer to modification history
	 */
	public OrderTransactonDetail getOrderTransDetailsByOrderTransId(String transId, String srcTransId, String loggerCategory) throws SQLException {
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getOrderTransDetailsByOrderTransId]OrderTransId=" + transId, loggerCategory);
		List<OrderTransactonDetail> orderTransactonDetailsList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		orderTransactonDetailsList = jdbcTemplate.query(ApplicationConstants.SQL_GET_ORDER_TRANS_DETAILS_BY_ORDERTRANSID, new Object[] {transId, srcTransId}, new BeanPropertyRowMapper<OrderTransactonDetail>(OrderTransactonDetail.class));
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getOrderTransDetailsByOrderTransId] orderTransactonDetailsList: \n"+orderTransactonDetailsList, loggerCategory);
		OrderTransactonDetail orderTransactonDetail = null;
		if(orderTransactonDetailsList.size()!=0)
			orderTransactonDetail = orderTransactonDetailsList.get(0);
		return orderTransactonDetail;
	}
	
	/**
	 * @param orderTransactonDetail
	 * Added by Billy Lim in Bug 2142. Refer to modification history
	 */
	public void updateOrderTransactonCorrID(OrderTransactonDetail orderTransactonDetail, String loggerCategory) throws SQLException {
		String srcTransId = orderTransactonDetail.getCrmOrderId();
		String transId = orderTransactonDetail.getOrderId();
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][updateOrderTransactonCorrID] TRANS_ID=" + orderTransactonDetail.getOrderId() + " CORR_ID="+orderTransactonDetail.getCorrId() , loggerCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String corrId = orderTransactonDetail.getCorrId();
		jdbcTemplate
		.update("update T_OM_CMD_TRANS set UPDATED_DT=?, UPDATED_BY=?, CORR_ID=? where TRANS_ID = ? AND SRC_TRANS_ID=?",
				new Object[] { new Date(), orderTransactonDetail.getUpdatedBy(),corrId, orderTransactonDetail.getOrderId(), srcTransId });

		jdbcTemplate
				.update("update T_OM_ORDER_TRANS_DTLS set UPDATED_DT=?, UPDATED_BY=?, CORR_ID=? where TRANS_ID = ? AND SRC_TRANS_ID=?",
						new Object[] { new Date(), orderTransactonDetail.getUpdatedBy(), corrId, orderTransactonDetail.getOrderId(), srcTransId });
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][updateOrderTransactonCorrID] End ", loggerCategory);
	}
	
	/**
	 * @param orderTransId
	 * @return
	 * @throws SQLException
	 * Added by Billy Lim in Bug 2142. Refer to modification history
	 */
	public OrderTransactonDetail getLastCompletedCmdRefId(String orderTransId, String srcTransId, String loggerCategory) throws SQLException {
		taLogger.log(srcTransId, orderTransId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getLastCompletedCmdRefId] OrderTransId=" + orderTransId, loggerCategory);
		List<OrderTransactonDetail> orderTransactonDetailsList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		orderTransactonDetailsList = jdbcTemplate.query(ApplicationConstants.SQL_GET_LAST_COMPLETED_CMD_TRANS, new Object[] {orderTransId, srcTransId, orderTransId, srcTransId}, new BeanPropertyRowMapper<OrderTransactonDetail>(OrderTransactonDetail.class));
		taLogger.log(srcTransId, orderTransId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getLastCompletedCmdRefId] orderTransactonDetailsList=" + orderTransactonDetailsList, loggerCategory);
		OrderTransactonDetail orderTransactonDetail = null;
		if(orderTransactonDetailsList.size()!=0)
			orderTransactonDetail = orderTransactonDetailsList.get(0);
		return orderTransactonDetail;
	}	
	/**
	 *
	 * @param applicationName
	 * @param orderId
	 * @param cmdRefId
	 * @param errSrc
	 * @param errCode
	 * @param errMsg
	 */
	public void insertError(String applicationName, String orderId, String cmdRefId, String errSrc, String errCode, String errMsg) {
		taLogger.log(orderId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][insertError]cmdRefId" + cmdRefId, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		try {
			JdbcTemplate jdbcTemplate = getJdbcTemplate();

	    	Object sqlParams[] = new Object[] {applicationName, orderId, cmdRefId, errSrc, errCode, errMsg};

	    			int[] sqlTypes = new int[] {Types.VARCHAR,
	    					Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
	    					Types.CLOB};

	    	jdbcTemplate.update(ApplicationConstants.SQL_INSERT_ERROR, sqlParams, sqlTypes);
		}catch (Exception exception) {
			taLogger.log(orderId, ApplicationConstants.LOG_ERROR, "[DataBaseDAO][insertError][Inside Catch]Exception " + exception.getMessage(), ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING, exception);
	    }
	}

	public void updateTaskResponseDetails(List<CommandTransDetails> cmdTransDtlsList, String srcTransId, String transId, String cmdRowId) {
		try {
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO][updateTaskResponseDetails]Cmd Trans Dtls List Size " + cmdTransDtlsList.size(), ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
			Iterator<CommandTransDetails> cmdTransDetails = cmdTransDtlsList.iterator();


			while(cmdTransDetails.hasNext()) {
				CommandTransDetails commandTransDetails = cmdTransDetails.next();
				String parRowId = commandTransDetails.getParRowId();
				taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO][updateTaskResponseDetails]parRowId " + parRowId, ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
				String xmlEleName = commandTransDetails.getXmlEleName();
				taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO][updateTaskResponseDetails]xmlEleName " + xmlEleName, ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
				String xmlEleId = commandTransDetails.getXmlEleId();
				taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO][updateTaskResponseDetails]xmlEleId " + xmlEleId, ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
				String xmlEleType = commandTransDetails.getXmlEleType();
				taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO][updateTaskResponseDetails]xmlEleType " + xmlEleType, ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
				String xmlEleCurrValue = commandTransDetails.getXmlEleCurrValue();
				taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO][updateTaskResponseDetails]xmlEleCurrValue " + xmlEleCurrValue, ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
				String sqlQuery = "";
				JdbcTemplate jdbcTemplate = getJdbcTemplate();
				//Bug 1954 start Refer to MODIFICATION HISTORY 
				//if(xmlEleName.equalsIgnoreCase("LINEITEMID")) {//Viknesh: Removed XML_ELE_NAME for Bug 1274, Added XML_ELE_NAME for LINEITEM only
				if(xmlEleName!=null && xmlEleName.equalsIgnoreCase("LINEITEMID")) {
				//Bug 1954 end Refer to MODIFICATION HISTORY 	
					sqlQuery = "UPDATE T_OM_CMD_TRANS_DTLS SET XML_ELE_CURR_VALUE=?, UPDATED='Y' WHERE upper(PAR_ROW_ID)=upper(?) AND (upper(XML_ELE_NAME)=upper(?) OR upper(XML_ELE_NAME)=upper('ParentLineItemID')) AND upper(XML_ELE_ID)=upper(?) AND upper(XML_ELE_TYPE)=upper(?)";
					jdbcTemplate.update(sqlQuery, new Object[] { xmlEleCurrValue, parRowId, xmlEleName, xmlEleId, xmlEleType });
				}else {
					if(xmlEleType!=null && xmlEleType.equalsIgnoreCase("LINEITEM")) {
						sqlQuery = "UPDATE T_OM_CMD_TRANS_DTLS SET XML_ELE_CURR_VALUE=?, UPDATED='Y' WHERE upper(PAR_ROW_ID)=upper(?) AND upper(XML_ELE_NAME)=upper(?) AND  upper(XML_ELE_ID)=upper(?) AND upper(XML_ELE_TYPE)=upper(?)";
						jdbcTemplate.update(sqlQuery, new Object[] { xmlEleCurrValue, parRowId, xmlEleName, xmlEleId, xmlEleType });
					}else {
						sqlQuery = "UPDATE T_OM_CMD_TRANS_DTLS SET XML_ELE_CURR_VALUE=?, UPDATED='Y' WHERE upper(PAR_ROW_ID)=upper(?) AND  upper(XML_ELE_ID)=upper(?) AND upper(XML_ELE_TYPE)=upper(?)";
						jdbcTemplate.update(sqlQuery, new Object[] { xmlEleCurrValue, parRowId, xmlEleId, xmlEleType });
					}
				}
				taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DatabaseDAO][updateTaskResponseDetails]SQL=" + sqlQuery, ApplicationConstants.LOG_CATEGORY_CRM_LOGGING);
			}
		}catch(Exception e) {
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_ERROR, "[DatabaseDAO][updateTaskResponseDetails][Inside Catch]List Size " + cmdTransDtlsList.size(), ApplicationConstants.LOG_CATEGORY_CRM_LOGGING, e);
		}
	}

	private String getParentLineItemEleName(String parRowId, String xmlEleName) throws SQLException {
		taLogger.log("[DataBaseDAO][getParentLineItemEleName][OrderId=]" + parRowId, ApplicationConstants.LOG_INFO, "xmlEleName=" + xmlEleName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		int count = jdbcTemplate.queryForInt("SELECT COUNT(*) AS COUNT FROM T_OM_CMD_TRANS_DTLS WHERE PAR_ROW_ID=? AND XML_ELE_NAME=?", new Object[]{parRowId, xmlEleName});
		if(count==0)
			xmlEleName = "ParentLineItemID";
		return xmlEleName;
	}

	/*************************************
	 *  Methods used for WCF Translator
	 ***********************************/

	public List<CommandTransDetails> getWcfCmdTransDtls(String cmdTransId, String systemName) throws SQLException {
		taLogger.log("[DataBaseDAO][getWcfCmdTransDtls][OrderId=]" + cmdTransId, ApplicationConstants.LOG_INFO, "cmdTransId=" + cmdTransId, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		List<CommandTransDetails> cmdTransactonDetailsList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sqlQuery = "";
		if(ApplicationConstants.SYSTEM_CRM.equals(systemName))
			sqlQuery = ApplicationConstants.SQL_GET_WCF_CMD_TRANS_DETAILS;
		else
			sqlQuery = ApplicationConstants.SQL_GET_WCF_VMS_CMD_TRANS_DETAILS;
		taLogger.log("[DataBaseDAO][getWcfCmdTransDtls][OrderId=]" + cmdTransId, ApplicationConstants.LOG_INFO, "sqlQuery: "+sqlQuery, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		cmdTransactonDetailsList = jdbcTemplate.query(sqlQuery, new Object[] {cmdTransId}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
		taLogger.log("[DataBaseDAO][getWcfCmdTransDtls][OrderId=]" + cmdTransId, ApplicationConstants.LOG_INFO, "orderTransactonDetailsList: \n"+cmdTransactonDetailsList, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return cmdTransactonDetailsList;
	}

	public String getParamValue(String cmdTransId, String paramName) throws SQLException {
		taLogger.log("[DataBaseDAO][getParamValue][OrderId=]" + cmdTransId, ApplicationConstants.LOG_INFO, "paramName=" + paramName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		int count = jdbcTemplate.queryForInt(SQL_PARAM_NAME_COUNT, new Object[]{cmdTransId, paramName});
		String paramValue = "";
		if(count!=0)
			paramValue = jdbcTemplate.queryForObject(ApplicationConstants.SQL_GET_PARAMVALUE, new Object[]{cmdTransId, paramName}, String.class);
		return paramValue;
	}

	public CommandTransDetails getReturnCodeAndMessage(String cmdTransId) throws SQLException {
		taLogger.log("[DataBaseDAO][getReturnCodeAndMessage][OrderId=]" + cmdTransId, ApplicationConstants.LOG_INFO, "cmdTransId=" + cmdTransId, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		List<CommandTransDetails> cmdTransactonDetailsList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		cmdTransactonDetailsList = jdbcTemplate.query(ApplicationConstants.SQL_GET_RETURNCODE_AND_MESSAGE, new Object[] {cmdTransId}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
		CommandTransDetails commandTransDetails = null;
		if(cmdTransactonDetailsList.size()!=0)
			commandTransDetails = cmdTransactonDetailsList.get(0);
		taLogger.log("[DataBaseDAO][getWcfCmdTransDtls][OrderId=]" + cmdTransId, ApplicationConstants.LOG_INFO, "CommandTransDetails: \n"+commandTransDetails, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return commandTransDetails;
	}

	/**
	 * Method to get the location url
	 * @return
	 * @throws SQLException
	 */
	public String location() throws SQLException {
		CommandTransDetails commandTransDetails = ThreadLocalInstance.get();
		String systemName = commandTransDetails.getSystemName();
		String interfaceName = commandTransDetails.getInterfaceName();
		taLogger.log("[DataBaseDAO][getLocation][systemName=" + systemName+"]", ApplicationConstants.LOG_INFO, "SystemName=" +systemName + " InterfaceName="+interfaceName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		// Ravi: added to get interface's http method and pass url as querystring to GET method
		String httpMethod = httpMethod();
		String location = "";
		if(ApplicationConstants.HTTP_METHOD_POST.equalsIgnoreCase(httpMethod)) {
			taLogger.log("[DataBaseDAO][getLocation][systemName=" + systemName+"]", ApplicationConstants.LOG_INFO, "Http Method=" +systemName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
			JdbcTemplate jdbcTemplate = getJdbcTemplate();
			//location = jdbcTemplate.queryForObject("SELECT LOCATION FROM C_OM_CMD_DEST_REF WHERE SYSTEM_NAME=? AND INTERFACE_NAME=?", new Object[]{systemName,interfaceName}, String.class);
			location = jdbcTemplate.queryForObject("SELECT LOCATION FROM C_OM_INTERFACE_REF WHERE SYSTEM_NAME=? AND INTERFACE_NAME=?", new Object[]{systemName,interfaceName}, String.class);
		}else {
			location = commandTransDetails.getQueryString();
		}
		taLogger.log("[DataBaseDAO][getLocation][systemName=" + systemName+"]", ApplicationConstants.LOG_INFO, "Location=" + location, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return location;
		
	}
	
	/**
	 * Method to get the Http Method Get or Post
	 * @return
	 * @throws SQLException
	 */
	public String httpMethod() throws SQLException {
		CommandTransDetails commandTransDetails = ThreadLocalInstance.get();
		Properties properties = (Properties) BeanFactory.getBean("properties");
		String systemName = commandTransDetails.getSystemName();
		String httpMethod ="";
		httpMethod = properties.getProperty(systemName);
		taLogger.log("[DataBaseDAO][getHttpMethod][systemName=" + systemName+"]", ApplicationConstants.LOG_INFO, "SystemName=" + systemName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		taLogger.log("[DataBaseDAO][getHttpMethod][systemName=" + systemName+"]", ApplicationConstants.LOG_INFO, "HttpMethod=" + httpMethod, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return httpMethod;
		
	}
	
	/**
	 * Method to get connection timeout
	 * @return
	 * @throws Exception
	 */
	public String getTimeOut() throws Exception {
		CommandTransDetails commandTransDetails = ThreadLocalInstance.get();
		String timeOut = commandTransDetails.getTimeOut();
		taLogger.log("[DataBaseDAO][getTimeOut]", ApplicationConstants.LOG_INFO, "getTimeOut=" + timeOut, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return timeOut;
	}
	
	/**
	 * Method to get read timeout
	 * @return
	 * @throws Exception
	 */
	public String getReadTimeOut() throws Exception {
		CommandTransDetails commandTransDetails = ThreadLocalInstance.get();
		String readTimeOut = commandTransDetails.getReadTimeOut();
		taLogger.log("[DataBaseDAO][getReadTimeOut]", ApplicationConstants.LOG_INFO, "getReadTimeOut=" + readTimeOut, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return readTimeOut;
	}
	
	public String user(String systemName) throws SQLException {
		taLogger.log("[DataBaseDAO][getUser][systemName=]" + systemName, ApplicationConstants.LOG_INFO, "systemName=" + systemName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		//String user = jdbcTemplate.queryForObject("SELECT USER_NAME FROM C_OM_CMD_DEST_REF WHERE SYSTEM_NAME=?", new Object[]{systemName}, String.class);
		String user = jdbcTemplate.queryForObject("SELECT USER_NAME FROM C_OM_INTERFACE_REF WHERE SYSTEM_NAME=?", new Object[]{systemName}, String.class);
		taLogger.log("[DataBaseDAO][getUser][location=]" + systemName, ApplicationConstants.LOG_INFO, "User=" + user, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return user;
	}

	public String pwd(String systemName) throws SQLException {
		taLogger.log("[DataBaseDAO][getPwd][systemName=]" + systemName, ApplicationConstants.LOG_INFO, "systemName=" + systemName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		//String pwd = jdbcTemplate.queryForObject("SELECT PWD FROM C_OM_CMD_DEST_REF WHERE SYSTEM_NAME=?", new Object[]{systemName}, String.class);
		String pwd = jdbcTemplate.queryForObject("SELECT PWD FROM C_OM_INTERFACE_REF WHERE SYSTEM_NAME=?", new Object[]{systemName}, String.class);
		taLogger.log("[DataBaseDAO][getPwd][location=]" + systemName, ApplicationConstants.LOG_INFO, "pwd=" + pwd, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return pwd;
	}

	/*************************************
	 *  End Methods used for WCF Translator
	 ***********************************/

	/************** Start Async Response Handler **************/

	public CommandTransDetails getCmdDestRefDtls(String interfaceName) throws SQLException {
		taLogger.log("[DataBaseDAO][getUserAndPwd][interfaceName=]" + interfaceName, ApplicationConstants.LOG_INFO, "interfaceName=" + interfaceName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		List<CommandTransDetails> cmdTransactonDetailsList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		cmdTransactonDetailsList = jdbcTemplate.query(SQL_CMD_DEST_REF_DTLS, new Object[] {interfaceName}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
		CommandTransDetails commandTransDetails = null;
		if(cmdTransactonDetailsList.size()!=0)
			commandTransDetails = cmdTransactonDetailsList.get(0);
		taLogger.log("[DataBaseDAO][getUserAndPwd][interfaceName=]" + interfaceName, ApplicationConstants.LOG_INFO, "CommandTransDetails: \n"+commandTransDetails, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return commandTransDetails;
	}

	public void updateLastExecutionTime(Date lastExecutionTime, String pollerName) {
		taLogger.log("[DataBaseDAO][updateLastExecutionTime][pollerName=]" + pollerName, ApplicationConstants.LOG_INFO, "pollerName=" + pollerName, "ILresponseadaptorlogging");
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		jdbcTemplate.update("update C_OM_POLL_INT set LAST_POLL_TIME='"+ lastExecutionTime + "' where POLLER_NAME='" + pollerName + "'");
		taLogger.log("Inside updateLastPollTime Method", ApplicationConstants.LOG_DEBUG, "update C_OM_POLL_INT set " +
				"LAST_POLL_TIME='" + lastExecutionTime + "' where POLLER_NAME='" + pollerName + "'", "ILresponseadaptorlogging");
	}
	/************** End Async Response Handler **************/
	public String getOrderRowId(String srcTransId,String transactionId,String parRowId,String logCategory){
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[DataBaseDAO][getOrderRowId]", logCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String ORDER_ROW_ID = jdbcTemplate.queryForObject("SELECT ORDER_ROW_ID FROM T_OM_CMD_TRANS WHERE ROW_ID=?", new Object[]{parRowId}, String.class);
		taLogger.log(srcTransId,transactionId,parRowId, ApplicationConstants.LOG_DEBUG, "[DataBaseDAO][getOrderRowId]ORDER_ROW_ID="+ORDER_ROW_ID,logCategory);
		return ORDER_ROW_ID;
	}

	/**
	 * Bug#2184 : Update the STUB flag in cmd_trans table
	 * @param srcTransId
	 * @param transId
	 * @param cmdRowId
	 * @param isStub
	 * @param loggerCategory
	 */
	public void updateStubbingStatus(String srcTransId, String transId, String cmdRowId, boolean isStub, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO]Inside updateStubbingStatus", loggerCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String stub = "N";
		if(isStub)
			stub = "Y";
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO]Inside updateStubbingStatus SQL: update T_OM_CMD_TRANS set STATUS ='"+ stub +"' where ROW_ID =" + cmdRowId , loggerCategory);
		jdbcTemplate.update("update T_OM_CMD_TRANS set STUB = ? where ROW_ID = ?", new Object[] { stub, cmdRowId });
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO]updateStubbingStatus End" , loggerCategory);
	}
	
	/**
	 * Method to update stub and request message
	 * @param srcTransId
	 * @param transId
	 * @param cmdRowId
	 * @param requestMsgFlag
	 * @param requestMessage
	 * @param isStub
	 * @param loggerCategory
	 */
	public void updateCmdTrans(String srcTransId, String transId, String cmdRowId, String requestMsgFlag, String requestMessage, boolean isStub, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCmdTrans] Start", loggerCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String stub = "N";
		if(isStub)
			stub = "Y";
		String sql = "";
		if("Y".equalsIgnoreCase(requestMsgFlag)) {
			sql = "update T_OM_CMD_TRANS set STUB = ?, REQUEST_MSG = ? where ROW_ID = ?";
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCmdTrans] sql " + sql , loggerCategory);
			jdbcTemplate.update(sql, new Object[] { stub, requestMessage, cmdRowId });
		}else {
			sql = "update T_OM_CMD_TRANS set STUB = ? where ROW_ID = ?";
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCmdTrans] sql " + sql , loggerCategory);
			jdbcTemplate.update(sql, new Object[] { stub, cmdRowId });
		}
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCmdTrans] End" , loggerCategory);
	}
	
	/**
	 * 
	 * @param cmdRefId
	 * @return
	 */
	public String getSql(String cmdRefId)
    {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
	   	String sql="select parameter_value from c_om_cmd_sys_map where cmd_ref_id='" +cmdRefId + "'" ;
	   	String sqlrequest = jdbcTemplate.queryForObject(sql, String.class);
	   	taLogger.log("Inside getSql Method", ApplicationConstants.LOG_DEBUG,"sql request is" + sqlrequest,"instrutorlogging");
	   	return sqlrequest;
    }
	/**
	 * 
	 * @param interfaceName
	 * @return
	 */
	public String getLocation(String interfaceName)
    {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
	   	//String sql="select location from c_om_cmd_dest_ref where interface_name='" +interfaceName + "'" ;
		String sql="select location from C_OM_INTERFACE_REF where interface_name='" +interfaceName + "'" ;
	   	String location = jdbcTemplate.queryForObject(sql, String.class);
	   	taLogger.log("Inside getLocation Method", ApplicationConstants.LOG_DEBUG,"sql request is" + location,"instrutorlogging");
	   	return location;
    }

	/**
	 * Method to call store proc for DB Adaptor request - Bug#20592
	 * @param requestMessage
	 * @param loggerCategory
	 * @param srcTransId
	 * @param transId
	 * @param cmdRowId
	 * @throws Exception
	 */
	public void processDBAdaptorRequest(String requestMessage, String storedProcName, String loggerCategory, String srcTransId, String transId, String cmdRowId) throws Exception {
		//System.out.println("storedProcName " + storedProcName);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dbAdaptorDataSource);
		DBAdaptorStoredProc dbAdaptorStoredProc = new DBAdaptorStoredProc(jdbcTemplate, storedProcName);
		String output = dbAdaptorStoredProc.execute(requestMessage);
		//System.out.println("STORED PROC OUTPUT " + output);
	}
	
	/**
	 * 
	 * @param cmdTransDetailsList
	 * @param storedProcName
	 * @param srcTransId
	 * @param transId
	 * @param cmdRowId
	 * @param loggerCategory
	 * @throws Exception
	 */
	public void processDBAdaptorRequest(List<CommandTransDetails> cmdTransDetailsList, String storedProcName, String srcTransId, String transId, String cmdRowId, String loggerCategory) throws Exception {
		//System.out.println("storedProcName " + storedProcName);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(opcoBillingDataSource);
		DBAdaptorStoredProc dbAdaptorStoredProc = new DBAdaptorStoredProc(cmdTransDetailsList, jdbcTemplate, storedProcName, srcTransId, transId, cmdRowId, loggerCategory);
		dbAdaptorStoredProc.execute(cmdTransDetailsList, srcTransId, transId, cmdRowId, loggerCategory);
		//System.out.println("STORED PROC OUTPUT " + output);
	}
	
	/**
	 * Method to get DB Adaptor trans dtls - Bug#20592
	 * @param cmdRowId
	 * @param loggerCategory
	 * @return
	 */
	public CommandTransDetails getDBAdaptorTransDtls(String cmdRowId, String loggerCategory) {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		CommandTransDetails dbAdaptorTransDtls = jdbcTemplate.queryForObject(ApplicationConstants.SQL_GET_DB_ADAPTOR_TRANS_DTLS, new Object[] {cmdRowId}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
		return dbAdaptorTransDtls;
	}
	
	public String getPriorityFlag(String priority, String loggerCategory) {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String priorityFlag = jdbcTemplate.queryForObject("SELECT NEW_VALUE FROM C_OM_LOOKUP_REF WHERE LOOKUP_REF_ID='Order_Priority_Mapping' AND LOOKUP_VALUE=?", new Object[] {priority}, String.class);
		return priorityFlag;
	}
	
	public OrderTransactonDetail getOrderTransDtlsByRowId(String cmdRowId, String transId, String srcTransId, String orderRowId, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getOrderTransDtlsByRowId]orderRowId=" + orderRowId, loggerCategory);
		List<OrderTransactonDetail> orderTransactonDetailsList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		orderTransactonDetailsList = jdbcTemplate.query(ApplicationConstants.SQL_GET_ORDER_TRANS_DTLS_BY_ROWID, new Object[] {orderRowId}, new BeanPropertyRowMapper<OrderTransactonDetail>(OrderTransactonDetail.class));
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getOrderTransDtlsByRowId] orderTransactonDetailsList: \n"+orderTransactonDetailsList, loggerCategory);
		OrderTransactonDetail orderTransactonDetail = null;
		if(orderTransactonDetailsList.size()!=0)
			orderTransactonDetail = orderTransactonDetailsList.get(0);
		return orderTransactonDetail;
	}
	
	public OrderTransactonDetail getOrderTransDtlsByCmdRowId(String cmdRowId, String transId, String srcTransId, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getOrderTransDtlsByCmdRowId]cmdRowId=" + cmdRowId, loggerCategory);
		List<OrderTransactonDetail> orderTransactonDetailsList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sql = "select row_id, corr_id, source from t_om_order_trans_dtls where row_id = (select order_row_id from t_om_cmd_trans where row_id=?)";
		orderTransactonDetailsList = jdbcTemplate.query(sql, new Object[] {cmdRowId}, new BeanPropertyRowMapper<OrderTransactonDetail>(OrderTransactonDetail.class));
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getOrderTransDtlsByCmdRowId] orderTransactonDetailsList: \n"+orderTransactonDetailsList, loggerCategory);
		OrderTransactonDetail orderTransactonDetail = null;
		if(orderTransactonDetailsList.size()!=0)
			orderTransactonDetail = orderTransactonDetailsList.get(0);
		return orderTransactonDetail;
	}
	
	public void updateStartHubApprovalFlag(String cmdRowId, String transId, String srcTransId, String flag, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][updateStartHubApprovalFlag]cmdRowId=" + cmdRowId, loggerCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sql = "update T_OM_CMD_TRANS_DTLS set XML_ELE_CURR_VALUE = ?, XML_ELE_PREV_VALUE = ?, UPDATED='Y' where PAR_ROW_ID = ? AND PARAM_NAME='Starhub Approval Flag'";
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateStartHubApprovalFlag] sql " + sql , loggerCategory);
		jdbcTemplate.update(sql, new Object[] { flag, "N", cmdRowId });
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateStartHubApprovalFlag] End" , loggerCategory);
	}

}
