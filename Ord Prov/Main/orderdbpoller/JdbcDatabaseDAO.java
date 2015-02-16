package com.m1.bcc.spl.orderdbpoller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.comverse.api.csm.ordering.client.OrderClient;
import com.comverse.api.csm.ordering.data.OrderIdentifier;
import com.comverse.api.csm.ordering.message.OrderGetOutputMessage;
import com.comverse.api.framework.client.UserContext;
import com.comverse.api.framework.errors.ApiException;
import com.comverse.api.framework.security.JAASClient;
import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.instructor.CommandParamRefDtls;
import com.m1.bcc.spl.instructor.CommandParameters;
import com.m1.bcc.spl.instructor.HandlerVariables;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.model.OrderTransactonDetail;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 22/02/2013					Ravikumar G				Modified for logging cleanup
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 22/05/2013					Sudharsan				Implemented Radius server	
 * 25/06/2013					Sudharsan				Implemented RIM SIM Exchange error handling logic
 * 08/07/2013					Sudharsan				Added isResponseCodeMatch method to check response code with response code configured in table
 * 02/09/2013					Sudharsan				Implemented C1 Order Status Poller		
 * 11/09/2013					Ravikumar G				Added method getSubscriberId and updated saveBillCommandResponse
 * 12/09/2013					Ravikumar G				Added method updateNextC1Order, getBillCmdTrans, getNoOfBillCommands and updated method updateCommandTransBillComplete, 
 * 														updateBillCommandTransBillError, insertOrderTransBill
 * 13/09/2013					Ravikumar G				Added method updateC1OrderStatusToNew, checkC1OrderForError and getCOneErrorOrderTransId to implement fire order by minimum sequence 
 * 														and skip retry logic for billing
 * 17/09/2013					Ravikumar G				Added method removeBillCmdTransDtls for Retry
 * 18/09/2013					Ravikumar G				Added method getMaxRowsPerPoll for bill response poller
 * 20/09/2013					Ravikumar G				defect#188:added new methods for c1 command sequence logic
 * 23/09/2013					Ravikumar G				defect#188:added sql condition status Received in method checkC1OrderForError 
 * 27/09/2013					Ravikumar G				defect#203 added method to get no of pending cmd for TOM
 * 21/10/2013					Ravikumar G				added method to update C1 intermediate API call
 * 06/11/2013					Ravikumar G				Bug#20397: added new method insertCmdTransDetails
 * 11/11/2013					Ravikumar G				Bug#20465 - Status not updated to order_trans_dtls table
 * 12/11/2013					Ravikumar G				[Bug 20459] Modified saveBillCommandResponse, getC1OrderStatus and updateBillCmdResponse
 * 20/11/2013					Ravikumar G				Bug#20538 - Changed method getCmdDestRefDtls to get timeout and read timeout
 * 28/11/2013					Kalyan			        Added method getSubscriberIdResets and updated saveBillCommandResponse 
 * 04/12/2013					Ravikumar G				Bug#20592 - Changed for DB Adaptor design change
 * 04/12/2013					Ravikumar G				Bug#21086 - Changed for Enable Flag to enable and disable system interface
 * 04/12/2013					Ravikumar G				Bug#21087 - Changed constant RECEIVED to Received in method updateOrderTransDtlsTable and removed comments
 * 04/12/2013					Ravikumar G				Bug#21221 - Removed unused imports, commented codes
 * 05/12/2013					Ravikumar G				Bug#21274 - Added condition c1 status committed in method updateBillCmdResponse
 * 12/12/2013					Ravikumar G				Bug#21569 - Updated c_om_cmd_dest_ref to c_om_interface_ref
 * 16/01/2014					Ravikumar G				Bug#23590 - added method updateCmdTrans to update request message in cmd_trans table
 * 17/01/2014					Ravikumar G				Bug#23710 - Updated cmd_trans column row_id type to number
 * 23/01/2014					Ravikumar G				Bug#24043 - Updated for column row_id type to number
 * 27/01/2014					Ravikumar G				Bug#23086 - updated transaction for ADS provisioning
******************************************************************************/

