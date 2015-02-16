package com.m1.bcc.spl.translator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import common.util.TALogger;
import javax.sql.DataSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.SPLExceptionHandler;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 ******************************************************************************/


public class SplCmdParamCustomFns {

	private DataSource dataSource;
	private String cmdTransRowId;

	// For logging purposes
	String inputIdentifier = "";
	String loggercategory = "dbpollerlogging";
	SPLCommonComponent splCommonComponent;

	TALogger taLogger = TALogger.getTALogger();

	public void setBasicDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}


	// Added by velu for Bug 124

		public String getLast4MSISDN(String paramValue)

		 {
			String last4MSISDN=paramValue.substring(paramValue.length()-4, paramValue.length());
			//System.out.println("last4MSISDN value is "+last4MSISDN);
			return last4MSISDN;
		 }

		public String convertIpToHex(String paramValue){

			taLogger.log("TransId:" + inputIdentifier, ApplicationConstants.LOG_DEBUG, "Inside convertIpToHex method paramValue="+paramValue , loggercategory);

			//String ip="10.144.95.198";
			String[] values=paramValue.split("[.]+");
			taLogger.log("TransId:" + inputIdentifier, ApplicationConstants.LOG_DEBUG, "convertIpToHex method values="+values , loggercategory);

			//int x=Integer.parseInt(values[0]);
			String hex1=Integer.toHexString(Integer.parseInt(values[0]));
			//System.out.println(hex1);
			taLogger.log("TransId:" + inputIdentifier, ApplicationConstants.LOG_DEBUG, "convertIpToHex method hex1="+hex1 , loggercategory);

			//int y=Integer.parseInt(values[1]);
			String hex2=Integer.toHexString(Integer.parseInt(values[1]));
			//System.out.println(hex2);
			taLogger.log("TransId:" + inputIdentifier, ApplicationConstants.LOG_DEBUG, "convertIpToHex method hex2="+hex2 , loggercategory);

			//int z=Integer.parseInt(values[2]);
			String hex3=Integer.toHexString(Integer.parseInt(values[2]));
			//System.out.println(hex3);
			taLogger.log("TransId:" + inputIdentifier, ApplicationConstants.LOG_DEBUG, "convertIpToHex method hex3="+hex3 , loggercategory);

			//int z1=Integer.parseInt(values[3]);
			String hex4=Integer.toHexString(Integer.parseInt(values[3]));
			//System.out.println(hex4);
			taLogger.log("TransId:" + inputIdentifier, ApplicationConstants.LOG_DEBUG, "convertIpToHex method hex4="+hex4 , loggercategory);

			String out1=hex1+hex2+hex3+hex4;
			//System.out.println(out1);
			taLogger.log("TransId:" + inputIdentifier, ApplicationConstants.LOG_DEBUG, "convertIpToHex method out1="+out1 , loggercategory);
			return out1;
		}

		public static String Crypt(Integer key, String s, String s1)
		        throws Exception
		    {
			    int i = key.intValue();
				int ai[] = new int[64];
				int ai1[] = new int[64];
				int ai2[] = new int[64];
				String s2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz-_";
		        String s3 = "";
		        String s4 = "";
		        String s5 = "";
		        String s6 = "";
		        boolean flag1 = false;
		        Exception exception = null;

		        try
		        {
		            for(int l = 0; l < 64; l++)
		                ai[l] = -1;

		            int k2 = s.length();
		            int j2 = 0;
		            for(int i1 = 0; i1 < k2; i1++)
		            {
		                char c = s.charAt(i1);
		                if(!Character.isLetterOrDigit(c))
		                    switch(c)
		                    {
		                    default:
		                        continue;

		                    case 45: // '-'
		                    case 95: // '_'
		                        break;
		                    }
		                int i4 = s2.indexOf(c, 0);
		                if(ai[i4] == -1)
		                {
		                    ai[i4] = j2;
		                    s3 = s3 + s2.charAt(i4);
		                    j2++;
		                }
		            }

		            for(int j1 = 0; j1 < 64; j1++)
		                if(ai[j1] == -1)
		                {
		                    ai[j1] = j2;
		                    s3 = s3 + s2.charAt(j1);
		                    j2++;
		                }

		            for(int k1 = 0; k1 < 64; k1++)
		            {
		                ai1[k1] = ai[k1] / 8;
		                ai2[k1] = ai[k1] % 8;
		            }

		            j2 = 0;
		            int l2 = s1.length();
		            for(int l1 = 0; l1 < l2; l1++)
		            {
		                if(!Character.isLetterOrDigit(s1.charAt(l1)))
		                    switch(s1.charAt(l1))
		                    {
		                    default:
		                        continue;

		                    case 45: // '-'
		                    case 95: // '_'
		                        break;
		                    }
		                s4 = s4 + s1.charAt(l1);
		                j2++;
		            }

		            boolean flag11 = false;
		            if(l2 % 2 == 1)
		            {
		                s4 = s4 + s4.charAt(l2 - 1);
		                l2++;
		                flag11 = true;
		            }
		            for(int i2 = 0; i2 < l2; i2 += 2)
		            {
		                int j = s2.indexOf(s4.charAt(i2), 0);
		                int k = s2.indexOf(s4.charAt(i2 + 1), 0);
		                int k3 = ai2[j];
		                int l3 = ai2[k];
		                int i3 = ai1[j];
		                int j3 = ai1[k];
		                if(j == k)
		                {
		                    if(i > 0)
		                    {
		                        s5 = s5 + s3.charAt((k3 + 8 * i3 + 1) % 64);
		                        s5 = s5 + s3.charAt((k3 + 8 * i3 + 1) % 64);
		                    } else
		                    {
		                        s5 = s5 + s3.charAt((k3 + 8 * i3 + 63) % 64);
		                        s5 = s5 + s3.charAt((k3 + 8 * i3 + 63) % 64);
		                    }
		                } else
		                if(i3 == j3)
		                {
		                    if(i > 0)
		                    {
		                        s5 = s5 + s3.charAt((k3 + 1) % 8 + 8 * i3);
		                        s5 = s5 + s3.charAt((l3 + 1) % 8 + 8 * j3);
		                    } else
		                    {
		                        s5 = s5 + s3.charAt((k3 + 7) % 8 + 8 * i3);
		                        s5 = s5 + s3.charAt((l3 + 7) % 8 + 8 * j3);
		                    }
		                } else
		                if(k3 == l3)
		                {
		                    if(i > 0)
		                    {
		                        s5 = s5 + s3.charAt(k3 + 8 * ((i3 + 1) % 8));
		                        s5 = s5 + s3.charAt(l3 + 8 * ((j3 + 1) % 8));
		                    } else
		                    {
		                        s5 = s5 + s3.charAt(k3 + 8 * ((i3 + 7) % 8));
		                        s5 = s5 + s3.charAt(l3 + 8 * ((j3 + 7) % 8));
		                    }
		                } else
		                {
		                    s5 = s5 + s3.charAt(l3 + 8 * i3);
		                    s5 = s5 + s3.charAt(k3 + 8 * j3);
		                }
		            }

		            if(flag11)
		            {
		                l2--;
		                s6 = s5.substring(0, l2);
		               // System.out.println("Encrypted/decrypted value is 1"+s6);
		            } else
		            {
		                s6 = s5;
		               // System.out.println("Encrypted/decrypted value is 1"+s6);
		            }
		        }
		        catch(Exception exception1)
		        {
		            s6 = "";
		          //  System.out.println("Exception is " + exception1);
		           // System.out.println("Input Value is iOption=[" + i + "] sUserId=[" + s + "] sPassword=[" + s1 + "]");
		            flag1 = true;
		            exception = exception1;

		        }
		        finally { }
		        if(flag1)
		            throw exception;
		        else
		            return s6;
		    }

		/**
		 * Added for Bug#317
		 * @param cmdRefId
		 * @param paramName
		 * @return
		 * @throws SPLExceptionHandler
		 */
		public String getCospId(String lookUpRefId, String concatValue) throws SPLExceptionHandler {
			JdbcDatabaseDAO databaseDAO = (JdbcDatabaseDAO) BeanFactory.getBean("jdbcDatabaseDAO");
			String value = databaseDAO.getNewParamValue(lookUpRefId, concatValue, "", "", "",loggercategory);
			return value;
		}

		public String getArrayOfBundleSubscriptionInput(String paramSub, String paramValue, String outputValue){

			/*String result="";*/
			try{

				/*for (Map<String, Object> cmdTransDtls : cmdTransDtlsList) {*/

					/*String paramSub = (String) cmdTransDtls.get("PARAM_SUB");
					String xPathValue = "Envelope/Body/ProvRequest/ListOfRef/LineItemXML"+(String) cmdTransDtls.get("XPATH");*/

					/*paramValue = evaluateXPath(xpath, doc, xPathValue);
					taLogger.log("TransId:" + inputIdentifier, ApplicationConstants.LOG_DEBUG, "paramValue.. is :" +paramValue, loggercategory);*/
				if(paramValue!=null && !paramValue.equals("") && !paramValue.equals("-")){

				taLogger.log("TransId:" + inputIdentifier, ApplicationConstants.LOG_DEBUG, "paramSub..paramValue..xpath.. is :" + paramSub + paramValue+ "--->" , loggercategory);

				outputValue = outputValue + paramSub + "=" + paramValue + "|";
				}
				else{
					taLogger.log("TransId:" + inputIdentifier, ApplicationConstants.LOG_DEBUG, "inside getArrayOfBundleSubscriptionInput method ---> param value is null !!!" , loggercategory);
				}

				/*}*/
				}catch(Exception e){

			}
			return outputValue;

		}

		public static String generateNextHop(String sStartIP, String sEndIP, Integer position)
		{
	        String sIPAddress = null;
	        try {
	               int iLoop = 2, i = 0;
	               int iStart = 0, iEnd = 0;
	               String[] sTemp;
	               int iPosition = 1;
	               while (i < iLoop)
	               {
	                     if (i == 0)
	                     {
	                           // System.out.println("Inside if loop & starting IP is:\n" + sStartIP);
	                            sTemp = sStartIP.split("\\.");
	                           // System.out.println("split size is:\n" + sTemp.length);
	                            // increment 1 to ignore the 1st IP
	                            iStart = Integer.parseInt(sTemp[3]) + 1;
	                           // System.out.println("Inside if iStart  " + iStart);
	                            sIPAddress = sTemp[0] + "." + sTemp[1] + "." + sTemp[2] + ".";
	                           // System.out.println("Inside If sIPAddress  " + sIPAddress);
	                     }
	                     else
	                     {
	                            sTemp = sEndIP.split("\\.");
	                           // System.out.println("Inside else sTemp  " + sTemp);
	                            // decrement 1 to ignore the last IP
	                            iEnd = Integer.parseInt(sTemp[3]) - 1;
	                           // System.out.println("Inside elseiEnd  " + iEnd);
	                     }
	                     sTemp = null;
	                     i++;
	               }
	               // load IP range into array
	               sTemp = new String[(iEnd - iStart) + 1];
	               //System.out.println("sTemp after iEnd - iStart inside If " + sTemp);
	               i = iStart;
	               for (int j = 0; j < sTemp.length; j++) {
	                     sTemp[j] = String.valueOf(i);
	                     //System.out.println("sTemp inside for " + sTemp[j]);

	                     i++;
	               }
	               sIPAddress = sIPAddress + sTemp[iPosition];
	               //System.out.println("sIPAddress " + sIPAddress);
	        }

	        catch (Exception exception)
	        {

	               exception.printStackTrace();
	        }
	        return sIPAddress;
	 }

		public String getDateConversion(String paramValue, String interfaceName, String dateFormat) throws Exception
		{

			taLogger.log(inputIdentifier,ApplicationConstants.LOG_DEBUG, "inside getDateConversion method paramValue="+paramValue, "instrutorlogging");
			taLogger.log(inputIdentifier,ApplicationConstants.LOG_DEBUG, "inside getDateConversion method dateformat="+dateFormat, "instrutorlogging");


			try{

			//paramValue=splCommonComponent.formatSysDateToDate(paramValue, dateFormat, interfaceName);
				// Ravi: modified to generic date format method
			paramValue=SPLCommonComponent.formatDate(paramValue, dateFormat);
			taLogger.log("formatDate",ApplicationConstants.LOG_INFO, "Final Format dateValue=" + paramValue, "instrutorlogging");


			}catch(Exception e){
				//System.out.println(e.getMessage());

			}

			return paramValue;

		}

		public String getNextBillCycleDate(String paramValue, String sysDate) throws Exception
		{


			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			Calendar cal = Calendar.getInstance();
			//System.out.println(dateFormat.format(cal.getTime()));
			 //Calendar cal = Calendar.getInstance();
			 cal.setTime(cal.getTime());
			 cal.add(Calendar.MONTH, -1);
			// System.out.println(dateFormat.format(cal.getTime()));

			 String finalDate = dateFormat.format(cal.getTime());
			//System.out.println("finalDate"+finalDate);

	            //string sYear = cal.ToString("yyyy");


			 taLogger.log(inputIdentifier,ApplicationConstants.LOG_DEBUG, "final date value is"+ finalDate, "instrutorlogging");
			taLogger.log(inputIdentifier,ApplicationConstants.LOG_DEBUG, "inside getNextBillCycleDate method paramValue="+paramValue, "instrutorlogging");
			taLogger.log(inputIdentifier,ApplicationConstants.LOG_DEBUG, "inside getNextBillCycleDate method sysdate="+sysDate, "instrutorlogging");

			String YearMon = finalDate.substring(0 ,6);
			taLogger.log(inputIdentifier,ApplicationConstants.LOG_DEBUG, "inside getNextBillCycleDate method NEWsysdate="+YearMon, "instrutorlogging");
			String newSysDate = finalDate.substring(6 ,8);
			taLogger.log(inputIdentifier,ApplicationConstants.LOG_DEBUG, "inside getNextBillCycleDate method NEWsysdate="+newSysDate, "instrutorlogging");


			String newParamValue = paramValue.substring(1, 3);
			taLogger.log(inputIdentifier,ApplicationConstants.LOG_DEBUG, "inside getNextBillCycleDate method NEWParamValue="+newParamValue, "instrutorlogging");
			paramValue = YearMon  + newParamValue;

			//System.out.println("paramValue " + paramValue);


			return paramValue;

		}

		public String evaluateXPath(XPath xpath, Document doc, String expr)
				throws XPathExpressionException {

			XPathExpression exp = xpath.compile(expr);

			String xmlDetails = (String) exp.evaluate(doc, XPathConstants.STRING);
			taLogger.log("DRMID:" + inputIdentifier,ApplicationConstants.LOG_DEBUG, "xmlDetails="+xmlDetails, loggercategory);
			return xmlDetails;


		}

		public String getMainOrderLineItemId(String parentLineItemId, String elementName, String lineItemId) {
			taLogger.log("Inside getMainOrderLineItemId",ApplicationConstants.LOG_DEBUG, "parentLineItemId="+parentLineItemId, "instrutorlogging");
			taLogger.log("Inside getMainOrderLineItemId",ApplicationConstants.LOG_DEBUG, "elementName="+elementName, "instrutorlogging");
			taLogger.log("Inside getMainOrderLineItemId",ApplicationConstants.LOG_DEBUG, "lineItemId="+lineItemId, "instrutorlogging");
			if(parentLineItemId.equals("0")) {
				parentLineItemId = lineItemId;
			}
			return parentLineItemId;
		}
		public  String getReverseOrder(String string)
		{
			taLogger.log("Inside getReverseOrder",ApplicationConstants.LOG_DEBUG, "String Received string="+string, "instrutorlogging");
			  String reversed="";
			   StringBuilder sb = new StringBuilder();
			    for (int i = string.length() - 1; i >= 0; --i)
			    {

			    	reversed=reversed+string.charAt(i)+".";
			        taLogger.log("Inside getReverseOrder",ApplicationConstants.LOG_DEBUG,"i="+i+"reversed="+reversed      , "instrutorlogging");
			    }
			   // System.out.println("String after reverse "+"reversed="+reversed);
			    String str=reversed+"5.6.npdb.arpa";
			    taLogger.log("Inside getReverseOrder",ApplicationConstants.LOG_DEBUG, "Returned str="+str, "instrutorlogging");
			    return str;


		}
		public  String gpa_get_ngn_info(String Option82,String paramSub)
		{
			String paramsubval="";

			taLogger.log("Inside gpa_get_ngn_info ",ApplicationConstants.LOG_DEBUG,"Option82="+Option82, "instrutorlogging");
			taLogger.log("Inside gpa_get_ngn_info ",ApplicationConstants.LOG_DEBUG,"paramSub="+paramSub, "instrutorlogging");
			//public String method1
			String[] values =Option82.split("@");
			String left =values[0];
			String BASNameValue=values[0];
			String UserNameValue=values[0];
			String AuthDomainValue=values[1];
			String[] leftSubString=left.split("_");
			String PEVIDValue=leftSubString[0];
			String CEVIDValue=leftSubString[1];


			taLogger.log("Inside gpa_get_ngn_info ",ApplicationConstants.LOG_DEBUG," left="+left+" BASNameValue="+BASNameValue+" UserNameValue="+UserNameValue+
					" AuthDomainValue="+AuthDomainValue+" PEVIDValue="+PEVIDValue+" CEVIDValue="+CEVIDValue, "instrutorlogging");

			Map<String, Object> paramSubValues = new HashMap<String, Object>();
			paramSubValues.put("BASNameValue",BASNameValue);
			paramSubValues.put("UserNameValue",UserNameValue);
			paramSubValues.put("AuthDomainValue",AuthDomainValue);
			paramSubValues.put("PEVIDValue",PEVIDValue);
			paramSubValues.put("CEVIDValue",CEVIDValue);



			paramsubval=(String) paramSubValues.get(paramSub);
			//System.out.println("paramsubval="+paramsubval);


			taLogger.log("Inside gpa_get_ngn_info ",ApplicationConstants.LOG_DEBUG," returned paramsubval="+paramsubval, "instrutorlogging");

			return paramsubval;

		}


}