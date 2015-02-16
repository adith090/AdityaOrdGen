package com.m1.bcc.spl.instructor;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/11/2012					Viknesh				Created for getting values
 ******************************************************************************/
import java.math.BigDecimal;
import java.util.Date;

import javax.xml.xpath.XPath;

import org.w3c.dom.Document;

public class Variables {
	String row_id;
	String cmdRefId;
	String transId;
	String MOLI_ID;
	String OLI_ID;
	String SRC_ID;
	BigDecimal seqNo;
	String corrId;
	Date insertDt;
	String insertBy;
	Date updatedDt;String updatedBy;
	String parRowId;
	String paramName;
	String paramValue;
	String xmlEleType;
	String xmlEleId;
	String xmlEleName;
	String xmlEleCurrValue;
	String xmlElePrevValue;
	String lkupRefId;
	XPath xpath;
	Document doc;
	String order_Xml;
	String xPathVariable;
	String rowId;
	String status;
	String pollStatus;
	boolean concatenateFlag;

	public String getRow_id() {
		return row_id;
	}
	public void setRow_id(String row_id) {
		this.row_id = row_id;
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
	public String getMOLI_ID() {
		return MOLI_ID;
	}
	public void setMOLI_ID(String mOLI_ID) {
		MOLI_ID = mOLI_ID;
	}
	public String getOLI_ID() {
		return OLI_ID;
	}
	public void setOLI_ID(String oLI_ID) {
		OLI_ID = oLI_ID;
	}
	public String getSRC_ID() {
		return SRC_ID;
	}
	public void setSRC_ID(String sRC_ID) {
		SRC_ID = sRC_ID;
	}
	public BigDecimal getSeqNo() {
		return seqNo;
	}
	public void setSeqNo(BigDecimal seqNo) {
		this.seqNo = seqNo;
	}
	public String getCorrId() {
		return corrId;
	}
	public void setCorrId(String corrId) {
		this.corrId = corrId;
	}
	public Date getInsertDt() {
		return insertDt;
	}
	public void setInsertDt(Date insertDt) {
		this.insertDt = insertDt;
	}
	public String getInsertBy() {
		return insertBy;
	}
	public void setInsertBy(String insertBy) {
		this.insertBy = insertBy;
	}
	public Date getUpdatedDt() {
		return updatedDt;
	}
	public void setUpdatedDt(Date updatedDt) {
		this.updatedDt = updatedDt;
	}
	public String getUpdatedBy() {
		return updatedBy;
	}
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
	public String getParRowId() {
		return parRowId;
	}
	public void setParRowId(String parRowId) {
		this.parRowId = parRowId;
	}
	public String getParamName() {
		return paramName;
	}
	public void setParamName(String paramName) {
		this.paramName = paramName;
	}
	public String getParamValue() {
		return paramValue;
	}
	public void setParamValue(String paramValue) {
		this.paramValue = paramValue;
	}
	public String getXmlEleType() {
		return xmlEleType;
	}
	public void setXmlEleType(String xmlEleType) {
		this.xmlEleType = xmlEleType;
	}
	public String getXmlEleId() {
		return xmlEleId;
	}
	public void setXmlEleId(String xmlEleId) {
		this.xmlEleId = xmlEleId;
	}
	public String getXmlEleName() {
		return xmlEleName;
	}
	public void setXmlEleName(String xmlEleName) {
		this.xmlEleName = xmlEleName;
	}
	public String getXmlEleCurrValue() {
		return xmlEleCurrValue;
	}
	public void setXmlEleCurrValue(String xmlEleCurrValue) {
		this.xmlEleCurrValue = xmlEleCurrValue;
	}
	public String getXmlElePrevValue() {
		return xmlElePrevValue;
	}
	public void setXmlElePrevValue(String xmlElePrevValue) {
		this.xmlElePrevValue = xmlElePrevValue;
	}
	public String getLkupRefId() {
		return lkupRefId;
	}
	public void setLkupRefId(String lkupRefId) {
		this.lkupRefId = lkupRefId;
	}
	public XPath getXpath() {
		return xpath;
	}
	public void setXpath(XPath xpath) {
		this.xpath = xpath;
	}
	public Document getDoc() {
		return doc;
	}
	public void setDoc(Document doc) {
		this.doc = doc;
	}
	public String getOrder_Xml() {
		return order_Xml;
	}
	public void setOrder_Xml(String order_Xml) {
		this.order_Xml = order_Xml;
	}
	public String getxPathVariable() {
		return xPathVariable;
	}
	public void setxPathVariable(String xPathVariable) {
		this.xPathVariable = xPathVariable;
	}

	public String getRowId() {
		return rowId;
	}
	public void setRowId(String rowId) {
		this.rowId = rowId;
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
	public boolean isConcatenateFlag() {
		return concatenateFlag;
	}
	public void setConcatenateFlag(boolean concatenateFlag) {
		this.concatenateFlag = concatenateFlag;
	}


}
