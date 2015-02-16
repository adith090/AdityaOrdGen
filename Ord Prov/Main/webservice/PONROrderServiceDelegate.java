package com.m1.bcc.spl.webservice;

import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Holder;

@WebService(targetNamespace = "http://webservice.spl.bcc.m1.com/", serviceName = "PONROrderServiceService", portName = "PONROrderServicePort", wsdlLocation = "WEB-INF/wsdl/PONROrderServiceService.wsdl")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL, parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
@HandlerChain(file = "PONRhandler-chain.xml")
public class PONROrderServiceDelegate {

	PONROrderService ponrOrderService = new PONROrderService();

	@WebMethod
	public void ponrProcessRequest(Object[] provResponse, @WebParam(name = "ALLOW_CANCEL", mode = WebParam.Mode.OUT) Holder<String> ALLOW_CANCEL){ 
			/*@WebParam(name = "error_code", mode = WebParam.Mode.OUT) Holder<Integer> error_code, @WebParam(name = "rowcount", mode = WebParam.Mode.OUT) Holder<Integer> rowcount) {*/
		// System.out.println("Web MEthods" + provResponse);
		//System.out.println("allow cancel");
		
		ALLOW_CANCEL.value= "Y";
		/*ponrOrderService
				.ponrProcessRequest(provResponse, ALLOW_CANCEL);*/
	}

}