public class JdbcDatabaseDAO
 {
	private DataSource dataSource;
	//private String cmdTransRowId;
	// For logging purposes
	String inputIdentifier = "";
	String loggercategory = "dbpollerlogging";
	TALogger taLogger = TALogger.getTALogger();
	OrderTransactonDetail orderTransactonDetail = new OrderTransactonDetail();
	
	private static final String DEFAULT_USERNAME = "bcpsdev4acn";
	private static final String DEFAULT_PASSWORD = "passw0rd";
	private static final String DEFAULT_REALM = "CSM";
	
	/**
	 *
	 * @param dataSource
	 */
	public void setBasicDataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}
	/**
	 *
	 * @return
	 */
	public JdbcTemplate getJdbcTemplate()
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		return jdbcTemplate;
	}
	/**
	 *
	 * @param pollerName
	 * @return
	 */
	public long getTimePeriodOfDBPoller(String pollerName)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select TIME_INT from C_OM_POLL_INT where POLLER_NAME='"+pollerName+"'";
		long timePeriod = jdbcTemplate.queryForLong(sql);
		taLogger.log("Inside getTimePeriodOfDBPoller Method ", ApplicationConstants.LOG_DEBUG,sql,loggercategory);
		return timePeriod;
	}
	/**
	 *
	 * @return
	 */
	public Number getMaxRowsPerPollofDBPoller()
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select MAX_ROWS_PER_POLL from C_OM_POLL_INT where POLLER_NAME='DBPoller'";
		Number maxRowsPerPollofDBPoller = jdbcTemplate.queryForLong(sql);
		taLogger.log("Inside getMaxRowsPerPollofDBPoller Method ", ApplicationConstants.LOG_DEBUG,sql,loggercategory);
		return maxRowsPerPollofDBPoller;
	}
	/**
	 *
	 * @return
	 */
	public Number getMaxRowsPerPollOfCommandDBPoller()
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select MAX_ROWS_PER_POLL from C_OM_POLL_INT where POLLER_NAME='CommandDBPoller'" ;
		Number maxRowsPerPollOfCommandDBPoller = jdbcTemplate.queryForLong(sql);
		taLogger.log("Inside getMaxRowsPerPollOfCommandDBPoller Method",ApplicationConstants.LOG_DEBUG,sql, loggercategory);
		return maxRowsPerPollOfCommandDBPoller;
	}
	/**
	 *
	 * @return
	 */
	public Number getMaxRowsPerPollOfOrderStatusDBPoller()
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select MAX_ROWS_PER_POLL from C_OM_POLL_INT where POLLER_NAME='OrderStatusDBPoller'";
		Number maxRowsPerPollOfOrderStatusDBPoller = jdbcTemplate.queryForLong(sql);
		taLogger.log("Inside getMaxRowsPerPollOfOrderStatusDBPoller Method", ApplicationConstants.LOG_DEBUG,sql, loggercategory);
		return maxRowsPerPollOfOrderStatusDBPoller;
	}
	/**
	 *
	 * @return
	 */
	public Number getMaxRowsPerPollOfRespTransCmdPoller()
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select MAX_ROWS_PER_POLL from C_OM_POLL_INT where POLLER_NAME='RespTransCmdPoller'";
		Number maxRowsPerPollOfRespTransCmdPoller = jdbcTemplate.queryForLong(sql);
		taLogger.log("Inside getMaxRowsPerPollOfRespTransCmdPoller Method", ApplicationConstants.LOG_DEBUG,sql, loggercategory);
		return maxRowsPerPollOfRespTransCmdPoller;
	}
	/**
	 *
	 * @param lastPollTime
	 * @param pollerName
	 */
	public void updateLastPollTime(Date lastPollTime, String pollerName)
	{
		inputIdentifier = pollerName;
		String sql="update C_OM_POLL_INT set LAST_POLL_TIME='"+ lastPollTime + "' where POLLER_NAME='" + pollerName + "'";
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.update(sql);
		taLogger.log("Inside updateLastPollTime Method", ApplicationConstants.LOG_DEBUG,sql,loggercategory);
	}
	
	/**
	 * 
	 * @param srcTransId
	 * @param transId
	 * @return
	 */
	public String selectRowId(String srcTransId, String transId) {
		String transIdNew = "";
		try
		{
			JdbcTemplate jdbcTemplate =  new JdbcTemplate(dataSource);
			String sql="select * from T_OM_CMD_TRANS where status='Error' and trans_id='"+ transId +"' and src_trans_id='" + srcTransId + "'";
			List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
			transIdNew = "" + (BigDecimal) ((Map<String, Object>) list.get(0)).get("ROW_ID");
		}
		catch(Exception e)
		{
			taLogger.log("Inside selectRowId Method", ApplicationConstants.LOG_ERROR,"Exception is"+e,loggercategory);
			e.printStackTrace();
		}
		return transIdNew;
	}
	/**
	 *
	 * @param cmdrowid
	 * @return
	 */
	public String updateerrortask(String cmdrowid)
	{
		String cmdrefid = "";
		String transid = "";
		try
		{
			inputIdentifier = cmdrowid;
			taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG,"Inside updateerrortask Method:"+cmdrowid, loggercategory);
			JdbcTemplate jdbcTemplate =  new JdbcTemplate(dataSource);
			String sql="select * from T_OM_CMD_TRANS where row_id='"+ cmdrowid +"'";
			List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
			cmdrefid = (String) ((Map<String, Object>) list.get(0)).get("CMD_REF_ID");
			taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG,"Inside updateerrortask Method:"+cmdrefid, loggercategory);
			transid = (String) ((Map<String, Object>) list.get(0)).get("TRANS_ID");
			taLogger.log(inputIdentifier, ApplicationConstants.LOG_DEBUG,"Inside updateerrortask Method:"+transid, loggercategory);
			if(cmdrefid.equalsIgnoreCase("ERRCMDCRMTASK_001"))
			{
			 jdbcTemplate.update("update T_OM_ORDER_TRANS_DTLS set STATUS='Completed' where TRANS_ID='"+ transid + "' and FUNC_REF_ID = 'ERRTASK.0001'");
			}
		}

		catch(Exception e)
		{
			taLogger.log(inputIdentifier, ApplicationConstants.LOG_ERROR,"Error is"+e, loggercategory);
		}
		return cmdrefid;
	}
	/**
	 *
	 * @param rowId
	 */
	public void updateOrderTransDtlsTable(String rowId, String srcTransId, String transId)
	{
		inputIdentifier = rowId;
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		// Ravi: Bug#21087: Changed constant RECEIVED to Received
		String sql="update T_OM_ORDER_TRANS_DTLS set STATUS='" + ApplicationConstants.STATUS_RECEIVED + "' where ROW_ID='"+ rowId + "'";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateOrderTransDtlsTable]Sql= " + sql, "instrutorlogging");
	}
	/**
	 *
	 * @param parRowId
	 * @return
	 */
	public List<Map<String, Object>> getCmdParamValues(String parRowId)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select PARAM_NAME,PARAM_VALUE from T_OM_CMD_TRANS_DTLS where PAR_ROW_ID ='"+ parRowId + "' order by SEQ_NO";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		taLogger.log("Inside getCmdParamValues Method", ApplicationConstants.LOG_DEBUG,sql+list, "adapterlogging");
		return list;
	}
	/**
	 *
	 * @param System_name
	 * @return
	 * @throws SQLException
	 */
	public CommandTransDetails getCommandTransDetails(String System_name) throws SQLException
	{
		//taLogger.log("getCommandTransDetails", ApplicationConstants.LOG_INFO, "System_name " + System_name, "ILrequestadaptorlogging");
		List<CommandTransDetails> cmdTransactonDetailsList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sql = "select COALESCE(USER_NAME, ' ') USER_NAME, COALESCE(PWD,' ') PWD, COALESCE(ACCOUNT_TYPE,' ') ACCOUNT_TYPE, COALESCE(LOCATION,' ') LOCATION, " +
				//" TIMEOUT, READ_TIMEOUT AS READTIMEOUT from C_OM_CMD_DEST_REF where INTERFACE_NAME = ?";
				" TIMEOUT, READ_TIMEOUT AS READTIMEOUT from C_OM_INTERFACE_REF where INTERFACE_NAME = ?";
		cmdTransactonDetailsList = jdbcTemplate.query(sql, new Object[] {System_name}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
		CommandTransDetails commandTransDetails = null;
		if(cmdTransactonDetailsList.size()!=0)
		commandTransDetails = cmdTransactonDetailsList.get(0);
		//taLogger.log("getCommandTransDetails", ApplicationConstants.LOG_INFO, "System_name " + System_name, "ILrequestadaptorlogging");
		return commandTransDetails;
	}
	/**
	 *
	 * @param parRowId
	 * @return
	 */
	public List<Map<String, Object>>getUID(String parRowId)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select PARAM_NAME,PARAM_VALUE from T_OM_CMD_TRANS_DTLS where PAR_ROW_ID='"+parRowId+"'";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		taLogger.log("Inside getUID Method", ApplicationConstants.LOG_DEBUG,sql+ list, "adapterlogging");
		return list;
	}
	/**
	 *
	 * @return
	 */
	public String getAdminLogin(String srcTransId,String transId,String parRowId,String loggerCategory)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select PARAM_VALUE from T_OM_CMD_TRANS_DTLS where PARAM_NAME='AdminLogin' and PAR_ROW_ID='"+parRowId+"'";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		String adminLogin = (String) ((Map<String, Object>) list.get(0)).get("PARAM_VALUE");
		taLogger.log(srcTransId,transId,parRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][getAdminLogin]SQL :"+sql+" PARAM_VALUE: " + adminLogin+" PAR_ROW_ID: "+parRowId, loggerCategory);
		return adminLogin;
	}

	/**
	 *
	 * @param funcRefId
	 * @param opgRefId
	 * @return
	 */
	public List<Map<String, Object>> getCommandRefId(String funcRefId,String opgRefId, String srcTransId, String transId,String loggercategory)
	{
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getCommandRefId] Inside Method" , loggercategory);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select cmd_ref_id, SEQ_NO from c_om_Func_cmd_map where func_ref_id='"+ funcRefId + "'" + "and opg_ref_id='" + opgRefId + "'";
		List<Map<String, Object>> cmdRefId = jdbcTemplate.queryForList(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getCommandRefId] Sql = " + sql , loggercategory);
		return cmdRefId;
	}
	/*M.Rahman: Adding new method to extract billing related cmd_ref_id*/
	/**
	 *
	 * @param funcRefId
	 * @param opgRefId
	 * @return
	 */
	public List<Map<String, Object>> getCommandRefIdBill(String funcRefId, String subscrAction, String compId, String compType, String compAction, String extIdAction, String compSubType, String loggercategory)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select CMD_REF_ID, SEQ_NO from c_om_bill_func_cmd_map where func_ref_id='"+ funcRefId + "' and comp_type='" + compType + "' and comp_action='" + compAction + "' and extid_action='" + extIdAction + "' and subs_action='" +subscrAction + "' and comp_sub_type='" + compSubType +"'";
		List<Map<String, Object>> cmdRefId = jdbcTemplate.queryForList(sql);
		taLogger.log(funcRefId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getCommandRefIdBill] Sql = " + sql , loggercategory);
		taLogger.log(funcRefId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getCommandRefIdBill] CmdRefId returned = " + cmdRefId , loggercategory);
		return cmdRefId;
	}
	
	/**
	 *
	 * @param comndRefId
	 * @return
	 */
	public List<Map<String, Object>> getId(String comndRefId)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select cmd_ref_id, param_name, xpath, seq_no, default_value, xml_ele_type, lookup_ref_id from c_om_cmd_param_map where cmd_ref_id='"+ comndRefId + "'" + "and param_sub ='-'";
		List<Map<String, Object>> xPath = jdbcTemplate.queryForList(sql);
		taLogger.log("Inside getId Method", ApplicationConstants.LOG_INFO,sql, "adapterlogging");
		return xPath;
	}

	/**
	 *
	 * @param comndRefId
	 * @return
	 */
	public List<Map<String, Object>> getCmdParameters(String comndRefId, String srcTransId, String transId, String cmdRowId,String loggercategory) {

		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getCmdParameters] Inside Method commndRefId = "+comndRefId, loggercategory);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select param_name, param_sub, xpath, seq_no, default_value, xml_ele_type, lookup_ref_id, append, prepend from c_om_cmd_param_map where cmd_ref_id='"+ comndRefId + "'";
		List<Map<String, Object>> xPath = jdbcTemplate.queryForList(sql);
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getCmdParameters] Sql = "+sql, loggercategory);
		return xPath;
	}

	/**
	 *
	 * @param trans_Id
	 * @param commndRefId
	 * @param orderTransRowId
	 * @return
	 */
	public List<Map<String, Object>> getNewCmdRefId(String trans_Id,String commndRefId, String orderTransRowId, String srcTransId,String loggercategory)
	{
		taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefId] Inside Method commndRefId = "+commndRefId, loggercategory);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		if (commndRefId.equals(ApplicationConstants.ERROR_TASK_CMD_REF_ID))
		{
			String sql ="select row_id, cmd_ref_id from T_OM_CMD_TRANS where TRANS_ID='"+ trans_Id+ "' and ORDER_ROW_ID='" + orderTransRowId + "'" 
					   + " and cmd_ref_id='"+ApplicationConstants.ERROR_TASK_CMD_REF_ID +"' and status in ('"+ApplicationConstants.STATUS_NEW+"','"
					   + ApplicationConstants.STATUS_NOT_STARTED+"')";
			List<Map<String, Object>> newCmdRefId = jdbcTemplate.queryForList(sql);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefId] Error Task Sql = "+sql, loggercategory);
			return newCmdRefId;

		}
		else
		{
			String sql="select row_id, cmd_ref_id from T_OM_CMD_TRANS where ORDER_ROW_ID='"+ orderTransRowId+ "'"+ " and "
					  + "seq_no = (select min(seq_no) from T_OM_CMD_TRANS where ORDER_ROW_ID='"+ orderTransRowId
					  + "' and status='"+ ApplicationConstants.STATUS_NOT_STARTED + "')";
			List<Map<String, Object>> newCmdRefId = jdbcTemplate.queryForList(sql);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefId] Sql = "+sql, loggercategory);
			return newCmdRefId;
		}
	}
	/**
	 *
	 * @param comndRefId
	 * @return
	 */
	public List<Map<String, Object>> getNotNullParamName(String comndRefId)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select distinct param_name from c_om_cmd_param_map where cmd_ref_id='"+ comndRefId + "'" + "and param_sub !='-'";
		List<Map<String, Object>> paramName = jdbcTemplate.queryForList(sql);
		taLogger.log("Inside getNotNullParamName Method", ApplicationConstants.LOG_DEBUG,sql,ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return paramName;
	}
	/**
	 *
	 * @param cmdRefId
	 * @param param_name
	 * @return
	 */
	public List<Map<String, Object>> getConcatenate(String cmdRefId,String param_name)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select CONCATENATE from C_OM_CMD_PARAM_REF where CMD_REF_ID='"+ cmdRefId+ "'"+ "and param_name='"+ param_name+ "'";
		List<Map<String, Object>> concatenate = jdbcTemplate.queryForList(sql);
		taLogger.log("Inside getConcatenate Method", ApplicationConstants.LOG_DEBUG,sql,loggercategory);
		return concatenate;
	}
	/**
	 *
	 * @param commandRefId
	 * @param param_name
	 * @param key
	 * @return
	 */
	public List<Map<String, Object>> getXPath(String commandRefId,String param_name, String key)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql ="select XPATH, DEFAULT_VALUE, SEQ_NO, LOOKUP_REF_ID from C_OM_CMD_PARAM_MAP where PARAM_SUB='"+ key+ "'"
				   + "and CMD_REF_ID='"+ commandRefId+ "'"+ "and PARAM_NAME='" + param_name + "'";
		List<Map<String, Object>> xPath = jdbcTemplate.queryForList(sql);
		taLogger.log("Inside getXPath Method", ApplicationConstants.LOG_DEBUG,sql,"instrutorlogging");
		return xPath;
	}
	/**
	 *
	 * @param lookupRefId
	 * @param lookupValue
	 * @return
	 * @throws SPLExceptionHandler
	 */
	public String getNewParamValue(String lookupRefId, String lookupValue, String srcTransId, String transId, String cmdRowId,String loggercategory) throws SPLExceptionHandler
	{
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getNewParamValue]lookupRefId="+lookupRefId + ",lookupValue="+lookupValue,loggercategory);
		String newParamValue = "";
		try
		{
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			String sql="select new_value from c_om_cmd_param_lookup_ref where lookup_value='"+ lookupValue + "'" + " and lookup_ref_id='"+ lookupRefId + "'";
			newParamValue = jdbcTemplate.queryForObject(sql, String.class);
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getNewParamValue]Sql = "+sql ,loggercategory);
		}
		catch(EmptyResultDataAccessException exception)
		{
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_ERROR, "[JdbcDatabaseDAO][getNewParamValue][Inside Catch]Exception="+exception.getMessage() + ",lookupValue="+lookupValue,loggercategory, exception);
		}
		return newParamValue;
	}
	/**
	 *
	 * @param row_ID
	 * @param returnCode
	 * @param returnMessage
	 */
	public void instructorErrorUpdate(String row_ID, String returnCode, String returnMessage,String loggercategory)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		taLogger.log("Inside instructorErrorUpdate Method", ApplicationConstants.LOG_DEBUG,"returnCode="+returnCode+"returnMessage="+returnMessage+"row_ID="+row_ID,loggercategory);
		if(returnMessage!=null && !returnMessage.equals("") && returnMessage.length()>500)
		{
			returnMessage = returnMessage.substring(0, 500);
			taLogger.log("Inside instructorErrorUpdate Method", ApplicationConstants.LOG_DEBUG,"returnMessage="+returnMessage,loggercategory);
		}
		String sql="update T_OM_CMD_TRANS set status='" + ApplicationConstants.STATUS_ERROR + "',"+ " RETURN_CODE=?," +"RETURN_MSG= ?,"+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user"+ " where ROW_ID=?" ;
		jdbcTemplate.update(sql,new Object[] {returnCode,returnMessage,row_ID});
		taLogger.log("Inside instructorErrorUpdate Method", ApplicationConstants.LOG_DEBUG,sql,loggercategory);
	}
	/**
	 *
	 * @param rowId
	 * @param cmdRefId
	 * @param transId
	 * @param SRC_TRANS_ID
	 * @param MOLI_ID
	 * @param OLI_ID
	 * @param seqNo
	 * @param corrId
	 * @param insertDt
	 * @param insertBy
	 * @param updatedDt
	 * @param updatedBy
	 * @param orderRowId
	 * @throws SQLException
	 */
	public void insertTransValues(String rowId, String cmdRefId,String transId, String SRC_TRANS_ID, String MOLI_ID, String OLI_ID,BigDecimal seqNo,
				String corrId, Date insertDt, String insertBy,Date updatedDt, String updatedBy, String orderRowId,String loggercategory, boolean immediatelyFlag) throws SQLException
	 {
	 taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][insertTransValues] rowId="+orderRowId +",cmdRefId="+cmdRefId+",transId="+transId+"," +
	 		      "SRC_TRANS_ID="+SRC_TRANS_ID+"," +"MOLI_ID="+MOLI_ID+",OLI_ID="+OLI_ID+",seqNo="+seqNo+",corrId="+corrId+"," +
	 			  "insertDt="+insertDt+",insertBy="+insertBy+",updatedDt="+updatedBy+",updatedBy="+updatedBy+",Order Row Id="+orderRowId ,loggercategory );

		// Modified to prepared statement to get generated key
		PreparedStatement preparedStatement = null;
		Connection connection = null;
		String cmdTransRowId = "";
		try
		{
			String sqlQuery = "INSERT INTO T_OM_CMD_TRANS (ORDER_ROW_ID, CMD_REF_ID, TRANS_ID, SRC_TRANS_ID, MOLI_ID, OLI_ID, STATUS, " +
							  "SEQ_NO, CORR_ID, POLL_STATUS, INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY ) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate, user, sysdate, user)";
			connection = dataSource.getConnection();
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			String columnNames[] = { "ROW_ID" };
			preparedStatement = connection.prepareStatement(sqlQuery, columnNames);
			preparedStatement.setString(1, orderRowId);
			preparedStatement.setString(2, cmdRefId);
			preparedStatement.setString(3, transId);
			preparedStatement.setString(4, SRC_TRANS_ID);
			preparedStatement.setString(5, MOLI_ID);
			preparedStatement.setString(6, OLI_ID);
			preparedStatement.setString(7, ApplicationConstants.STATUS_NOT_STARTED);
			preparedStatement.setBigDecimal(8, seqNo);
			preparedStatement.setString(9, corrId);
			if(!immediatelyFlag)preparedStatement.setString(10, "N");else preparedStatement.setString(10, "Y");
			preparedStatement.executeUpdate();
			ResultSet resultSet = preparedStatement.getGeneratedKeys();
			if (resultSet.next())
			{
				cmdTransRowId = resultSet.getString(1);
				taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransValues]cmdTransRowId ="+cmdTransRowId,loggercategory);
			}

		}
		catch (Exception exception)
		{
			taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_ERROR,"[JdbcDatabaseDAO][insertTransValues][Inside Catch]cmdTransRowId ="+cmdTransRowId,loggercategory, exception);
		}
		finally
		{
			connection.close();
			taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransValues][inside finally]Connection closed",loggercategory);
			preparedStatement.close();
		}

		taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransValues]cmdTransRowId ="+cmdTransRowId,loggercategory);

		if(cmdRefId.equals(ApplicationConstants.ERROR_TASK_CMD_REF_ID))
		{
		  try
		   {
			 insertCommandTransDtlsErrorTask(transId,cmdTransRowId, SRC_TRANS_ID, orderRowId, loggercategory);
		   }
		  catch(Exception e)
		   {
			taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_ERROR,"[JdbcDatabaseDAO][insertTransValues][Inside Catch]cmdRefId.equals(ApplicationConstants.ERROR_TASK_CMD_REF_ID)="+ApplicationConstants.ERROR_TASK_CMD_REF_ID,loggercategory, e);
		   }
	   }

	 }

	/**
	 *
	 * @param rowId
	 * @param parRowId
	 * @param paramName
	 * @param paramValue
	 * @param paramSub
	 * @param xmlEleType
	 * @param xmlEleId
	 * @param xmlEleName
	 * @param xmlEleCurrValue
	 * @param xmlElePrevValue
	 * @param seqNo
	 * @param lkupRefId
	 * @param insertDt
	 * @param insertBy
	 * @param updatedDt
	 * @param updatedBy
	 * @throws Exception
	 */
	private void insertTransDetails(String rowId, String parRowId,String paramName, String paramValue, String paramSub, String xmlEleType,
								   String xmlEleId, String xmlEleName, String xmlEleCurrValue,String xmlElePrevValue, BigDecimal seqNo,
								   String lkupRefId,Date insertDt, String insertBy, Date updatedDt, String updatedBy, String srcTransId, String transId,String loggercategory)throws Exception
	 {

	  taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransDetails]ROW_ID="+rowId+"PAR_ROW_ID="+parRowId+", PARAM_NAME="+paramName+", PARAM_VALUE="+paramValue+", XML_ELE_TYPE="+xmlEleType+", XML_ELE_ID="+xmlEleId+
				", XML_ELE_NAME="+xmlEleName+", XML_ELE_CURR_VALUE="+xmlEleCurrValue+", XML_ELE_PREV_VALUE="+xmlElePrevValue+",SEQ_NO="+seqNo+", LKUP_REF_ID="+lkupRefId+", INSERT_DT="+insertDt+
				", INSERT_BY="+insertBy+", UPDATED_DT="+updatedDt+", UPDATED_BY="+updatedBy ,loggercategory);

	  if (paramValue==null || paramValue.equals("") || paramValue.equalsIgnoreCase("<blank>"))
     {
	 paramValue = "";
	 }
     JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
	 String sqlQuery = "INSERT INTO T_OM_CMD_TRANS_DTLS ("+" PAR_ROW_ID, PARAM_NAME, PARAM_VALUE, PARAM_SUB, XML_ELE_TYPE, XML_ELE_ID, " +
					  "XML_ELE_NAME, XML_ELE_CURR_VALUE, XML_ELE_PREV_VALUE, "+"SEQ_NO, LKUP_REF_ID, INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY )" +
					  "values( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,sysdate,user,sysdate,user)";
	 Object sqlParams[] = new Object[]
	 {
	  parRowId, paramName, paramValue, paramSub, xmlEleType, xmlEleId, xmlEleName,xmlEleCurrValue, xmlElePrevValue, seqNo, lkupRefId
	  };

	 int[] sqlTypes = new int[] { Types.VARCHAR, Types.VARCHAR,Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
								  Types.VARCHAR, Types.VARCHAR, Types.NUMERIC, Types.VARCHAR };
	 int res;
	 res = jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
	 }

	/**
	 * 
	 * @param TomOrderId
	 * @param cmdTransRowId
	 * @param srcTransId
	 * @param orderRowId
	 * @param loggercategory
	 * @throws Exception
	 */
	public void insertCommandTransDtlsErrorTask(String TomOrderId, String cmdTransRowId, String srcTransId, String orderRowId, String loggercategory) throws Exception
	 {
		taLogger.log(srcTransId, TomOrderId, cmdTransRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertCommandTransDtlsErrorTask]Inside Method Start",loggercategory);
		String rowIdErrorTask="";
		String parRowIdErrorTask="";
		String paramNameErrorTask="";
		String paramValueErrorTask="";
		String xmlEleTypeErrorTask="";
		String xmlEleIdErrorTask="";
		String xmlEleNameErrorTask="";
		String xmlElePrevValueErrorTask="";
		String xmlEleCurrValueErrorTask="";
		BigDecimal seqNoErrorTask=null;
		String lkupRefIdErrorTask="";
		String paramSubErrorTask="";
		//List<Map<String, Object>> getRowIdErrorTaskList= getRowIdErrorTask(TomOrderId);
		List<Map<String, Object>> getRowIdErrorTaskList= getRowIdErrorTask(srcTransId, TomOrderId, orderRowId, loggercategory);
		String parRowId = "";
		for (Object getRowIdErrorTaskListItr : getRowIdErrorTaskList)
		{
			Map newMap = (Map) getRowIdErrorTaskListItr;
			parRowId = "" + (BigDecimal) newMap.get("ROW_ID");
			List<Map<String, Object>> getCmdTransDetailsErrorTask= getCmdTransDetailsErrorTask(parRowId);
			for (Object getCmdTransDetailsErrorTaskItr : getCmdTransDetailsErrorTask)
			 {
				Map getCmdTransDetailsErrorTaskMap = (Map) getCmdTransDetailsErrorTaskItr;
				parRowIdErrorTask=(String)getCmdTransDetailsErrorTaskMap.get("PAR_ROW_ID");
				paramNameErrorTask=(String)getCmdTransDetailsErrorTaskMap.get("PARAM_NAME");
				paramValueErrorTask=(String)getCmdTransDetailsErrorTaskMap.get("PARAM_VALUE");
				xmlEleTypeErrorTask=(String)getCmdTransDetailsErrorTaskMap.get("XML_ELE_TYPE");
				xmlEleIdErrorTask=(String)getCmdTransDetailsErrorTaskMap.get("XML_ELE_ID");
				xmlEleNameErrorTask=(String)getCmdTransDetailsErrorTaskMap.get("XML_ELE_NAME");
				xmlElePrevValueErrorTask=(String)getCmdTransDetailsErrorTaskMap.get("XML_ELE_PREV_VALUE");
				xmlEleCurrValueErrorTask=(String)getCmdTransDetailsErrorTaskMap.get("XML_ELE_CURR_VALUE");
				seqNoErrorTask=(BigDecimal)getCmdTransDetailsErrorTaskMap.get("SEQ_NO");
				lkupRefIdErrorTask=(String)getCmdTransDetailsErrorTaskMap.get("LKUP_REF_ID");
				paramSubErrorTask=(String)getCmdTransDetailsErrorTaskMap.get("PARAM_SUB");
				insertTransDetailsValues(rowIdErrorTask, cmdTransRowId,paramNameErrorTask, paramValueErrorTask, xmlEleTypeErrorTask, xmlEleIdErrorTask,
						                 xmlEleNameErrorTask,xmlEleCurrValueErrorTask,xmlElePrevValueErrorTask, seqNoErrorTask,lkupRefIdErrorTask,
						                 new Date(),"DBASLAPP", new Date(), "DBASLAPP", TomOrderId, srcTransId,loggercategory);
				taLogger.log(srcTransId, TomOrderId, cmdTransRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertCommandTransDtlsErrorTask]Values Inserted",loggercategory);
			  }
			// Added to update cmd trans status as 'New'
			updateCmdTransStatus(parRowId);
		}
	}
	/**
	 * 
	 * @param srcTransId
	 * @param transId
	 * @param orderRowId
	 * @param loggerCategory
	 * @return
	 * @throws SQLException
	 */
	public List<Map<String,Object>> getRowIdErrorTask(String srcTransId, String transId, String orderRowId, String loggerCategory) throws SQLException
	{
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getRowIdErrorTask] transId " + transId , loggerCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		List<Map<String,Object>> commandTransDetailsList = jdbcTemplate.queryForList(ApplicationConstants.SQL_GET_CMD_TRANS_ERROR_TASK, new Object[] {transId, orderRowId});
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_INFO, "[DataBaseDAO][getRowIdErrorTask] commandTransDetailsList " + commandTransDetailsList, loggerCategory);
		return commandTransDetailsList;
    }
	/**
	 *
	 * @param parRowId
	 * @return
	 * @throws SQLException
	 */
	public List<Map<String,Object>> getCmdTransDetailsErrorTask(String parRowId) throws SQLException
	{
		taLogger.log("DataBaseDAO=>getCmdTransDetailsErrorTask", ApplicationConstants.LOG_INFO, "Par Row Id for getCmdTransDetailsErrorTask=" + parRowId , ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		List<Map<String,Object>> commandTransDetailsErrorTask = jdbcTemplate.queryForList(ApplicationConstants.SQL_GET_CMD_TRANS_DTLS_ERROR_TASK, new Object[] {parRowId});
		taLogger.log("DataBaseDAO=>getCmdTransDetailsErrorTask", ApplicationConstants.LOG_INFO, "Cmd Trans Values : \n"+commandTransDetailsErrorTask, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		return commandTransDetailsErrorTask;
    }
	/**
	 *
	 * @param rowId
	 * @param parRowId
	 * @param paramName
	 * @param paramValue
	 * @param xmlEleType
	 * @param xmlEleId
	 * @param xmlEleName
	 * @param xmlEleCurrValue
	 * @param xmlElePrevValue
	 * @param seqNo
	 * @param lkupRefId
	 * @param insertDt
	 * @param insertBy
	 * @param updatedDt
	 * @param updatedBy
	 * @throws Exception
	 */
	public void insertTransDetailsValues(String rowId, String parRowId,String paramName, String paramValue, String xmlEleType,
										 String xmlEleId, String xmlEleName, String xmlEleCurrValue,String xmlElePrevValue, BigDecimal seqNo,
										 String lkupRefId,Date insertDt, String insertBy, Date updatedDt, String updatedBy, String transId, String srcTransId,String loggercategory)throws Exception
	{
		taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][insertTransDetailsValues] ROW_ID="+rowId+",PAR_ROW_ID="+parRowId+", PARAM_NAME="+paramName+", " +
				     "PARAM_VALUE="+paramValue+", XML_ELE_TYPE="+xmlEleType+", XML_ELE_ID="+xmlEleId+", " +"XML_ELE_NAME="+xmlEleName+", " +
				     "XML_ELE_CURR_VALUE="+xmlEleCurrValue+", XML_ELE_PREV_VALUE="+xmlElePrevValue+",SEQ_NO="+seqNo+", LKUP_REF_ID="+lkupRefId+", " +
				     "INSERT_DT="+insertDt+", INSERT_BY="+insertBy+", UPDATED_DT="+updatedDt+", UPDATED_BY="+updatedBy, loggercategory);

		if (paramValue.equalsIgnoreCase("<blank>"))
		{
			paramValue = "";
		}

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sqlQuery = "INSERT INTO T_OM_CMD_TRANS_DTLS ("+" PAR_ROW_ID, PARAM_NAME, PARAM_VALUE, XML_ELE_TYPE, XML_ELE_ID, XML_ELE_NAME, XML_ELE_CURR_VALUE, XML_ELE_PREV_VALUE, "
				        + "SEQ_NO, LKUP_REF_ID, INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY ) values( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,sysdate,user,sysdate,user)";

		Object sqlParams[] = new Object[] { parRowId, paramName, paramValue, xmlEleType, xmlEleId, xmlEleName,xmlEleCurrValue, xmlElePrevValue, seqNo, lkupRefId };
		int[] sqlTypes = new int[] { Types.VARCHAR, Types.VARCHAR,Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,Types.VARCHAR, Types.VARCHAR, Types.NUMERIC, Types.VARCHAR };
		int res;
		res = jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
	}
	/**
	 *
	 * @param trans_Id
	 * @param orderRowId
	 */
	public void deleteCommandTransDetailsRecords(String trans_Id, String orderRowId, String srcTransId)
	{
		taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][deleteCommandTransDetailsRecords] Inside Method Start", "instrutorlogging");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="delete from t_om_cmd_trans_dtls where par_row_id in (select row_id from t_om_cmd_trans where order_row_id='"+ orderRowId + "' and upper(status)=upper('Error'))";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][deleteCommandTransDetailsRecords] Delete Sql= " + sql, "instrutorlogging");
		String sql1="update t_om_cmd_trans set status='Not Started',poll_status='N' where row_id in (select row_id from t_om_cmd_trans where order_row_id='"+ orderRowId + "' and upper(status)=upper('Error'))";
		jdbcTemplate.update(sql1);
		taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][deleteCommandTransDetailsRecords] Update Sql= " + sql1, "instrutorlogging");
	}
	/**
	 *
	 * @param commands
	 * @param newRowId
	 * @return
	 */
	public int getNoOfCommands(int commands, String srcTransId,String transid,String newRowId,String loggerCategory)
	{
		taLogger.log(srcTransId,transid,newRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][getNoOfCommands] newRowId="+newRowId+"commands"+commands,loggerCategory);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select ORDER_ROW_ID, TRANS_ID from T_OM_CMD_TRANS where ROW_ID='"+ newRowId + "'";
		List<Map<String, Object>> transId = jdbcTemplate.queryForList(sql);
		taLogger.log(srcTransId,transid,newRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][getNoOfCommands]TransId size :"+transId.size()+ " SQL : "+sql,loggerCategory);
		for (Object id : transId)
		{
			Map map = (Map) id;
			String trans_id = (String) map.get("TRANS_ID");
			String orderRowId = (String) map.get("ORDER_ROW_ID");
			String sql1="select COUNT(STATUS) AS NUMBEROFCOMMANDS from T_OM_CMD_TRANS where ORDER_ROW_ID='" + orderRowId + "' and TRANS_ID='"+ trans_id+ "'"+ " and STATUS IN ('"+ApplicationConstants.STATUS_NOT_STARTED + "','"+ApplicationConstants.STATUS_ERROR + "','"+ApplicationConstants.STATUS_NEW + "')";
			commands = jdbcTemplate.queryForInt(sql1);
		}
		taLogger.log(srcTransId,transid,newRowId, ApplicationConstants.LOG_DEBUG,"Commands returned ="+commands,loggerCategory);
		return commands;
	}
	/**
	 * 
	 * @param commands
	 * @param cmdRowId
	 * @return
	 */
	public int getNoOfCommandsBill(int commands, String cmdRowId,String transId, String cmdRefId)
	{
		taLogger.log("Inside getNoOfCommandsBill Method", ApplicationConstants.LOG_DEBUG,"In getNoOfCommandsBill cmdRowId="+cmdRowId+" commands="+commands,"instrutorlogging");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			String sql1="select COUNT(STATUS) AS NUMBEROFCOMMANDS from T_OM_BILL_CMD_TRANS  where TRANS_ID='"+ transId+ "'"+ " and STATUS IN ('"+ApplicationConstants.STATUS_RECEIVED + "','"+ApplicationConstants.STATUS_NEW+"') ";
			taLogger.log("Inside getNoOfCommandsBill Method", ApplicationConstants.LOG_DEBUG,sql1,"instrutorlogging");
			commands = jdbcTemplate.queryForInt(sql1);
		
		taLogger.log("Inside getNoOfCommandsBill Method", ApplicationConstants.LOG_DEBUG,"commands returned ="+commands,"instrutorlogging");
		return commands;
	}
	
	/**
	 *
	 * @param newRowId
	 * @return
	 */
	public List<Map<String, Object>> getOrderXml(String newRowId)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="select  ord.ROW_ID, ord.ORDER_XML, ord.TRANS_ID from T_OM_ORDER_TRANS_DTLS ord where ord.ROW_ID= (select cmd.ORDER_ROW_ID from T_OM_CMD_TRANS cmd where cmd.ROW_ID='"+ newRowId + "'" + ") and FUNC_REF_ID not in ('ERRTASK.0001')";
		List<Map<String, Object>> orderXml = jdbcTemplate.queryForList(sql);
		taLogger.log("Inside getOrderXml Method", ApplicationConstants.LOG_DEBUG,sql,"instrutorlogging");
		return orderXml;
	}
	/**
	 *
	 * @param newRowId
	 * @param returnCode
	 * @param returnMessage
	 * @return
	 * @throws Exception
	 */
	//Added splFlag parameter for RIM SIM Exchange special case error handling 
	public String updateStatus(String newRowId, String returnCode,String returnMessage,String splFlag) throws Exception
	{
		String rOWID = "";
		BigDecimal seqNumber = null;
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		taLogger.log("Inside updateStatus Method ", ApplicationConstants.LOG_DEBUG,"newRowId="+newRowId+"returnCode="+returnCode+"returnMessage="+returnMessage,"instrutorlogging");

		if (returnCode.equals(ApplicationConstants.TOM_RESPONSE_SUCCESS))
		 {
			jdbcTemplate.update("update T_OM_CMD_TRANS set "+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user,"+ "RETURN_CODE='" + returnCode + "', RETURN_MSG='" + returnMessage + "', "
							  + "STATUS=" + "'" + ApplicationConstants.STATUS_COMPLETED+ "'" + " where ROW_ID= '" + newRowId + "'");
			taLogger.log("Inside updateStatus Method ", ApplicationConstants.LOG_DEBUG,"errorCode == '' && errorMessage == '' update T_OM_CMD_TRANS set "
					+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user,"+ "STATUS=" + "'" + ApplicationConstants.STATUS_COMPLETED+ "'" + " where ROW_ID= '" + newRowId + "'","instrutorlogging");
		 }
		else
		 {
			if(splFlag.trim().equalsIgnoreCase("Y"))
			{
				jdbcTemplate.update("update T_OM_CMD_TRANS set "+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user,"+ "RETURN_CODE='" + returnCode + "', RETURN_MSG='" + returnMessage + "', "
						  + "STATUS=" + "'" + ApplicationConstants.STATUS_ERROR + "Temp'"+ " where ROW_ID='" + newRowId + "'");
				taLogger.log("Inside updateStatus Method ", ApplicationConstants.LOG_DEBUG,"update T_OM_CMD_TRANS set "+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user,"
				   + "STATUS=" + "'" + ApplicationConstants.STATUS_ERROR + "Temp'"+ " where ROW_ID='" + newRowId + "'","instrutorlogging");
			
			}
			else{
			jdbcTemplate.update("update T_OM_CMD_TRANS set "+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user,"+ "RETURN_CODE='" + returnCode + "', RETURN_MSG='" + returnMessage + "', "
							  + "STATUS=" + "'" + ApplicationConstants.STATUS_ERROR + "'"+ " where ROW_ID='" + newRowId + "'");
			taLogger.log("Inside updateStatus Method ", ApplicationConstants.LOG_DEBUG,"update T_OM_CMD_TRANS set "+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user,"
					   + "STATUS=" + "'" + ApplicationConstants.STATUS_ERROR + "'"+ " where ROW_ID='" + newRowId + "'","instrutorlogging");
			}
		 }

		return newRowId;
	}
	/**
	 *
	 * @param newRowId
	 */
	 public void finalSuccessUpdate(String newRowId)
	 {
	    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
	    String sql = "update T_OM_ORDER_TRANS_DTLS set STATUS='"+ ApplicationConstants.STATUS_COMPLETED+ "',"+ "UPDATED_DT=sysdate,"+ "UPDATED_BY="+ "user"
			    + " where ROW_ID=(select ORDER_ROW_ID from T_OM_CMD_TRANS where ROW_ID= '"+ newRowId + "')";
	    jdbcTemplate.update(sql);
	    taLogger.log("Inside finalSuccessUpdate Method ", ApplicationConstants.LOG_DEBUG,sql,"instrutorlogging");
	 }
	 /**
	 *
	 * @param newRowId
	 */
	 public void finalSuccessUpdateBill(String newRowId)
	 {
	    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
	    String sql = "update T_OM_ORDER_TRANS_DTLS set STATUS='"+ ApplicationConstants.STATUS_COMPLETED+ "',"+ "UPDATED_DT=sysdate,"+ "UPDATED_BY="+ "user"
			    + " where ROW_ID=(select ORDER_ROW_ID from T_OM_BILL_CMD_TRANS where ROW_ID= '"+ newRowId + "')";
	    jdbcTemplate.update(sql);
	    taLogger.log("Inside finalSuccessUpdateBill Method ", ApplicationConstants.LOG_DEBUG,sql,"instrutorlogging");
	 }
	 
	 /**
	  * 
	  * @param orderRowId
	  */
	 public void orderTransDetailsErrorUpdate(String orderRowId){
	    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
	    String sql="update T_OM_ORDER_TRANS_DTLS set STATUS='"+ ApplicationConstants.STATUS_ERROR+"',"+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user"+ " where ROW_ID='" + orderRowId + "'";
	    jdbcTemplate.update(sql);
	    taLogger.log("Inside orderTransDetailsErrorUpdate Method ", ApplicationConstants.LOG_DEBUG,sql,"instrutorlogging");
	 }
	 
	 /**
	  *
	  * @param APPLN_NAME
	  * @param ORDER_ID
	  * @param CMD_REF_ID
	  * @param ERR_SRC
	  * @param ERR_CODE
	  * @param ERR_MSG
	  */
	 public void insertError(String APPLN_NAME, String ORDER_ID,String CMD_REF_ID, String ERR_SRC, String ERR_CODE, String ERR_MSG)
	 {
	  try
	   {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			String sqlQuery = "insert into T_OM_ERR_LOG(APPLN_NAME,ORDER_ID,CMD_REF_ID,ERR_SRC,ERR_CODE,ERR_MSG,INSERT_DT,INSERT_BY,UPDATED_DT,UPDATED_BY) values (?,?,?,?,?,?,sysdate,user,sysdate,user)";
			Object sqlParams[] = new Object[] { APPLN_NAME, ORDER_ID,CMD_REF_ID, ERR_SRC, ERR_CODE, ERR_MSG };
			int[] sqlTypes = new int[] { Types.VARCHAR, Types.VARCHAR,Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.CLOB };
			jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
	   }
	   catch (Exception e)
	   {
			e.printStackTrace();
	   }
	}
	 /**
	  *
	  * @param newRowId
	  * @param errorCode
	  * @param errorMessage
	  */
	public void statusRetryUpdate(String newRowId, String errorCode,String errorMessage)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.update("update T_OM_CMD_TRANS set "+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user," + "STATUS= '"+ ApplicationConstants.STATUS_RETRY + "' where ROW_ID="+ newRowId);
		taLogger.log("Inside statusRetryUpdate Method ", ApplicationConstants.LOG_DEBUG,"update T_OM_CMD_TRANS set "+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user," +
					 "STATUS= '"+ ApplicationConstants.STATUS_RETRY + "' where ROW_ID="+ newRowId,"instrutorlogging");
		jdbcTemplate.update("update T_OM_CMD_TRANS set "+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user,"+ "RETURN_CODE='"+ errorCode + "'"
					 	  + ", RETURN_MSG='" + errorMessage + "'"+ " where ROW_ID='" + newRowId + "'");
		taLogger.log("Inside statusRetryUpdate Method ", ApplicationConstants.LOG_DEBUG,"update T_OM_CMD_TRANS set "+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user,"
				   + "RETURN_CODE='"+ errorCode + "'" + ", RETURN_MSG='" + errorMessage + "'"+ " where ROW_ID='" + newRowId + "'","instrutorlogging");
	}
	/**
	 *
	 * @param cmdRowId
	 * @param cmdRefId
	 * @param transId
	 * @param responseMessage
	 * @param interfaceName
	 * @throws SQLException
	 */
	 public void saveCommandResponse(String cmdRowId, String cmdRefId, String transId, String responseMessage, String interfaceName) throws SQLException
	 {
		taLogger.log("", transId, cmdRowId, ApplicationConstants.LOG_INFO, "[JdbcDataBaseDAO][saveCommandResponse]\ncmdRowId="+cmdRowId+"\ncmdRefId=" + cmdRefId+"\ntransId="+transId+"\nresponseMessage="+responseMessage+"\ninterfaceName="+interfaceName, ApplicationConstants.LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sqlQuery = ApplicationConstants.SQL_SAVE_COMMAND_RESPONSE;
		Object sqlParams[] = new Object[] {cmdRowId, cmdRefId, transId, responseMessage, interfaceName, ApplicationConstants.STATUS_NEW};
		int[] sqlTypes = new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.CLOB, Types.VARCHAR, Types.VARCHAR};
		jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
	 }
	 /**
	  *
	  * @param cmdRefId
	  * @return
	  * @throws SQLException
	  * @throws SPLExceptionHandler
	  */
	 public CommandTransDetails getSystemDetails(String cmdRefId) throws SQLException, SPLExceptionHandler
	 {
		CommandTransDetails commandTransDetails = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		List<CommandTransDetails> commandTransList = jdbcTemplate.query(ApplicationConstants.SQL_GET_SYSTEMDETAILS, new Object[] {cmdRefId}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
		if(commandTransList.size()!=0)
			commandTransDetails = commandTransList.get(0);
		else
			throw new SPLExceptionHandler("Exception in getSystemDetails: No System Details found for Command " + cmdRefId);
		return commandTransDetails;
    }
	 /**
	  *
	  * @param commandRefId
	  * @param paramName
	  * @return
	  */
     public List<Map<String, Object>> getCmdTransDtlsByParamName(String commandRefId,String paramName)
     {
    	JdbcTemplate jdbcTemplate = getJdbcTemplate();
    	String sqlQuery = "select XPATH, DEFAULT_VALUE, PARAM_SUB, SEQ_NO, LOOKUP_REF_ID, cmdParamRef.SPL_FUNC_REF_ID, cmdParamMap.xml_ele_type " +
    					  "from C_OM_CMD_PARAM_MAP cmdParamMap, C_OM_CMD_PARAM_REF cmdParamRef " +"where cmdParamMap.PARAM_SUB !='-' and cmdParamMap.CMD_REF_ID='" + commandRefId + "' "+
    					  "and cmdParamMap.CMD_REF_ID = cmdParamRef.CMD_REF_ID " +"and cmdParamMap.PARAM_NAME='"+paramName+"' " +"and cmdParamMap.PARAM_NAME=cmdParamRef.PARAM_NAME";
        taLogger.log("Inside getXPathMSISDN Method", ApplicationConstants.LOG_DEBUG,sqlQuery,"instrutorlogging");
        List<Map<String, Object>> cmdTransDtlsList = jdbcTemplate.queryForList(sqlQuery);
        return cmdTransDtlsList;
     }
    /**
     *
     * @param commandRefId
     * @return
     */
     public List<Map<String, Object>> getCmdParamRefValues(String commandRefId, String srcTransId, String transId, String cmdRowId) {
    	 taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getCmdParamRefValues]commandRefId="+commandRefId,"instrutorlogging");
     	JdbcTemplate jdbcTemplate = getJdbcTemplate();
     	String sql="select param_name, spl_func_ref_id, concatenate from c_om_cmd_param_ref where cmd_ref_id='" + commandRefId + "'";
     	List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
     	taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getCmdParamRefValues]Sql="+sql,"instrutorlogging");
        return list;
     }

     /**
      *
      * @param cmdRefId
      * @param paramName
      * @return
      */
     public String getDefaultValue(String cmdRefId, String paramName)
     {
    	JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    	String sql="select default_value from c_om_cmd_param_map where cmd_ref_id='" +cmdRefId + "'" +" and param_sub='DateFormat' and param_name='"+ paramName +"'" ;
    	String defaultValue = jdbcTemplate.queryForObject(sql, String.class);
    	taLogger.log("Inside getXPathMSISDN Method", ApplicationConstants.LOG_DEBUG,sql,"instrutorlogging");
    	taLogger.log("Inside getXPathMSISDN Method", ApplicationConstants.LOG_DEBUG,"default value is" + defaultValue,"instrutorlogging");
    	return defaultValue;
     }
     /**
      *
      * @param correlationId
      * @param status
      */
     public void updateCommandTransactionStatus(String correlationId, String status)
	 {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sql="update T_OM_CMD_TRANS set STATUS = ? where ROW_ID = ?";
		jdbcTemplate.update(sql,new Object[] { status, correlationId });
	 }
     /**
      *
      * @param rowId
      */
     public void updateCmdTransStatus(String rowId)
	 {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sql="UPDATE T_OM_CMD_TRANS set STATUS = ? where ROW_ID = ? AND upper(STATUS)=upper(?)";
		jdbcTemplate.update(sql,new Object[] { ApplicationConstants.STATUS_NEW, rowId, ApplicationConstants.STATUS_NOT_STARTED });
	 }
     
     /*Sudharsan : Updating all the commands for same trans id from 'Not started' to 'New' after inserting parameter details for all commands in T_OM_BILL_CMD_TRANS_DTLS*/
     public void updateBillCmdTransStatus(String rowId,String transId,String loggerCategory )
	 {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sql="UPDATE T_OM_BILL_CMD_TRANS set STATUS = ? where ORDER_ROW_ID = ? AND trans_id= ? AND  upper(STATUS)=upper(?)";
		taLogger.log("updateBillCmdTransStatus", ApplicationConstants.LOG_INFO, "Sql : " + sql +" ORDER_ROW_ID="+rowId+" transId="+transId, loggerCategory);
		jdbcTemplate.update(sql,new Object[] { ApplicationConstants.STATUS_NEW, rowId,transId, ApplicationConstants.STATUS_NOT_STARTED });
	 }
     /**
      *
      * @param cmdRefId
      * @return
      * @throws SQLException
      */
	 public CommandTransDetails getCmdDestRefDtls(String cmdRefId) throws SQLException
	 {
		//taLogger.log("getCmdDestRefDtls", ApplicationConstants.LOG_INFO, "cmdRefId " + cmdRefId, "ILrequestadaptorlogging");
		List<CommandTransDetails> cmdTransactonDetailsList = null;
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sqlQuery = "SELECT CMDSYSMAP.SYSTEM_NAME,CMDSYSMAP.INTERFACE_NAME,CMDSYSMAP.PARAMETER_VALUE,INTERFACEREF.USER_NAME, INTERFACEREF.PWD,INTERFACEREF.LOCATION, " +
				"INTERFACEREF.TECH_METHOD, INTERFACEREF.ACCOUNT_TYPE, INTERFACEREF.TIMEOUT, INTERFACEREF.READ_TIMEOUT AS READTIMEOUT, INTERFACEREF.REQUEST_MSG_FLAG " +
				//"FROM C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_CMD_DEST_REF CMDDESTREF " +
				"FROM C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_INTERFACE_REF INTERFACEREF " +
				"WHERE CMDSYSMAP.CMD_REF_ID=? AND CMDSYSMAP.SYSTEM_NAME= INTERFACEREF.SYSTEM_NAME AND CMDSYSMAP.INTERFACE_NAME= INTERFACEREF.INTERFACE_NAME ";
		cmdTransactonDetailsList = jdbcTemplate.query(sqlQuery, new Object[] {cmdRefId}, new BeanPropertyRowMapper<CommandTransDetails>(CommandTransDetails.class));
		CommandTransDetails commandTransDetails = null;
		if(cmdTransactonDetailsList.size()!=0)
			commandTransDetails = cmdTransactonDetailsList.get(0);
		//taLogger.log("getCmdDestRefDtls", ApplicationConstants.LOG_INFO, "cmdRefId " + cmdRefId, "ILrequestadaptorlogging");
		return commandTransDetails;
	 }
	/**
	 *
	 * @param rowId
	 * @return
	 */
	 public String getCmdTransDetails(String rowId, String transId) {
	    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
	    List<Map<String, Object>> cmdTransRow = jdbcTemplate
				.queryForList("select * from T_OM_CMD_TRANS where ROW_ID='" + rowId + "'");
	    Map cmdTransDetails1 = (Map)cmdTransRow.get(0);
		String srcTransId=(String)cmdTransDetails1.get(ApplicationConstants.COLUMN_SRC_TRANS_ID);  //For Logging src_trans_id needed
		taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getCmdTransDetails]Sql=select SRC_TRANS_ID from T_OM_CMD_TRANS where ROW_ID='" + rowId + "'   srcTransId="+srcTransId,"adapterlogging");
	    return srcTransId;
	  }
	 /**
	     *
	     * @param parRowId
	     * @return
	     */
	 public List<Map<String, Object>>getXmlEleValues(String parRowId){

		 taLogger.log("Inside getCmdTransDetails Method",ApplicationConstants.LOG_DEBUG,"parRowId is" + parRowId, "splwstranslatorlogging");
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 List<Map<String, Object>> list = jdbcTemplate
					.queryForList("select xml_ele_type, xml_ele_id, xml_ele_name, xml_ele_curr_value, xml_ele_prev_value from t_om_cmd_trans_dtls where par_row_id='" + parRowId + "'" + "and UPDATED='Y'");
		 taLogger.log("Inside getCmdTransDetails Method",ApplicationConstants.LOG_DEBUG,"select xml_ele_type, xml_ele_id, xml_ele_name, xml_ele_curr_value, xml_ele_prev_value from t_om_cmd_trans_dtls where par_row_id='" + parRowId, "splwstranslatorlogging");

		 return list;
	 }
	 /**
	     *
	     * @param transId
	     * @return
	     */
	 public String getTaskResponse(String rowId){

		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql="select task_response from t_om_cmd_trans where row_id='" + rowId + "'";
		 taLogger.log("Inside getTaskResponse Method",ApplicationConstants.LOG_DEBUG,"SQL: " + sql, "splwstranslatorlogging");
		 String taskResponse = jdbcTemplate.queryForObject(sql, String.class);
			return taskResponse;
	 }
	 /**
	  * 
	  * @param c1OrderId
	  * @param loggerCategory
	  * @return
	  * @throws ApiException
	  */
	 public String getC1OrderStatus(String c1OrderId,String loggerCategory ) throws ApiException {
		 JAASClient jaasClient = new JAASClient ();
		 String securityToken = jaasClient.login (DEFAULT_USERNAME, DEFAULT_REALM, DEFAULT_PASSWORD);
		 UserContext userContext = UserContext.getInstance();
		 userContext.setUser (DEFAULT_USERNAME);
		 userContext.setSecurityToken (securityToken);

		 OrderIdentifier OrderId = new OrderIdentifier();
		 OrderId.setOrderId( Long.parseLong(c1OrderId));//34009043);			
		 OrderClient orderClient = new OrderClient();

		 OrderGetOutputMessage orderOutputmsg = orderClient.orderGet(OrderId);
		 String c1OrderStatus= orderOutputmsg.getOutput().getOrderStatusId().toString();
		 taLogger.log("Inside getC1OrderStatus Method",ApplicationConstants.LOG_DEBUG,"Status "+ orderOutputmsg.getOutput().getOrderStatusId(), loggerCategory);
		 taLogger.log("Inside getC1OrderStatus Method",ApplicationConstants.LOG_DEBUG,"C1 Order Status : "+ c1OrderStatus, loggerCategory);

		 return c1OrderStatus;
	 }
	 
	 /**
	  * 
	  * @param rowId
	  * @param loggerCategory
	  */
	 public void updateBillCmdResponse(String rowId, String c1OrderStatus, String loggerCategory) {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sql = "";
		taLogger.log("updateBillCmdResponse", ApplicationConstants.LOG_INFO, "Bill Command Response Row Id : " + rowId , loggerCategory);
		if (ApplicationConstants.C1ORDER_STATUS.equalsIgnoreCase(c1OrderStatus)
				|| c1OrderStatus.equalsIgnoreCase(ApplicationConstants.C1ORDER_STATUS_COMMITTED)) {
			sql = "UPDATE T_OM_BILL_CMD_RESPONSE set STATUS = ?, POLL_STATUS=?, C1_ORDER_STATUS_ID =? where ROW_ID = ?";
			taLogger.log("updateBillCmdResponse", ApplicationConstants.LOG_INFO, "Sql : "+sql , loggerCategory);
			jdbcTemplate.update(sql, new Object[] {ApplicationConstants.STATUS_RECEIVED, "Y", c1OrderStatus, rowId});
		}else if ("-".equals(c1OrderStatus)) {
			sql = "UPDATE T_OM_BILL_CMD_RESPONSE set STATUS = ?, POLL_STATUS=?, C1_ORDER_STATUS_ID =? where ROW_ID = ?";
			taLogger.log("updateBillCmdResponse", ApplicationConstants.LOG_INFO, "Sql : "+sql , loggerCategory);
			jdbcTemplate.update(sql, new Object[] {ApplicationConstants.STATUS_RECEIVED, "Y", null, rowId});
		}else {
			sql = "UPDATE T_OM_BILL_CMD_RESPONSE set POLL_STATUS=?, C1_ORDER_STATUS_ID =? where ROW_ID = ?";
			taLogger.log("updateBillCmdResponse", ApplicationConstants.LOG_INFO, "Sql : "+sql , loggerCategory);
			jdbcTemplate.update(sql, new Object[] {"Y", c1OrderStatus, rowId});
		}
	 }

