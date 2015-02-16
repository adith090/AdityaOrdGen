package com.m1.bcc.spl.constants;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/11/2012					Ravikumar G				Created
 * 11/04/2013					Ravikumar G				Bug#1901-Changed Request translator method using DB config flag
 * 														for search param tag or replace param tag Enhancement
 * 16/05/2013					Billy Lim				Bug 2142 [Internal] For RETRY TaskRequest, return TOM Success Response when Order is in Completed status in T_OM_ORDER_TRANS_DTLS
 * 17/05/2013					Ravikumar G				Bug#2170: for JMS Authentication added splapp.properties file path
 * 10/05/2013					Sudharsan				Implemented RIM adaptor
 * 17/05/2013					Sudharsan				Implemented Engineering Authentication server
 * 22/05/2013					Sudharsan				Implemented Radius server
 * 27/05/2013   				Sudharsan				Implemented ADS
 * 05/06/2013   				Sudharsan				Implemented SAS
 * 11/07/2013					Sudharsan				Added two constants to support third interface of RIM adaptor
 * 19/08/2013					Sudharsan				Added new system name - Database 
 * 23/08/2013					Ravikumar G				Added MEMBER_TYPE_RC_TERM_INST
 * 10/09/2013					Ravikumar G				Added DATE_FORMAT_YMD
 * 11/09/2013					Ravikumar G				Added SQL_GET_SUBSCRIBER_ID
 * 12/09/2013					Ravikumar G				Added COLUMN_CRM_SVC_ID
 * 13/09/2013					Ravikumar G				Added XPATH_CRM_SVC_ID, C1_FLAG_YES
 * 25/09/2013					Ravikumar G				Update error code and description for RIM
 * 17/10/2013					Ravikumar G				Added constants for HTTP_METHOD_GET and POST
 * 24/10/2013					Ravikumar G				Added constants for GAZELLE CMD 004
 * 28/10/2013					Kalyan				    Added Constants for MultiSIM
 * 06/11/2013					Ravikumar G				Bug#20397: added xml_par_ele_id,  changed xpath for crm interface and added constant OPCO_FLAG
 * 11/11/2013					Ravikumar G				Bug#20466: updated gazelle 004 response zpath
 * 18/11/2013					Ravikumar G				Bug#20568: changed error_message to response_msg
 * 18/11/2013					Ravikumar G				Bug 20601 - Added constant service internal id resets
 * 20/11/2013					Ravikumar G				Bug#20538 - Added timeout and read timeout
 * 28/11/2013					Kalyan			        Bug#20495 - Updated ServiceInternalIdResets
 * 04/12/2013					Ravikumar G				Bug#20592 - Changed for DB Adaptor design change and to get stored proc name
 * 04/12/2013					Ravikumar G				Bug#21087 - Changed constant SKIP to Skip for Title case update
 * 04/12/2013					Ravikumar G				Bug#21221 - Removed unused imports, commented codes and interface xpath
 * 05/12/2013					Ravikumar G				Bug#21274 - Added constatns for c1 status committed
 * 12/12/2013					Ravikumar G				Bug#21569 - Updated c_om_cmd_dest_ref to c_om_interface_ref
 * 31/12/2013					Ravikumar G				Bug#22498 - Added xpath constatns for priority
 * 08/01/2014					Ravikumar G				Bug#22917 - Added Constant PARAM_TYPE_RESOURCE_PROPERTY
 * 10/01/2014					Ravikumar G				Bug#23067 - Changed Constant C1_FLAG to C1_FLAG_NO
 * 16/01/2014					Ravikumar G				Bug#23590 - updated to get request_msg_flag to update request message in cmd_trans table
 * 27/01/2014					Ravikumar G				Bug#23086 - added source constant
 * 31/01/2014					Ravikumar G				Bug#24614 - Updated DbAdaptor query and added constants for OPCO/PS billing
 * 03/02/2014					Ravikumar G				Bug#24615 - Updated RS interface xpath
 * 03/02/2014					Ravikumar G				Bug#21970 - Added constant for STARHUB interface
 ******************************************************************************/

//Added comments to check in the codes
public class ApplicationConstants {

	public static final String APPLICATION_NAME = "SPL";
	public static final String SOURCE_OSM = "OSM";

	public static final String XPATH_TOM_ORDER_ID = "/Envelope/Body/ProvRequest/ListOfRef/TomOrderID";
	public static final String XPATH_OPG_REF_ID = "/Envelope/Body/ProvRequest/ListOfRef/OPGRefID";
	public static final String XPATH_FUN_REF_ID = "/Envelope/Body/ProvRequest/ListOfRef/FuncRefID";
	public static final String XPATH_TASK = "/Envelope/Body/ProvRequest/ListOfRef/TaskRequest";
	public static final String XPATH_CRM_ORDER_ID = "/Envelope/Body/ProvRequest/ListOfRef/LineItemXML/LineItem/OrderHeader/OrderID";
	public static final String XPATH_NEXT_HOP_WAN_IP_FROM = "Envelope/Body/ProvRequest/ListOfRef/LineItemXML/LineItem/LineItemAttributeInfo/LineItemAttribute[AttributeName='WAN IP FROM']/CurrentValue";
	public static final String XPATH_NEXT_HOP_WAN_IP_TO = "Envelope/Body/ProvRequest/ListOfRef/LineItemXML/LineItem/LineItemAttributeInfo/LineItemAttribute[AttributeName='WAN IP TO']/CurrentValue";
	public static final String XPATH_TECHREQUEST_CRM_ORDER_ID = "/Envelope/Body/ProvRequest/ListOfRef/LineItemXML/TechRequest/TechRequestHeader/TechRequestID";
	public static final String XPATH_PRIORITY = "/Envelope/Body/ProvRequest/ListOfRef/LineItemXML/LineItem/OrderHeader/Priority";
	/*New XPATH for C1 Flag*/
	public static final String XPATH_C1_FLAG = "/Envelope/Body/ProvRequest/ListOfRef/C1Flag";
	/*New XPATH for CRM order ID for Billing Order*/
	public static final String XPATH_CRM_ORD_BILL = "/Envelope/Body/ProvRequest/ListOfRef/OrderHeader/OrderID";
	
	public static final String XPATH_CRM_SVC_ID =  "Envelope/Body/ProvRequest/ListOfRef/ServiceInfo/CRMServiceId";
	
	public static final String XPATH_MULTI_SIM = "//ComponentList/ComponentInfo[CompType='CCTSM']/CompLineItemID";
	
	public static final String XPATH_External_IDs = "//ListOfExternalID/ExternalID[ExternalIDType!=21 and ExternalIDType!=91]/";
	
	public static final String SIM_COMPONENT_TYPE ="CCTSM";
	/**
	 *   Message Severity Level Constants
	 */


	public static final int LOG_DEBUG	= 01;
	public static final int LOG_INFO	= 02;
	public static final int LOG_ERROR	= 03;
	public static final int LOG_FATAL	= 04;
	public static final int LOG_TRACE	= 05;


	/**
	 * Order and Command Status Constants
	 * */

	public static final String STATUS_NOT_STARTED 	=	"Not Started";
	public static final String STATUS_NEW			=	"New";
	public static final String STATUS_RECEIVED		=	"Received";
	public static final String STATUS_SENT			=	"Sent";
	public static final String STATUS_COMPLETED		=	"Completed";
	public static final String STATUS_ERROR			=	"Error";
	public static final String STATUS_CANCELLED		=	"Cancelled";
	public static final String STATUS_RETRY			=	"Retry";
    public static final String STATUS_ERROR_TASK	=	"ERRORTASK";
    public static final String STATUS_SKIP			=	"Skip";
    public static final String STATUS_FAIL			= 	"FAIL";

