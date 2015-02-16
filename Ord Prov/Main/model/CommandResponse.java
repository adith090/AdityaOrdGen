package com.m1.bcc.spl.model;

public class CommandResponse {
	private String cmdRowId;
	private String transId;
	private String cmdRefId;
	private String responseXml;
	private String status;
	private String interfaceName;

	public String getCmdRowId() {
		return cmdRowId;
	}
	public void setCmdRowId(String cmdRowId) {
		this.cmdRowId = cmdRowId;
	}
	public String getTransId() {
		return transId;
	}
	public void setTransId(String transId) {
		this.transId = transId;
	}
	public String getCmdRefId() {
		return cmdRefId;
	}
	public void setCmdRefId(String cmdRefId) {
		this.cmdRefId = cmdRefId;
	}
	public String getResponseXml() {
		return responseXml;
	}
	public void setResponseXml(String responseXml) {
		this.responseXml = responseXml;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getInterfaceName() {
		return interfaceName;
	}
	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

}
