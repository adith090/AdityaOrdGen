package com.m1.bcc.spl.model;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 11/04/2013					Ravikumar G				Bug#1901-Changed Request translator method using DB config flag
 * 														for search param tag or replace param tag Enhancement
 * 13/06/2013					Sudharsan  				Added getter setter for two variables dbParamType and dbParamMode
 * 06/11/2013					Ravikumar G				Bug#20397: added xml_par_ele_id 
 * 20/11/2013					Ravikumar G				Bug#20538 - Added timeout and read timeout
 * 28/11/2013					Ravikumar G				Bug#20592- Changed for DB Adaptor design change
 * 16/01/2014					Ravikumar G				Bug#23590 - added setter and getter for request_msg_flag
 ******************************************************************************/

public class CommandTransDetails {
	private String systemName;
	private String userName;
	private String pwd;
	private String cmdRefId;
	private String seqNo;
	private String paramName;
	private String paramValue;
	private String parameterValue;
	private String cmdName;
	private String techMethod;
	private String location;
	private String authTokenRequired;
	private String accountType;
	private String xmlEleType;
	private String xmlEleName;
	private String xmlEleId;
	private String xmlEleCurrValue;
	private String xmlElePrevValue;
	private String returnCode;
	private String returnMsg;
	private String interfaceName;
	private String parRowId;
	private String searchParamTag;
	private String replaceParamTag;
	private String dbParamType;
	private String dbParamMode;
	private String xmlParEleId;
	private String timeOut;
	private String readTimeOut;
	private String queryString;
	private String srcTransId;
	private String transId;
	private String requestMsgFlag;
	
	public String getParRowId() {
		return parRowId;
	}

	public void setParRowId(String parRowId) {
		this.parRowId = parRowId;
	}

	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getCmdRefId() {
		return cmdRefId;
	}

	public void setCmdRefId(String cmdRefId) {
		this.cmdRefId = cmdRefId;
	}

	public String getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(String seqNo) {
		this.seqNo = seqNo;
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

	public String getParameterValue() {
		return parameterValue;
	}

	public void setParameterValue(String parameterValue) {
		this.parameterValue = parameterValue;
	}

	public String getCmdName() {
		return cmdName;
	}

	public void setCmdName(String cmdName) {
		this.cmdName = cmdName;
	}

	public String getTechMethod() {
		return techMethod;
	}

	public void setTechMethod(String techMethod) {
		this.techMethod = techMethod;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getAuthTokenRequired() {
		return authTokenRequired;
	}

	public void setAuthTokenRequired(String authTokenRequired) {
		this.authTokenRequired = authTokenRequired;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getXmlEleType() {
		return xmlEleType;
	}

	public void setXmlEleType(String xmlEleType) {
		this.xmlEleType = xmlEleType;
	}

	public String getXmlEleName() {
		return xmlEleName;
	}

	public void setXmlEleName(String xmlEleName) {
		this.xmlEleName = xmlEleName;
	}

	public String getXmlEleId() {
		return xmlEleId;
	}

	public void setXmlEleId(String xmlEleId) {
		this.xmlEleId = xmlEleId;
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

	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	// Ravi: 20130411: Bug#1901: Added as part of search param tag or replace param tag Enhancement - Start
	public String getSearchParamTag() {
		return searchParamTag;
	}

	public void setSearchParamTag(String searchParamTag) {
		this.searchParamTag = searchParamTag;
	}

	public String getReplaceParamTag() {
		return replaceParamTag;
	}

	public void setReplaceParamTag(String replaceParamTag) {
		this.replaceParamTag = replaceParamTag;
	}

	public String getDbParamType() {
		return dbParamType;
	}

	public void setDbParamType(String dbParamType) {
		this.dbParamType = dbParamType;
	}

	public String getDbParamMode() {
		return dbParamMode;
	}

	public void setDbParamMode(String dbParamMode) {
		this.dbParamMode = dbParamMode;
	}

	public String getXmlParEleId() {
		return xmlParEleId;
	}

	public void setXmlParEleId(String xmlParEleId) {
		this.xmlParEleId = xmlParEleId;
	}

	public String getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(String timeOut) {
		this.timeOut = timeOut;
	}

	public String getReadTimeOut() {
		return readTimeOut;
	}

	public void setReadTimeOut(String readTimeOut) {
		this.readTimeOut = readTimeOut;
	}

	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public String getSrcTransId() {
		return srcTransId;
	}

	public void setSrcTransId(String srcTransId) {
		this.srcTransId = srcTransId;
	}

	public String getTransId() {
		return transId;
	}

	public void setTransId(String transId) {
		this.transId = transId;
	}

	public String getRequestMsgFlag() {
		return requestMsgFlag;
	}
	public void setRequestMsgFlag(String requestMsgFlag) {
		this.requestMsgFlag = requestMsgFlag;
	}
	
}
