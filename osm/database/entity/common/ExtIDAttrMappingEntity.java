package com.m1.sg.osm.database.entity.common;

public class ExtIDAttrMappingEntity {

	public static final String SQL_QUERY_BILL_EXTID_ATTR_MAP = "SQL_QUERY_BILL_EXTID_ATTR_MAP";
	
	private String attrName;
	private String extIDType;
	private String crmXpath;
	private String c1Xpath;
	private String opgRefID;
	
	public String getAttrName() {
		return attrName;
	}
	public void setAttrName(String attrName) {
		this.attrName = attrName;
	}
	public String getExtIDType() {
		return extIDType;
	}
	public void setExtIDType(String extIDType) {
		this.extIDType = extIDType;
	}
	public String getCrmXpath() {
		return crmXpath;
	}
	public void setCrmXpath(String crmXpath) {
		this.crmXpath = crmXpath;
	}
	public String getC1Xpath() {
		return c1Xpath;
	}
	public void setC1Xpath(String c1Xpath) {
		this.c1Xpath = c1Xpath;
	}
	public String getOpgRefID() {
		return opgRefID;
	}
	public void setOpgRefID(String opgRefID) {
		this.opgRefID = opgRefID;
	}
}
