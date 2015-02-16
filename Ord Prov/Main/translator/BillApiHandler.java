package com.m1.bcc.spl.translator;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.SPLCommonComponent;
import common.util.TALogger;
//import com.m1.bcc.spl.api.SubscriberUpdate;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 22/07/2013					Sudharsan				Created the class for triggering API call based on commands
 * 18/11/2013					Ravikumar G				Bug#20568: Added encrypt logic to billing password
 ******************************************************************************/

public class BillApiHandler {
	
	
	TALogger taLogger = TALogger.getTALogger();
	String loggercategory = ApplicationConstants.LOGGER_CMD_PARAMETER;
	
	public Object callBillAPI(List<Map<String, Object>> paramList,String rowId, String transId, String srcTransId, String cmdRefId,JdbcDatabaseDAO jdbcDatabaseDAO) throws Exception {
		
		Object outputValue = null;
			
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[BillApiHandler][callBillAPI] ParamList : "+paramList ,loggercategory);
		
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[BillApiHandler][callBillAPI] cmdRefId is: "+cmdRefId ,loggercategory);
		Properties appProperties = SPLCommonComponent.loadProperty(ApplicationConstants.SPLAPP_FILENAME);
		CommandTransDetails cmdSystemDetails = jdbcDatabaseDAO.getSystemDetails(cmdRefId);
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[BillApiHandler][callBillAPI] cmdSystemDetails : "+cmdSystemDetails.toString() ,loggercategory);
		String userName = cmdSystemDetails.getUserName();
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[BillApiHandler][callBillAPI] userName : "+userName ,loggercategory);
		String password = cmdSystemDetails.getPwd();
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[BillApiHandler][callBillAPI] password : "+password ,loggercategory);
		String key = (String) appProperties.get("KEY");
		String decryptedPwd = "";
		if(password!=null && !password.trim().equals("")) {
			decryptedPwd = SPLCommonComponent.Crypt(0, key, password);
		}
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[BillApiHandler][callBillAPI] decryptedPwd : "+ decryptedPwd ,loggercategory);
		String realm = cmdSystemDetails.getLocation();
		taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[BillApiHandler][callBillAPI] realm : "+realm ,loggercategory);
		List<Map<String, Object>>  apiParams=jdbcDatabaseDAO.getAPIParams(cmdRefId ,transId, srcTransId);
		Map<String, Object> map = (Map<String, Object>)apiParams.get(0);
		String apiObject=(String) map.get("API_OBJECT_NAME");
		String functionVariable=(String) map.get("API_FUNCTION");
		Class[] parameters = null;

		// load the Class at runtime
		Class apiFunctions = Class.forName(apiObject);

		Object apiFunctionsObj = apiFunctions.newInstance();
		parameters = new Class[9];
		parameters[0] = List.class;
		parameters[1] = String.class;
		parameters[2] = String.class;
		parameters[3] = String.class;
		parameters[4] = String.class;
		parameters[5] = String.class;
		parameters[6] = String.class;
		parameters[7] = String.class;
		parameters[8] = JdbcDatabaseDAO.class;
		

		Method method = apiFunctions.getDeclaredMethod(functionVariable, parameters);

		outputValue = (Object) method.invoke(apiFunctionsObj, paramList,rowId, transId, srcTransId, cmdRefId, userName, decryptedPwd, realm, jdbcDatabaseDAO);
		
		return outputValue;
	}
}
