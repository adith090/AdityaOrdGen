package com.m1.sg.osm.database.entity.common;

public class MappingEntity {

	public static final String SQL_QUERY_BILL_C1_MAP = "SQL_QUERY_BILL_C1_MAP";
	
	private String c1Tag;
	private String c1TagXpath;
	private String crmTagXpath;
	private String seqElement;
	private String elementGrouping;
	private String functionID;
	private String funcLookupRefId;
	private String defaultValue;
	private String attrLookupRefId;
	
	public String getC1Tag() {
		return c1Tag;
	}
	public void setC1Tag(String c1Tag) {
		this.c1Tag = c1Tag;
	}
	public String getC1TagXpath() {
		return c1TagXpath;
	}
	public void setC1TagXpath(String c1TagXpath) {
		this.c1TagXpath = c1TagXpath;
	}
	public String getCrmTagXpath() {
		return crmTagXpath;
	}
	public void setCrmTagXpath(String crmTagXpath) {
		this.crmTagXpath = crmTagXpath;
	}
	public String getSeqElement() {
		return seqElement;
	}
	public void setSeqElement(String seqElement) {
		this.seqElement = seqElement;
	}
	public String getElementGrouping() {
		return elementGrouping;
	}
	public void setElementGrouping(String elementGrouping) {
		this.elementGrouping = elementGrouping;
	}
	public String getFunctionID() {
		return functionID;
	}
	public void setFunctionID(String functionID) {
		this.functionID = functionID;
	}
	public String getFuncLookupRefId() {
		return funcLookupRefId;
	}
	public void setFuncLookupRefId(String funLookupRefId) {
		this.funcLookupRefId = funLookupRefId;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	public String getAttrLookupRefId() {
		return attrLookupRefId;
	}
	public void setAttrLookupRefId(String attrLookupRefId) {
		this.attrLookupRefId = attrLookupRefId;
	}
}
