package com.m1.bcc.spl.instructor;

import java.math.BigDecimal;

public class HandlerVariables {

	String row_ID;
	String trans_Id;
	String orderTransRowId;
	String status;
	String corr_Id;
	String orderXml;
	String xPathVariable;
	String xmlEleType;
	String paramName;
	String paramSub;
	String paramValue;
	String lookupRefId;
	String defaultValue;	
	String parRowId;
	String pollStatus;
	BigDecimal seqNo;
	String xmlEleId;
	String xmlEleName;
	String XmlElePrevValue;
	String XmlEleCurrValue;
	String funcRefId;
	String opgRefId;
	String commndRefId;
	String append;
	String prepend;
	String srcTransId;
	String offerId;
	
	String xmlParEleId;
	
	//M.rahman: M1 Phase 2: Adding parameters required for Billing order processing//
	String externalIDList;

	
	public String getSrcTransId() {
		return srcTransId;
	}
	public void setSrcTransId(String srcTransId) {
		this.srcTransId = srcTransId;
	}
	public String getAppend() {
		return append;
	}
	public void setAppend(String append) {
		this.append = append;
	}
	public String getPrepend() {
		return prepend;
	}
	public void setPrepend(String prepend) {
		this.prepend = prepend;
	}
	public BigDecimal getSeqNo() {
		return seqNo;
	}
	public void setSeqNo(BigDecimal seqNo) {
		this.seqNo = seqNo;
	}
	public String getParRowId() {
		return parRowId;
	}
	public void setParRowId(String parRowId) {
		this.parRowId = parRowId;
	}
	public String getPollStatus() {
		return pollStatus;
	}
	public void setPollStatus(String pollStatus) {
		this.pollStatus = pollStatus;
	}
	public String getParamValue() {
		return paramValue;
	}
	public void setParamValue(String paramValue) {
		this.paramValue = paramValue;
	}
	public String getParamSub() {
		return paramSub;
	}
	public void setParamSub(String paramSub) {
		this.paramSub = paramSub;
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
	public String getXmlEleType() {
		return xmlEleType;
	}
	public String getParamName() {
		return paramName;
	}
	public void setParamName(String paramName) {
		this.paramName = paramName;
	}
	public void setXmlEleType(String xmlEleType) {
		this.xmlEleType = xmlEleType;
	}
	public String getxPathVariable() {
		return xPathVariable;
	}
	public void setxPathVariable(String xPathVariable) {
		this.xPathVariable = xPathVariable;
	}
	public String getOrderXml() {
		return orderXml;
	}
	public void setOrderXml(String orderXml) {
		this.orderXml = orderXml;
	}
	public String getRow_ID() {
		return row_ID;
	}
	public void setRow_ID(String row_ID) {
		this.row_ID = row_ID;
	}
	public String getTrans_Id() {
		return trans_Id;
	}
	public void setTrans_Id(String trans_Id) {
		this.trans_Id = trans_Id;
	}
	public String getOrderTransRowId() {
		return orderTransRowId;
	}
	public void setOrderTransRowId(String orderTransRowId) {
		this.orderTransRowId = orderTransRowId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getCorr_Id() {
		return corr_Id;
	}
	public void setCorr_Id(String corr_Id) {
		this.corr_Id = corr_Id;
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
	public String getCommndRefId() {
		return commndRefId;
	}
	public void setCommndRefId(String commndRefId) {
		this.commndRefId = commndRefId;
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
	public String getXmlElePrevValue() {
		return XmlElePrevValue;
	}
	public void setXmlElePrevValue(String xmlElePrevValue) {
		XmlElePrevValue = xmlElePrevValue;
	}
	public String getXmlEleCurrValue() {
		return XmlEleCurrValue;
	}
	public void setXmlEleCurrValue(String xmlEleCurrValue) {
		XmlEleCurrValue = xmlEleCurrValue;
	}
	//M.Rahman: M1 Phase 2: Adding additional paramters for Billing order processing//

	public void setExternalIDList(String externalIDList) {
		this.externalIDList = externalIDList;
	}
	
	public String getExternalIDList() {
		return externalIDList;
	}
	public String getOfferId() {
		return offerId;
	}
	public void setOfferId(String offerId) {
		this.offerId = offerId;
	}
	public String getXmlParEleId() {
		return xmlParEleId;
	}
	public void setXmlParEleId(String xmlParEleId) {
		this.xmlParEleId = xmlParEleId;
	}
	
}
