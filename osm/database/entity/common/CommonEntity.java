package com.m1.sg.osm.database.entity.common;

public class CommonEntity {

	public static final String SQL_SELECT_RETRY_VALUE = "SQL_SELECT_RETRY_VALUE";
	public static final String SQL_INSERT_ERR_LOG = "SQL_INSERT_ERR_LOG";
	
	private String cmdRefId;
	private String errorCode;
	private String action;
	private String maxRetry;
	private String insertDt;
	private String insertBy;
	private String updateDt;
	private String updateBy;
	private String orderId;
	private String errorMessage;
	
	
	public String getCmdRefId() {
		return cmdRefId;
	}
	public void setCmdRefId(String cmdRefId) {
		this.cmdRefId = cmdRefId;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getMaxRetry() {
		return maxRetry;
	}
	public void setMaxRetry(String maxRetry) {
		this.maxRetry = maxRetry;
	}
	public String getInsertDt() {
		return insertDt;
	}
	public void setInsertDt(String insertDt) {
		this.insertDt = insertDt;
	}
	public String getInsertBy() {
		return insertBy;
	}
	public void setInsertBy(String insertBy) {
		this.insertBy = insertBy;
	}
	public String getUpdateDt() {
		return updateDt;
	}
	public void setUpdateDt(String updateDt) {
		this.updateDt = updateDt;
	}
	public String getUpdateBy() {
		return updateBy;
	}
	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}
	public String getOrderId() {
		return orderId;
	}
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	
}
