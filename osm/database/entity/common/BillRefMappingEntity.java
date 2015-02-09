package com.m1.sg.osm.database.entity.common;

public class BillRefMappingEntity {

	public static final String SQL_QUERY_BILL_LOOKUP_REF_LOOKUPREFID = "SQL_QUERY_BILL_LOOKUP_REF_LOOKUPREFID"; // Gets entries based on the lookupRefID
	public static final String SQL_QUERY_BILL_LOOKUP_REF = "SQL_QUERY_BILL_LOOKUP_REF"; // Gets all the entries in the table
	
	private String lookupRefId;
	private String lookupValue;
	private String resultValue;
	
	public BillRefMappingEntity() {
	}
	
	public BillRefMappingEntity(String lookupRefId, String lookupValue) {
		this.lookupRefId = lookupRefId;
		this.lookupValue = lookupValue;
	}
	
	public BillRefMappingEntity(String lookupRefId) {
		this.lookupRefId = lookupRefId;
	}
	
	public String getLookupRefId() {
		return lookupRefId;
	}
	public void setLookupRefId(String lookupRefId) {
		this.lookupRefId = lookupRefId;
	}
	public String getLookupValue() {
		return lookupValue;
	}
	public void setLookupValue(String lookupValue) {
		this.lookupValue = lookupValue;
	}
	public String getResultValue() {
		return resultValue;
	}
	public void setResultValue(String resultValue) {
		this.resultValue = resultValue;
	}
}
