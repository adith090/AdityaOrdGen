package com.m1.sg.osm.database.entity.common;

public class OSMTransactionEntity {

	public static final String SQL_INSERT_OSM_TRANS = "SQL_INSERT_OSM_TRANS";
	public static final String SQL_UPDATE_STATUS_OSM_TRANS = "SQL_UPDATE_STATUS_OSM_TRANS";
	public static final String SQL_UPDATE_REVISION_OSM_TRANS = "SQL_UPDATE_REVISION_OSM_TRANS";
	public static final String SQL_SWAP_STATUS_OSM_TRANS = "SQL_SWAP_STATUS_OSM_TRANS";
	public static final String SQL_RETRIEVE_ORIGINAL_ORDER_ID = "SQL_RETRIEVE_ORIGINAL_ORDER_ID";
	public static final String SQL_UPDATE_PAR_RECV_FLAG_TRANS = "SQL_UPDATE_PAR_RECV_FLAG_TRANS";
	
	private String orderId;
	private String sourceTransactionId;
	private String transactionType;
	private String orderLineItemId;
	private String mainOrderLineItemId;
	private String previousStatus;
	private String status;
	private String pollStatus;
	private String insertDateTime;
	private String insertBy;
	private String updateDateTime;
	private String updateBy;
	private String revision;
	private String parentTransactionId;
	private String opCoFlag;
	private String crmOrderId;
	
	public String getOrderId() {
		return orderId;
	}
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	public String getSourceTransactionId() {
		return sourceTransactionId;
	}
	public void setSourceTransactionId(String sourceTransactionId) {
		this.sourceTransactionId = sourceTransactionId;
	}
	public String getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}
	public String getOrderLineItemId() {
		return orderLineItemId;
	}
	public void setOrderLineItemId(String orderLineItemId) {
		this.orderLineItemId = orderLineItemId;
	}
	public String getMainOrderLineItemId() {
		return mainOrderLineItemId;
	}
	public void setMainOrderLineItemId(String mainOrderLineItemId) {
		this.mainOrderLineItemId = mainOrderLineItemId;
	}
	public String getPreviousStatus() {
		return previousStatus;
	}
	public void setPreviousStatus(String previousStatus) {
		this.previousStatus = previousStatus;
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
	public String getRevision() {
		return revision;
	}
	public void setRevision(String revision) {
		this.revision = revision;
	}
	public String getParentTransactionId() {
		return parentTransactionId;
	}
	public void setParentTransactionId(String parentTransactionId) {
		this.parentTransactionId = parentTransactionId;
	}
	public String getOpCoFlag() {
		return opCoFlag;
	}
	public void setOpCoFlag(String opCoFlag) {
		this.opCoFlag = opCoFlag;
	}
	public String getCrmOrderId() {
		return crmOrderId;
	}
	public void setCrmOrderId(String crmOrderId) {
		this.crmOrderId = crmOrderId;
	}
}
