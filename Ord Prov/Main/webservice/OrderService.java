package com.m1.bcc.spl.webservice;

import java.io.Serializable;

//import javax.activation.DataHandler;
import javax.jws.WebParam;
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.JAXBException;
//import javax.xml.bind.Marshaller;
//import javax.xml.bind.Unmarshaller;
import javax.xml.ws.Holder;

import com.m1.bcc.spl.constants.ApplicationConstants;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 18/11/2012					Ravikumar G				Created
 * 06/11/2013					Ravikumar G				Bug#20397: modifed for new request format
 * 18/11/2013					Ravikumar G				Bug 20601 - service internal id resets to update back CRM
 ******************************************************************************/

public class OrderService implements Serializable {

	private static final long serialVersionUID = -5601766457969700996L;
	private String response;

	/**
	 *
	 * @param provResponse
	 * @param statusCode
	 * @param statusDescription
	 */
	/*public void processRequest(@WebParam(name = "correlationID", mode = WebParam.Mode.OUT) Holder<String> correlationID,
            @WebParam(name = "returnCode", mode = WebParam.Mode.OUT) Holder<String> returnCode, @WebParam(name = "returnMessage", mode = WebParam.Mode.OUT) Holder<String> returnMessage) {
		correlationID.value = "";
		returnCode.value = "";
		returnMessage.value = "";
	}*/
	
	public void processRequest() {
		/*MsgResHeader msgResHeader = new MsgResHeader();
		msgResHeader.setCorrelationID("111");
		msgResHeader.setReturnCode("0");
		msgResHeader.setReturnMessage("SS");
		
		MsgResBody msgResBody = new MsgResBody();
		
		ProcessRequest1 processRequest1 = new ProcessRequest1();
		processRequest1.setMsgResHeader(msgResHeader);
		processRequest1.setMsgResBody(msgResBody);*/
		//return processRequest1;
	}
}