	/**
	 *   Logging category
	 */

    public static final String LOG_CATEGORY_CRM_LOGGING="CRMlogging";
	public static final String LOG_CATEGORY_SPL_WS_TRANSLATOR_LOGGING	= "splwstranslatorlogging";
	public static final String LOGGER_CMD_PARAMETER	= "cmdparameterlogging";


	/**
	 * Contants for Webservice
	 */

	public static final String MESSAGE_STATUSCODE = "StatusCode";
	public static final String MESSAGE_CORRELATIONID = "CorrelationId";
	public static final String MESSAGE_PROCESSREQUESTRESPONSE = "processRequestResponse";
	public static final String MESSAGE_PONRPROCESSREQUESTRESPONSE = "ponrProcessRequestResponse";
	public static final String MESSAGE_LISTOFREF = "ListOfRef";
	public static final String MESSAGE_TOMORDERID = "TomOrderID";
	public static final String MESSAGE_OPGREFID = "OPGRefID";
	public static final String MESSAGE_FUNCREFID = "FuncRefID";
	public static final String MESSAGE_CMDREFID = "CmdRefID";
	public static final String MESSAGE_SUCCESS = "Success";
	public static final String MESSAGE_TRANSID = "TRANS_ID";
	public static final String MESSAGE_TRANSTYPE = "TRANS_TYPE";
	public static final String MESSAGE_REVISION = "REVISION";
	public static final String MESSAGE_ALLOWCANCEL = "ALLOW_CANCEL";
	public static final String MESSAGE_ERRORCODE = "error_code";
	public static final String MESSAGE_ROWCOUNT = "rowcount";
	public static final String MESSAGE_ALLOWCANCEL_SUCCESS = "Y";
	public static final String MESSAGE_ALLOWCANCEL_ERROR = "N";
	public static final Integer MESSAGE_ERRORCODE_SUCCESS = 0;
	public static final Integer MESSAGE_ROWCOUNT_SUCCESS = 1;



	public static final String BEAN_DATABASEDAO = "databaseDAO";
	public static final String BEAN_JDBCDATABASEDAO = "jdbcDatabaseDAO";
	public static final String BEAN_ORDERTRANSACTIONSENDER = "orderTransactionSender";

	/**
	 *  SQL Query
	 */

	public static final String SQL_GET_CMD_ORDER_TRANS_DETAILS = "SELECT CMDTRANS.ROW_ID, CMDTRANS.CMD_REF_ID, ORDERTRANS.FUNC_REF_ID, " +
			"ORDERTRANS.OPG_REF_ID, ORDERTRANS.CORR_ID, ORDERTRANS.TRANS_ID " +
			"FROM T_OM_CMD_TRANS CMDTRANS, T_OM_ORDER_TRANS_DTLS ORDERTRANS " +
			"WHERE CMDTRANS.ROW_ID=? " +
			"AND CMDTRANS.ORDER_ROW_ID= ORDERTRANS.ROW_ID " +
			"AND CMDTRANS.TRANS_ID= ORDERTRANS.TRANS_ID";

	public static final String SQL_GET_SYSTEMNAME = "SELECT SYSTEM_NAME FROM C_OM_CMD_SYS_MAP WHERE CMD_REF_ID=?";

	public static final String SQL_GET_SYSTEMDETAILS = "SELECT CMDSYSMAP.SYSTEM_NAME, CMDSYSMAP.CMD_NAME, " +
			"COALESCE(INTERFACEREF.USER_NAME, ' ') USER_NAME, COALESCE(INTERFACEREF.PWD, ' ') PWD, " +
			"INTERFACEREF.AUTH_TOKEN_REQUIRED, COALESCE(INTERFACEREF.ACCOUNT_TYPE,' ') ACCOUNT_TYPE, " +
			"CMDSYSMAP.CMD_REF_ID, INTERFACEREF.TECH_METHOD, INTERFACEREF.LOCATION, INTERFACEREF.INTERFACE_NAME, " +
			// Ravi: 20130411: Bug#1901: Added as part of search param tag or replace param tag Enhancement - Start
			"CMDSYSMAP.SEARCH_PARAM_TAG, CMDSYSMAP.REPLACE_PARAM_TAG, INTERFACEREF.TIMEOUT, INTERFACEREF.READ_TIMEOUT AS READTIMEOUT, " +
			"CMDSYSMAP.PARAMETER_VALUE, INTERFACEREF.REQUEST_MSG_FLAG " +
			// Ravi: 20130411: Bug#1901: Added as part of search param tag or replace param tag Enhancement - End
			//"FROM C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_CMD_DEST_REF CMDDESTREF " +
			"FROM C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_INTERFACE_REF INTERFACEREF " +
			"WHERE CMDSYSMAP.CMD_REF_ID=? " +
			"AND CMDSYSMAP.INTERFACE_NAME = INTERFACEREF.INTERFACE_NAME " +
			"AND CMDSYSMAP.SYSTEM_NAME = INTERFACEREF.SYSTEM_NAME";

	public static final String SQL_GET_ACCOUNTTYPE = "SELECT PARAM_VALUE FROM T_OM_CMD_TRANS_DTLS WHERE CMD_REF_ID=? AND upper(PARAM_NAME)=upper(?)";

	/*private static final String SQL_GET_CMD_TRANS_DTLS_DB = "SELECT CMDSYSMAP.SYSTEM_NAME, CMDSYSMAP.CMD_NAME, INTERFACEREF.USER_NAME, INTERFACEREF.PWD, " +
			"CMDTRANS.CMD_REF_ID, INTERFACEREF.TECH_METHOD, INTERFACEREF.LOCATION, " +
			"CMDTRANSDTLS.SEQ_NO, CMDTRANSDTLS.PARAM_NAME, COALESCE(CMDTRANSDTLS.PARAM_VALUE, ' ') PARAM_VALUE, CMDTRANSDTLS.PARAM_SUB ,CMDPARAMMAP.DB_PARAM_MODE,CMDPARAMMAP.DB_PARAM_TYPE "+
			//"FROM T_OM_CMD_TRANS CMDTRANS, T_OM_CMD_TRANS_DTLS CMDTRANSDTLS, C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_CMD_DEST_REF CMDDESTREF  ,C_OM_CMD_PARAM_MAP CMDPARAMMAP " +
			"FROM T_OM_CMD_TRANS CMDTRANS, T_OM_CMD_TRANS_DTLS CMDTRANSDTLS, C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_INTERFACE_REF INTERFACEREF  ,C_OM_CMD_PARAM_MAP CMDPARAMMAP " +
			"WHERE CMDTRANS.ROW_ID=? " +
			"AND CMDTRANS.CMD_REF_ID = CMDSYSMAP.CMD_REF_ID " +
			"AND CMDPARAMMAP.PARAM_NAME=CMDTRANSDTLS.PARAM_NAME "+ 
            "AND CMDPARAMMAP.CMD_REF_ID=CMDSYSMAP.CMD_REF_ID "+
			"AND CMDSYSMAP.INTERFACE_NAME = INTERFACEREF.INTERFACE_NAME " +
			"AND CMDSYSMAP.SYSTEM_NAME = INTERFACEREF.SYSTEM_NAME " +
			"AND CMDTRANS.ROW_ID= CMDTRANSDTLS.PAR_ROW_ID ORDER BY CMDTRANSDTLS.SEQ_NO";*/
	
