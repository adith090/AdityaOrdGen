package com.m1.bcc.spl.webservice;

import java.io.Serializable;

import javax.jws.WebParam;
import javax.xml.ws.Holder;

import org.springframework.jdbc.core.JdbcTemplate;

import com.m1.bcc.spl.constants.ApplicationConstants;
import com.m1.bcc.spl.orderdbpoller.JdbcDatabaseDAO;

public class PONROrderService implements Serializable {

	private static final long serialVersionUID = -5601766457969700996L;
	private String response;
	JdbcDatabaseDAO jdbcDatabaseDAO;

	/**
	 * 
	 * @param provResponse
	 * @param statusCode
	 * @param statusDescription
	 */
	public void ponrProcessRequest(Object[] provResponse, @WebParam(name = "ALLOW_CANCEL", mode = WebParam.Mode.OUT) Holder<String> ALLOW_CANCEL) {
			/*@WebParam(name = "error_code", mode = WebParam.Mode.OUT) Holder<Integer> error_code, @WebParam(name = "rowcount", mode = WebParam.Mode.OUT) Holder<Integer> rowcount) {*/
		
	//jdbcDatabaseDAO.PONRStatusWebservice();
		
		//System.out.println("PONROrderservice"+ provResponse[0]);
		
		
		//System.out.println("service");
		//ALLOW_CANCEL.value= "N";
		/*error_code.value= ApplicationConstants.MESSAGE_ERRORCODE_SUCCESS;
		rowcount.value= ApplicationConstants.MESSAGE_ROWCOUNT_SUCCESS;*/
		
	}
}
