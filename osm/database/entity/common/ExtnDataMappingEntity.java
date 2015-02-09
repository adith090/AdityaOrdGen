package com.m1.sg.osm.database.entity.common;

public class ExtnDataMappingEntity {

	public static final String SQL_QUERY_BILL_EXTNDATA_MAP = "SQL_QUERY_BILL_EXTNDATA_MAP";

	private String mappingName;
	private String paramTag;
	private String paramC1Xpath;
	private String value;
	private String defaultValue;
	private String lookupRefId;
	private String seqNo;
	
	public String getMappingName() {
		return mappingName;
	}
	public void setMappingName(String mappingName) {
		this.mappingName = mappingName;
	}
	public String getparamTag() {
		return paramTag;
	}
	public void setParamTag(String paramTag) {
		this.paramTag = paramTag;
	}
	public String getParamC1Xpath() {
		return paramC1Xpath;
	}
	public void setParamC1Xpath(String paramC1Xpath) {
		this.paramC1Xpath = paramC1Xpath;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	public String getLookupRefId() {
		return lookupRefId;
	}
	public void setLookupRefId(String lookupRefId) {
		this.lookupRefId = lookupRefId;
	}
	public String getSeqNo() {
		return seqNo;
	}
	public void setSeqNo(String seqNo) {
		this.seqNo = seqNo;
	}
}