	public static final String SQL_GET_CMD_TRANS_DTLS = "SELECT CMDSYSMAP.SYSTEM_NAME, CMDSYSMAP.CMD_NAME, INTERFACEREF.USER_NAME, INTERFACEREF.PWD, " +
			"CMDTRANS.CMD_REF_ID, INTERFACEREF.TECH_METHOD, INTERFACEREF.LOCATION, " +
			"CMDTRANSDTLS.SEQ_NO, CMDTRANSDTLS.PARAM_NAME, COALESCE(CMDTRANSDTLS.PARAM_VALUE, ' ') PARAM_VALUE, CMDTRANSDTLS.PARAM_SUB " +
			//"FROM T_OM_CMD_TRANS CMDTRANS, T_OM_CMD_TRANS_DTLS CMDTRANSDTLS, C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_CMD_DEST_REF CMDDESTREF " +
			"FROM T_OM_CMD_TRANS CMDTRANS, T_OM_CMD_TRANS_DTLS CMDTRANSDTLS, C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_INTERFACE_REF INTERFACEREF " +
			"WHERE CMDTRANS.ROW_ID=? " +
			"AND CMDTRANS.CMD_REF_ID = CMDSYSMAP.CMD_REF_ID " +
			"AND CMDSYSMAP.INTERFACE_NAME = INTERFACEREF.INTERFACE_NAME " +
			"AND CMDSYSMAP.SYSTEM_NAME = INTERFACEREF.SYSTEM_NAME " +
			"AND CMDTRANS.ROW_ID= CMDTRANSDTLS.PAR_ROW_ID ORDER BY CMDTRANSDTLS.SEQ_NO";

	public static final String SQL_GET_DB_CMD_TRANS_DTLS = "SELECT CMDTRANSDTLS.PARAM_NAME, COALESCE(CMDTRANSDTLS.PARAM_VALUE, ' ') PARAM_VALUE, " +
			"CMDPARAMMAP.DB_PARAM_MODE,CMDPARAMMAP.DB_PARAM_TYPE " +
			"FROM T_OM_CMD_TRANS CMDTRANS, T_OM_CMD_TRANS_DTLS CMDTRANSDTLS , C_OM_CMD_PARAM_MAP CMDPARAMMAP " +
			"WHERE CMDTRANS.ROW_ID = ? " +
			"AND CMDTRANS.ROW_ID = CMDTRANSDTLS.PAR_ROW_ID " +
			"AND CMDTRANS.CMD_REF_ID = CMDPARAMMAP.CMD_REF_ID " +
			"AND CMDTRANSDTLS.PARAM_NAME = CMDPARAMMAP.PARAM_NAME ORDER BY CMDTRANSDTLS.SEQ_NO";
	
	public static final String SQL_GET_CMD_TRANS_ERROR_TASK="SELECT * FROM T_OM_CMD_TRANS WHERE trans_id=? and order_row_id=? AND STATUS = 'Error'";

	public static final String SQL_GET_CMD_TRANS_DTLS_ERROR_TASK="SELECT * FROM  T_OM_CMD_TRANS_DTLS WHERE PAR_ROW_ID=? AND XML_ELE_TYPE IS NOT NULL";

	public static final String SQL_GET_CMD_AUTH_TOKEN_TRANS_DTLS = "SELECT CMDSYSMAP.SYSTEM_NAME, CMDSYSMAP.CMD_NAME, INTERFACEREF.USER_NAME, INTERFACEREF.PWD, CMDTRANS.CMD_REF_ID, " +
			"CMDTRANSDTLS.SEQ_NO, CMDTRANSDTLS.PARAM_NAME, COALESCE(CMDTRANSDTLS.PARAM_VALUE, ' ') PARAM_VALUE " +
			//"FROM T_OM_CMD_TRANS CMDTRANS, T_OM_CMD_TRANS_DTLS CMDTRANSDTLS, C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_CMD_DEST_REF CMDDESTREF " +
			"FROM T_OM_CMD_TRANS CMDTRANS, T_OM_CMD_TRANS_DTLS CMDTRANSDTLS, C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_INTERFACE_REF INTERFACEREF " +
			"WHERE CMDTRANS.CMD_REF_ID=? " +
			"AND CMDTRANS.CMD_REF_ID = CMDSYSMAP.CMD_REF_ID " +
			"AND CMDSYSMAP.INTERFACE_NAME = INTERFACEREF.INTERFACE_NAME " +
			"AND CMDSYSMAP.SYSTEM_NAME = INTERFACEREF.SYSTEM_NAME " +
			"AND CMDTRANS.ROW_ID= CMDTRANSDTLS.PAR_ROW_ID ORDER BY CMDTRANSDTLS.SEQ_NO";

	public static final String SQL_SAVE_COMMAND_RESPONSE = "INSERT INTO T_OM_CMD_RESPONSE(CMD_ROW_ID, CMD_REF_ID, TRANS_ID, RESPONSE_XML, INTERFACE_NAME, STATUS, INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY) VALUES(?, ?, ?, ?, ?, ?, SYSDATE, USER, SYSDATE, USER)";

	public static final String SQL_GET_AUTH_TOKEN_PARAMS = "SELECT COALESCE(USER_NAME, ' ') USER_NAME, COALESCE(PWD, ' ') PWD, " +
			"ACCOUNT_TYPE, LOCATION, INTERFACE_NAME, SYSTEM_NAME " +
			//"FROM C_OM_CMD_DEST_REF CMDDESTREF WHERE CMDDESTREF.SYSTEM_NAME=? AND ACCOUNT_TYPE IS NOT NULL";
			"FROM C_OM_INTERFACE_REF WHERE SYSTEM_NAME=? AND ACCOUNT_TYPE IS NOT NULL";

	public static final String SQL_INSERT_ERROR = "INSERT INTO T_OM_ERR_LOG(APPLN_NAME, ORDER_ID, CMD_REF_ID, ERR_SRC, ERR_CODE, ERR_MSG, INSERT_DT, INSERT_BY, UPDATED_DT, UPDATED_BY) VALUES (?, ?, ?, ?, ?, ?, SYSDATE, USER, SYSDATE, USER)";

	public static final String SQL_GET_ORDER_TRANS_DETAILS = "SELECT ORDERTRANSDTLS.TRANS_ID, ORDERTRANSDTLS.STATUS, CMDTRANS.CMD_REF_ID, ORDERTRANSDTLS.SRC_TRANS_ID, ORDERTRANSDTLS.ROW_ID, ORDERTRANSDTLS.FUNC_REF_ID, " +
			"ORDERTRANSDTLS.OPG_REF_ID, ORDERTRANSDTLS.CORR_ID, " +
			"COALESCE(RETURN_CODE, ' ') RETURN_CODE, COALESCE(RETURN_MSG, ' ') RETURN_MSG " +
			"FROM T_OM_CMD_TRANS CMDTRANS, T_OM_ORDER_TRANS_DTLS ORDERTRANSDTLS " +
			"WHERE CMDTRANS.ROW_ID=? " +
			"AND CMDTRANS.ORDER_ROW_ID=ORDERTRANSDTLS.ROW_ID " +
			"AND CMDTRANS.TRANS_ID=ORDERTRANSDTLS.TRANS_ID";
	public static final String SQL_GET_ORDER_TRANS_DETAILS_BILL = "SELECT ORDERTRANSDTLS.TRANS_ID, ORDERTRANSDTLS.STATUS, CMDTRANS.CMD_REF_ID, ORDERTRANSDTLS.SRC_TRANS_ID, ORDERTRANSDTLS.ROW_ID, ORDERTRANSDTLS.FUNC_REF_ID, " +
			"ORDERTRANSDTLS.OPG_REF_ID, ORDERTRANSDTLS.CORR_ID, " +
			"COALESCE(RETURN_CODE, ' ') RETURN_CODE, COALESCE(RETURN_MSG, ' ') RETURN_MSG " +
			"FROM T_OM_BILL_CMD_TRANS CMDTRANS, T_OM_ORDER_TRANS_DTLS ORDERTRANSDTLS " +
			"WHERE CMDTRANS.ROW_ID=? " +
			"AND CMDTRANS.ORDER_ROW_ID=ORDERTRANSDTLS.ROW_ID " +
			"AND CMDTRANS.TRANS_ID=ORDERTRANSDTLS.TRANS_ID";

