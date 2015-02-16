package com.m1.bcc.spl.model;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/11/2012					Ravikumar G				Created
 * 13/05/2013					Ravikumar G				Bug#2116: Added cmd row id
 * 17/09/2013					Ravikumar G				Added crm service id
 * 31/12/2013					Ravikumar G				Bug#22498 - added setter and getter for priority
 * 27/01/2014					Ravikumar G				Bug#23086 - added setter and getter for source
 ******************************************************************************/

public class OrderTransactonDetail {

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
	private String cmdRowId;
	/*Adding new column for C1_Flag*/
	private String cOneFlag;
	private String crmSvcId;
	private String priority;
	private String source;

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
	public String getCmdRowId() {
		return cmdRowId;
	}
	public void setCmdRowId(String cmdRowId) {
		this.cmdRowId = cmdRowId;
	}
	/*Adding new method to Insert C1_Flag value - Begin*/
	public void setCOneFlag(String cOneFlag) {
		this.cOneFlag = cOneFlag;
	}
	public String getCOneFlag() {
		return cOneFlag;
	}
	/*Adding new method to Insert C1_Flag value - End*/
	public String getCrmSvcId() {
		return crmSvcId;
	}
	public void setCrmSvcId(String crmSvcId) {
		this.crmSvcId = crmSvcId;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}

}