/*M.Rahman: Insert into T_OM_BILL_CMD_TRANS table: Begin*/	 
	 /**
	 *
	 * @param rowId
	 * @param cmdRefId
	 * @param transId
	 * @param SRC_TRANS_ID
	 * @param MOLI_ID
	 * @param OLI_ID
	 * @param seqNo
	 * @param corrId
	 * @param custId
	 * @param effectiveDate
	 * @param insertDt
	 * @param insertBy
	 * @param updatedDt
	 * @param updatedBy
	 * @param orderRowId
	 * @throws SQLException
	 */
	public void insertTransValuesBill(String rowId, String cmdRefId,String transId, String SRC_TRANS_ID, String MOLI_ID, String OLI_ID,BigDecimal seqNo,
				String corrId, String crmSvcId, Date effectiveDate, Date insertDt, String insertBy,Date updatedDt, String updatedBy, String orderRowId,String loggercategory) throws SQLException
	 {
	taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][insertTransValuesBill] rowId="+rowId+",cmdRefId="+cmdRefId+",transId="+transId+"," +
	 		      "SRC_TRANS_ID="+SRC_TRANS_ID+"," +"MOLI_ID="+MOLI_ID+",OLI_ID="+OLI_ID+",seqNo="+seqNo+",corrId="+corrId+"," +"crmSvcId="+crmSvcId+",effectiveDate="+effectiveDate+
	 			  "insertDt="+insertDt+",insertBy="+insertBy+",updatedDt="+updatedBy+",updatedBy="+updatedBy+",Order Row Id="+orderRowId ,loggercategory );

		// Modified to prepared statement to get generated key
		
		
		PreparedStatement preparedStatement = null;
		Connection connection = null;
		String cmdTransRowId = "";
		try
		{
			String sqlQuery = "INSERT INTO T_OM_BILL_CMD_TRANS (ORDER_ROW_ID, CMD_REF_ID, TRANS_ID, SRC_TRANS_ID, MOLI_ID, OLI_ID, STATUS, " +
							  "SEQ_NO, CORR_ID, CRM_SVC_ID, POLL_STATUS, EFFECTIVE_DT,  INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY) " +
							  "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate, user, sysdate, user)";
			
			java.sql.Date effectiveDateSQL = new java.sql.Date(effectiveDate.getTime());
			connection = dataSource.getConnection();
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			String columnNames[] = { "ROW_ID" };
			preparedStatement = connection.prepareStatement(sqlQuery, columnNames);
			preparedStatement.setString(1, orderRowId);
			preparedStatement.setString(2, cmdRefId);
			preparedStatement.setString(3, transId);
			preparedStatement.setString(4, SRC_TRANS_ID);
			preparedStatement.setString(5, MOLI_ID);
			preparedStatement.setString(6, OLI_ID);
			preparedStatement.setString(7, ApplicationConstants.STATUS_NOT_STARTED);
			preparedStatement.setBigDecimal(8, seqNo);
			preparedStatement.setString(9, corrId);
			preparedStatement.setString(10, crmSvcId);
			preparedStatement.setString(11, "N");
			preparedStatement.setDate(12, effectiveDateSQL);
			preparedStatement.executeUpdate();
			ResultSet resultSet = preparedStatement.getGeneratedKeys();
			
			if (resultSet.next())
			{
				cmdTransRowId = resultSet.getString(1);
				taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransValues]cmdTransRowId ="+cmdTransRowId,loggercategory);
			}

		}
		catch (Exception exception)
		{
			//taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransValues][Inside Catch]cmdTransRowId ="+cmdTransRowId,loggercategory, exception);
			//System.out.println(exception.getMessage());
		}
		finally
		{
			connection.close();
			taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransValues][inside finally]Connection closed",loggercategory);
			preparedStatement.close();
		}

		//taLogger.log("Inside insertTransValues", ApplicationConstants.LOG_DEBUG,"cmdTransRowId ="+cmdTransRowId,"instrutorlogging");
		taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransValues]cmdTransRowId ="+cmdTransRowId,loggercategory);

		if(cmdRefId.equals(ApplicationConstants.ERROR_TASK_CMD_REF_ID))
		{
		  try
		   {
			 insertCommandTransDtlsErrorTask(transId,cmdTransRowId, SRC_TRANS_ID, orderRowId, loggercategory);
		   }
		  catch(Exception e)
		   {
			taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransValues][Inside Catch]cmdRefId.equals(ApplicationConstants.ERROR_TASK_CMD_REF_ID)="+ApplicationConstants.ERROR_TASK_CMD_REF_ID,loggercategory, e);
		   }
	   }

	 }
	
	/*M.Rahman: Look for Billing related commands in the T_OM_BILL_CMD_TRANS table*/
	/**
	 *
	 * @param trans_Id
	 * @param commndRefId
	 * @param orderTransRowId
	 * @return
	 */
	public List<Map<String, Object>> getNewCmdRefIdBill(String trans_Id,String commndRefId, String orderTransRowId, String srcTransId,String loggercategory)
	{
		taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefId] Inside Method commndRefId = "+commndRefId, loggercategory);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		if (commndRefId.equals(ApplicationConstants.ERROR_TASK_CMD_REF_ID))
		{
			String sql ="select row_id, cmd_ref_id from T_OM_BILL_CMD_TRANS where TRANS_ID='"+ trans_Id+ "'"
					   + " and cmd_ref_id='"+ApplicationConstants.ERROR_TASK_CMD_REF_ID +"' and status in ('"+ApplicationConstants.STATUS_NEW+"','"
					   + ApplicationConstants.STATUS_NOT_STARTED+"')";
			
			List<Map<String, Object>> newCmdRefId = jdbcTemplate.queryForList(sql);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefId] Error Task Sql = "+sql, loggercategory);
			return newCmdRefId;

		}
		else
		{
			String sql="select row_id, cmd_ref_id, oli_id from T_OM_BILL_CMD_TRANS where ORDER_ROW_ID='"+ orderTransRowId+ "'"+ " and "
					  + "seq_no = (select min(seq_no) from T_OM_BILL_CMD_TRANS where ORDER_ROW_ID='"+ orderTransRowId
					  + "' and status='"+ ApplicationConstants.STATUS_NOT_STARTED + "')";
			List<Map<String, Object>> newCmdRefId = jdbcTemplate.queryForList(sql);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefId] Sql = "+sql, loggercategory);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefId] NewCmdRefId = "+newCmdRefId.toString(), loggercategory);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefId] NewCmdRefId = ", loggercategory);
			return newCmdRefId;
				
		}
		
	}
	
	/*Sudharsan : Looking all commands for particular trans id to insert parameters */
	
	public List<Map<String, Object>> getNewCommandsForTrans(String trans_Id,String commndRefId, String orderTransRowId, String srcTransId,String loggercategory)
	{
		taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCommandsForTrans] Inside Method commndRefId = "+commndRefId, loggercategory);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		if (commndRefId.equals(ApplicationConstants.ERROR_TASK_CMD_REF_ID))
		{
			String sql ="select row_id, cmd_ref_id from T_OM_BILL_CMD_TRANS where TRANS_ID='"+ trans_Id+ "'"
					   + " and cmd_ref_id='"+ApplicationConstants.ERROR_TASK_CMD_REF_ID +"' and status in ('"+ApplicationConstants.STATUS_NEW+"','"
					   + ApplicationConstants.STATUS_NOT_STARTED+"')";
			
			List<Map<String, Object>> newCmdRefId = jdbcTemplate.queryForList(sql);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCommandsForTrans] Error Task Sql = "+sql, loggercategory);
			return newCmdRefId;

		}
		else
		{
			String sql="select row_id, cmd_ref_id, oli_id from T_OM_BILL_CMD_TRANS where ORDER_ROW_ID='"+ orderTransRowId+ "'"+ " and "
					  + "seq_no in (select seq_no from T_OM_BILL_CMD_TRANS where ORDER_ROW_ID='"+ orderTransRowId
					  + "' and status='"+ ApplicationConstants.STATUS_NOT_STARTED + "')";
			List<Map<String, Object>> newCmdRefId = jdbcTemplate.queryForList(sql);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCommandsForTrans] Sql = "+sql, loggercategory);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCommandsForTrans] NewCmdRefId = "+newCmdRefId.toString(), loggercategory);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCommandsForTrans] NewCmdRefId = ", loggercategory);
			return newCmdRefId;
				
		}
		
	}
	/*Sudharsan : Looking for next command after handling response */
	/**
	 * 
	 * @param trans_Id
	 * @param commndRefId
	 * @param orderTransRowId
	 * @param srcTransId
	 * @param loggercategory
	 * @return
	 */
	public List<Map<String, Object>> getNewCmdRefIdBillResponse(String trans_Id,String commndRefId, String orderTransRowId, String srcTransId,String loggercategory)
	{
		taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefIdBill] Inside Method trans_Id ="+trans_Id+" commndRefId = "+commndRefId+" orderTransRowId="+orderTransRowId+ " srcTransId="+srcTransId , loggercategory);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		if (commndRefId.equals(ApplicationConstants.ERROR_TASK_CMD_REF_ID))
		{
			String sql ="select row_id, cmd_ref_id from T_OM_BILL_CMD_TRANS where TRANS_ID='"+ trans_Id+ "'"
					   + " and cmd_ref_id='"+ApplicationConstants.ERROR_TASK_CMD_REF_ID +"' and status in ('"+ApplicationConstants.STATUS_NEW+"','"
					   + ApplicationConstants.STATUS_NOT_STARTED+"')";
			
			List<Map<String, Object>> newCmdRefId = jdbcTemplate.queryForList(sql);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefIdBillResponse] Error Task Sql = "+sql, loggercategory);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefIdBillResponse] NewCmdRefId = "+newCmdRefId, loggercategory);
			return newCmdRefId;

		}
		else
		{
			String sql="select row_id, cmd_ref_id, oli_id from T_OM_BILL_CMD_TRANS where ORDER_ROW_ID='"+ orderTransRowId+ "'"+ " and "
					  + "seq_no = (select min(seq_no) from T_OM_BILL_CMD_TRANS where ORDER_ROW_ID='"+ orderTransRowId
					  + "' and status='"+ ApplicationConstants.STATUS_RECEIVED + "') ";
			List<Map<String, Object>> newCmdRefId = jdbcTemplate.queryForList(sql);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefIdBillResponse] Sql = "+sql, loggercategory);
			taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getNewCmdRefIdBillResponse] NewCmdRefId = "+newCmdRefId, loggercategory);
			return newCmdRefId;
				
		}
		
	}
	
	
	
	/*M.Rahman: Insert Billing related command parameters in the T_OM_BILL_CMD_TRANS_DTLS table*/	
	/**
	 *
	 * @param rowId
	 * @param parRowId
	 * @param paramName
	 * @param paramValue
	 * @param paramSub
	 * @param xmlEleType
	 * @param xmlEleId
	 * @param xmlEleName
	 * @param xmlEleCurrValue
	 * @param xmlElePrevValue
	 * @param seqNo
	 * @param lkupRefId
	 * @param insertDt
	 * @param insertBy
	 * @param updatedDt
	 * @param updatedBy
	 * @throws Exception
	 */
	public void insertTransDetailsBill(String rowId, String parRowId,String paramName, String paramValue, String paramSub, String xmlEleType,
								   String xmlEleId, String xmlEleName, String xmlEleCurrValue,String xmlElePrevValue, BigDecimal seqNo,
								   String lkupRefId,Date insertDt, String insertBy, Date updatedDt, String updatedBy, String srcTransId, String transId,String loggercategory)throws Exception
	 {

	  taLogger.log(srcTransId, transId, parRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransDetails]ROW_ID="+rowId+"PAR_ROW_ID="+parRowId+", PARAM_NAME="+paramName+", PARAM_VALUE="+paramValue+", XML_ELE_TYPE="+xmlEleType+", XML_ELE_ID="+xmlEleId+
				", XML_ELE_NAME="+xmlEleName+", XML_ELE_CURR_VALUE="+xmlEleCurrValue+", XML_ELE_PREV_VALUE="+xmlElePrevValue+",SEQ_NO="+seqNo+", LKUP_REF_ID="+lkupRefId+", INSERT_DT="+insertDt+
				", INSERT_BY="+insertBy+", UPDATED_DT="+updatedDt+", UPDATED_BY="+updatedBy ,loggercategory);

	  if (paramValue==null || paramValue.equals("") || paramValue.equalsIgnoreCase("<blank>"))
    {
	 paramValue = "";
	 }
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
	 String sqlQuery = "INSERT INTO T_OM_BILL_CMD_TRANS_DTLS ("+" PAR_ROW_ID, PARAM_NAME, PARAM_VALUE, PARAM_SUB, XML_ELE_TYPE, XML_ELE_ID, " +
					  "XML_ELE_NAME, XML_ELE_CURR_VALUE, XML_ELE_PREV_VALUE, "+"SEQ_NO, LKUP_REF_ID, INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY )" +
					  "values( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,sysdate,user,sysdate,user)";
	 Object sqlParams[] = new Object[]
	 {
	  parRowId, paramName, paramValue, paramSub, xmlEleType, xmlEleId, xmlEleName,xmlEleCurrValue, xmlElePrevValue, seqNo, lkupRefId
	  };

	 int[] sqlTypes = new int[] { Types.VARCHAR, Types.VARCHAR,Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
								  Types.VARCHAR, Types.VARCHAR, Types.NUMERIC, Types.VARCHAR };
	 int res;
	 	res = jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
	 }
	
	/*Viknesh: Insert Billing command parameters in the T_OM_BILL_ORDER_TRANS_DTLS table*/	
	/**
	 *
	 * @param rowId
	 * @param cmdRefId
	 * @param transId
	 * @param srcTransId
	 * @param billOrderId
	 * @param status
	 * @param pollStatus
	 * @param returnCode
	 * @param returnMsg
	 * @param insertDt
	 * @param insertBy
	 * @param updatedDt
	 * @param updatedBy
	 * @throws Exception
	 */
	
	public String insertOrderTransBill(String commandRowId, String cmdRefId,String transId, String srcTransId, BigDecimal seqNo,String billOrderId, String status, String pollStatus,
			   String returnCode, String returnMsg, Date insertDt, String insertBy, Date updatedDt, String updatedBy, String crmSvcId, String loggercategory)throws Exception
	 {
		String orderTransRowId= "";
		
		taLogger.log(srcTransId, transId, cmdRefId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertOrderTransBill] INSIDE insertOrderTransBill" ,loggercategory);
		
		taLogger.log(srcTransId, transId, cmdRefId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertOrderTransBill]ROW_ID="+commandRowId+", cmdRefId="+cmdRefId+", transId="+transId+", srcTransId="+srcTransId+", seqNo="+seqNo+", status="+status+", pollStatus="+pollStatus+
				", returnCode="+returnCode+", returnMsg="+returnMsg+", billOrderId="+billOrderId+", INSERT_DT="+insertDt+
				", INSERT_BY="+insertBy+", UPDATED_DT="+updatedDt+", UPDATED_BY="+updatedBy ,loggercategory);
		
		PreparedStatement preparedStatement = null;
		Connection connection = null;
		try
		{
			String sqlQuery = "INSERT INTO T_OM_BILL_ORDER_TRANS (ROW_ID,CMD_REF_ID, TRANS_ID, SRC_TRANS_ID, SEQ_NO, BILL_ORDER_ID, STATUS, " +
							  "POLL_STATUS, RETURN_CODE, RETURN_MSG, CRM_SVC_ID, INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY) " +
							  "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate, user, sysdate, user)";
			
			//java.sql.Date effectiveDateSQL = new java.sql.Date(effectiveDate.getTime());
			connection = dataSource.getConnection();
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			String columnNames[] = { "ROW_ID" };
			preparedStatement = connection.prepareStatement(sqlQuery, columnNames);
			preparedStatement.setString(1, commandRowId);
			preparedStatement.setString(2, cmdRefId);
			preparedStatement.setString(3, transId);
			preparedStatement.setString(4, srcTransId);
			//preparedStatement.setString(5, seqNo);
			preparedStatement.setBigDecimal(5, seqNo);
			preparedStatement.setString(6, billOrderId);
			preparedStatement.setString(7, status);
			preparedStatement.setString(8, pollStatus);
			preparedStatement.setString(9, returnCode);
			preparedStatement.setString(10,returnMsg);
			preparedStatement.setString(11, crmSvcId);
			preparedStatement.executeUpdate();
			ResultSet resultSet = preparedStatement.getGeneratedKeys();
			
			if (resultSet.next())
			{
				orderTransRowId = resultSet.getString(1);
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertOrderTransBill]OrderTransRowId ="+orderTransRowId,loggercategory);
			}

		}
		catch (Exception exception)
		{
			//taLogger.log(SRC_TRANS_ID, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertTransValues][Inside Catch]cmdTransRowId ="+cmdTransRowId,loggercategory, exception);
			//System.out.println(exception.getMessage());
		}
		finally
		{
			connection.close();
			taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][insertOrderTransBill][inside finally]Connection closed",loggercategory);
			preparedStatement.close();
		}
		return orderTransRowId;
		
	 }

	 public String getOrderRowId(String srcTransId, String transId, String cmdRowId, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getOrderRowId]", loggerCategory);
		String orderRowId = "";
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		orderRowId = jdbcTemplate.queryForObject("select order_row_id from t_om_cmd_trans where row_id=?", new Object[]{cmdRowId}, String.class);
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getOrderRowId]SQL=select order_row_id from t_om_cmd_trans where row_id=?", loggerCategory);
		return orderRowId;
	 }
	 
	 public String getOrderRowIdBill(String srcTransId, String transId, String cmdRowId, String loggerCategory) {
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getOrderRowIdBill]", loggerCategory);
			String orderRowId = "";
			JdbcTemplate jdbcTemplate = getJdbcTemplate();
			orderRowId = jdbcTemplate.queryForObject("select order_row_id from T_OM_BILL_CMD_TRANS where row_id=?", new Object[]{cmdRowId}, String.class);
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getOrderRowIdBill]SQL=select order_row_id from T_OM_BILL_CMD_TRANS where row_id=?", loggerCategory);
			return orderRowId;
		 }
	 public String getCorrId(String rowId){

		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql="select CORR_ID from T_OM_ORDER_TRANS_DTLS where row_id='" + rowId + "'";
		 taLogger.log("Inside getCorrId Method",ApplicationConstants.LOG_DEBUG,"SQL: " + sql, "CRMlogging");
		 String CRMcorrId = jdbcTemplate.queryForObject(sql, String.class);
			return CRMcorrId;
	 }
	 
	 // PHASE 2 methods
	 
	 //Sudharsan:25/06/2013:RIM SIM Exchange error handling-Start
	 
	 public List<Map<String, Object>>  getSplHandlingParams(String cmdRefId){

		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql="select spl_case ,error_code,success_code from c_om_cmd_sys_map where cmd_ref_id='"+cmdRefId+"' and error_code is not null";
		 List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		
		 return list;
	 }
	 public List<Map<String, Object>>  getSplHandlingCommands(String cmdRefId){

		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql="select new_cmd_ref_id , seq_no from C_OM_CMD_SPL_CASE where cmd_ref_id='"+cmdRefId+"' order  by seq_no";
		 List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		
		 return list;
	 }
	 public HashMap<String, Object>  getOrderXmlIds(String order_Xml) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException{
			InputStream is = new ByteArrayInputStream(order_Xml.getBytes());
			
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			
			domFactory.setNamespaceAware(false);
			
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			
			Document doc = builder.parse(is);
			
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			
			String mOLI_ID = evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/LineItemXML/LineItem/ParentLineItemID");

			String oLI_ID = evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/LineItemXML/LineItem/LineItemID");
			
			HashMap<String, Object> hashMap = new HashMap<String, Object>();
			 
			hashMap.put("MOLI", mOLI_ID);
			hashMap.put("OLI", oLI_ID);
			
			return hashMap;
	 }
	 public  String evaluateXPath(XPath xpath, Document doc, String expr)
				throws XPathExpressionException {

			XPathExpression exp = xpath.compile(expr);
			String xmlDetails = (String) exp.evaluate(doc, XPathConstants.STRING);
			return xmlDetails;

		}
	 
	 public void getStartNewCommand(List<Map<String, Object>> orderXml,String srcTransId,String transId,String cmdRowId,String responseCode, String  responseDescription,String cmdRefId,String loggerCategory,String splFlag) throws Exception
	
	 {

		 	JdbcDatabaseDAO jdbcDatabaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("jdbcDatabaseDAO");
		 	taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][getStartNewCommand] Order Xml Size : " +orderXml.size(), loggerCategory);
			for (Object xml : orderXml) {
				@SuppressWarnings("rawtypes")
				Map map1 = (Map) xml;
				String order_Xml = (String) map1.get("ORDER_XML");
				String orderTransRowId = "" + (BigDecimal) map1.get("ROW_ID");
				String trans_ID = (String) map1.get("TRANS_ID");
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][getStartNewCommand]Command reference id :" + cmdRefId, loggerCategory);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][getStartNewCommand]Is Spl Cmd : "+splFlag, loggerCategory);
				jdbcDatabaseDAO.updateStatus(cmdRowId, responseCode, responseDescription,splFlag);

				List<Map<String, Object>> cmdRefIds = jdbcDatabaseDAO.getNewCmdRefId(trans_ID,cmdRefId, orderTransRowId, srcTransId, loggerCategory);
				Map<String, Object> map = cmdRefIds.get(0);
				String cmdRefIdList = (String) map.get("CMD_REF_ID");
				cmdRefId = cmdRefIdList;
				String parRowId = "" + (BigDecimal) map.get("ROW_ID");
				cmdRowId = parRowId;
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][getStartNewCommand]New Command reference id :" + cmdRefIdList +" cmdRowId = "+cmdRowId+" parRowId = "+parRowId, loggerCategory);

				CommandParameters parameters = new CommandParameters();
				CommandParamRefDtls paramRefDtls = new CommandParamRefDtls();
				List<HandlerVariables> cmdParametersList = parameters.getParameters(srcTransId, order_Xml, cmdRefIdList, transId, parRowId, orderTransRowId, jdbcDatabaseDAO, true);

				InputStream is = new ByteArrayInputStream(order_Xml.getBytes());
				DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
				domFactory.setNamespaceAware(false);
				DocumentBuilder builder = domFactory.newDocumentBuilder();
				Document doc = builder.parse(is);
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();

				List<HandlerVariables> cmdTransDtlsList = paramRefDtls.getParamValues(cmdRefIdList, cmdParametersList, xpath, doc, jdbcDatabaseDAO);
				taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[JdbcDatabaseDAO][getStartNewCommand]Saving details in command trans detail table", loggerCategory);
				SPLCommonComponent.saveCmdTransDetails(cmdTransDtlsList, srcTransId, transId, loggerCategory, jdbcDatabaseDAO);
													
				
			}
			
			
		
	 }
	 private  String getNewCmdRowId(String cmdRefId,String transId,String srcTransId,String cmdRowId,String orderTransRowId,String loggerCategory)
		{
			 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			 String sql="select row_id from t_om_cmd_trans where cmd_ref_id = '"+cmdRefId+"' and trans_id ='"+transId+"'";
			 taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[JdbcDatabaseDAO][getNewCmdRowId] SQL :"+sql, loggerCategory);
			 String newCmdRowId = jdbcTemplate.queryForObject(sql, String.class);
			 return newCmdRowId;
		}
	 public void updateParentCmdFlag (String CmdRefId,String srcTransId,String transId,String cmdRowId,String loggerCategory)
	 {
		 JdbcTemplate jdbcTemplate = getJdbcTemplate();
		 String sql="UPDATE T_OM_CMD_TRANS set SPL_FLAG = 'Y' where CMD_REF_ID = '"+CmdRefId+"' AND TRANS_ID ='"+transId+"' AND ROW_ID='"+cmdRowId+"'";
		 taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[JdbcDatabaseDAO][updateParentCmdFlag] SQL :"+sql, loggerCategory);
		 jdbcTemplate.update(sql);
			
	 }

	 public void updateParentCmdStatusComplete (String srcTransId,String transId,String cmdRowId,String loggerCategory)
	 {
		 JdbcTemplate jdbcTemplate = getJdbcTemplate();
		 //String sql="UPDATE T_OM_CMD_TRANS set STATUS='"+ApplicationConstants.STATUS_COMPLETED+"' where TRANS_ID ='"+transId+"' AND SPL_FLAG='Y' and Status='ErrorTemp'";
		 String sql="UPDATE T_OM_CMD_TRANS set STATUS='"+ApplicationConstants.STATUS_COMPLETED+"' where ROW_ID ='"+cmdRowId+"' and Status='ErrorTemp'";
		 taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[JdbcDatabaseDAO][updateParentCmdStatusComplete] SQL :"+sql, loggerCategory);
		 jdbcTemplate.update(sql);
			
	 }

	 public String isSplCmd(String srcTransId,String transId,String cmdRowId,String cmdRefId,String loggerCategory)
	 {
		 JdbcTemplate jdbcTemplate = getJdbcTemplate();
		 String sql="SELECT SPL_CASE FROM C_OM_CMD_SYS_MAP  WHERE CMD_REF_ID='"+cmdRefId+"'";
		 taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[JdbcDatabaseDAO][isSplCmd] SQL :"+sql, loggerCategory);
		 String splFlag= jdbcTemplate.queryForObject(sql, String.class);
		 taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[JdbcDatabaseDAO][isSplCmd] SplFlag :"+splFlag, loggerCategory);
		 if (splFlag==null ||splFlag.equalsIgnoreCase("")) 
			 return "N";
		 else
			 return splFlag.trim();
	 }
	 //Added isResponseCodeMatch method to check response code with response code configured in table
	 public Boolean isResponseCodeMatch(String srcTransId,String transId,String cmdRowId,String cmdRefId,String CodeConfigured,String respCode,String loggerCategory)
	 {
		 JdbcTemplate jdbcTemplate = getJdbcTemplate();
		 taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[JdbcDatabaseDAO][isResponseCodeMatch] Code Configured : "+CodeConfigured , loggerCategory);
		 taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[JdbcDatabaseDAO][isResponseCodeMatch] Response Code : "+respCode, loggerCategory);
		 String[] str=CodeConfigured.split(",");
		 taLogger.log(srcTransId,transId,cmdRowId, ApplicationConstants.LOG_INFO,"[JdbcDatabaseDAO][isResponseCodeMatch] Code length :"+str.length, loggerCategory);
		 for (int i=0;i<str.length;i++)
			{
				if (str[i].equalsIgnoreCase(respCode))
				{	
					return true ;
				}
			}
			 
		 return false;
	 }
	 
	
	 
	//Sudharsan:25/06/2013:RIM SIM Exchange error handling-End
	 
		/**
	 *
	 * @param rowId
	 */
	
	public void updateOrderTransBill(String rowId, String srcTransId, String transId)
	{
		
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="update T_OM_BILL_ORDER_TRANS set STATUS='"+ ApplicationConstants.STATUS_RECEIVED +"', POLL_STATUS='Y' where ROW_ID='"+ rowId + "'";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateOrderTransBill]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
	}
	
	/**
	 * 
	 * @param rowId
	 * @param srcTransId
	 * @param transId
	 */
	
	public void updateCommandTransBill(String cmdRefId, String transId, String CRM_SVC_ID, String effectiveDt ,String srcTransId )
	{
		
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="update T_OM_BILL_CMD_TRANS set STATUS='"+ApplicationConstants.STATUS_RECEIVED+"' where CMD_REF_ID = '"+cmdRefId+"' and TRANS_ID ='"+transId+"' and EFFECTIVE_DT ='"+effectiveDt+"' and CRM_SVC_ID='"+CRM_SVC_ID+"'";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateCommandTransBill]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
	}
	
	/**
	 * 
	 * @param rowId
	 * @param srcTransId
	 * @param transId
	 */
	
	public  void updateOrderTransBillComplete(String rowId, String srcTransId, String transId)
	{
		
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="update T_OM_BILL_ORDER_TRANS set STATUS='"+ApplicationConstants.STATUS_COMPLETED+"' where ROW_ID='"+ rowId + "'";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateOrderTransBillComplete]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
	}
	/**
	 * 
	 * @param rowId
	 * @param srcTransId
	 * @param transId
	 */
	public  void updateBillOrderTransError(String rowId, String srcTransId, String transId)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="update T_OM_BILL_ORDER_TRANS set STATUS='"+ApplicationConstants.STATUS_ERROR+"' ,UPDATED_DT=sysdate,UPDATED_BY=user where ROW_ID='"+rowId+"'";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateBillOrderTransError]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
	}
	
	/**
	 * 
	 * @param rowId
	 * @param srcTransId
	 * @param transId
	 * @param billOrderRowId
	 */
	public void updateCommandTransBillComplete(String rowId, String srcTransId, String transId,String billOrderRowId, String returnCode, String returnMsg)
	{
		
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="update T_OM_BILL_CMD_TRANS set STATUS='"+ApplicationConstants.STATUS_COMPLETED+"', return_code='"+ returnCode + "', return_msg='" + returnMsg +"'," +
				" UPDATED_DT=sysdate, UPDATED_BY=user where BILL_ORDER_ROW_ID='"+ billOrderRowId + "'";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateCommandTransBillComplete]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
	}
	/**
	 * 
	 * @param cmdRefId
	 * @param transId
	 * @param CRM_SVC_ID
	 * @param effectiveDt
	 * @return
	 */
	 
	public List<Map<String, Object>> getBillCmdTransValues(String cmdRefId, String transId, String CRM_SVC_ID, String effectiveDt){
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> list = jdbcTemplate
		.queryForList("select ROW_ID , SRC_TRANS_ID,SEQ_NO from T_OM_BILL_CMD_TRANS where CMD_REF_ID='" + cmdRefId + "' and TRANS_ID='"+ transId + "' and CRM_SVC_ID='" + CRM_SVC_ID +"' and EFFECTIVE_DT='" + effectiveDt + "'");
		taLogger.log("", transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getBillCmdTransValues] Sql= select ROW_ID from T_OM_BILL_CMD_TRANS where CMD_REF_ID='" + cmdRefId + "' and TRANS_ID='"+ transId + "' and CRM_SVC_ID='" + CRM_SVC_ID +"' and EFFECTIVE_DT='" + effectiveDt + "'", ApplicationConstants.LOGGER_CMD_PARAMETER);
		taLogger.log("", transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getBillCmdTransValues] List :"+list, ApplicationConstants.LOGGER_CMD_PARAMETER);
		return list;

	}


	
	/**
	 *
	 * @param cmdRefId
	 * @param transId
	 */
	public List<Map<String, Object>> getBillCmdTransDtlsValues(String transId, String srcTransId, String rowId){

		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql="select cmdDtls.param_name, cmdDtls.param_value, cmdtrans.row_id from t_om_bill_cmd_trans_dtls cmdDtls, t_om_bill_cmd_trans cmdTrans, t_om_bill_order_trans orderTrans where ordertrans.row_id='" + rowId + "' and cmdtrans.bill_order_row_id = ordertrans.row_id	and cmdtrans.row_id = cmddtls.par_row_id order by cmddtls.par_row_id";
		 List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		 taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getBillCmdTransDtlsValues] Sql : "+sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getBillCmdTransDtlsValues] List: "+list, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 return list;
	 }
	
	/**
	 *
	 * @param parRowId
	 */

	public List<Map<String, Object>>getBillCmdTransDtlsValuesResponse(String parRowId){
		 taLogger.log("Inside getBillCmdTransDtlsValues Method",ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][getBillCmdTransDtlsValues]parRowId is" + parRowId, "splwstranslatorlogging");
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql="select param_name, param_value from t_om_bill_cmd_trans_dtls where par_row_id='" + parRowId + "' order by seq_no";
		 taLogger.log("Inside getBillCmdTransDtlsValues Method",ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][getBillCmdTransDtlsValues]SQL : "+sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		 taLogger.log("Inside getBillCmdTransDtlsValues Method",ApplicationConstants.LOG_DEBUG,"[JdbcDatabaseDAO][getBillCmdTransDtlsValues]List : "+list, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 return list;
	 }
	 
	 /**
	 *
	 * @param cmdRefId
	 * @param transId
	 * @param srcTransId
	 * @param rowId
	 */
	public String getBillCmdTransOrderRowId(String cmdRefId, String transId, String srcTransId, String rowId){

		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql="select order_row_id from T_OM_BILL_CMD_TRANS where TRANS_ID='" + transId +"' and CMD_REF_ID='" + cmdRefId + "' and ROW_ID='" + rowId+ "'" ;
		 taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getBillCmdTransOrderRowId]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 String orderRowId = jdbcTemplate.queryForObject(sql, String.class);
			return orderRowId;
	 }
	
	/**
	 * 
	 * @param billOrderRowId
	 * @param transId
	 * @param srcTransId
	 * @return
	 */
	public List<Map<String, Object>>getBillCmdTransRowId(String billOrderRowId, String transId, String srcTransId)
	{
		taLogger.log("Inside getBillCmdTransRowId Method",ApplicationConstants.LOG_DEBUG,"rowId is" + billOrderRowId, "splwstranslatorlogging");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String, Object>> list = jdbcTemplate.queryForList("select row_id from t_om_bill_cmd_trans where bill_order_row_id='" +billOrderRowId+"'");
		taLogger.log("Inside getBillCmdTransRowId Method",ApplicationConstants.LOG_DEBUG,"select row_id from t_om_bill_cmd_trans where bill_order_row_id='" +billOrderRowId+"'", ApplicationConstants.LOGGER_CMD_PARAMETER);
		return list;
	
	}

	/**
	 *
	 * @param parRowId
	 */
	 public List<Map<String, Object>>getOrderTransDtlsValues(String rowId, String transId, String srcTransId){

		 taLogger.log("Inside getOrderTransDtlsValues Method",ApplicationConstants.LOG_DEBUG,"rowId is" + rowId, "splwstranslatorlogging");
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 List<Map<String, Object>> list = jdbcTemplate
					.queryForList("select opg_ref_id, func_ref_id, corr_id from t_om_order_trans_dtls where trans_id='" +transId+ "'and row_id='" +rowId+"'");
		 taLogger.log("Inside getOrderTransDtlsValues Method",ApplicationConstants.LOG_DEBUG,"select opg_ref_id, func_ref_id, corr_id from t_om_order_trans_dtls where trans_id='" +transId+ "'and row_id='" +rowId+"'", ApplicationConstants.LOGGER_CMD_PARAMETER);

		 return list;
	 }
	 
	 
	 /**
	 *
	 * @param orderTransRowId
	 * @param transId
	 * @param cmdRefId
	 * @param subscriberId
	 * @param effectiveDt
	 * 
	 */
	
	public void updateBillOrderTransRowId(String orderTransRowId, String transId, String cmdRefId, String crmSvcId, String effectiveDt, String srcTransId)
	{
		
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="update T_OM_BILL_CMD_TRANS set BILL_ORDER_ROW_ID='" + orderTransRowId + "' ,UPDATED_DT=sysdate,UPDATED_BY=user where TRANS_ID='" + transId +"' and CMD_REF_ID='" + cmdRefId + "' and CRM_SVC_ID='" + crmSvcId + "' and EFFECTIVE_DT='" + effectiveDt + "'";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateBillOrderTransRowId]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
		
		 
	}
	
	/**
	 *
	 * @param row_ID
	 * @param returnCode
	 * 
	 * @param returnMessage
	 */
	public void updateBillcmdTransError(String row_ID, String returnCode, String returnMessage,String loggercategory)
	{
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		taLogger.log("Inside updateBillcmdTransError Method", ApplicationConstants.LOG_DEBUG,"returnCode="+returnCode+"returnMessage="+returnMessage+"row_ID="+row_ID,loggercategory);
		if(returnMessage!=null && !returnMessage.equals("") && returnMessage.length()>500)
		{
			returnMessage = returnMessage.substring(0, 500);
			taLogger.log("Inside updateBillcmdTransError Method", ApplicationConstants.LOG_DEBUG,"returnMessage="+returnMessage,loggercategory);
		}
		String sql="update T_OM_BILL_CMD_TRANS set status='" + ApplicationConstants.STATUS_ERROR + "',"+ " RETURN_CODE=?," +"RETURN_MSG= ?,"+ "UPDATED_DT=sysdate," + "UPDATED_BY=" + "user"+ " where ROW_ID=?" ;
		jdbcTemplate.update(sql,new Object[] {returnCode,returnMessage,row_ID});
		taLogger.log("Inside updateBillcmdTransError Method", ApplicationConstants.LOG_DEBUG,sql,loggercategory);
	}
	
	/**
	 *
	 * @param parRowId
	 */
	public void updateOrderTransError(String billOrderRowId, String transId, String srcTransId)
	{
		
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql="update T_OM_BILL_ORDER_TRANS set STATUS='"+ ApplicationConstants.STATUS_ERROR+"' where row_id='" + billOrderRowId +"'";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateBillOrderTransRowId]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
	}
	
	/**
	 * 
	 * @param serviceId
	 * @param cmdRefId
	 * @param transId
	 * @param cmdRowId
	 * @param interfaceName
	 * @throws SQLException
	 */
	public void saveBillCommandResponse(String serviceId,String OfferInstId,String c1OrderId,String rowId, String cmdRefId, String transId, String cmdRowId, String interfaceName, String srcTransId, String errorCode,String errorMsg,String ServiceInternalidresets) throws SQLException {
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][saveBillCommandResponse]", ApplicationConstants.LOGGER_CMD_PARAMETER);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		
		String sql="select OLI_ID from T_OM_BILL_CMD_TRANS where TRANS_ID='" + transId +"' and CMD_REF_ID='" + cmdRefId + "' and ROW_ID='" + cmdRowId + "'" ;
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][saveBillCommandResponse]Sql=" + sql,ApplicationConstants.LOGGER_CMD_PARAMETER);
		String Lineitemid = jdbcTemplate.queryForObject(sql, String.class);
		 
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][saveBillCommandResponse]CMD_ROW_ID="+cmdRowId+" BILL_ORDER_ROW_ID="+rowId+" CMD_REF_ID="+cmdRefId+" TRANS_ID="+transId, ApplicationConstants.LOGGER_CMD_PARAMETER);
		String sqlQuery = "INSERT INTO T_OM_BILL_CMD_RESPONSE(CMD_ROW_ID,C1_ORDER_ID,BILL_ORDER_ROW_ID, CMD_REF_ID, TRANS_ID, SRC_TRANS_ID, OFFER_INSTANCE_ID, SERVICE_INTERNAL_ID, INTERFACE_NAME,STATUS,ERROR_CODE,RESPONSE_MSG, POLL_STATUS, LINE_ITEM_ID,SERVICE_INTERNAL_ID_RESETS,INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY) VALUES(?, ?, ?, ?, ?, ?, ? , ?, ? ,? , ? , ?, ?, ?,?,SYSDATE, USER, SYSDATE, USER)";
		Object sqlParams[] = new Object[] {cmdRowId, c1OrderId,rowId,cmdRefId, transId, srcTransId, OfferInstId, serviceId,interfaceName, ApplicationConstants.STATUS_NEW,errorCode,errorMsg, "N",Lineitemid,ServiceInternalidresets};
		int[] sqlTypes = new int[] {Types.VARCHAR,  Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,Types.VARCHAR};
		jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
	 }
	
	/**
	 * 
	 * @param cmdRefId
	 * @param transId
	 * @param srcTransId
	 * @return
	 * @throws SQLException
	 */
	public  List<Map<String, Object>>  getBillCmdTransDtlsByCmdRwId(String srcTransId, String transId,String cmdRowId) throws SQLException
	 {	
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getSuplOfferByCmdRwId]", loggercategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sqlQuery = "SELECT PARAM_NAME,PARAM_VALUE FROM T_OM_BILL_CMD_TRANS_DTLS  WHERE PAR_ROW_ID='"+cmdRowId+"'";
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getSuplOfferByCmdRwId]SqlQuery="+sqlQuery, loggercategory);
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sqlQuery);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getSuplOfferByCmdRwId]List="+list , loggercategory);
		return list;
	 }
	
	/**
	 * 
	 * @param cmdRefId
	 * @param transId
	 * @param srcTransId
	 * @return
	 * @throws SQLException
	 */
	public List<Map<String, Object>>  getAPIParams(String cmdRefId, String transId, String srcTransId) throws SQLException
	 {	
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getAPIParams]", loggercategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sqlQuery = "SELECT * FROM C_OM_BILL_CMD_API_MAP  WHERE CMD_REF_ID='"+cmdRefId+"'";
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getAPIParams]sqlQuery="+sqlQuery, loggercategory);
		List<Map<String, Object>> apiParams = jdbcTemplate.queryForList(sqlQuery);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getAPIParams] Sql = " + sqlQuery+" apiParams="+apiParams , loggercategory);
		return apiParams;
	 }
	
	/**
	 * Method to get subscriber id passing src_trans_id
	 * @param srcTransId
	 * @param transId
	 * @param cmdRowId
	 * @param loggerCategory
	 * @return subscriber id
	 */
	public Integer getSubscriberId(String srcTransId, String transId, String cmdRowId, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getSubscriberId]", loggerCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		Integer subscriberId = jdbcTemplate.queryForObject(ApplicationConstants.SQL_GET_SUBSCRIBER_ID, new Object[]{srcTransId}, Integer.class);
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getSubscriberId] subscriberId " + subscriberId, loggerCategory);
		return subscriberId;
	}
	
	public Integer getSubscrIdResets(String srcTransId, String transId, String cmdRowId, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getSubscriberId]", loggerCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		Integer SubscrIdResets = jdbcTemplate.queryForObject(ApplicationConstants.SQL_GET_SUBSCRIDRESETS, new Object[]{srcTransId}, Integer.class);
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][getSubscriberId] SubscrIdResets " + SubscrIdResets, loggerCategory);
		return SubscrIdResets;
	}
	
	/**
	 * Method to check is there any commands for process next in same order and crm service
	 * @param srcTransId
	 * @param transId
	 * @param cmdRowId
	 * @param crmSvcId
	 * @param loggerCategory
	 * @return
	 *//*
	public int getNoOfBillCommands(String srcTransId, String transId, String cmdRowId, String crmSvcId, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In getNoOfCommandsBill cmdRowId="+cmdRowId, loggerCategory);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		
		String sql = "select COUNT(STATUS) AS NUMBEROFCOMMANDS from T_OM_BILL_ORDER_TRANS where SRC_TRANS_ID='"+ srcTransId +"' and " +
				"CRM_SVC_ID='"+ crmSvcId +"' AND STATUS IN ('"+ApplicationConstants.STATUS_NOT_STARTED + "','"+ApplicationConstants.STATUS_ERROR+"','"+ApplicationConstants.STATUS_NEW+"')";
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, sql, loggerCategory);
		int cmdCount = jdbcTemplate.queryForInt(sql);
		
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"commands returned ="+cmdCount, loggerCategory);
		return cmdCount;
	}*/
	
	 public Map getBillCmdTrans(String rowId, String transId) {
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 List<Map<String, Object>> cmdTransRow = jdbcTemplate.queryForList("select * from T_OM_BILL_CMD_TRANS where ROW_ID='" + rowId + "'");
		 Map cmdTransDetails = (Map)cmdTransRow.get(0);
		 taLogger.log("", transId, rowId, ApplicationConstants.LOG_INFO, "[JdbcDatabaseDAO][getBillCmdTrans]Sql=select SRC_TRANS_ID from T_OM_BILL_CMD_TRANS where ROW_ID='" + rowId + "'","adapterlogging");
		 return cmdTransDetails;
	 }

	 /**
	  * method to update next c1 order to New status
	  * @param srcTransId
	  * @param transId
	  * @param cmdRowId
	  * @param crmSvcId
	  * @param loggerCategory
	  */
	 /*public void updateNextC1Order(String srcTransId, String transId, String cmdRowId, String crmSvcId, String loggerCategory) {
		 taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In updateNextC1Order cmdRowId="+cmdRowId, loggerCategory);
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
	     String sql="update T_OM_BILL_ORDER_TRANS set STATUS='"+ApplicationConstants.STATUS_NEW+"', UPDATED_DT=sysdate, UPDATED_BY=user " +
	     		"where src_trans_id='"+srcTransId+"' and crm_svc_id='"+ crmSvcId +"' and " +
	     				"seq_no=(select min(seq_no) from T_OM_BILL_ORDER_TRANS " +
	     				"where src_trans_id='"+srcTransId+"' and crm_svc_id='"+ crmSvcId +"' " +
	     						"and status in ('"+ApplicationConstants.STATUS_NOT_STARTED +"'))";
	     taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In updateNextC1Order SQL="+sql, loggerCategory);
	     jdbcTemplate.update(sql);
	     taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In updateNextC1Order cmdRowId="+cmdRowId, loggerCategory);
	 }*/
	 
	 /**
	  * Method to update c1 order to New by finding minimum seq no
	  * @param srcTransId
	  * @param transId
	  * @param cmdRowId
	  * @param crmSvcId
	  * @param loggerCategory
	  */
	 public void updateC1OrderStatusToNew(String srcTransId, String transId, String cmdRowId, String crmSvcId, String loggerCategory) {
		 taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In updateC1OrderStatus cmdRowId="+cmdRowId, loggerCategory);
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql="update T_OM_BILL_ORDER_TRANS set STATUS='"+ApplicationConstants.STATUS_NEW+"', UPDATED_DT=sysdate, UPDATED_BY=user " +
		 		"where src_trans_id='"+srcTransId+"' and crm_svc_id='"+ crmSvcId +"' and " +
		 				"seq_no=(select min(seq_no) from T_OM_BILL_ORDER_TRANS " +
		 				"where src_trans_id='"+srcTransId+"' and crm_svc_id='"+ crmSvcId +"' and status in ('"+ApplicationConstants.STATUS_NOT_STARTED +"'))";
		 int billOrder=jdbcTemplate.update(sql);
		 taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateC1OrderStatus]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateC1OrderStatus]Updated BillOrder Count="+billOrder, ApplicationConstants.LOGGER_CMD_PARAMETER);
	 }
	 
	 /**
	  * Method to check, any c1 order status in error for same src_trans_id and crm_svc_id
	  * @param srcTransId
	  * @param transId
	  * @param cmdRowId
	  * @param crmSvcId
	  * @param loggerCategory
	  * @return
	  */
	 public Integer checkC1OrderForError(String srcTransId, String transId, String cmdRowId, String crmSvcId, String loggerCategory) throws Exception {
		 taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In checkC1OrderForError cmdRowId="+cmdRowId, loggerCategory);
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql = "select count(*) as status from t_om_bill_order_trans where src_trans_id=? and crm_svc_id=? and status=?";
		 Integer status = jdbcTemplate.queryForObject(sql, new Object[]{srcTransId, crmSvcId, ApplicationConstants.STATUS_ERROR}, Integer.class);
		 taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][checkC1OrderForError]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][checkC1OrderForError]Status="+ status, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 return status;
	 }
	 
	 /**
	  * Method to get bill order row id of error order
	  * @param srcTransId
	  * @param transId
	  * @param cmdRowId
	  * @param crmSvcId
	  * @param loggerCategory
	  * @return
	  * @throws Exception
	  */
	 public String getCOneErrorOrderTransId(String srcTransId, String transId, String cmdRowId, String crmSvcId, String loggerCategory) throws Exception {
		 taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In checkC1OrderForError cmdRowId="+cmdRowId, loggerCategory);
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql = "select row_id from t_om_bill_order_trans where src_trans_id=? and crm_svc_id=? and status=?";
		 String billOrderTransId = jdbcTemplate.queryForObject(sql, new Object[]{srcTransId, crmSvcId, ApplicationConstants.STATUS_ERROR}, String.class);
		 taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][checkC1OrderForError]Sql= " + sql, loggerCategory);
		 taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][checkC1OrderForError]billOrderTransId="+ billOrderTransId, loggerCategory);
		 return billOrderTransId;
	 }
	 
	 /**
	  * Method to update logic for retry
	  * @param srcTransId
	  * @param transId
	  * @param orderRowId
	  * @param loggerCategory
	  */
	 public void removeBillCmdTransDtls(String srcTransId, String transId, String orderRowId, String loggerCategory) {
		 taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][removeBillCmdTransDtls] Inside Method Start", loggerCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sql = "delete from t_om_bill_order_trans where row_id in (select bill_order_row_id from t_om_bill_cmd_trans where order_row_id='"+ orderRowId + "' and upper(status)=upper('Error'))";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][removeBillCmdTransDtls] Delete Bill order trans Sql= " + sql, loggerCategory);

		sql = "delete from t_om_bill_cmd_trans_dtls where par_row_id in (select row_id from t_om_bill_cmd_trans where order_row_id='"+ orderRowId + "' and upper(status)=upper('Error'))";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][removeBillCmdTransDtls] Delete Sql= " + sql, loggerCategory);
		
		sql = "update t_om_bill_cmd_trans set status='Not Started', poll_status='N' where row_id in (select row_id from t_om_bill_cmd_trans where order_row_id='"+ orderRowId + "' and upper(status)=upper('Error'))";
		jdbcTemplate.update(sql);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][removeBillCmdTransDtls] Update Sql= " + sql, loggerCategory);
		
	 }
	
	 /**
	  * Method used to get Max rows per poll by passing poller name
	  * @param pollerName
	  * @return
	  */
	 public Number getMaxRowsPerPoll(String pollerName) {
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql="select MAX_ROWS_PER_POLL from C_OM_POLL_INT where POLLER_NAME='" + pollerName + "'";
		 Number maxRowsPerPoll = jdbcTemplate.queryForLong(sql);
		 taLogger.log("Inside getMaxRowsPerPoll Method", ApplicationConstants.LOG_DEBUG, sql, loggercategory);
		 taLogger.log("Inside getMaxRowsPerPoll Method", ApplicationConstants.LOG_DEBUG, "maxRowsPerPoll = " + maxRowsPerPoll, loggercategory);
		 return maxRowsPerPoll;
	 }
	 
	 /**
	  * Method to check, any c1 Cmd status in error for same src_trans_id and crm_svc_id
	  * @param srcTransId
	  * @param transId
	  * @param cmdRowId
	  * @param crmSvcId
	  * @param loggerCategory
	  * @return
	  */
	 public Integer checkC1CmdForError(String srcTransId, String transId, String cmdRowId, String crmSvcId, String loggerCategory) throws Exception {
		 taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In checkC1CmdForError cmdRowId="+cmdRowId, loggerCategory);
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql = "select count(*) as status from t_om_bill_cmd_trans where src_trans_id=? and crm_svc_id=? and status in (?, ?)";
		 Integer status = jdbcTemplate.queryForObject(sql, new Object[]{srcTransId, crmSvcId, ApplicationConstants.STATUS_ERROR, ApplicationConstants.STATUS_RECEIVED}, Integer.class);
		 taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][checkC1CmdForError]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][checkC1CmdForError]Status="+ status, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 return status;
	 }
	 
	 /**
	  * Method to update c1 Cmd to New by finding minimum seq no
	  * @param srcTransId
	  * @param transId
	  * @param cmdRowId
	  * @param crmSvcId
	  * @param loggerCategory
	  */
	 public void updateC1CmdStatusToNew(String srcTransId, String transId, String cmdRowId, String crmSvcId, String loggerCategory) {
		 taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In updateC1CmdStatusToNew cmdRowId="+cmdRowId, loggerCategory);
		 JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		 String sql="update T_OM_BILL_CMD_TRANS set STATUS='"+ApplicationConstants.STATUS_NEW+"', UPDATED_DT=sysdate, UPDATED_BY=user " +
		 		"where src_trans_id='"+srcTransId+"' and crm_svc_id='"+ crmSvcId +"' and " +
		 				"seq_no=(select min(seq_no) from T_OM_BILL_CMD_TRANS " +
		 				"where src_trans_id='"+srcTransId+"' and crm_svc_id='"+ crmSvcId +"' and status in ('"+ApplicationConstants.STATUS_NOT_STARTED +"'))";
		 int billOrder=jdbcTemplate.update(sql);
		 taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateC1CmdStatusToNew]Sql= " + sql, ApplicationConstants.LOGGER_CMD_PARAMETER);
		 taLogger.log(srcTransId, transId, cmdRowId,ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateC1CmdStatusToNew]Updated BillOrder Count="+billOrder, ApplicationConstants.LOGGER_CMD_PARAMETER);
	 }
	 
	 /**
	 * Method to check is there any commands for process next in same order and crm service
	 * @param srcTransId
	 * @param transId
	 * @param cmdRowId
	 * @param crmSvcId
	 * @param loggerCategory
	 * @return
	 */
	public int getNoOfBillCommands(String srcTransId, String transId, String cmdRowId, String crmSvcId, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In getNoOfCommandsBill cmdRowId="+cmdRowId, loggerCategory);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		
		String sql = "select COUNT(STATUS) AS NUMBEROFCOMMANDS from T_OM_BILL_CMD_TRANS where SRC_TRANS_ID='"+ srcTransId +"' and " +
				"CRM_SVC_ID='"+ crmSvcId +"' AND STATUS IN ('"+ApplicationConstants.STATUS_NOT_STARTED + "','"+ApplicationConstants.STATUS_ERROR+"','"+ApplicationConstants.STATUS_NEW+"')";
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, sql, loggerCategory);
		int cmdCount = jdbcTemplate.queryForInt(sql);
		
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"commands returned ="+cmdCount, loggerCategory);
		return cmdCount;
	}
	
	/**
	 * Method to update bill command status
	 * @param rowId
	 * @param srcTransId
	 * @param transId
	 * @param billOrderRowId
	 * @param returnCode
	 * @param returnMsg
	 * @param status
	 * @param loggerCategory
	 */
	public void updateBillCommandTransStatus(String rowId, String srcTransId, String transId, String billOrderRowId, String returnCode, String returnMsg, String status, String loggerCategory) {
		taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateBillCommandTransStatus]status= " + status, loggerCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sql="update T_OM_BILL_CMD_TRANS set STATUS='"+status+"', return_code='"+ returnCode + "', return_msg='" + returnMsg +"', UPDATED_DT=sysdate, UPDATED_BY=user " +
				"where row_id ='"+rowId+"'";
		taLogger.log(srcTransId, transId, rowId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][updateCommandTransBillError]Sql= " + sql, loggerCategory);
		jdbcTemplate.update(sql);
	}
	
	/**
	 * Method to get no of pending cmd for TOM
	 * @param srcTransId
	 * @param transId
	 * @param cmdRowId
	 * @param crmSvcId
	 * @param loggerCategory
	 * @return
	 */
	public int getNoOfPendingTOMCommands(String srcTransId, String transId, String cmdRowId, String crmSvcId, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"In getNoOfPendingTOMCommands cmdRowId="+cmdRowId, loggerCategory);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		
		String sql = "select COUNT(STATUS) AS NUMBEROFCOMMANDS from T_OM_BILL_CMD_TRANS where SRC_TRANS_ID='"+ srcTransId +"' and " +
				"CRM_SVC_ID='"+ crmSvcId +"' AND trans_id='" + transId + "' AND STATUS IN ('"+ApplicationConstants.STATUS_NOT_STARTED + "','"+ApplicationConstants.STATUS_ERROR+"','"+ApplicationConstants.STATUS_NEW+"')";
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, sql, loggerCategory);
		int cmdCount = jdbcTemplate.queryForInt(sql);
		
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG,"commands returned ="+cmdCount, loggerCategory);
		return cmdCount;
	}
	
	public void saveBillSubCmdTrans(String APIName, String intermediateAPIName, String instId, String transId, String cmdRowId, String srcTransId) throws SQLException {
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][saveBillSubCmdTrans]", ApplicationConstants.LOGGER_CMD_PARAMETER);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[JdbcDatabaseDAO][saveBillSubCmdTrans]intermediateAPIName="+intermediateAPIName, ApplicationConstants.LOGGER_CMD_PARAMETER);
		String sqlQuery = "INSERT INTO T_OM_BILL_SUB_CMD_TRANS (PAR_ROW_ID, C1_API, INTERMEDIATE_API, RETURN_MSG, STATUS, INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY) " +
				"VALUES(?, ?, ?, ?, ?, SYSDATE, USER, SYSDATE, USER)";
		Object sqlParams[] = new Object[] {cmdRowId, APIName, intermediateAPIName, instId, "-"};
		int[] sqlTypes = new int[] {Types.VARCHAR,  Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
	}
	
	/**
	 * Method to insert into cmd_trans_dtls
	 * @param handlerVariables
	 * @param srcTransId
	 * @param transId
	 * @param loggercategory
	 * @throws Exception
	 */
	public void insertCmdTransDetails(HandlerVariables handlerVariables, String srcTransId, String transId,String loggercategory)throws Exception {
		
		String parRowId = handlerVariables.getRow_ID();
		String paramName = handlerVariables.getParamName();
		String paramValue = handlerVariables.getParamValue();
		String paramSub = handlerVariables.getParamSub();
		String xmlEleType = handlerVariables.getXmlEleType();
		String xmlEleId = handlerVariables.getXmlEleId();
		String xmlEleCurrValue = handlerVariables.getXmlEleCurrValue();
		String xmlElePrevValue = handlerVariables.getXmlElePrevValue();
		String xmlEleName = handlerVariables.getXmlEleName();
		BigDecimal seqNo = handlerVariables.getSeqNo();
		String lkupRefId = handlerVariables.getLookupRefId();
		String xmlParEleId = handlerVariables.getXmlParEleId();
		
		if (paramValue==null || paramValue.equals("") || paramValue.equalsIgnoreCase("<blank>")) {
			paramValue = "";
		}
		
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		String sqlQuery = "INSERT INTO T_OM_CMD_TRANS_DTLS (PAR_ROW_ID, PARAM_NAME, PARAM_VALUE, PARAM_SUB, XML_ELE_TYPE, XML_ELE_ID, XML_PAR_ELE_ID, " + 
						"XML_ELE_NAME, XML_ELE_CURR_VALUE, XML_ELE_PREV_VALUE, SEQ_NO, LKUP_REF_ID, INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY )" + 
						"values( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, sysdate, user, sysdate, user)";
		
		Object sqlParams[] = new Object[] { parRowId, paramName, paramValue, paramSub, xmlEleType, xmlEleId, xmlParEleId, xmlEleName,xmlEleCurrValue, xmlElePrevValue, seqNo, lkupRefId };
		
		int[] sqlTypes = new int[] { Types.VARCHAR, Types.VARCHAR,Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.NUMERIC, Types.VARCHAR };
		jdbcTemplate.update(sqlQuery, sqlParams, sqlTypes);
	}

	/**
	 * 
	 * @param srcTransId
	 * @param transId
	 * @param cmdRowId
	 * @param requestMsgFlag
	 * @param requestMessage
	 * @param loggerCategory
	 */
	public void updateCmdTrans(String srcTransId, String transId, String cmdRowId, String requestMsgFlag, String requestMessage, String loggerCategory) {
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCmdTrans] Start", loggerCategory);
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		if("Y".equalsIgnoreCase(requestMsgFlag)) {
			String sql = "update T_OM_CMD_TRANS set REQUEST_MSG = ? where ROW_ID = ?";
			taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCmdTrans] sql " + sql , loggerCategory);
			jdbcTemplate.update(sql, new Object[] { requestMessage, cmdRowId });
		}
		taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_DEBUG, "[DatabaseDAO][updateCmdTrans] End" , loggerCategory);
	}
		
}