	public static final String SQL_GET_WCF_CMD_TRANS_DETAILS = "SELECT CMDTRANSDTLS.PARAM_NAME, COALESCE(CMDTRANSDTLS.PARAM_VALUE, ' ') PARAM_VALUE, COALESCE(CMDTRANSDTLS.XML_ELE_TYPE, ' ') XML_ELE_TYPE, " +
			"COALESCE(CMDTRANSDTLS.XML_ELE_NAME, ' ') XML_ELE_NAME, COALESCE(CMDTRANSDTLS.XML_ELE_ID, ' ') XML_ELE_ID, COALESCE(CMDTRANSDTLS.XML_PAR_ELE_ID, ' ') XML_PAR_ELE_ID, " +
			"COALESCE(CMDTRANSDTLS.XML_ELE_CURR_VALUE, ' ') XML_ELE_CURR_VALUE, COALESCE(CMDTRANSDTLS.XML_ELE_PREV_VALUE, ' ') XML_ELE_PREV_VALUE FROM T_OM_CMD_TRANS_DTLS CMDTRANSDTLS " +
			"WHERE CMDTRANSDTLS.PAR_ROW_ID=? AND CMDTRANSDTLS.XML_ELE_TYPE IS NOT NULL AND UPPER(CMDTRANSDTLS.XML_ELE_TYPE)!='CUSTINFO' ORDER BY CMDTRANSDTLS.SEQ_NO";

	public static final String SQL_GET_WCF_VMS_CMD_TRANS_DETAILS = "SELECT CMDTRANSDTLS.PARAM_NAME, CMDTRANSDTLS.PARAM_VALUE, COALESCE(CMDTRANSDTLS.XML_ELE_TYPE, ' ') XML_ELE_TYPE, " +
			"COALESCE(CMDTRANSDTLS.XML_ELE_NAME, ' ') XML_ELE_NAME, COALESCE(CMDTRANSDTLS.XML_ELE_ID, ' ') XML_ELE_ID, " +
			"COALESCE(CMDTRANSDTLS.XML_ELE_CURR_VALUE, ' ') XML_ELE_CURR_VALUE, COALESCE(CMDTRANSDTLS.XML_ELE_PREV_VALUE, ' ') XML_ELE_PREV_VALUE FROM T_OM_CMD_TRANS_DTLS CMDTRANSDTLS " +
			"WHERE CMDTRANSDTLS.PAR_ROW_ID=? ";


	public static final String SQL_GET_PARAMVALUE = "SELECT COALESCE(PARAM_VALUE, ' ') PARAM_VALUE FROM T_OM_CMD_TRANS_DTLS WHERE PAR_ROW_ID=? AND upper(PARAM_NAME)=upper(?)";

	public static final String SQL_GET_RETURNCODE_AND_MESSAGE = "SELECT COALESCE(RETURN_CODE, ' ') RETURN_CODE, COALESCE(RETURN_MSG, ' ') RETURN_MSG FROM T_OM_CMD_TRANS where ROW_ID=?";
	//Start by Billy Lim Added in Bug 2142. Refer to modification history
	public static final String SQL_GET_ORDER_TRANS_DETAILS_BY_ORDERTRANSID = "SELECT ROW_ID,  ORDER_XML, FUNC_REF_ID, OPG_REF_ID, STATUS, POLL_STATUS, CORR_ID, INSERT_DT, INSERT_BY, " +
					"UPDATED_DT, UPDATED_BY, TRANS_ID, SRC_TRANS_ID, SOURCE FROM T_OM_ORDER_TRANS_DTLS WHERE TRANS_ID=? AND SRC_TRANS_ID=?";
	
	public static final String SQL_GET_LAST_COMPLETED_CMD_TRANS = "SELECT ROW_ID AS CMD_ROW_ID FROM T_OM_CMD_TRANS WHERE SEQ_NO=(SELECT MAX(SEQ_NO) FROM T_OM_CMD_TRANS WHERE TRANS_ID=? AND SRC_TRANS_ID=? AND UPPER(STATUS)='COMPLETED') AND TRANS_ID=? AND SRC_TRANS_ID=?";
	//End by Billy Lim Added in Bug 2142. Refer to modification history
	
	// Ravi: Added to get subscriber id
	public static final String SQL_GET_SUBSCRIBER_ID = "SELECT COALESCE( SERVICE_INTERNAL_ID, '') SERVICE_INTERNAL_ID FROM (SELECT  DISTINCT SERVICE_INTERNAL_ID  FROM T_OM_BILL_CMD_RESPONSE WHERE SRC_TRANS_ID= ? and SERVICE_INTERNAL_ID is not null ) ";
	
	public static final String SQL_GET_DB_ADAPTOR_TRANS_DTLS = "SELECT CMDTRANS.CMD_REF_ID, SRC_TRANS_ID, TRANS_ID, SYSTEM_NAME, INTERFACE_NAME FROM T_OM_CMD_TRANS CMDTRANS, " +
									"C_OM_CMD_SYS_MAP CMDSYSMAP WHERE CMDTRANS.ROW_ID=? AND CMDTRANS.CMD_REF_ID = CMDSYSMAP.CMD_REF_ID";

         public static final String SQL_GET_SUBSCRIDRESETS = "SELECT COALESCE( SERVICE_INTERNAL_ID_RESETS, '') SERVICE_INTERNAL_ID_RESETS FROM (SELECT  DISTINCT SERVICE_INTERNAL_ID_RESETS  FROM T_OM_BILL_CMD_RESPONSE WHERE SRC_TRANS_ID= ? and SERVICE_INTERNAL_ID_RESETS is not null ) ";
         
    public static final String SQL_GET_ORDER_TRANS_DTLS_BY_ROWID = "SELECT CORR_ID, SOURCE FROM T_OM_ORDER_TRANS_DTLS WHERE ROW_ID=?";     
	/**
	 * Constants for Table columns
	 */

	public static final String COLUMN_OM_STATUS_CD = "OM_STATUS_CD";
	public static final String COLUMN_ERROR_CODE = "ERROR_CODE";
	public static final String COLUMN_ERROR_MESSAGE = "ERROR_MESSAGE";
	public static final String COLUMN_CMD_REF_ID = "CMD_REF_ID";
	public static final String COLUMN_STATUS = "STATUS";
	public static final String COLUMN_ROW_ID = "ROW_ID";
	public static final String COLUMN_TRANS_ID = "TRANS_ID";
	public static final String COLUMN_SRC_TRANS_ID = "SRC_TRANS_ID";
	public static final String COLUMN_RESPONSE_XML = "RESPONSE_XML";
	public static final String COLUMN_INTERFACE_NAME = "INTERFACE_NAME";
	public static final String COLUMN_CMD_ROW_ID = "CMD_ROW_ID";
	public static final String AUTH_TOKEN_REQUIRED = "Y";
	public static final String C1_FLAG_NO = "N";
	
