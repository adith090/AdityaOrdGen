package com.m1.bcc.spl.translator;

import java.util.ArrayList;
import java.util.Map;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.scheduling.annotation.Async;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.dao.DatabaseDAO;
import com.m1.bcc.spl.model.CommandTransDetails;
import com.m1.bcc.spl.util.SPLCommonComponent;

import common.util.TALogger;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 29/11/2013					Ravikumar G				Created to process DB Adaptor Command Response
 ******************************************************************************/

public class DBAdaptorResponseHandler {
	private DatabaseDAO dbAdaptorResponseDAO;

	/**
	 *
	 * @param asyncTransactionDAO
	 */
	public void setDatabaseDAO(DatabaseDAO dbAdaptorResponseDAO) {
		this.dbAdaptorResponseDAO = dbAdaptorResponseDAO;
	}

	@Async
	@ServiceActivator
	public void handleDbAdaptorResponse(Message<ArrayList<Map<String, Object>>> message) {
		TALogger taLogger = TALogger.getTALogger();
		ArrayList<Map<String, Object>> responseDetailsList = (ArrayList<Map<String, Object>>) message.getPayload();
		for(final Map<String, Object> responseDetails : responseDetailsList) {
			String loggerCategory = "DATABASElogging";
			String cmdRowId = "";
			String errorCode = "";
			String errorMessage = "";
			String systemName = "";
			String interfaceName = "";
			String transId = "";
			String srcTransId = "";
			String cmdRefId = "";
			String status = "";
			
			try {
				cmdRowId = (String) responseDetails.get(ApplicationConstants.COLUMN_TRANS_ID);
				errorCode = (String) responseDetails.get(ApplicationConstants.COLUMN_ERROR_CODE);
				errorMessage = (String) responseDetails.get(ApplicationConstants.COLUMN_ERROR_MESSAGE);
				status = (String) responseDetails.get(ApplicationConstants.COLUMN_OM_STATUS_CD);
				CommandTransDetails dbAdaptorTransDetails = dbAdaptorResponseDAO.getDBAdaptorTransDtls(cmdRowId, loggerCategory);
				systemName = dbAdaptorTransDetails.getSystemName();
				interfaceName = dbAdaptorTransDetails.getInterfaceName();
				cmdRefId = dbAdaptorTransDetails.getCmdRefId();
				srcTransId = dbAdaptorTransDetails.getSrcTransId();
				transId = dbAdaptorTransDetails.getTransId();
				loggerCategory = systemName.toUpperCase()+"logging";
				taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_INFO, "[DBAdaptorResponseHandler][handleDbAdaptorResponse]System Name :" + systemName, loggerCategory);
				SPLCommonComponent splCommonComponent = new SPLCommonComponent(taLogger, loggerCategory);
				splCommonComponent.setLogCmdRowId(cmdRowId);
				splCommonComponent.setLogTransId(transId);
				splCommonComponent.setLogSrcTransId(srcTransId);
				Document document = splCommonComponent.getDocument();
				
				Element rootElement = document.createElement("DBAdaptorResponse");
	            rootElement.setAttribute("xmlns", "http://tempuri.org/");
				document.appendChild(rootElement);
				
				Node msgHeaderNode = splCommonComponent.createElement(document, rootElement, "msgHeader", "");
				splCommonComponent.createElement(document, msgHeaderNode, "transId", cmdRowId);
				if(ApplicationConstants.DBADAPTOR_STATUS_SUCCESS.equals(status)) {
					splCommonComponent.createElement(document, msgHeaderNode, "returnCode", ApplicationConstants.TOM_RESPONSE_SUCCESS);
					splCommonComponent.createElement(document, msgHeaderNode, "returnMessage", ApplicationConstants.MESSAGE_SUCCESS);
				}else {
					splCommonComponent.createElement(document, msgHeaderNode, "returnCode", errorCode);
					splCommonComponent.createElement(document, msgHeaderNode, "returnMessage", errorMessage);
				}
				String responseMessage = splCommonComponent.convertDocumentToString(document);
				dbAdaptorResponseDAO.saveCommandResponse(cmdRowId, cmdRefId, transId, responseMessage, interfaceName, false);
			}catch(Exception exception) {
				taLogger.log(srcTransId, transId, cmdRowId, ApplicationConstants.LOG_ERROR,"[DBAdaptorResponseHandler][handleDbAdaptorResponse]Catch: ", loggerCategory, exception);
				dbAdaptorResponseDAO.insertError(ApplicationConstants.APPLICATION_NAME, srcTransId, cmdRefId, "Error in DB Adaptor", cmdRefId, SPLCommonComponent.getStackTrace(exception));
			}
		}
	}

}
