package com.m1.sg.bcc.om.entity;

import java.util.List;
import java.util.Map;

/************************************************************************************************************************************
 *  
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.entity
 * Description: Entity class or Object store the application value processing retrieve from database or configuration file 
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class MappingElementEntity {

	/**
	 * attribute from table c_om_gen_sql_map
	 */
	private String sqlStatementId;
	private String sqlStatement;
	private String[] sqlKey;
	private String[] nextSqlKey;
	private String sqlDependency;
	
	/**
	 * rowSet from table c_om_gen_element_map
	 */
	private List<Map<String, Object>> mappingElementDetails;
	
	public String getSqlStatementId() {
		return sqlStatementId;
	}
	public void setSqlStatementId(String sqlStatementId) {
		this.sqlStatementId = sqlStatementId;
	}
	public String[] getSqlKey() {
		return sqlKey;
	}
	public void setSqlKey(String[] sqlKey) {
		this.sqlKey = sqlKey;
	}
	public String getSqlStatement() {
		return sqlStatement;
	}
	public void setSqlStatement(String sqlStatement) {
		this.sqlStatement = sqlStatement;
	}
	public List<Map<String, Object>> getMappingElementDetails() {
		return mappingElementDetails;
	}
	public void setMappingElementDetails(
			List<Map<String, Object>> mappingElementDetails) {
		this.mappingElementDetails = mappingElementDetails;
	}
	public String[] getNextSqlKey() {
		return nextSqlKey;
	}
	public void setNextSqlKey(String[] nextSqlKey) {
		this.nextSqlKey = nextSqlKey;
	}
	public String getSqlDependency() {
		return sqlDependency;
	}
	public void setSqlDependency(String sqlDependency) {
		this.sqlDependency = sqlDependency;
	}
	
}
