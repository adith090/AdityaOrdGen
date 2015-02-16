package com.m1.sg.bcc.om.database.dao;

import javax.sql.DataSource;

import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.m1.sg.bcc.om.util.OMBeanFactory;
import com.m1.sg.bcc.om.util.OMTransformationJDBCTemplate;

/************************************************************************************************************************************
 * 
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.database.dao
 * Description: This class created for service JDBC Protocol framework base on spring framework properties the application use this
 * 				class for connecting to database. for the Database we use datasource in weblogic implementation
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMJDBCDao {
	
	private DataSourceTransactionManager crmDatasourceTransactionManager;
	private DataSourceTransactionManager omDatasourceTransactionManager;
	
	
	/**
	 * setter method use in spring properties for CRM database transaction manager
	 * 
	 * @param crmDatasourceTransactionManager	CRM database transaction manager
	 */
	public void setCRMDatasourceTransactionManager(
			DataSourceTransactionManager crmDatasourceTransactionManager) {
		this.crmDatasourceTransactionManager = crmDatasourceTransactionManager;
	}
	
	/**
	 * setter method use in spring properties for OM database transaction manager
	 * 
	 * @param omDatasourceTransactionManager	OM database transaction manager
	 */
	public void setOMDatasourceTransactionManager(
			DataSourceTransactionManager omDatasourceTransactionManager){
		this.omDatasourceTransactionManager = omDatasourceTransactionManager;
	}
	
	/**
	 * Getter for OM Data Source Object
	 * @return
	 */
	public DataSource getOMDataSource(){
		omDatasourceTransactionManager = (DataSourceTransactionManager) OMBeanFactory.getBean("OMTransactionManager");
		return this.omDatasourceTransactionManager.getDataSource();
	}
	
	
	/**
	 * Getter for CRM Data Source Object
	 * @return
	 */
	public DataSource getCRMDataSource(){
		crmDatasourceTransactionManager = (DataSourceTransactionManager) OMBeanFactory.getBean("CRMTransactionManager");
		return this.crmDatasourceTransactionManager.getDataSource();
	}
	/**
	 * getter method for CRM JDBC Template
	 * 
	 * @return	JdbcTemplate Object for CRM Datasource base on spring framework
	 */
	public OMTransformationJDBCTemplate getCRMJDBCTemplate(){
		crmDatasourceTransactionManager = (DataSourceTransactionManager) OMBeanFactory.getBean("CRMTransactionManager");
		return new OMTransformationJDBCTemplate(crmDatasourceTransactionManager.getDataSource());
	}
	
	public NamedParameterJdbcTemplate getCRMNamedParameterJdbcTemplate(){
		crmDatasourceTransactionManager = (DataSourceTransactionManager) OMBeanFactory.getBean("CRMTransactionManager");
		return new NamedParameterJdbcTemplate(crmDatasourceTransactionManager.getDataSource());
	}
	
	/**
	 * getter method for OM JDBC Template
	 * 
	 * @return	JdbcTemplate Object for OM Datasource base on spring framework
	 */
	public OMTransformationJDBCTemplate getOMJDBCTemplate(){
		omDatasourceTransactionManager = (DataSourceTransactionManager) OMBeanFactory.getBean("OMTransactionManager");
		return new OMTransformationJDBCTemplate(omDatasourceTransactionManager.getDataSource());
	}
	
	
	/**
	 * JDBC Type Mapping from Java Object to JDBC Type we use StatementCreatorUtils in
	 * core spring framework for mapping each type
	 * 
	 * @param params	Object parameter pass to bind variable in SQL Statement 
	 * @return			JDBC Type for each parameter
	 */
	public static int[] jdbcTypeMapping(Object[] params){
		int[] typeList = new int[params.length];
		for(int i=0; i < params.length; i++){
			typeList[i] = StatementCreatorUtils.javaTypeToSqlParameterType(params[i].getClass());
		}
		return typeList;
	}
	
}
