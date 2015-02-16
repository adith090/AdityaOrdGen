package com.m1.bcc.spl.model;

public class OrderTransactonDetailBill {


	private String rowId;
	private String orderId;
	private String orderXml;
	private String funcRefId;
	private String opgRefId;
	private String status;
	private String pollStatus;
	private String corrId;
	private String insertDate;
	private String insertBy;
	private String updatedDate;
	private String updatedBy;
	private String taskRequest;
	private String crmOrderId;
	private String returnCode;
	private String returnMsg;
	private String cmdRefId;
	private String transId;
	private String srcTransId;
	/*Adding new column for C1_Flag*/
	private String cOneFlag;

	public String getSrcTransId() {
		return srcTransId;
	}
	public void setSrcTransId(String srcTransId) {
		this.srcTransId = srcTransId;
	}
	public String getRowId() {
		return rowId;
	}
	public void setRowId(String rowId) {
		this.rowId = rowId;
	}
	public String getOrderId() {
		return orderId;
	}
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	public String getOrderXml() {
		return orderXml;
	}
	public void setOrderXml(String orderXml) {
		this.orderXml = orderXml;
	}
	public String getFuncRefId() {
		return funcRefId;
	}
	public void setFuncRefId(String funcRefId) {
		this.funcRefId = funcRefId;
	}
	public String getOpgRefId() {
		return opgRefId;
	}
	public void setOpgRefId(String opgRefId) {
		this.opgRefId = opgRefId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getPollStatus() {
		return pollStatus;
	}
	public void setPollStatus(String pollStatus) {
		this.pollStatus = pollStatus;
	}
	public String getCorrId() {
		return corrId;
	}
	public void setCorrId(String corrId) {
		this.corrId = corrId;
	}
	public String getInsertDate() {
		return insertDate;
	}
	public void setInsertDate(String insertDate) {
		this.insertDate = insertDate;
	}
	public String getInsertBy() {
		return insertBy;
	}
	public void setInsertBy(String insertBy) {
		this.insertBy = insertBy;
	}
	public String getUpdatedDate() {
		return updatedDate;
	}
	public void setUpdatedDate(String updatedDate) {
		this.updatedDate = updatedDate;
	}
	public String getUpdatedBy() {
		return updatedBy;
	}
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
	public String getTaskRequest() {
		return taskRequest;
	}
	public void setTaskRequest(String taskRequest) {
		this.taskRequest = taskRequest;
	}

	public String getCrmOrderId() {
		return crmOrderId;
	}
	public void setCrmOrderId(String crmOrderId) {
		this.crmOrderId = crmOrderId;
	}
	public String getReturnCode() {
		return returnCode;
	}
	public void setReturnCode(String returnCode) {
		this.returnCode = returnCode;
	}
	public String getReturnMsg() {
		return returnMsg;
	}
	public void setReturnMsg(String returnMsg) {
		this.returnMsg = returnMsg;
	}
	public String getCmdRefId() {
		return cmdRefId;
	}
	public void setCmdRefId(String cmdRefId) {
		this.cmdRefId = cmdRefId;
	}
	public String getTransId() {
		return transId;
	}
	public void setTransId(String transId) {
		this.transId = transId;
	}
	/*Adding new method to Insert C1_Flag value - Begin*/
	public void setCOneFlag(String cOneFlag) {
		this.cOneFlag = cOneFlag;
	}
	public String getCOneFlag() {
		return cOneFlag;
	}
	/*Adding new method to Insert C1_Flag value - End*/


}
