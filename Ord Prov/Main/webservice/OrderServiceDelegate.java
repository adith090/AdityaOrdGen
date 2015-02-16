package com.m1.bcc.spl.webservice;

import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Holder;

@WebService (targetNamespace="http://webservice.spl.bcc.m1.com/", serviceName="OrderServiceService", portName="OrderServicePort", wsdlLocation="WEB-INF/wsdl/OrderServiceService.wsdl")
@SOAPBinding(style=SOAPBinding.Style.DOCUMENT,use=SOAPBinding.Use.LITERAL,parameterStyle=SOAPBinding.ParameterStyle.WRAPPED)
@HandlerChain(file="handler-chain.xml")
public class OrderServiceDelegate {

    OrderService orderService = new OrderService();

    /*@WebMethod
    public void processRequest(@WebParam(name = "correlationID", mode = WebParam.Mode.OUT) Holder<String> correlationID,
            @WebParam(name = "returnCode", mode = WebParam.Mode.OUT) Holder<String> returnCode, @WebParam(name = "returnMessage", mode = WebParam.Mode.OUT) Holder<String> returnMessage) {
		//System.out.println("Web MEthods" + provResponse);
        orderService.processRequest(correlationID, returnCode, returnMessage);
    }*/

    @WebMethod
    public void processRequest() {
		//System.out.println("Web MEthods" + provResponse);
        //return 
       	orderService.processRequest();
    }
}