	public static final String COLUMN_BILL_ORDER_ROW_ID = "BILL_ORDER_ROW_ID";
	public static final String CMD_ROW_ID="CMD_ROW_ID";
	public static final String OFFER_INSTANCE_ID="OFFER_INSTANCE_ID";
	public static final String SERVICE_INTERNAL_ID="SERVICE_INTERNAL_ID";
	public static final String SERVICE_INTERNAL_ID_RESETS = "SERVICE_INTERNAL_ID_RESETS";
	public static final String ERROR_CODE="ERROR_CODE";
	public static final String ERROR_MESSAG="RESPONSE_MSG";
	
	public static final String COLUMN_C1ORDER_ID="C1_ORDER_ID";
	public static final String COLUMN_CRM_SVC_ID = "CRM_SVC_ID";

	/**
	 * Constants for TOM Response XML
	 */

	public static final String TOM_RES_NS = "http://www.m1.com/bcc/spl/provresponse";
	public static final String TOM_RES_PROVRESPONSE = "ProvResponse";
	public static final String TOM_RES_LISTOFREF = "ListOfRef";
	public static final String TOM_RES_STATUSCODE = "StatusCode";
	public static final String TOM_RES_ERRORMESSAGE = "ErrorMessage";
	public static final String TOM_RES_ERRORCODE = "ErrorCode";
	public static final String TOM_RES_CORRELATIONID = "CorrelationId";
	public static final String TOM_RES_TXNSTARTTIME = "TxnStartTime";
	public static final String TOM_RES_TOMORDERID = "TomOrderID";
	public static final String TOM_RES_OPGREFID = "OPGRefID";
	public static final String TOM_RES_FUNCREFID = "FuncRefID";
	public static final String TOM_RES_CMDREFID = "CmdRefID";
	public static final String TOM_RES_TASKRESPONSE = "TaskResponse";
	public static final String TOM_RES_FUNCPARAMINFO = "FuncParamInfo";
	public static final String TOM_RES_PARAMINFO = "ParamInfo";

	public static final String TOM_RES_PARAMTYPE = "ParamType";
	public static final String TOM_RES_PARAMID = "ParamID";
	public static final String TOM_RES_PARAMNAME = "ParamName";
	public static final String TOM_RES_CURRENTVALUE = "CurrentValue";
	public static final String TOM_RES_PREVIOUSVALUE = "PreviousValue";


	/**
	 * Constants for Response to TOM status
	 */
	public static final String TOM_RESPONSE_SUCCESS = "0";
	public static final String TOM_RESPONSE_ERROR = "1";
	
	public static final String C1ORDER_STATUS = "80";
	public static final String C1ORDER_STATUS_COMMITTED = "20";
	/*public static final String STATUS_B2B_SUCCESS = "SUCCESS";*/

	/**
	 * SYSTEM NAMES AND FILE LOCATION CONSTANTS
	 */
	public static final String ERROR_TASK_FUNCREFID="ERRTASK.0001";
	public static final String ERROR_TASK_OPGREFID ="ERRTASK.0001";
	public static final String ERROR_TASK_CMD_REF_ID="ERRCMDCRMTASK_001";


	public static final String SYSTEM_TECH_METHOD = "WebService";
	public static final String SYSTEM_TECH_METHOD_DB = "Database";
	public static final String SYSTEM_TECH_METHOD_HTTP = "HTTP";
	
	public static final String SYSTEM_BIS = "BIS";
	public static final String SYSTEM_BBRR = "BBRR";
	public static final String SYSTEM_ENTMSG = "ENTMSG";
	public static final String SYSTEM_NETSECURITY = "NETSECURITY";
	public static final String SYSTEM_MMSC = "MMSC";
	public static final String SYSTEM_NFC = "NFC";
	public static final String SYSTEM_PRS = "PRS";
	public static final String SYSTEM_LOCKCUBE = "LOCKCUBE";
	public static final String SYSTEM_ENS = "ENS";
	public static final String SYSTEM_IDS = "IDS";
	public static final String SYSTEM_SDP = "SDP";
	public static final String SYSTEM_U2KRSP = "U2KRSP";
	public static final String SYSTEM_ARBOR = "ARBOR";
	public static final String SYSTEM_GAZELLE = "GAZELLE";
	public static final String SYSTEM_PROXIMITY = "PROXIMITY";
	public static final String SYSTEM_CRM = "CRM";
	public static final String SYSTEM_MMA = "MMA";
	public static final String SYSTEM_VMS = "VMS";
	public static final String SYSTEM_IL = "IL";
	public static final String SYSTEM_U2KOPCO = "U2KOPCO";
	public static final String SYSTEM_SIMA = "SIMA";
	public static final String SYSTEM_ARBOR_REQDATE = "sCustReqDate";
	public static final String SYSTEM_RIM="RIM";
	public static final String SYSTEM_EAS="EAS";
	public static final String SYSTEM_RS="RS";
	public static final String SYSTEM_ADS="ADS";
	public static final String SYSTEM_SAS="SAS";
	//private static final String SYSTEM_DATABASE="DB";
	public static final String SYSTEM_FMS = "FMS";
	public static final String SYSTEM_STARHUB = "STARHUB";
	public static final String SYSTEM_SMARTROAM = "SMARTROAM";
	public static final String SYSTEM_DP7 = "DP7";
	public static final String SYSTEM_WAP = "WAP";
	public static final String SYSTEM_OPCOBILLING = "OPCO_BILLING";
	public static final String SYSTEM_PSBILLING = "PS_BILLING";


	public static String CRM_TASK = "MANUALTASK";
	public static String CRM_OMDNETTASK = "CRM_001";
	public static String CRM_OMDNETMODULE = "CRM_002";


	public static final String INTERFACE_SIMA_001 = "SIMA_001";
	public static final String INTERFACE_GAZELLE_001 = "GAZELLE_001";
    public static final String INTERFACE_GAZELLE_002 = "GAZELLE_002";
    public static final String INTERFACE_GAZELLE_003 = "GAZELLE_003";
    public static final String INTERFACE_GAZELLE_004 = "GAZELLE_004";
    public static final String INTERFACE_GAZELLE_005 = "GAZELLE_005";
    public static final String INTERFACE_VMS001 ="VMS_001";
    public static final String INTERFACE_PROXIMITY_003 = "PROXIMITY_003";

    public static final String INTERFACE_RIM_001= "RIM_001";
    public static final String INTERFACE_RIM_002= "RIM_002";
    public static final String INTERFACE_RIM_003= "RIM_003";
    
    public static final String INTERFACE_EAS_001= "EAS_001";
    
    public static final String INTERFACE_RS_001= "RS_001";
    public static final String INTERFACE_RS_002= "RS_002";
    public static final String INTERFACE_RS_003= "RS_003";
    public static final String INTERFACE_RS_004= "RS_004";
    
    public static final String INTERFACE_ADS_001= "ADS_001";
    public static final String INTERFACE_ADS_002= "ADS_002";
    public static final String INTERFACE_ADS_003= "ADS_003";
    public static final String INTERFACE_ADS_004= "ADS_004";
    public static final String INTERFACE_ADS_005= "ADS_005";
    public static final String INTERFACE_ADS_006= "ADS_006";
    public static final String INTERFACE_ADS_007= "ADS_007";
    
    public static final String INTERFACE_STARHUB_001= "STARHUB_001";
    
	public static final String FOLDER_ADAPTORS = "conf/spl/adaptors/";
	public static final String APPEND_REQUEST = "/request/";
	public static final String APPEND_RESPONSE = "/response/";
	public static final String SYSTEM_APPEND_REQ_DOTXML = "_Request.xml";
	public static final String SYSTEM_APPEND_RES_DOTXML = "_Response.xml";
	
	public static final String SYSTEM_APPEND_REQ_DOTTXT = "_Request.txt";
	public static final String SYSTEM_APPEND_RES_DOTTXT = "_Response.txt";

