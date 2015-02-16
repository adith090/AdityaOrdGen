package com.m1.bcc.spl.instructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.sender.OrderTransactionSender;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import com.m1.bcc.spl.util.XPathReader;

import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 04/06/2013					Mayaz Rahman		Created
 * 06/09/2013					Sudharsan			After inserting all parameters in T_OM_BILL_CMD_TRANS_DTLS , we are sending Ack TOM Message to OSM (Not immediately after picking the records from SPL Request Queue)
 * 10/09/2013                   Kalyan              Changed date format for effective date,Changed comptype and action code in suspend and reconnect section
 * 17/09/2013					Ravikumar G			Implemented retry logic for billing commands
 * 17/09/2013					Ravikumar G			Defect#188 - add logic to fire c1 commands Sequence
 * 28/10/2013					Kalyan				Added some conditions for sim component type 'CCTSM'
 * 12/11/2013					Ravikumar G			Bug#20421 - to implement multithreading using thread poll task executor
 * 18/11/2013					Ravikumar G			Bug#20568: changed method for billing to post message in different JMS type
 * 31/12/2013					Ravikumar G			Bug#22498 - Updated order_trans_dtls row id type from string to integer
 ******************************************************************************/

public class HandlerBill {
	JdbcDatabaseDAO jdbcDatabaseDAO;
	TALogger taLogger = TALogger.getTALogger();
	private ThreadPoolTaskExecutor taskExecutor;

	// For logging purposes
	//Bug 1908 Refer to Modification History
	//String inputIdentifier = "";

	String loggercategory = ApplicationConstants.LOGGER_CMD_PARAMETER;
	//Bug 1908 Refer to Modification History
	//String inputIdentfier = "";


	public void setJdbcDatabaseDAO(JdbcDatabaseDAO jdbcDatabaseDAO) {
		this.jdbcDatabaseDAO = jdbcDatabaseDAO;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	
	@Async
	@ServiceActivator
	// receives message from T_OM_ORDER_TRANS_DTLS table
	public ArrayList<?> handleMessage(Message<ArrayList<Map<String, Object>>> message) throws TransformerConfigurationException, ParserConfigurationException, TransformerException {
		ArrayList<Map<String, Object>> orderList = (ArrayList<Map<String, Object>>) message.getPayload();
		System.out.println("orderList " + orderList.size());
		for(final Map<String, Object> orderMap : orderList) {
			taskExecutor.submit(new Runnable() {
				
				@Override
				public void run() {
					processBillingOrder(orderMap);
				}
			});
		}
		return null;	
	}

	public String evaluateXPath(XPath xpath, Document doc, String expr)
			throws XPathExpressionException {

		XPathExpression exp = xpath.compile(expr);
		String xmlDetails = (String) exp.evaluate(doc, XPathConstants.STRING);
		return xmlDetails;

	}
	
	private void processBillingOrder(Map<String, Object> map) {
		String comndRefId = "-1";
		String sRC_ID = "-1";

		String row_ID = "";
		String trans_Id = "";
		String orderTransRowId = "";
		String status = "";
		String corr_Id="-1";
		String funcRefId="-1";
		String opgRefId="-1";
		String commndRefId="-1";
		XPath xpath=null;
		Document doc=null;

		String srcTransId = "";

		/*M.Rahman: New Billing related Input parameters - Begin*/
		String compLineItemId = "";
		String compType = "";
		String compAction = "";
		String compSubType = "";
		String extIdAction = "";
		String subscrAction = "";
		String crmSvcId="";
		String effectiveDt = "";
		String accountID = "";
		Date effectiveDate = null;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); 

		/*M.Rahman: New Billing related Input parameters - End*/

		//ArrayList<?> messages = (ArrayList<?>) message.getPayload();

		// for each record picked by the OrderTransDBPoller
		//for (Object mess : messages) {

			// Moving try and catch inside for loop - Start
			try {
				taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[Instructor Handler Bill][handleMessage] Inside try ", loggercategory);


				HandlerVariables handlerVar = new HandlerVariables();
				CommandParametersBill parameters = new CommandParametersBill();
				CommandParamRefDtlsBill paramRefDtls = new CommandParamRefDtlsBill();
				SplFunctions splFunc = new SplFunctions();


				// Moving try and catch inside for loop - End

				//Map map = (Map) mess;

				String order_Xml;


				orderTransRowId = "" + (BigDecimal) map.get("ROW_ID");
				order_Xml = (String) map.get("Order_XML");
				funcRefId = (String) map.get("FUNC_REF_ID");
				opgRefId = (String) map.get("OPG_REF_ID");
				trans_Id = (String) map.get("TRANS_ID");
				corr_Id = (String) map.get("CORR_ID");
				status = (String) map.get("STATUS");

				// added srcTransId for logging
				srcTransId = (String) map.get("SRC_TRANS_ID");
				taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[Instructor Handler Bill][handleMessage] Values from Order trans dtls table: transId=" + trans_Id 
						+ ",funcRefId=" + funcRefId 
						+ ",opgRefId=" + opgRefId 
						+ ",corrId=" + corr_Id
						+ ",Status=" + status
						+ ",Order Trans Row Id=" + orderTransRowId, loggercategory);
				if (status.equalsIgnoreCase("RETRY")) {

					// Passing srcTransId, transId for logging
					jdbcDatabaseDAO.updateOrderTransDtlsTable(orderTransRowId, srcTransId, trans_Id);
					// Ravi: implementation for Retry
					jdbcDatabaseDAO.removeBillCmdTransDtls(srcTransId, trans_Id, orderTransRowId, loggercategory);

				}else {
					// setting input identifier for logging
					//Bug 1908 Refer to Modification History
					String inputIdentifier = orderTransRowId;

					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[Instructor Handler Bill][handleMessage] Values from Order trans dtls table: transId=" + trans_Id + ",funcRefId=" + funcRefId + ",opgRefId=" + opgRefId + ",corrId=" + corr_Id, loggercategory);

					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[Instructor Handler Bill][handleMessage]Order Trans Dtls Row Id=" + orderTransRowId, loggercategory);

					// Passing srcTransId, transId for logging
					jdbcDatabaseDAO.updateOrderTransDtlsTable(orderTransRowId, srcTransId, trans_Id);

					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] Status Updated as Received", loggercategory);

					//jdbcDatabaseDAO.updateOrderTransDtlsTable(orderTransRowId);
					NodeList MultiSIMlst = XPathReader.executeXPath(ApplicationConstants.XPATH_MULTI_SIM, doc);
					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] MultiSIMlst " +MultiSIMlst, loggercategory);

