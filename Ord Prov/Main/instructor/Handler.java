package com.m1.bcc.spl.instructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
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
import org.springframework.integration.message.GenericMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.w3c.dom.Document;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.model.OrderTransactonDetail;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;
import com.m1.bcc.spl.sender.OrderTransactionSender;
import com.m1.bcc.spl.translator.CommandHandler;
import com.m1.bcc.spl.translator.ResponseTranslator;
import com.m1.bcc.spl.util.BeanFactory;
import com.m1.bcc.spl.util.SPLCommonComponent;
import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 22/02/2013					Ravikumar G				Modified for logging cleanup
 * 17/04/2013					Billy Lim				Bug 1908. Move class variables used by @Async function into local function. This is to prevent class variables from by being overridden when called asynchronously
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 06/11/2013					Ravikumar G				Bug#20397: added xml_par_ele_id
 * 12/11/2013					Ravikumar G				Bug#20421 - to implement multithreading using thread poll task executor
 * 31/12/2013					Ravikumar G				Bug#22498 - Updated order_trans_dtls row id type from string to integer
 * 17/01/2014					Ravikumar G				Bug#23710 - Updated cmd_trans column row_id type to number
 * 27/01/2014					Ravikumar G				Bug#23086 - added condition if source=OSM then send tomresponse
 ******************************************************************************/

public class Handler {
	JdbcDatabaseDAO jdbcDatabaseDAO;
	private ThreadPoolTaskExecutor taskExecutor;
	TALogger taLogger = TALogger.getTALogger();

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
	
	//@Autowired AsyncTaskExecutor executor;
	@Async
	@ServiceActivator
	// receives message from T_OM_ORDER_TRANS_DTLS table
	public void handleMessage(Message<ArrayList<Map<String, Object>>> message) throws TransformerConfigurationException, ParserConfigurationException, TransformerException {
		ArrayList<Map<String, Object>> orderList = (ArrayList<Map<String, Object>>) message.getPayload();
		System.out.println("orderList " + orderList.size());
		// for each record picked by the OrderTransDBPoller
		for(final Map<String, Object> orderMap : orderList) {
			taskExecutor.submit(new Runnable() {
				
				@Override
				public void run() {
					processOrder(orderMap);
				}
			});
			
		}
	}


	public void removeCmdTransDetailsValues(String status, String trans_Id, String orderRowId, String srcTransId,String loggercategory) {
		// Passing srcTransId, transId for logging
		jdbcDatabaseDAO.deleteCommandTransDetailsRecords(trans_Id, orderRowId, srcTransId);
	}

	public String evaluateXPath(XPath xpath, Document doc, String expr)
			throws XPathExpressionException {

		XPathExpression exp = xpath.compile(expr);
		String xmlDetails = (String) exp.evaluate(doc, XPathConstants.STRING);
		return xmlDetails;

	}
	