	public static final String FOLDER_STUBS = "_STUBS/";

	public static final String SYSTEM_PROXIMITY_AUTHTOKEN_REQ = "PROXIMITY_002_Request.xml";
	public static final String SYSTEM_PROXIMITY_AUTHTOKEN_RES = "PROXIMITY_002_Response.xml";

	/***************** ARBOR ******************/
	public static final String SYSTEM_ARBOR_AUTHTOKEN_REQUEST_PATH = "conf/ARBOR/AuthenticationRequest.xml";
	public static final String SYSTEM_ARBOR_AUTHTOKEN_RESPONSE_PATH = "conf/ARBOR/AuthenticationResponse.xml";
	public static final String SYSTEM_ARBOR_REQUEST_PATH = "conf/ARBOR/ARBORRequest.xml";
	public static final String SYSTEM_ARBOR_RESPONSE_PATH = "conf/ARBOR/ARBORResponse.xml";

	/***************** B2B ******************/
	public static final String SYSTEM_B2B = "B2B";

	public static final String SYSTEM_B2B_REQUEST_PATH_PREPEND = "conf/B2B/";
	public static final String SYSTEM_B2B_REQUEST_PATH_APPEND = "Request.xml";
	public static final String SYSTEM_B2B_RESPONSE_PATH_PREPEND = "conf/B2B/";
	public static final String SYSTEM_B2B_RESPONSE_PATH_APPEND = "Response.xml";

	public static final String SYSTEM_B2B_OPERATION_COVERAGECHECK = "CoverageCheckRequest";
	public static final String SYSTEM_B2B_OPERATION_ADVANCEDCOVERAGECHECK = "AdvancedCoverageCheck";
	public static final String SYSTEM_B2B_OPERATION_CREATECANCELLATIONORDER = "CreateCancellationOrder";
	public static final String SYSTEM_B2B_OPERATION_CREATECONNECTIONORDER = "CreateConnectionOrder";
	public static final String SYSTEM_B2B_OPERATION_CREATETERMINATIONORDER = "CreateTerminationOrder";

	/***************** BIS - Basic Information Service ******************/
	public static final String SYSTEM_BIS_REQUEST_PATH = "conf/BIS/BISRequest.xml";
	public static final String SYSTEM_BIS_RESPONSE_PATH = "conf/BIS/BISResponse.xml";


	/***************** BBRR - BYE BYE RING RING ******************/
    public static final String SYSTEM_BBRR_REQUEST_PATH = "conf/BBRR/BBRRRequest.xml";
    public static final String SYSTEM_BBRR_RESPONSE_PATH = "conf/BBRR/BBRRResponse.xml";


    /***************** ENS ******************/
    //public static final String SYSTEM_ENS="ENS";
	public static final String SYSTEM_ENS_REQUEST_PATH = "conf/ENS/ENSRequest.xml";
	public static final String SYSTEM_ENS_RESPONSE_PATH = "conf/ENS/ENSResponse.xml";

	/***************** EM *******************/
	//public static final String SYSTEM_EM="EM";
	public static final String SYSTEM_EM_REQUEST_PATH = "conf/EM/EMRequest.xml";
	public static final String SYSTEM_EM_RESPONSE_PATH = "conf/EM/EMResponse.xml";

	/***************** GAZELLE *******************/

	public static final String SYSTEM_GAZELLE_OPERATION_ADDVAS = "ORDCMDGAZELLE_001";
	public static final String SYSTEM_GAZELLE_OPERATION_REMOVEVAS = "ORDCMDGAZELLE_002";
	public static final String SYSTEM_GAZELLE_OPERATION_NUMBERCHANGE = "ORDCMDGAZELLE_003";
	public static final String SYSTEM_GAZELLE_OPERATION_TERMINATELINE = "ORDCMDGAZELLE_004";
	public static final String SYSTEM_GAZELLE_OPERATION_CALLWRAPUP = "ORDCMDGAZELLE_005";

	public static final String SYSTEM_GAZELLE_PATH_PREPEND = "conf/GAZELLE/";
	public static final String SYSTEM_GAZELLE_REQUEST_PATH_APPEND = "Request.xml";
	public static final String SYSTEM_GAZELLE_RESPONSE_PATH_APPEND = "Response.xml";


	/***************** IMEI *******************/
	public static final String SYSTEM_IMEI="IMEI";
	public static final String SYSTEM_IMEI_REQUEST_PATH = "conf/IMEI/IMEIRequest.xml";
	public static final String SYSTEM_IMEI_RESPONSE_PATH = "conf/IMEI/IMEIResponse.xml";

	/***************** IS - Internet Security*******************/
	public static final String SYSTEM_IS="IS";
	public static final String SYSTEM_IS_REQUEST_PATH = "conf/IS/ISRequest.xml";
	public static final String SYSTEM_IS_RESPONSE_PATH = "conf/IS/ISResponse.xml";

	/***************** IS - Manage My Account *******************/
	public static final String SYSTEM_MMA_REQUEST_PATH = "conf/MMA/MMARequest.xml";
	public static final String SYSTEM_MMA_RESPONSE_PATH = "conf/MMA/MMAResponse.xml";

	/***************** NewOpCo Order service*******************/
	public static final String SYSTEM_NEWOPCO = "NEWOPCO";
	public static final String SYSTEM_NEW_OP_CO_ORDER_INSTALLNEWLINE = "NEW_OP_CO_ORDER_INSTALLNEWLINE";
	public static final String SYSTEM_NEW_OP_CO_ORDER_SUSPFORCANCELLINE = "NEW_OP_CO_ORDER_SUSPFORCANCELLINE";
	public static final String SYSTEM_NEW_OP_CO_ORDER_MAINTAINCOMPONENT = "NEW_OP_CO_ORDER_MAINTAINCOMPONENT";
	public static final String SYSTEM_NEW_OP_CO_ORDER_CHANGEBILLPLAN = "NEW_OP_CO_ORDER_CHANGEBILLPLAN";

	public static final String SYSTEM_NEWOPCO_PATH_PREPEND = "conf/NEWOPCO/";
	public static final String SYSTEM_NEWOPCO_REQUEST_PATH_APPEND = "Request.xml";
	public static final String SYSTEM_NEWOPCO_RESPONSE_PATH_APPEND = "Response.xml";


	/***************** PRS - Premium Rated Service *******************/

	public static final String SYSTEM_PRS_REQUEST_PATH_PREPEND = "SPLStubs/PRS/request/";
	public static final String SYSTEM_PRS_REQUEST_PATH_APPEND = "Request.xml";
	public static final String SYSTEM_PRS_RESPONSE_PATH_PREPEND = "SPLStubs/PRS/response/";
	public static final String SYSTEM_PRS_RESPONSE_PATH_APPEND = "Response.xml";

	/***************** PROXIMITY *******************/

	public static final String SYSTEM_PROXIMITY_OPERATION_AUTHTOKEN = "ORDCMDPROXIMITY_002";

	public static final String SYSTEM_PROXIMITY_REQUEST_PATH_PREPEND = "SPLStubs/PROXIMITY/request/";
	public static final String SYSTEM_PROXIMITY_REQUEST_PATH_APPEND = "Request.xml";
	public static final String SYSTEM_PROXIMITY_RESPONSE_PATH_PREPEND = "SPLStubs/PROXIMITY/response/";
	public static final String SYSTEM_PROXIMITY_RESPONSE_PATH_APPEND = "Response.xml";

	/***************** SS - Secure Storage *******************/
	public static final String SYSTEM_SS = "SS";

