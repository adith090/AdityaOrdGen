package com.m1.sg.osm.database.entity.common;

public class ErrorLoggingEntity {

	public static final String SQL_INSERT_CREATE_ORDER_ERROR_TRANS = "SQL_INSERT_CREATE_ORDER_ERROR_TRANS";
	
	private String transactionId;
	private String sourceTransactionId;
	private String orderHistId;
	private String cartridgeName;
	private String taskName;
	private String transactionType;
	private String stacktrace;
	private String pollStatus;
	private String errorId;
	private String insertDateTime;
	private String insertBy;
	private String updateDateTime;
	private String updateBy;
	
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	public String getSourceTransactionId() {
		return sourceTransactionId;
	}
	public void setSourceTransactionId(String sourceTransactionId) {
		this.sourceTransactionId = sourceTransactionId;
	}
	public String getOrderHistId() {
		return orderHistId;
	}
	public void setOrderHistId(String orderHistId) {
		this.orderHistId = orderHistId;
	}
	public String getCartridgeName() {
		return cartridgeName;
	}
	public void setCartridgeName(String cartridgeName) {
		this.cartridgeName = cartridgeName;
	}
	public String getTaskName() {
		return taskName;
	}
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	public String getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}
	public String getStacktrace() {
		return stacktrace;
	}
	public void setStacktrace(String stacktrace) {
		this.stacktrace = stacktrace;
	}
	public String getPollStatus() {
		return pollStatus;
	}
	public void setPollStatus(String pollStatus) {
		this.pollStatus = pollStatus;
	}
	public String getErrorId() {
		return errorId;
	}
	public void setErrorId(String errorId) {
		this.errorId = errorId;
	}
	public String getInsertDateTime() {
		return insertDateTime;
	}
	public void setInsertDateTime(String insertDateTime) {
		this.insertDateTime = insertDateTime;
	}
	public String getInsertBy() {
		return insertBy;
	}
	public void setInsertBy(String insertBy) {
		this.insertBy = insertBy;
	}
	public String getUpdateDateTime() {
		return updateDateTime;
	}
	public void setUpdateDateTime(String updateDateTime) {
		this.updateDateTime = updateDateTime;
	}
	public String getUpdateBy() {
		return updateBy;
	}
	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}
	
	
	
}