					InputStream is = new ByteArrayInputStream(order_Xml.getBytes());
					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] InputStream " + is, loggercategory);
					DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] domFactory " + domFactory, loggercategory);
					domFactory.setNamespaceAware(false);
					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] domFactory " + domFactory, loggercategory);
					DocumentBuilder builder = domFactory.newDocumentBuilder();
					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] DocumentBuilder " + builder, loggercategory);
					doc = builder.parse(is);
					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] Document " + doc, loggercategory);
					XPathFactory factory = XPathFactory.newInstance();
					xpath = factory.newXPath();
					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] Xpath " + xpath, loggercategory);
					String mOLI_ID = evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/LineItemXML/LineItem/ParentLineItemID");

					String oLI_ID = evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/ComponentList/ComponentInfo/CompLineItemID");

					/*M.Rahman: Adding subscriber_action and subscrId x_paths*/
					subscrAction = evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/ServiceInfo/ActionCode").replaceAll("-", "NA");
					crmSvcId =  evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/ServiceInfo/CRMServiceId");


					sRC_ID = srcTransId;

					/*The iteration needs to be skipped in case of Suspend and Reconnect APIs*/
					if(funcRefId.equalsIgnoreCase("SomFunc_BillingSuspend")||funcRefId.equalsIgnoreCase("SomFunc_BillingReconnect")){

						compLineItemId=evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/ComponentList/ComponentInfo/CompLineItemID");
						taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] compLineItemId " + compLineItemId, loggercategory);
						compType=evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/ComponentList/ComponentInfo/CompType");
						taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] compType " +compType, loggercategory);
						compAction=evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/ComponentList/ComponentInfo/ActionCode");
						taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] compAction " +compAction, loggercategory);		
						compSubType="NA";
						extIdAction= "NA";

						String date = evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/ComponentList/ComponentInfo/EffectiveDate");
						taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] date " + date, loggercategory);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); 
						effectiveDate = (Date)formatter.parse(date);
						taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler Bill][handleMessage] effectiveDate " + effectiveDate, loggercategory);
						taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[HandlerBill][handleMessage] funcRefId="+funcRefId+ " subscrAction="+subscrAction+" compLineItemId="+compLineItemId+" compType="+compType+" compAction="+compAction+" extIdAction="+extIdAction, loggercategory);	    
						List<Map<String, Object>> listOfCommandRefIds = jdbcDatabaseDAO.getCommandRefIdBill(funcRefId, subscrAction, compLineItemId, compType, compAction, extIdAction, compSubType,loggercategory);

						// for each command ref id,
						for (Object cRefId : listOfCommandRefIds) {
							Map newMap = (Map) cRefId;

							commndRefId = (String) newMap.get("CMD_REF_ID");
							BigDecimal seqNo = (BigDecimal) newMap.get("SEQ_NO");


							jdbcDatabaseDAO.insertTransValuesBill(commndRefId, commndRefId, trans_Id, sRC_ID, mOLI_ID, compLineItemId, seqNo, corr_Id, crmSvcId, effectiveDate, new Date(), "DBASLAPP", new Date(), "DBASLAPP", orderTransRowId,loggercategory);

						}
					}/*Test section- Begin*/
					else {

						doc.getDocumentElement().normalize();
						boolean Multisim = false;
						int SIMcount =0;
						NodeList nListOffer = doc.getElementsByTagName("ComponentInfo");
						for (int temp = 0; temp < nListOffer.getLength(); temp++) {			

							Node nNodeOffer = nListOffer.item(temp);			
							if (nNodeOffer.getNodeType() == Node.ELEMENT_NODE) {

								Element eElement = (Element) nNodeOffer;

								compLineItemId =  eElement.getElementsByTagName("CompLineItemID").item(0).getTextContent().replaceAll("-", "NA");
								compType = eElement.getElementsByTagName("CompType").item(0).getTextContent().replaceAll("-", "NA");
								compAction = eElement.getElementsByTagName("ActionCode").item(0).getTextContent().replaceAll("-", "NA");
								compSubType = eElement.getElementsByTagName("CompSubType").item(0).getTextContent().replaceAll("-", "NA");
								System.out.println("funcRefId" +funcRefId);
								if(funcRefId.equals("SomFunc_BillingExtIDMaintenance")){							
									NodeList nodeList = doc.getElementsByTagName("ExternalID");
									System.out.println("node list:" +nodeList.getLength());
									for(int num=0; num < nodeList.getLength(); num++){

										Node nodeOffer = nodeList.item(num);			
										if (nodeOffer.getNodeType() == Node.ELEMENT_NODE) {
											Element element = (Element) nodeOffer;
											extIdAction = element.getElementsByTagName("ExternalIDAction").item(0).getTextContent().replace("-", "NA");
											System.out.println("extIDAction:" +extIdAction);
										}
									}
								} else {
									extIdAction = "NA";
								}
								effectiveDt = eElement.getElementsByTagName("EffectiveDate").item(0).getTextContent();

								try {
									effectiveDate = df.parse(effectiveDt);	
								} catch (ParseException e){
								}
								//Logic CCTSM Component to trigger only once to provsion to C1
								if(compType.equalsIgnoreCase(ApplicationConstants.SIM_COMPONENT_TYPE)){							
									Multisim = true;
									SIMcount++;
								}
								if((!Multisim)||(SIMcount<=1)||((!compType.equalsIgnoreCase(ApplicationConstants.SIM_COMPONENT_TYPE)))){
									// Passing srcTransId, transId for logging
									taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[HandlerBill][handleMessage] funcRefId="+funcRefId+ " subscrAction="+subscrAction+" compLineItemId="+compLineItemId+" compType="+compType+" compAction="+compAction+" extIdAction="+extIdAction, loggercategory);	    
									List<Map<String, Object>> listOfCommandRefIds = jdbcDatabaseDAO.getCommandRefIdBill(funcRefId, subscrAction, compLineItemId, compType, compAction, extIdAction, compSubType,loggercategory);


									// for each command ref id,
									for (Object cRefId : listOfCommandRefIds) {
										Map newMap = (Map) cRefId;

										commndRefId = (String) newMap.get("CMD_REF_ID");
										BigDecimal seqNo = (BigDecimal) newMap.get("SEQ_NO");


										jdbcDatabaseDAO.insertTransValuesBill(commndRefId, commndRefId, trans_Id, sRC_ID, mOLI_ID, compLineItemId, seqNo, corr_Id, crmSvcId, effectiveDate, new Date(), "DBASLAPP", new Date(), "DBASLAPP", orderTransRowId,loggercategory);

									}
								}
							}
						}
					}
				}

				/*Sudharsan : Looking all commands for particular trans id to insert parameters */

				List<Map<String, Object>> newCRefId = jdbcDatabaseDAO.getNewCommandsForTrans(trans_Id, commndRefId, orderTransRowId, srcTransId,loggercategory);

				for (Object newCmdRefId : newCRefId) {

					Map map5 = (Map) newCmdRefId;
					comndRefId = (String) map5.get("CMD_REF_ID");
					row_ID = (String) map5.get("ROW_ID");
					//XmlParser parser = new XmlParser();
					compLineItemId = (String) map5.get("OLI_ID");


					handlerVar.setCommndRefId(comndRefId);
					handlerVar.setTrans_Id(trans_Id);
					handlerVar.setRow_ID(row_ID);
					handlerVar.setOrderTransRowId(orderTransRowId);
					handlerVar.setOrderXml(order_Xml);

					// calling getParameters method
					List<HandlerVariables> cmdParametersList = parameters.getParameters(sRC_ID, order_Xml, comndRefId, trans_Id, row_ID, orderTransRowId, jdbcDatabaseDAO, true,compLineItemId);
					List<HandlerVariables> cmdTransDtlsList = paramRefDtls.getParamValues(comndRefId, cmdParametersList, xpath, doc, jdbcDatabaseDAO,order_Xml);
					SPLCommonComponent.saveCmdTransDetailsBill(cmdTransDtlsList, sRC_ID, trans_Id, loggercategory, jdbcDatabaseDAO);

				}
				// Ravi: Defect#188 - add logic to fire c1 commands Sequence
				//jdbcDatabaseDAO.updateBillCmdTransStatus(orderTransRowId,trans_Id,loggercategory);
				int statusCount = jdbcDatabaseDAO.checkC1CmdForError(srcTransId, trans_Id, row_ID, crmSvcId, loggercategory);
				if(statusCount==0)
					jdbcDatabaseDAO.updateC1CmdStatusToNew(srcTransId, trans_Id, row_ID, crmSvcId, loggercategory);

				System.out.println("After inserting all parameters in T_OM_BILL_CMD_TRANS_DTLS ");
				OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
				SPLCommonComponent splCommonComponent = new SPLCommonComponent(taLogger, loggercategory);
				String tomResponse = splCommonComponent.createAckTomResponseBill(trans_Id,funcRefId);
				String corrId = jdbcDatabaseDAO.getCorrId(orderTransRowId);
				System.out.println("corrId:"+corrId + " orderTransRowId:"+orderTransRowId);
				orderTransactionSender.postMessageAck(tomResponse, corrId);
				System.out.println("TOM Ack Sent .. ");
				//}

			}

			catch (Exception e)

			{
				taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[Instructor Handler Bill][handleMessage] Inside Catch", loggercategory, e);

				e.printStackTrace();
				taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[Instructor Handler Bill][handleMessage] Error Inside Instructor Handler method", loggercategory, e);

				jdbcDatabaseDAO.insertError(ApplicationConstants.APPLICATION_NAME, sRC_ID, commndRefId, "Instructor Handler", "ERR_CODE", SPLCommonComponent.getStackTrace(e));

				if (row_ID != null) {

					//update error status in t_om_cmd_trans table
					jdbcDatabaseDAO.updateBillcmdTransError(row_ID, "1", e.getMessage(),loggercategory);
				}

				//update error status in t_om_order_trans_dtls table
				jdbcDatabaseDAO.orderTransDetailsErrorUpdate(orderTransRowId);


				SPLCommonComponent splCommonComponent = null;
				try {
					splCommonComponent = new SPLCommonComponent(taLogger, loggercategory);
				} catch (IOException ioException) {
					taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[Instructor Handler Bill][handleMessage] Error instantiating SPLCommonComponent", loggercategory, ioException);
				}
				String tomResponse = "";
				try {
					tomResponse = splCommonComponent.createTomResponse("responseCode",""+e.getMessage(),corr_Id,trans_Id,opgRefId,funcRefId,commndRefId);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
				orderTransactionSender.postBillingResponse(tomResponse, corr_Id);

				taLogger.log(srcTransId, trans_Id, ApplicationConstants.LOG_DEBUG, "[Instructor Handler Bill][handleMessage] Inside Catch Sent Fail response back to OSM", loggercategory, e);
			}
		//}
	}

}