	@Async
	private void processOrder(Map<String, Object> orderMap) {
		System.out.println("processOrder ");
		String orderTransRowId = "";
		String orderXml = "";
		String funcRefId = "";
		String opgRefId = "";
		String transId = "";
		String corrId = "1";
		String status = "";
  		String srcTransId = "";
		XPath xpath = null;
		String cmdRefId = "";
		String source = "";

        HandlerVariables handlerVar = new HandlerVariables();
        CommandParameters parameters = new CommandParameters();
        CommandParamRefDtls paramRefDtls = new CommandParamRefDtls();
        SplFunctions splFunc = new SplFunctions();

		String comndRefId = "-1";

		String row_ID = "";
  		Document doc=null;

		try {
	
			orderTransRowId = "" + (BigDecimal) orderMap.get("ROW_ID");
			orderXml = (String) orderMap.get("Order_XML");
			funcRefId = (String) orderMap.get("FUNC_REF_ID");
			opgRefId = (String) orderMap.get("OPG_REF_ID");
			transId = (String) orderMap.get("TRANS_ID");
			corrId = (String) orderMap.get("CORR_ID");
			status = (String) orderMap.get("STATUS");
			source = (String) orderMap.get("SOURCE");
	
			taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[Instructor Handler][handleMessage]source=" + source, loggercategory);
			
			// added srcTransId for logging
			srcTransId = (String) orderMap.get("SRC_TRANS_ID");
	
			if (status.equalsIgnoreCase("RETRY")) {
	
				// Passing srcTransId, transId for logging
				jdbcDatabaseDAO.updateOrderTransDtlsTable(orderTransRowId, srcTransId, transId);
				removeCmdTransDetailsValues(status, transId, orderTransRowId, srcTransId,loggercategory);
	
			} else {
				// setting input identifier for logging
				//Bug 1908 Refer to Modification History
				//String inputIdentifier = orderTransRowId;
				
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[Instructor Handler][handleMessage] Values from Order trans dtls table: transId=" + transId + ",funcRefId=" + funcRefId + ",opgRefId=" + opgRefId + ",corrId=" + corrId, loggercategory);
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG, "[Instructor Handler][handleMessage]Order Trans Dtls Row Id=" + orderTransRowId, loggercategory);
	
				// Passing srcTransId, transId for logging
				jdbcDatabaseDAO.updateOrderTransDtlsTable(orderTransRowId, srcTransId, transId);
				
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler][handleMessage] Status Updated as Received", loggercategory);
	
				//jdbcDatabaseDAO.updateOrderTransDtlsTable(orderTransRowId);
	
				InputStream is = new ByteArrayInputStream(orderXml.getBytes());
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler][handleMessage] InputStream " + is, loggercategory);
				DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler][handleMessage] domFactory " + domFactory, loggercategory);
				domFactory.setNamespaceAware(false);
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler][handleMessage] domFactory " + domFactory, loggercategory);
				DocumentBuilder builder = domFactory.newDocumentBuilder();
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler][handleMessage] DocumentBuilder " + builder, loggercategory);
				doc = builder.parse(is);
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler][handleMessage] Document " + doc, loggercategory);
				XPathFactory factory = XPathFactory.newInstance();
				xpath = factory.newXPath();
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler][handleMessage] Xpath " + xpath, loggercategory);
				
				String moliId = evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/LineItemXML/LineItem/ParentLineItemID");
				String oliId = evaluateXPath(xpath, doc,"Envelope/Body/ProvRequest/ListOfRef/LineItemXML/LineItem/LineItemID");
	
				// Passing srcTransId, transId for logging
				List<Map<String, Object>> ListOfCommandRefIds = jdbcDatabaseDAO.getCommandRefId(funcRefId, opgRefId, srcTransId, transId,loggercategory);
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,	"[Instructor Handler][handleMessage] Moli Id=" + moliId + ", Oli Id=" + oliId, loggercategory);
	
				// for each command ref id,
				for (Object cRefId : ListOfCommandRefIds) {
					Map newMap = (Map) cRefId;
	
					cmdRefId = (String) newMap.get("CMD_REF_ID");
					BigDecimal seqNo = (BigDecimal) newMap.get("SEQ_NO");
	
					taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[Instructor Handler][handleMessage]Values to be inserted in cmd Trans table:" +
							"cmdRefId="+ cmdRefId + ",transId=" + transId + "" + ",seqNo=" + seqNo + ", Moli Id=" + moliId + ",oli Id=" + oliId + ",srcId=" + srcTransId, loggercategory);
	
					// insert commands into command trans table
	
					jdbcDatabaseDAO.insertTransValues(cmdRefId, cmdRefId, transId, srcTransId, moliId, oliId, seqNo, corrId, new Date(), "DBASLAPP", new Date(), "DBASLAPP", orderTransRowId, loggercategory, false);
				}
	
			}
	
			List<Map<String, Object>> newCRefId = jdbcDatabaseDAO.getNewCmdRefId(transId, cmdRefId, orderTransRowId, srcTransId,loggercategory);
	
			for (Object newCmdRefId : newCRefId) {
	
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_DEBUG,"[Instructor Handler][handleMessage]After getNewCmdRefId trans_Id="+transId, loggercategory);
	
	
				Map map5 = (Map) newCmdRefId;
				comndRefId = (String) map5.get("CMD_REF_ID");
				row_ID = "" + (BigDecimal) map5.get("ROW_ID");
				//XmlParser parser = new XmlParser();
	
				handlerVar.setCommndRefId(comndRefId);
				handlerVar.setTrans_Id(transId);
				handlerVar.setRow_ID(row_ID);
				handlerVar.setOrderTransRowId(orderTransRowId);
				handlerVar.setOrderXml(orderXml);
	
				// calling getParameters method
				List<HandlerVariables> cmdParametersList = parameters.getParameters(srcTransId, orderXml, comndRefId, transId, row_ID, orderTransRowId, jdbcDatabaseDAO, true);
				taLogger.log(srcTransId,transId, row_ID, ApplicationConstants.LOG_DEBUG, "[Instructor Handler][handleMessage]before calling getParamSubValues method", loggercategory);
	
				for(Object list:cmdParametersList){
					HandlerVariables var = (HandlerVariables) list;
					String paramName = var.getParamName();
					String paramValue = var.getParamValue();
					String paramSub = var.getParamSub();
					String xmlEleType = var.getXmlEleType();
					String xmlEleId = var.getXmlEleId();
					String xmlEleCurrValue = var.getXmlEleCurrValue();
					String xmlElePrevValue = var.getXmlElePrevValue();
					String xmlEleName = var.getXmlEleName();
					String xmlParEleId = var.getXmlParEleId();
					taLogger.log(srcTransId, transId, row_ID, ApplicationConstants.LOG_DEBUG, "[Instructor Handler][handleMessage]paramName=" + paramName + "\n paramValue= " + paramValue + "\n " +
							"paramSub=" + paramSub + "\nxmlEleType=" + xmlEleType + "\nxmlEleId=" + xmlEleId + "\nxmlEleCurrValue=" + xmlEleCurrValue + "\n" +
									"xmlElePrevValue=" + xmlElePrevValue + "\n xmlEleName=" + xmlEleName + "\n xmlParEleId =" + xmlParEleId, loggercategory);
				}
				List<HandlerVariables> cmdTransDtlsList = paramRefDtls.getParamValues(comndRefId, cmdParametersList, xpath, doc, jdbcDatabaseDAO);
	
	
				SPLCommonComponent.saveCmdTransDetails(cmdTransDtlsList, srcTransId, transId, loggercategory, jdbcDatabaseDAO);
			}
		}catch (Exception e) {
			taLogger.log(srcTransId, transId, ApplicationConstants.LOG_ERROR, "[Instructor Handler][handleMessage] Inside Catch", loggercategory, e);

			jdbcDatabaseDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "Instructor Handler", "ERR_CODE", SPLCommonComponent.getStackTrace(e));
			
			if (row_ID != null) {
				//update error status in t_om_cmd_trans table
				jdbcDatabaseDAO.instructorErrorUpdate(row_ID, "1", e.getMessage(),loggercategory);
			}
			
			//update error status in t_om_order_trans_dtls table
			jdbcDatabaseDAO.orderTransDetailsErrorUpdate(orderTransRowId);

			if(ApplicationConstants.SOURCE_OSM.equalsIgnoreCase(source)) {
				SPLCommonComponent splCommonComponent = null;
				try {
					splCommonComponent = new SPLCommonComponent(taLogger, loggercategory);
				} catch (IOException ioException) {
					taLogger.log(srcTransId, transId, ApplicationConstants.LOG_ERROR, "[Instructor Handler][handleMessage] Error instantiating SPLCommonComponent", loggercategory, ioException);
				}
				String tomResponse = "";
				try {
					tomResponse = splCommonComponent.createTomResponse("responseCode",""+e.getMessage(), corrId, transId,opgRefId,funcRefId,cmdRefId);
				} catch (Exception e1) {
					taLogger.log(srcTransId, transId, ApplicationConstants.LOG_ERROR, "[Instructor Handler][handleMessage] Error Creating TOM response", loggercategory, e1);
				}
				
				OrderTransactionSender orderTransactionSender = (OrderTransactionSender) BeanFactory.getBean(ApplicationConstants.BEAN_ORDERTRANSACTIONSENDER);
				orderTransactionSender.postMessage(tomResponse, corrId, "TOMOrderProv");
				taLogger.log(srcTransId, transId, ApplicationConstants.LOG_ERROR, "[Instructor Handler][handleMessage] Inside Catch Sent Fail response back to OSM", loggercategory, e);
			}
		}
	}
	
	public void processOrderStatus(String opgRefId, String funcRefId, String sourceTransId, String transId, String orderXML, String corrId, Document doc, XPath xpath) throws Exception{
		
		String cmdRefId = "";
		String pollStatement = "SELECT   ROW_ID, CMDTRANS.CMD_REF_ID, TRANS_ID, SRC_TRANS_ID, ORDER_ROW_ID, ENABLE_FLAG FROM T_OM_CMD_TRANS CMDTRANS, C_OM_CMD_SYS_MAP CMDSYSMAP, C_OM_INTERFACE_REF INTERFACEREF WHERE    CMDTRANS.CMD_REF_ID = CMDSYSMAP.CMD_REF_ID AND CMDSYSMAP.INTERFACE_NAME = INTERFACEREF.INTERFACE_NAME AND INTERFACEREF.ENABLE_FLAG = 'Y' AND row_id = ?";
		String commandResponseStatement = "select CMD_REF_ID,TRANS_ID,RESPONSE_XML,INTERFACE_NAME,CMD_ROW_ID,ROW_ID from T_OM_CMD_RESPONSE where CMD_ROW_ID = ?";
		String transDetails = "select return_code, return_msg, row_id, src_trans_id, corr_id, cmd_ref_id, trans_id, status from t_om_cmd_trans where row_id = ?";
		
		DataSource ds = (DataSource) BeanFactory.getBean("dataSource");
		JdbcDatabaseDAO jddLocal = (JdbcDatabaseDAO) BeanFactory.getBean("jdbcDatabaseDAO");
		jddLocal.setBasicDataSource(ds);
		
		JdbcTemplate jdbc = jddLocal.getJdbcTemplate();
		
		CommandParameters parameters = new CommandParameters();
		CommandParamRefDtls paramRefDtls = new CommandParamRefDtls();
		List<Map<String, Object>> listOfCommandRefIds = jddLocal.getCommandRefId(funcRefId, opgRefId, sourceTransId, transId, loggercategory);
		for (Map<String, Object> eachOfCommand : listOfCommandRefIds){
			cmdRefId = String.valueOf(eachOfCommand.get("CMD_REF_ID"));
			BigDecimal seqNo = (BigDecimal) eachOfCommand.get("SEQ_NO");
			jddLocal.insertTransValues(cmdRefId, cmdRefId, transId, sourceTransId, transId, "-", seqNo, corrId, new Date(), "DBASLAPP", new Date(), "DBASLAPP", transId, loggercategory, true);
		}
		
		List<Map<String, Object>> newCRefId = jddLocal.getJdbcTemplate().queryForList("select row_id, cmd_ref_id from T_OM_CMD_TRANS where trans_id = ?", transId);
		
		for (Map<String, Object> newCmdRefId : newCRefId) {
			List<HandlerVariables> cmdParametersList = parameters.getParameters(sourceTransId, orderXML, String.valueOf(newCmdRefId.get("CMD_REF_ID")), transId, String.valueOf(newCmdRefId.get("ROW_ID")) , transId, jddLocal, true);
			List<HandlerVariables> cmdTransDtlsList = paramRefDtls.getParamValues(cmdRefId, cmdParametersList, xpath, doc, jddLocal);
			SPLCommonComponent.saveCmdTransDetails(cmdTransDtlsList, sourceTransId, transId, loggercategory, jddLocal);
			List<Map<String, Object>> cmddetails = jdbc.queryForList(pollStatement, newCmdRefId.get("ROW_ID"));
			ArrayList<Map<String, Object>> cmddetailsStub = new ArrayList<Map<String,Object>>(cmddetails);
			GenericMessage<ArrayList<Map<String, Object>>> gMsg = new GenericMessage<ArrayList<Map<String, Object>>>(cmddetailsStub);
			CommandHandler commandHandler = new CommandHandler();
			commandHandler.translateCommandTransaction(gMsg, true);
			List<Map<String, Object>> cmdResponseDetails = jdbc.queryForList(commandResponseStatement, newCmdRefId.get("ROW_ID"));
			ArrayList<Map<String, Object>> cmdResponseDetailsStub = new ArrayList<Map<String,Object>>(cmdResponseDetails);
			GenericMessage<ArrayList<Map<String, Object>>> gMsgResponse = new GenericMessage<ArrayList<Map<String, Object>>>(cmdResponseDetailsStub);
			ResponseTranslator responseTrans = new ResponseTranslator();
			responseTrans.handleCommandResponse(gMsgResponse);
			
			OrderTransactonDetail otd = populateTransDetails(transDetails, newCmdRefId.get("ROW_ID"), jddLocal);
			
			otd.setFuncRefId(funcRefId);
			otd.setOpgRefId(opgRefId);
			
			String tomOrderXML = new SPLCommonComponent(taLogger, loggercategory).createTomResponse(
					otd.getReturnCode(), 
					otd.getReturnMsg(), 
					otd.getCorrId(), 
					otd.getTransId(),
					otd.getOpgRefId(),
					otd.getFuncRefId(),
					otd.getCmdRefId());
			
			new OrderTransactionSender().postMessage(tomOrderXML, otd.getCorrId(), "CRMUpdateOrderStatus");
			
		}
		
	}
	
	private OrderTransactonDetail populateTransDetails(String sql, Object rowid, JdbcDatabaseDAO jddLocal){
		OrderTransactonDetail otd = new OrderTransactonDetail();
		
		List<Map<String, Object>> result = jddLocal.getJdbcTemplate().queryForList(sql, new Object[]{rowid});
		
		otd.setReturnCode(String.valueOf(result.get(0).get("RETURN_CODE")));
		otd.setReturnMsg(String.valueOf(result.get(0).get("RETURN_MSG")));
		otd.setRowId(String.valueOf(result.get(0).get("ROW_ID")));
		otd.setCrmOrderId(String.valueOf(result.get(0).get("SRC_TRANS_ID")));
		otd.setCorrId(String.valueOf(result.get(0).get("CORR_ID")));
		otd.setCmdRefId(String.valueOf(result.get(0).get("CMD_REF_ID")));
		otd.setTransId(String.valueOf(result.get(0).get("TRANS_ID")));
		otd.setStatus(String.valueOf(result.get(0).get("STATUS")));
		
		return otd;
	}
}