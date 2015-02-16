package com.m1.bcc.spl.instructor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;
import com.m1.bcc.spl.util.XPathReader;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 22/02/2013					Ravikumar G				Modified for logging cleanup
 * 09/03/2013					Ravikumar G				Added function getTerminatedSerialNumber
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 17/05/2013					Billy Lim				Bug 2146 [Internal] Change logic in SPL Function gpa_get_ngn_info. Split paramvalue without @
 * 20/05/2013					Ravikumar G				Bug#2152: Crypt function logic changed to get the encrypkey and decrypt the key and pass to the Crypt function.
 * 06/06/2013					Ravikumar G				Bug#2276 : Passing iPosition from config table
 * 1/10/2013                    Kalyan                  Added function getExtendedDataList 
 * 18/10/2013                    Kalyan                  Changed getCorrpointlist 
 * 19/10/2013                    Kalyan                 Added handling for MultiSIM
 * 08/01/2014					Ravikumar G				Bug#22467 : Added method getONTID to change format for ONTID resource
 * 15/1/2014                    Kalyan                 Added method getModemPassword to truncate CustomerID to one character			
 ******************************************************************************/

public class SplFunctions {

	private DataSource dataSource;
	private String cmdTransRowId;

	// For logging purposes
	TALogger taLogger = TALogger.getTALogger();
	String loggercategory = ApplicationConstants.LOGGER_CMD_PARAMETER;
	SPLCommonComponent splCommonComponent;