	public static final String SYSTEM_SS_REQUEST_PATH_PREPEND = "SPLStubs/SS/request/";
	public static final String SYSTEM_SS_REQUEST_PATH_APPEND = "Request.xml";
	public static final String SYSTEM_SS_RESPONSE_PATH_PREPEND = "SPLStubs/SS/response/";
	public static final String SYSTEM_SS_RESPONSE_PATH_APPEND = "Response.xml";


	public static final String FOLDER_SPLSTUBS_WEBSERVICES = "SPLStubs/WebServices/";


	/**
	 * XPATH Constants for System Response XML
	 */


	public static final String XPATH_GENERAL_ERROR_CODE         = "Envelope/Body/Fault/detail/ServiceFault/errorCode";
	public static final String XPATH_GENERAL_ERROR_DESCRIPTION  = "Envelope/Body/Fault/detail/ServiceFault/errorDescription ";

	public static final String XPATH_ARBOR_SRETURN_CODES = "Envelope/Body/ProcessCorridorDetailResponse/ProcessCorridorDetailResult/sReturnCodes";
	public static final String XPATH_ARBOR_SRETURN_DESCS = "Envelope/Body/ProcessCorridorDetailResponse/ProcessCorridorDetailResult/sReturnDescs";

	public static final String XPATH_B2B_RETURNCODE = "responseHeader/returnCode";
	public static final String XPATH_B2B_RETURNMESSAGE = "responseHeader/returnMessage";
	public static final String XPATH_B2B_RETURN_MESSAGE_DESC = "responseHeader/returnMessageDesc";


	public static final String XPATH_BBRR_DESCRIPTION = "Envelope/Body/multiRef/description";
	public static final String XPATH_BBRR_RETURNCODE = "Envelope/Body/multiRef/returnCode";

	/*************/
	public static final String XPATH_BIS_PROCESS_REQUEST_RESULT = "ProcessRequestResponse/ProcessRequestResult";

	public static final String XPATH_ENS_PROCESS_REQUEST_RESULT = "ProcessRequestResponse/ProcessRequestResult";

	public static final String XPATH_EM_PROCESS_REQUEST_RESULT = "ProcessRequestResponse/ProcessRequestResult";

	public static final String XPATH_IS_PROCESS_REQUEST_RESULT = "ProcessRequestResponse/ProcessRequestResult";

	public static final String XPATH_MMA_PROCESS_REQUEST_RESULT = "ProcessRequestResponse/ProcessRequestResult";

	public static final String XPATH_SDP_PROCESS_REQUEST_RESULT = "ProcessRequestResponse/ProcessRequestResult ";

	public static final String XPATH_PROCESS_REQUEST_RESULT = "ProcessRequestResponse/ProcessRequestResult ";

	// VMS
	public static String XPATH_OMDNETTASK_TRANSTATUS = "CreateTaskListOMResponse/ds/diffgram/ds/OMDnetTaskModuleInput/InputHeader/TranStatus";
	public static String XPATH_OMDNETTASK_RETURNCODE = "CreateTaskListOMResponse/ds/diffgram/ds/OMDnetTaskModuleInput/InputHeader/ReturnCode";
	public static String XPATH_OMDNETTASK_RETURNMESSAGE = "CreateTaskListOMResponse/ds/diffgram/ds/OMDnetTaskModuleInput/InputHeader/ReturnMessage";
	public static String XPATH_OMDNETTASK_OPERATIONSTATUS = "CreateTaskListOMResponse/ds/diffgram/ds/OMDnetTaskModuleInput/InputBody/OperationStatus";

	public static String XPATH_OMDNETMODULE_STATUS = "CreateTaskListOMResponse/CreateTaskListOMResult/diffgram/DocumentElement/CreateTaskListOM/StatusDescription";

	public static String XPATH_OMDNETMODULE_RETURNCODE = "InvokeOMDnetModuleResponse/msgHeader/returnCode";
	public static String XPATH_OMDNETMODULE_RETURNMESSAGE = "InvokeOMDnetModuleResponse/msgHeader/returnMessage";

	public static String XPATH_DBADAPTOR_RETURNCODE = "DBAdaptorResponse/msgHeader/returnCode";
	public static String XPATH_DBADAPTOR_RETURNMESSAGE = "DBAdaptorResponse/msgHeader/returnMessage";
	
	public static String XPATH_VMS_OPERATIONSTATUS = "Envelope/Body/InvokeLDAPProcessorResponse/InvokeLDAPProcessorResult/msgBodyField/operationStatusField";
	public static String XPATH_VMS_RETURNCODE = "Envelope/Body/InvokeLDAPProcessorResponse/InvokeLDAPProcessorResult/msgHeaderField/returnCodeField";
	public static String XPATH_VMS_RETURNMESSAGE = "Envelope/Body/InvokeLDAPProcessorResponse/InvokeLDAPProcessorResult/msgHeaderField/returnMessageField";
	public static String XPATH_VMS_TRANSTATUS = "Envelope/Body/InvokeLDAPProcessorResponse/InvokeLDAPProcessorResult/msgHeaderField/tranStatusField";

	public static String XPATH_MANUALTASK_STATUSCODE = "Envelope/Body/arrayOfParamInfoType/StatusCode";
	public static String XPATH_MANUALTASK_ERRMSG = "Envelope/Body/ProvResponse/@ErrorMessage";
	public static String XPATH_MANUALTASK_ERRCODE = "Envelope/Body/ProvResponse/@ErrorCode";

	/*************/

	public static final String XPATH_GAZELLE_ADDREMOVE_STATUS = "Envelope/Body/GazelleGenericServiceResponse/return/OperationResult/Status";
	public static final String XPATH_GAZELLE_ADDREMOVE_ERRORCODE = "Envelope/Body/GazelleGenericServiceResponse/return/OperationResult/ErrorCode";
	public static final String XPATH_GAZELLE_ADDREMOVE_ERRORMESSAGE = "Envelope/Body/GazelleGenericServiceResponse/return/OperationResult/ErrorMessage";

	public static final String XPATH_GAZELLE_CHANGESIMSTATE_STATUS = "Envelope/Body/ChangeSIMStateResponse/ChangeSIMState/ChangeStatus";
	
	public static final String XPATH_GAZELLE_PORTIN_STATUS = "Envelope/Body/PortInResponse/PortInReturn/return";
	public static final String XPATH_GAZELLE_PORTIN_ERRORCODE = "Envelope/Body/PortInResponse/PortInReturn/WebServiceResult/ErrorCode";
	public static final String XPATH_GAZELLE_PORTIN_ERRORMESSAGE = "Envelope/Body/PortInResponse/PortInReturn/WebServiceResult/ErrorMessages";

	public static final String XPATH_GAZELLE_PORTOUT_STATUS = "Envelope/Body/PortOutResponse/PortOutReturn/return";
	public static final String XPATH_GAZELLE_PORTOUT_ERRORCODE = "Envelope/Body/PortOutResponse/PortOutReturn/WebServiceResult/ErrorCode";
	public static final String XPATH_GAZELLE_PORTOUT_ERRORMESSAGE = "Envelope/Body/PortOutResponse/PortOutReturn/WebServiceResult/ErrorMessages";

	public static final String XPATH_GAZELLE_WRAPUP_STATUS = "Envelope/Body/callWrapUpResponse/callWrapUpReturn";

	public static final String XPATH_GAZELLE_INTERFACE_GAZELLE_001="GAZELLE_001";
	public static final String XPATH_GAZELLE_INTERFACE_GAZELLE_002="GAZELLE_002";
	public static final String XPATH_GAZELLE_INTERFACE_GAZELLE_003="GAZELLE_003";
	public static final String XPATH_GAZELLE_INTERFACE_GAZELLE_004="GAZELLE_004";
	public static final String XPATH_GAZELLE_INTERFACE_GAZELLE_005="GAZELLE_005";
	public static final String XPATH_GAZELLE_INTERFACE_GAZELLE_006="GAZELLE_006";

	public static final String XPATH_IMEI_RESULT = "Envelope/Body/SendIDSCommandResponse/result";

	public static final String XPATH_MMSC_RESULT = "RESPONSE/RESULT/@returnval";
	public static final String XPATH_MMSC_MSGCODE = "RESPONSE/MESSAGE/@msgcode";
	public static final String XPATH_MMSC_MSGTEXT = "RESPONSE/MESSAGE/@msgtext";

	public static final String XPATH_PRS_ADD_VAS_RESULT = "ADDVASResponse/ADDVASResult";

	public static final String XPATH_PROXIMITY_ERROR_CODE = "Envelope/Body/ExecuteResponse/ExecuteResult/WebServiceResult/ErrorCode";
	public static final String XPATH_PROXIMITY_ERROR_MESSAGES = "Envelope/Body/ExecuteResponse/ExecuteResult/WebServiceResult/ErrorMessages";

	public static final String XPATH_U2KRSP_ERROR_CODE = "Envelope/Body/Fault/detail/ServiceFault/errorCode";
	public static final String XPATH_U2KRSP_ERROR_DESCRIPTION = "Envelope/Body/Fault/detail/ServiceFault/errorDescription ";

	public static final String XPATH_SS_PROCESS_RESPONSE_RESULT = "ProcessResponse/ProcessResponseResult";

	public static final String XPATH_RIM001_ERR_CODE = "Envelope/Body/multiRef/errorCode";
	public static final String XPATH_RIM001_ERR_MSG = "Envelope/Body/multiRef/errorDescription";
	
	public static final String XPATH_RIM002_ERR_CODE = "ProvisionReply/body/ProvisionReplyEntity/resultCode/errorCode";
	public static final String XPATH_RIM002_ERR_MSG = "ProvisionReply/body/ProvisionReplyEntity/resultCode/errorDescription";
	
	//Added two constants for RIM interface
	public static final String XPATH_RIM003_ERR_CODE = "ProvisionReply/body/ProvisionReplyEntity/resultCode/errorCode";
	public static final String XPATH_RIM003_ERR_MSG = "ProvisionReply/body/ProvisionReplyEntity/resultCode/errorDescription";

	public static final String XPATH_RS_RETURN_CODE = "provision/responsecode";
	public static final String XPATH_RS_SUCCESS_MSG = "provision/response";
	public static final String XPATH_RS_ERROR_MSG = "provision/error";
	
	/**
	 *  PARAMETER NAME CONSTANTS
	 */

	public static final String PARAMETER_ACCOUNTTYPE = "accountType";
	public static final String PARAMETER_USERNAME = "username";
	public static final String PARAMETER_PASSWORD = "password";
	public static final String PARAMETER_CREDENTIAL = "Credential";
	public static final String PARAMETER_AUTHTICKET = "AuthTicket";
	public static final String PARAMETER_SINPUTTAG = "sInputTag";
	public static final String PARAMETER_GAZELLE_AUTHTICKET = "authTicket";
	public static final String PARAMETER_GAZELLE_AUTH_TICKET = "auth_ticket";

	public static final String PARAMETER_GAZELLE_ARRAYOFBUNDLESUBSCRIPTIONINPUT = "ArrayOfBundleSubscriptionInput";

	public static final String PARAMETER_DNETOMMODULEINPUT = "DnetOMModuleInput";
	public static final String PARAMETER_MESSAGENAME = "messageName";
	public static final String PARAMETER_INPUTHEADER = "InputHeader";
	public static final String PARAMETER_VERSION = "version";
	public static final String PARAMETER_CORRELATIONID = "CorrelationID";
	public static final String PARAMETER_TRANSTATUS = "TranStatus";
	public static final String PARAMETER_RETURNCODE = "ReturnCode";
	public static final String PARAMETER_RETURNMESSAGE = "ReturnMessage";
	public static final String PARAMETER_INPUTBODY = "InputBody";
	public static final String PARAMETER_AUTHENTICATIONINFO = "AuthenticationInfo";
	public static final String PARAMETER_ACCTTYPE = "AcctType";
	public static final String PARAMETER_OPERATION = "OperationName";

	public static final String PARAMETER_OPERATIONSTATUS = "OperationStatus";
	public static final String PARAMETER_ORDERID = "OrderID";
	public static final String PARAMETER_SERVICEID = "ServiceID";
	public static final String PARAMETER_CUSTINFO = "CustInfo";
	public static final String PARAMETER_CUSTID = "CUST_ID";
	public static final String PARAMETER_CUSTIDTYPE = "CustIDType";
	public static final String PARAMETER_ARRAYOFFUNCPARAMSSET = "ArrayOfFuncParamsSet";
	public static final String PARAMETER_FUNCPARAMSSET = "FuncParamsSet";
	public static final String PARAMETER_PARAMTYPE = "ParamType";
	public static final String PARAMETER_PARAMID = "ParamID";
	public static final String PARAMETER_PARAMNAME = "ParamName";
	public static final String PARAMETER_CURRVALUE = "CurrValue";
	public static final String PARAMETER_PREVVALUE = "PrevValue";

	/**
	 * Stub File Contants
	 *
	 */

	public static final String STUB_FILENAME = "conf/stub.properties";
	public static final String STUB_PROPERTY = "STUB";
	public static final String STUB_YES = "Y";
	public static final String STUB_NO = "N";
	public static final String SSL_CERT = "SSL.CERT";

	public static final String STUBBING_FILENAME = "conf/spl/splstubbing.properties";
	public static final String WSSOAPACTION_FILENAME = "conf/spl/splwssoapaction.properties";
	public static final String U2KRSP_FILENAME = "conf/spl/splrsp.properties";
	public static final String U2KRSP_ERROR_RESPONSE_PATH = "conf/spl/adaptors/U2KRSP/response/U2KRSP_Error_Response.xml";
	public static final String SSLCERT_FILENAME = "conf/spl/sslcert.properties";
	public static final String OSMTRANSLATION_FILENAME = "conf/spl/splosmtranslation.properties";
	public static final String ILRESPQUEUEID_FILENAME = "conf/spl/splilrespqueueid.properties";
	public static final String SPLAPP_FILENAME = "conf/spl/splapp.properties";

	/**
	 * Remove Empty Tag XSLT path
	 */

	public static final String XSLT_REMOVE_EMPTY_TAG_PATH = "resources/removeemptytags.xslt";

	/**
	 * Contants for String Tokenizer Seperator
	 */

	public static final String SEPERATOR_PROCESS_REQUEST_RESULT = "|";


	/**
	 * Constants for Date Format
	 */

	public static final String DATE_FORMAT_ARBOR = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String DATE_FORMAT_BBRR = "yyyyMMddHHmmss";

	public static final String DATE_FORMAT_YMD = "yyyy-MM-dd'T'HH:mm:ss";
	
	/**
	 * Constants for Queue details
	 */
	
	public static final Short MEMBER_TYPE_RC_TERM_INST = 10;
	
	public static final String C1_FLAG_YES = "Y";
	public static final String OPCO_FLAG = "Y";
	
	public static final String HTTP_METHOD_GET = "GET";
	public static final String HTTP_METHOD_POST = "POST";
	
	public static final String DBADAPTOR_STATUS_SUCCESS = "3";
	
	public static final String PARAM_TYPE_RESOURCE_PROPERTY = "ResourceProperty";
}