	public void setBasicDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}



	//getLast4MSISDN function gets the last four digits of the param value
	public List<HandlerVariables> getLast4MSISDN(List<HandlerVariables> paramList)

	{

		List<HandlerVariables> outputValue = new ArrayList<HandlerVariables>();
		String paramValue;

		for(Object list:paramList){

			HandlerVariables var = (HandlerVariables)list;
			paramValue = var.getParamValue();
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getLast4MSISDN] param value is: " +paramValue, loggercategory);

			paramValue = paramValue.substring(paramValue.length()-4, paramValue.length());

			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getLast4MSISDN] final param value is: " +paramValue, loggercategory);

			var.setParamValue(paramValue);
			outputValue.add(var);
		}

		return outputValue;
	}

	/**
	 *
	 * This function is to convert IP address to a Hexadecimal format
	 *
	 * Expects only one param sub value e.g. "10.144.95.198"
	 *
	 * @param paramList
	 * @return List<HandlerVariables>
	 */

	public List<HandlerVariables> convertIpToHex(List<HandlerVariables> paramList){

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		String paramValue;
		HandlerVariables var;

		try{

			for(Object list:paramList){


				//assigning from paramlist
				var = (HandlerVariables) list;

				paramValue = var.getParamValue();
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[convertIpToHex] paramName and param value are: " +var.getParamName() + paramValue  , loggercategory);
				//example: String ip="10.144.95.198";
				String[] values=paramValue.split("[.]+");

				String hex1=Integer.toHexString(Integer.parseInt(values[0]));

				String hex2=Integer.toHexString(Integer.parseInt(values[1]));

				String hex3=Integer.toHexString(Integer.parseInt(values[2]));

				String hex4=Integer.toHexString(Integer.parseInt(values[3]));

				paramValue = hex1+hex2+hex3+hex4;

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[convertIpToHex] final param value is: " +paramValue, loggercategory);

				var.setParamValue(paramValue);

				outputValue.add(var);
			}
		}catch(Exception e){
			taLogger.log("","","", ApplicationConstants.LOG_ERROR, "[convertIpToHex]Exception :"+e, loggercategory);

		}


		return outputValue;
	}


	public List<HandlerVariables> convertIpToDec(List<HandlerVariables> paramList) {

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		String paramValue;
		HandlerVariables var = new HandlerVariables();
		String sComputeIPHex = "";
		String sTemp = "";
		String sChkHexValue = "7FFFFFFF";
		String sComputeHexValue = "100000000";

		long iComputeIP = 0, iChkValue=0, iComputeValue=0, iComputeResult=0;

		try{

			for(Object list:paramList) {

				//assigning from paramlist
				var = (HandlerVariables) list;

				paramValue = var.getParamValue();
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][convertIpToDec] paramName and param value are: " +var.getParamName() + paramValue  , loggercategory);
				//example: String ip="10.144.95.198";
				String[] values=paramValue.split("[.]+");

				for(int count=0; count<values.length; count++)
					sComputeIPHex = sComputeIPHex + String.format("%02X",Integer.parseInt(values[count]));

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][convertIpToDec] After split IP and concat converted hex value sComputeIPHex: " + sComputeIPHex, loggercategory);

				iComputeIP = Long.parseLong(sComputeIPHex, 16);
				iChkValue = Long.parseLong(sChkHexValue, 16);
				iComputeValue = Long.parseLong(sComputeHexValue, 16);

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][convertIpToDec] After convert into Decimal iComputeIP: " + iComputeIP  , loggercategory);
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][convertIpToDec] After convert into Decimal iChkValue: " + iChkValue  , loggercategory);
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][convertIpToDec] After convert into Decimal sComputeIPHex: " + iComputeValue  , loggercategory);

				if (iComputeIP > iChkValue) {
					taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][convertIpToDec] Inside validation iComputeIP > iChkValue" , loggercategory);
					//perform conversion
					//100000000 - value
					iComputeResult = iComputeValue - iComputeIP;

					taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][convertIpToDec] iComputeResult = iComputeValue - iComputeIP: " + iComputeResult  , loggercategory);
					//convert result into hex
					sTemp = String.format("%02X", iComputeResult);
					taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][convertIpToDec] format into hex sTemp = String.format(%02X, iComputeResult): " + sTemp  , loggercategory);
					//trim away FFFFFFFF
					sTemp = sTemp.replace("FFFFFFFF", "");
					taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][convertIpToDec] sTemp Replace FFFFFFFF: " + sTemp  , loggercategory);
					paramValue = String.valueOf((Long.parseLong(sTemp, 16) * -1));
				}else {
					paramValue = String.valueOf(iComputeIP);
				}

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][convertIpToDec] final param value is: " +paramValue, loggercategory);

				var.setParamValue(paramValue);

				outputValue.add(var);
			}
		}catch(Exception e){
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_ERROR, "[SplFunctions][convertIpToDec] Exception: " + e.getMessage(), loggercategory, e);
		}
		return outputValue;
	}

	/**
	 * Changed Crypt method logic as part of Bug#2152
	 * @param paramList
	 * @return
	 * @throws Exception
	 */
	public List<HandlerVariables> Crypt(List<HandlerVariables> paramList) throws Exception {

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		String paramValue;
		String defaultValue;
		String paramSub;

		String encryptedKey = "";
		String decryptedKey = "";
		String cryptKey = "";
		HandlerVariables var = null;
		String xpathValue = "";
		String encryptedPwd = "";

		for(Object list:paramList){

			var = (HandlerVariables)list;
			paramValue = var.getParamValue();
			defaultValue = var.getDefaultValue();
			paramSub = var.getParamSub();

			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[Crypt] paramValue: " +paramValue, loggercategory);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[Crypt] defaultValue: " +defaultValue, loggercategory);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[Crypt] paramSub: " +paramSub, loggercategory);

			if(paramSub.equalsIgnoreCase("Service_Identifier")){

				xpathValue = paramValue;
			}

			if(paramSub.equalsIgnoreCase("EncryptedKey")){
				encryptedKey = paramValue;
			}

			if(paramSub.equalsIgnoreCase("CRYPT_KEY")){
				cryptKey = paramValue;
			}
		}

		taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[Crypt] xpathValue: " +xpathValue, loggercategory);
		taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[Crypt] encryptedKey: " +encryptedKey, loggercategory);
		taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[Crypt] cryptKey: " +cryptKey, loggercategory);

		if(encryptedKey!=null && cryptKey!=null) {
			decryptedKey = SPLCommonComponent.Crypt(0, cryptKey, encryptedKey);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[Crypt] decryptedKey: " +decryptedKey, loggercategory);
		}

		if(decryptedKey!=null && xpathValue!=null) {
			encryptedPwd = SPLCommonComponent.Crypt(1, decryptedKey, xpathValue);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[Crypt] encryptedPwd: " +encryptedPwd, loggercategory);
			var.setParamValue(encryptedPwd);
			outputValue.add(var);
		}

		return outputValue;
	}



	public List<HandlerVariables> getNextBillCycleDate(List<HandlerVariables> paramList) throws Exception
	{

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		HandlerVariables var = new HandlerVariables();
		String paramValue;

		for(Object list:paramList){


			//assigning from paramlist
			var = (HandlerVariables) list;

			paramValue = var.getParamValue();

			if(!paramValue.equals("yyymmdd")){
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNextBillCycleDate]param value is: " +paramValue, loggercategory);

				DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
				Calendar cal = Calendar.getInstance();

				cal.setTime(cal.getTime());
				cal.add(Calendar.MONTH, -1);

				String finalDate = dateFormat.format(cal.getTime());
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNextBillCycleDate] final date is: " +finalDate, loggercategory);

				String sysDate = SPLCommonComponent.formatSysDateToText();

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNextBillCycleDate]sysDate is: " +sysDate, loggercategory);

				String YearMon = finalDate.substring(0 ,6);

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNextBillCycleDate] yearMon is: " +YearMon, loggercategory);
				String newSysDate = finalDate.substring(6 ,8);

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNextBillCycleDate] newSysDate is: " +newSysDate, loggercategory);


				String newParamValue = paramValue.substring(1, 3);
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNextBillCycleDate] newParamValue is: " +newParamValue, loggercategory);
				paramValue = YearMon  + newParamValue;

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getNextBillCycleDate] final paramValue is: " +paramValue, loggercategory);

				var.setParamValue(paramValue);
				outputValue.add(var);
			}
		}

		if(outputValue.size()==0) {
			var.setParamValue("remove");
			outputValue.add(var);
		}
		return outputValue;

	}



	public List<HandlerVariables> getCospId(List<HandlerVariables> paramList) throws SPLExceptionHandler {

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		HandlerVariables var = new HandlerVariables();
		String paramValue;
		String RCPID = null;
		String DRMID = null;
		String lookupValue = null;
		String concatValue = null;
		String lookupRefId="";
		for(Object list:paramList){

			var = (HandlerVariables) list;


			paramValue = var.getParamValue();
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getCospId] paramValue is: " +paramValue, loggercategory);

			String paramSub = var.getParamSub();
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getCospId] paramSub is: " +paramSub, loggercategory);

			String defaultValue = var.getDefaultValue();
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getCospId] defaultValue is: " +defaultValue, loggercategory);

			if(paramSub.equalsIgnoreCase("LOOKUP_REF_ID") && !(defaultValue==null)) {
				lookupValue = defaultValue;
			}
			else if(paramSub.equalsIgnoreCase("RCPID")) {
				RCPID = paramValue;
			}
			else if(paramSub.equalsIgnoreCase("DRMID")) {
				DRMID = paramValue;
			}

			if(!(defaultValue ==null)){
				lookupRefId = lookupValue;
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getCospId] defaultValue is: " +lookupRefId, loggercategory);
			}

			if (lookupValue == null || RCPID==null || DRMID==null) {
				continue;
			}
			else {

				concatValue = RCPID + "_" + DRMID;
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getCospId] defaultValue in else is: " +lookupValue, loggercategory);
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getCospId] concatValue is: " +concatValue, loggercategory);
			}

			JdbcDatabaseDAO databaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("jdbcDatabaseDAO");

			// Passing srcTransId, transId and rowId for logging
			paramValue = databaseDAO.getNewParamValue(lookupRefId, concatValue, var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(),loggercategory);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getCospId] final paramValue to be added in the outputValue list is: " +paramValue, loggercategory);

			var.setParamValue(paramValue);
			outputValue.add(var);

		}
		if(outputValue.size()==0) {
			var.setParamValue("remove");
			outputValue.add(var);
		}
		return outputValue;
	}



	public List<HandlerVariables> getArrayOfBundleSubscriptionInput(List<HandlerVariables> paramList){

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		String paramValue;
		String paramSub;

		try{

			String value = "";
			HandlerVariables var = new HandlerVariables();

			for(Object list:paramList){

				var = (HandlerVariables) list;
				paramValue = var.getParamValue();
				paramSub = var.getParamSub();
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getArrayOfBundleSubscriptionInput] paramSub is: " +paramSub, loggercategory);

				if(paramValue!=null && !paramValue.equals("") && !paramValue.equals("-")){

					value = value + paramSub + "=" + paramValue + "|";

					taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getArrayOfBundleSubscriptionInput] Final value is: " +value, loggercategory);
				}

				else{


				}


			}

			var.setParamValue(value);
			outputValue.add(var);

		}catch(Exception e){
			taLogger.log("","","", ApplicationConstants.LOG_ERROR, "[getArrayOfBundleSubscriptionInput] Exception :"+e.getMessage(), loggercategory,e);	

		}
		return outputValue;

	}



	public static List<HandlerVariables> generateNextHop(List<HandlerVariables> paramList) {
		TALogger taLogger = TALogger.getTALogger();
		String sIPAddress = null;
		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		HandlerVariables var = new HandlerVariables();

		String paramValue;
		String paramSub;
		String sStartIP = "";
		String sEndIP = "";
		int iPosition = -1;

		for(Object list:paramList){

			var = (HandlerVariables) list;
			paramValue = var.getParamValue();
			paramSub = var.getParamSub();
			//System.out.println("paramValue is:\n" + paramValue);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] paramValue is: " + paramValue, ApplicationConstants.LOGGER_CMD_PARAMETER);
			//System.out.println("paramSub is:\n" + paramSub);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] paramSub is: " + paramSub, ApplicationConstants.LOGGER_CMD_PARAMETER);

			if(paramSub.equalsIgnoreCase("WAN_IP_FROM")){

				sStartIP = paramValue;
				//System.out.println("sStartIP is:\n" + sStartIP);
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] sStartIP : " + sStartIP, ApplicationConstants.LOGGER_CMD_PARAMETER);
			}

			else if(paramSub.equalsIgnoreCase("WAN_IP_TO")){

				sEndIP = paramValue;
				//System.out.println("sEndIP is:\n" + sEndIP);
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] sEndIP:" + sEndIP, ApplicationConstants.LOGGER_CMD_PARAMETER);
			}

			//Ravi : 20130606 : Bug 2276 : Passing iPosition from config table : Start
			else if(paramSub.equalsIgnoreCase("Position")){
				if(paramValue!=null && !paramValue.equals(""))
					iPosition = Integer.parseInt(paramValue);
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] iPosition:" + iPosition, ApplicationConstants.LOGGER_CMD_PARAMETER);
			}
			//Ravi : 20130606 : Bug 2276 : Passing iPosition from config table : End

			if(sStartIP.equals("") || sEndIP.equals("") || iPosition==-1){

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] Ip is empty", ApplicationConstants.LOGGER_CMD_PARAMETER);
				continue;
			}

			int iLoop = 2, i = 0;
			int iStart = 0, iEnd = 0;
			String[] sTemp;
			//int iPosition = 1;

			try{
				while (i < iLoop)
				{
					if (i == 0)
					{
						taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] starting IP :\n" + sStartIP, ApplicationConstants.LOGGER_CMD_PARAMETER);
						sTemp = sStartIP.split("\\.");
						taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] split size :" + sTemp.length, ApplicationConstants.LOGGER_CMD_PARAMETER);
						// increment 1 to ignore the 1st IP
						iStart = Integer.parseInt(sTemp[3]) + 1;
						taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] Inside if iStart  " + iStart, ApplicationConstants.LOGGER_CMD_PARAMETER);
						sIPAddress = sTemp[0] + "." + sTemp[1] + "." + sTemp[2] + ".";
						taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] Inside If sIPAddress  " + sIPAddress, ApplicationConstants.LOGGER_CMD_PARAMETER);
					}
					else
					{
						sTemp = sEndIP.split("\\.");
						taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] Inside else sTemp  " + sTemp, ApplicationConstants.LOGGER_CMD_PARAMETER);
						// decrement 1 to ignore the last IP
						iEnd = Integer.parseInt(sTemp[3]) - 1;
						taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] Inside else iEnd  " + iEnd, ApplicationConstants.LOGGER_CMD_PARAMETER);
					}
					sTemp = null;
					i++;
				}
				// load IP range into array
				sTemp = new String[(iEnd - iStart) + 1];
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] sTemp after iEnd - iStart inside If " + sTemp, ApplicationConstants.LOGGER_CMD_PARAMETER);
				i = iStart;
				for (int j = 0; j < sTemp.length; j++) {
					sTemp[j] = String.valueOf(i);
					taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] sTemp inside for " + sTemp[j], ApplicationConstants.LOGGER_CMD_PARAMETER);

					i++;
				}
				sIPAddress = sIPAddress + sTemp[iPosition];
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[SplFunctions][generateNextHop] sIPAddress : " + sIPAddress, ApplicationConstants.LOGGER_CMD_PARAMETER);
			}catch(Exception e){
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_ERROR, "[SplFunctions][generateNextHop][catch block] IPAddress is not in the accepted format : " + sStartIP + " , " + sEndIP, ApplicationConstants.LOGGER_CMD_PARAMETER);
				sIPAddress = "-";
			}
			var.setParamValue(sIPAddress);
			outputValue.add(var);
		}
		if(outputValue.size()==0) {
			var.setParamValue("remove");
			outputValue.add(var);
		}
		return outputValue;
	}



	public List<HandlerVariables> getDateConversion(List<HandlerVariables> paramList) {

		List<HandlerVariables> outputValue = new ArrayList<HandlerVariables>();
		HandlerVariables var = new HandlerVariables();
		String paramValue;

		for (Object list : paramList) {

			var = (HandlerVariables) list;
			paramValue = var.getParamValue();
			String paramSub = var.getParamSub();
			String cmdRefId = var.getCommndRefId();
			String paramName = var.getParamName();
			String xmlEleCurrValue = var.getXmlEleCurrValue();
			String xmlElePrevValue = var.getXmlElePrevValue();



			if (paramSub.equalsIgnoreCase("DateValue")) {

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(), var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getDateConversion] paramValue is: " + paramValue, loggercategory);
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(), var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getDateConversion] cmdRefId is: " + cmdRefId, loggercategory);

				JdbcDatabaseDAO databaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("jdbcDatabaseDAO");
				String dateFormat = databaseDAO.getDefaultValue(cmdRefId, paramName);

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(), var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getDateConversion] dateFormat is: " + dateFormat, loggercategory);

				// if input parameter is '-' then return param value as '-'
				//if (!paramValue.equals("-"))
				paramValue = SPLCommonComponent.formatDate(paramValue, dateFormat);

				taLogger.log(var.getSrcTransId(), var.getTrans_Id(), var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getDateConversion] final param value is: " + paramValue, loggercategory);

				var.setParamValue(paramValue);

				if (xmlEleCurrValue != null && !xmlEleCurrValue.equals("") && !xmlEleCurrValue.equals("-")) {

					xmlEleCurrValue = SPLCommonComponent.formatDate(xmlEleCurrValue, dateFormat);
					taLogger.log(var.getSrcTransId(), var.getTrans_Id(), var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getDateConversion] xmlEleCurrValue: " + xmlEleCurrValue, loggercategory);
					var.setXmlEleCurrValue(xmlEleCurrValue);
				}
				if (xmlElePrevValue != null && !xmlElePrevValue.equals("") && !xmlElePrevValue.equals("-")) {
					xmlElePrevValue = SPLCommonComponent.formatDate(xmlElePrevValue, dateFormat);
					taLogger.log(var.getSrcTransId(), var.getTrans_Id(), var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getDateConversion] xmlEleCurrValue: " + xmlElePrevValue, loggercategory);
					var.setXmlElePrevValue(xmlElePrevValue);
				}

				outputValue.add(var);
			}else {
				if(!"DateFormat".equalsIgnoreCase(paramSub)) {
					outputValue.add(var);
				}
			}

		}
		/**
		 * To remove the entry from the main list if spl special function return empty list for paramname using more than one param sub.
		 * This would be done in case of the negative scenarios where one param sub would be found and one might not be found.
		 */
		if (outputValue.size() == 0) {
			var.setParamValue("remove");
			outputValue.add(var);
		}
		return outputValue;

	}




	public List<HandlerVariables> getMainOrderLineItemId(List<HandlerVariables> paramList) {

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		HandlerVariables var = new HandlerVariables();
		String paramValue;
		String paramSub;
		String parentLineItemId = "";
		String lineItemId = "";

		for(Object list:paramList){


			var = (HandlerVariables) list;
			paramValue = var.getParamValue();
			paramSub = var.getParamSub();

			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getMainOrderLineItemId] paramValue is: " +paramValue, loggercategory);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getMainOrderLineItemId] paramSub is: " +paramSub, loggercategory);

			if(paramSub.equalsIgnoreCase("ParentLineItemID")){

				parentLineItemId = paramValue;
			}

			else if(paramSub.equalsIgnoreCase("LineItemId")){

				lineItemId = paramValue;
			}

			if(parentLineItemId.equals("") || lineItemId.equals("")){
				continue;
			}

			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getMainOrderLineItemId] parentLineItemId is: " +parentLineItemId, loggercategory);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getMainOrderLineItemId] lineItemId is: " +lineItemId, loggercategory);

			/*if(paramSub.equalsIgnoreCase("ParentLineItemID") && parentLineItemId.equals("0")) {*/
			if(parentLineItemId.equals("0")) {
				parentLineItemId = lineItemId;

			}

			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getMainOrderLineItemId] final paramValue is: " +parentLineItemId, loggercategory);

			var.setParamValue(parentLineItemId);
			outputValue.add(var);
		}
		if(outputValue.size()==0) {
			var.setParamValue("remove");
			outputValue.add(var);
		}
		return outputValue;
	}



	public List<HandlerVariables> getReverseOrder(List<HandlerVariables> paramList)
	{

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		HandlerVariables var = new HandlerVariables();
		String paramValue;

		for(Object list:paramList){

			var = (HandlerVariables) list;
			paramValue = var.getParamValue();

			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getReverseOrder] paramValue is: " +paramValue, loggercategory);
			String reversed="";
			StringBuilder sb = new StringBuilder();

			for (int i = paramValue.length() - 1; i >= 0; --i)
			{

				reversed=reversed+paramValue.charAt(i)+".";
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getReverseOrder] reversed is: " +reversed, loggercategory);
			}

			// Ravi: removed hardcoded value '5.6.npdb.arpa' as its configured in cmd_ref for concat with reversed string
			//paramValue = reversed+"5.6.npdb.arpa";
			paramValue = reversed;
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getReverseOrder] final paramValue is: " +paramValue, loggercategory);

			var.setParamValue(paramValue);
			outputValue.add(var);
		}

		return outputValue;
	}



	public List<HandlerVariables> gpa_get_ngn_info(List<HandlerVariables> paramList)
	{

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		HandlerVariables var = new HandlerVariables();
		String paramValue;
		String paramSub;
		String paramsubval="";

		for(Object list:paramList){

			var = (HandlerVariables) list;
			paramValue = var.getParamValue();
			paramSub = var.getParamSub();

			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[gpa_get_ngn_info] paramValue is: " +paramValue, loggercategory);
			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[gpa_get_ngn_info] paramSub is: " +paramSub, loggercategory);
			//public String method1
			// Start Bug 2146 Billy Lim refer to modification history 
			//String[] values =paramValue.split("@");
			//String left =values[0];
			//String BASNameValue=values[0];
			//String UserNameValue=values[0];
			//String AuthDomainValue=values[1];
			//String[] leftSubString=left.split("_");
			String BASNameValue=paramValue;
			String UserNameValue=paramValue;
			String[] leftSubString=paramValue.split("_");

			String PEVIDValue=leftSubString[0];
			String CEVIDValue=leftSubString[1];
			// End Bug 2146 Billy Lim refer to modification history 


			Map<String, Object> paramSubValues = new HashMap<String, Object>();
			paramSubValues.put("BASNameValue",BASNameValue);
			paramSubValues.put("UserNameValue",UserNameValue);
			// Start Bug 2146 Billy Lim refer to modification history 
			//paramSubValues.put("AuthDomainValue",AuthDomainValue);
			// End Bug 2146 Billy Lim refer to modification history 
			paramSubValues.put("PEVIDValue",PEVIDValue);
			paramSubValues.put("CEVIDValue",CEVIDValue);

			paramsubval=(String) paramSubValues.get(paramSub);


			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[gpa_get_ngn_info] final paramValue is: " +paramsubval, loggercategory);

			var.setParamValue(paramsubval);
			outputValue.add(var);
		}

		return outputValue;

	}

	/**
	 * Added as part of Bug#1126
	 * @param paramList
	 * @return
	 */
	public List<HandlerVariables> getTCPPROFILECbqosValue(List<HandlerVariables> paramList) {

		List<HandlerVariables> outputValue = new ArrayList<HandlerVariables>();
		HandlerVariables handlerVariables = new HandlerVariables();
		String paramValue;
		String paramSub;

		for(Object list:paramList) {
			handlerVariables = (HandlerVariables) list;
			paramValue = handlerVariables.getParamValue();
			paramSub = handlerVariables.getParamSub();

			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(),handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[gpaTCPPROFILECbqosValue] paramValue is: " +paramValue, loggercategory);
			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(),handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[gpaTCPPROFILECbqosValue] paramSub is: " +paramSub, loggercategory);

			String[] values = paramValue.split("@");
			String rightValue = values[1];
			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(),handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[gpaTCPPROFILECbqosValue] rightValue is: " +rightValue, loggercategory);
			rightValue = rightValue.replace(".", "_hsi_");
			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(),handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[gpaTCPPROFILECbqosValue] rightValue with _hsi_: " +rightValue, loggercategory);
			Properties properties = (Properties) BeanFactory.getBean("properties");
			String tcpProfileValue = properties.getProperty("TCPPROFILECbqosValue");
			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(),handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[gpaTCPPROFILECbqosValue] tcpProfileValue: " +tcpProfileValue, loggercategory);
			tcpProfileValue = tcpProfileValue + rightValue;
			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(),handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[gpaTCPPROFILECbqosValue] tcpProfileValue: " +tcpProfileValue, loggercategory);

			handlerVariables.setParamValue(tcpProfileValue);
			outputValue.add(handlerVariables);
		}
		return outputValue;
	}

	/**
	 * Added as part of Bug#1433
	 * @param paramList
	 * @return
	 */
	public List<HandlerVariables> getTerminatedSerialNumber(List<HandlerVariables> paramList) {
		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		HandlerVariables handlerVariables = new HandlerVariables();
		String paramValue;
		String paramSub;
		String currentValue = "";
		String previousValue = "";

		for(Object list:paramList) {
			handlerVariables = (HandlerVariables) list;
			paramValue = handlerVariables.getParamValue();
			paramSub = handlerVariables.getParamSub();

			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(), handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getTerminatedSerialNumber] paramValue is: " +paramValue, loggercategory);
			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(), handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getTerminatedSerialNumber] paramSub is: " +paramSub, loggercategory);

			if(paramSub.equalsIgnoreCase("PreviousValue")){

				previousValue = paramValue;
			}

			else if(paramSub.equalsIgnoreCase("CurrentValue")){

				currentValue = paramValue;
			}

			if(previousValue.equals("") || currentValue.equals("")){
				continue;
			}

			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(), handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getTerminatedSerialNumber] CurrentValue is: " +currentValue, loggercategory);
			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(), handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getTerminatedSerialNumber] PreviousValue is: " +previousValue, loggercategory);

			if(previousValue.equals("-")) {
				previousValue = currentValue;
			}

			taLogger.log(handlerVariables.getSrcTransId(), handlerVariables.getTrans_Id(), handlerVariables.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getTerminatedSerialNumber] final paramValue is: " +previousValue, loggercategory);
			handlerVariables.setParamValue(previousValue);
			outputValue.add(handlerVariables);
		}
		/**
		 * To remove the entry from the main list if spl special function return empty list for paramname using more than one param sub.
		 * This would be done in case of the negative scenarios where one param sub would be found and one might not be found.
		 */
		if(outputValue.size()==0) {
			handlerVariables.setParamValue("remove");
			outputValue.add(handlerVariables);
		}
		return outputValue;
	}

	//M.Rahman: New method to extract the list of External IDs//

	public List<HandlerVariables> getExternalIdList(List<HandlerVariables> paramList){

		HandlerVariables var = new HandlerVariables();
		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		String paramName;
		String paramValue;
		String parRowId;
		String paramSub = null;
		String paramSubValue = null;
		String Externalid = null;
		String orderXML;
		String xpathvariable = null;

		BigDecimal seqNo;				
		HandlerVariables varResult = new HandlerVariables();

		try{

			for(Object list:paramList)
			{				

				var = (HandlerVariables) list;
				paramName = var.getParamName();
				paramValue = var.getParamValue();
				paramSub = var.getParamSub();
				parRowId = var.getRow_ID();
				orderXML = var.getOrderXml();
				xpathvariable =var.getxPathVariable();		
				seqNo = var.getSeqNo();

				if (!orderXML.isEmpty()){
					InputStream is = new ByteArrayInputStream(orderXML.getBytes());
					DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
					domFactory.setNamespaceAware(false);
					DocumentBuilder builder = domFactory.newDocumentBuilder();
					Document doc = builder.parse(is);
					(doc).getDocumentElement().normalize();
					ArrayList <String> ExternalList = new ArrayList<String>();
					//Kalyan for Retrieving External Id and ExternalIdtype

					//NodeList nListExtID = doc.getElementsByTagName("ExternalID");
					NodeList nListExtID = XPathReader.executeXPath(xpathvariable,doc); 				
					if(nListExtID.getLength()!=0){					
						//HandlerVariables varResult = new HandlerVariables();
						for (int temp = 0; temp < nListExtID.getLength(); temp++) {			
							Node nNodeExternalId = nListExtID.item(temp);
							/*if (nNodeExternalId.getNodeType() == Node.ELEMENT_NODE)
						{*/
							//Element eElement = (Element) nNodeExternalId;									
							varResult.setParamName(paramName);						
							varResult.setRow_ID(parRowId);
							varResult.setSeqNo(seqNo);
							varResult.setParamSub(paramSub);				
							Externalid=nNodeExternalId.getTextContent();							
							ExternalList.add(Externalid);
							taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "Externalid: " +Externalid, loggercategory);									
							/*}*/	
						}
						//paramSubValue=list1.toString();
						paramSubValue = StringUtils.collectionToCommaDelimitedString(ExternalList);
						varResult.setParamValue(paramSubValue);
						taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "paramSubValue: " +paramSubValue, loggercategory);
						outputValue.add(varResult);
					} else{
						varResult.setParamName(paramName);						
						varResult.setRow_ID(parRowId);
						varResult.setSeqNo(seqNo);
						varResult.setParamSub(paramSub);
						varResult.setParamValue("-");
						outputValue.add(varResult);
						taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "varResult.getParamValue(): " +varResult.getParamValue(), loggercategory);						

					}
				}		
			}
		}				
		catch(Exception e){
			e.printStackTrace();
		}			
		return outputValue;						
	}
	//Kalyan: duplicated the method to retrieve the Extended Data list//

	public List<HandlerVariables> getExtendedDataList(List<HandlerVariables> paramList){		
		HandlerVariables var = new HandlerVariables();
		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		String paramName;
		String paramValue;
		String parRowId;
		String paramSub = null;
		String paramSubValue = null;
		String Data = null;
		String orderXML;
		String xpathvariable = null;		
		BigDecimal seqNo;				
		HandlerVariables varResult = new HandlerVariables();							
		try{

			for(Object list:paramList)
			{				

				var = (HandlerVariables) list;
				paramName = var.getParamName();
				paramValue = var.getParamValue();
				paramSub = var.getParamSub();
				parRowId = var.getRow_ID();
				orderXML = var.getOrderXml();
				xpathvariable =var.getxPathVariable();		
				seqNo = var.getSeqNo();

				if (!orderXML.isEmpty()){
					InputStream is = new ByteArrayInputStream(orderXML.getBytes());
					DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
					domFactory.setNamespaceAware(false);
					DocumentBuilder builder = domFactory.newDocumentBuilder();
					Document doc = builder.parse(is);
					(doc).getDocumentElement().normalize();
					ArrayList <String> ExtendedDataList = new ArrayList<String>();
					NodeList ExtendedDataLst = XPathReader.executeXPath(xpathvariable,doc); 				
					if(ExtendedDataLst.getLength()!=0){					
						for (int temp = 0; temp < ExtendedDataLst.getLength(); temp++) {			
							Node nExtendedData = ExtendedDataLst.item(temp);							
							varResult.setParamName(paramName);						
							varResult.setRow_ID(parRowId);
							varResult.setSeqNo(seqNo);
							varResult.setParamSub(paramSub);				
							Data=nExtendedData.getTextContent();							
							ExtendedDataList.add(Data);
							taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "ExtendedDataList: " +ExtendedDataList, loggercategory);									
						}
						paramSubValue = StringUtils.collectionToCommaDelimitedString(ExtendedDataList);
						varResult.setParamValue(paramSubValue);
						taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "paramSubValue: " +paramSubValue, loggercategory);
						outputValue.add(varResult);
					} else{
						varResult.setParamName(paramName);						
						varResult.setRow_ID(parRowId);
						varResult.setSeqNo(seqNo);
						varResult.setParamSub(paramSub);
						varResult.setParamValue("-");
						outputValue.add(varResult);
						taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "varResult.getParamValue(): " +varResult.getParamValue(), loggercategory);						

					}
				}		
			}
		}				
		catch(Exception e){
			e.printStackTrace();
		}			
		return outputValue;						
	}

	//Kalyan: Created the method to retrieve the getCorrdiorPointList//	
	public List<HandlerVariables> getCorrdiorPointList(List<HandlerVariables> paramList){

		HandlerVariables var = new HandlerVariables();
		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		String paramName;
		String paramValue;
		String parRowId;
		String paramSub = null;
		String paramSubValue = null;
		String Data = null;
		String orderXML;
		String xpathvariable = null;		
		BigDecimal seqNo;				
		HandlerVariables varResult = new HandlerVariables();	

		try{

			for(Object list:paramList)
			{				
				var = (HandlerVariables) list;
				paramName = var.getParamName();
				paramValue = var.getParamValue();
				paramSub = var.getParamSub();
				parRowId = var.getRow_ID();
				orderXML = var.getOrderXml();
				xpathvariable =var.getxPathVariable();		
				seqNo = var.getSeqNo();

				if (!orderXML.isEmpty()){
					InputStream is = new ByteArrayInputStream(orderXML.getBytes());
					DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
					domFactory.setNamespaceAware(false);
					DocumentBuilder builder = domFactory.newDocumentBuilder();
					Document doc = builder.parse(is);
					(doc).getDocumentElement().normalize();
					ArrayList <String> CorrdiorPointList = new ArrayList<String>();
					NodeList CorrdiorPointLst = XPathReader.executeXPath(xpathvariable,doc); 				
					if(CorrdiorPointLst.getLength()!=0){					
						for (int temp = 0; temp < CorrdiorPointLst.getLength(); temp++) {			
							Node nExtendedData = CorrdiorPointLst.item(temp);							
							varResult.setParamName(paramName);						
							varResult.setRow_ID(parRowId);
							varResult.setSeqNo(seqNo);
							varResult.setParamSub(paramSub);				
							Data=nExtendedData.getTextContent();							
							CorrdiorPointList.add(Data);
							taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "ExtendedDataList: " +CorrdiorPointList, loggercategory);									
						}
						paramSubValue = StringUtils.collectionToCommaDelimitedString(CorrdiorPointList);
						varResult.setParamValue(paramSubValue);
						taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "paramSubValue: " +paramSubValue, loggercategory);
						outputValue.add(varResult);
					} else{
						varResult.setParamName(paramName);						
						varResult.setRow_ID(parRowId);
						varResult.setSeqNo(seqNo);
						varResult.setParamSub(paramSub);
						varResult.setParamValue("-");
						outputValue.add(varResult);
						taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "varResult.getParamValue(): " +varResult.getParamValue(), loggercategory);						

					}
				}		
			}				
		}
		catch(Exception e){
			e.printStackTrace();
		}			
		return outputValue;						
	}


	//Kalyan to implement the provision for Multi SIM
	public List<HandlerVariables> getMultiSIMExternalIdList(List<HandlerVariables> paramList){

		HandlerVariables var = new HandlerVariables();
		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		String paramName;
		String paramValue;
		String parRowId;
		String paramSub = null;
		String paramSubValue = null;
		String Externalid = null;
		String orderXML;
		String xpathvariable = null;

		BigDecimal seqNo;				
		HandlerVariables varResult = new HandlerVariables();

		try{

			for(Object list:paramList)
			{				

				var = (HandlerVariables) list;
				paramName = var.getParamName();
				paramValue = var.getParamValue();
				paramSub = var.getParamSub();
				parRowId = var.getRow_ID();
				orderXML = var.getOrderXml();
				xpathvariable =var.getxPathVariable();
				seqNo = var.getSeqNo();

				if (!orderXML.isEmpty()){
					InputStream is = new ByteArrayInputStream(orderXML.getBytes());
					DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
					domFactory.setNamespaceAware(false);
					DocumentBuilder builder = domFactory.newDocumentBuilder();
					Document doc = builder.parse(is);
					(doc).getDocumentElement().normalize();
					ArrayList <String> ExternalList = new ArrayList<String>();
					//Kalyan for Retrieving External Id and ExternalIdtype in MultiSIM
					NodeList MultiSIMlst = XPathReader.executeXPath(ApplicationConstants.XPATH_MULTI_SIM, doc);
					taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "MultiSIMlst:" +MultiSIMlst.getLength(), loggercategory);		
				 if(MultiSIMlst.getLength()>=1){
						String xpathofMultiSIM =ApplicationConstants.XPATH_External_IDs+paramName;
						NodeList MultiSIMLst = XPathReader.executeXPath(xpathofMultiSIM, doc);
						if(MultiSIMLst.getLength()!=0){
							for (int temp = 0; temp < MultiSIMLst.getLength(); temp++) {			
								Node nNodeExternalId = MultiSIMLst.item(temp);								
								varResult.setParamName(paramName);						
								varResult.setRow_ID(parRowId);
								varResult.setSeqNo(seqNo);
								varResult.setParamSub(paramSub);				
								Externalid=nNodeExternalId.getTextContent();							
								ExternalList.add(Externalid);
								taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "Externalid: " +Externalid, loggercategory);									
							}
							paramSubValue = StringUtils.collectionToCommaDelimitedString(ExternalList);
							varResult.setParamValue(paramSubValue);
							taLogger.log(paramName, paramValue,parRowId, ApplicationConstants.LOG_DEBUG, "paramSubValue: " +paramSubValue, loggercategory);
							outputValue.add(varResult);	
						}
							
					}					
				}		
			}
		}				
		catch(Exception e){
			e.printStackTrace();
		}			
		return outputValue;						
	}
	
	/**
	 * 
	 * @param paramList
	 * @return
	 */
	public List<HandlerVariables> getONTID(List<HandlerVariables> paramList) {

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		HandlerVariables var = new HandlerVariables();
		String paramValue;

		for(Object list:paramList){

			var = (HandlerVariables) list;
			paramValue = var.getParamValue();

			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getONTID] paramValue is: " +paramValue, loggercategory);
			if(paramValue!=null) {
				int lastIndex = paramValue.lastIndexOf('_');
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getONTID] lastIndex of _: " +lastIndex, loggercategory);
				paramValue = paramValue.substring(lastIndex+1);
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getONTID] ONT ID : " +paramValue, loggercategory);
			}
			var.setParamValue(paramValue);
			outputValue.add(var);
		}

		return outputValue;
	}
	
	/**
	 * 
	 * @param paramList
	 * @return
	 */
	public List<HandlerVariables> getModemPassword(List<HandlerVariables> paramList) {

		List<HandlerVariables> outputValue= new ArrayList<HandlerVariables>();
		HandlerVariables var = new HandlerVariables();
		String paramValue;

		for(Object list:paramList){

			var = (HandlerVariables) list;
			paramValue = var.getParamValue();

			taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getModemPassword] paramValue is: " +paramValue, loggercategory);
			if(paramValue!=null) {
				paramValue = paramValue.substring(1);
				taLogger.log(var.getSrcTransId(), var.getTrans_Id(),var.getRow_ID(), ApplicationConstants.LOG_DEBUG, "[getModemPassword]paramValue: " +paramValue, loggercategory);
			}
			var.setParamValue(paramValue);
			outputValue.add(var);
		}

		return outputValue;
	}
}
