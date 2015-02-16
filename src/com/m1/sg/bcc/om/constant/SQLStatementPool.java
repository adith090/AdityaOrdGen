package com.m1.sg.bcc.om.constant;

/************************************************************************************************************************************
 * 
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.constant
 * Description: This class create for store the SQL statement use in out application
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 15/05/2013		Apiluck Songwattanasakul		Initial Class and implement the logic inside
 * 22/05/2013		Apiluck Songwattanasakul		Add SQL Statement for Update Heart beat time for poller		
 ************************************************************************************************************************************/

public class SQLStatementPool {

	/**
	 * SQL Statement for query poller configure
	 */
	public static final String SQL_SELECT_POLLER_CONFIG = "select poller_id, poller_name, time_int, time_unit, max_rows_per_poll from c_om_poll_int where poller_id = ?";
	
	/**
	 * SQL Statement retrieve the SQL details List 
	 */
	public static final String SQL_SELECT_SQL_LIST = "select sql_stat_id, sql_stat, sql_key, sql_dependency, next_sql_key from c_om_gen_sql_map where order_type = ? and sql_stat_id in(select DISTINCT(sql_stat_id) from c_om_gen_element_map) order by sql_seq";
	
	/**
	 * SQL Statement retrieve the element XML configuration
	 */
	public static final String SQL_SELECT_ELEMENT_IN_SQL_ID = "select element_name, xml_path, from_table_name, from_column_name, sql_value_mapping, element_data_type, datetime_format_id from c_om_gen_element_map where sql_stat_id = ? order by seq_element";
	
	/**
	 * SQL Statement retrieve the dependency of each SQL_ID
	 */
	public static final String SQL_SELECT_SQL_DEPENDENCY = "select sql_stat_id from c_om_gen_sql_map where sql_dependency = ? order by sql_seq";

	/**
	 * SQL Statement mapping date time format
	 */
	public static final String SQL_SELECT_CONVERT_DATETIME_FORMAT = "select key_value as datetime_format from c_om_gen_value_map where key_ref_id = 4000 and key_mapping = ?";
	
	/**
	 * SQL Statement for Update Heart beat time for Poller
	 */
	public static final String SQL_UPDATE_HEARTBEAT_TIME = "update c_om_poll_int set last_poll_time = ? where poller_id = ?";
	
	/**
	 * SQL Statement for Update CRM Table T_ORDER_HEADER after send the message into the queue update the status to completed
	 */
	
	public static final String SQL_UPDATE_CREATION_STATUS_CRM = "update {0} set order_status_cd = {1}, last_updated_dt=sysdate, last_updated_by=''OMOrderGenerator'' where order_id = ?";
	
	public static final String SQL_UPDATE_COMPLETE_STATUS_CRM = "update {0} set order_status_cd = {1}, last_updated_dt=sysdate, last_updated_by=''OMOrderGenerator'' where order_id = ?";
	public static final String SQL_UPDATE_ERROR_STATUS_CRM = "update {0} set order_status_cd = {1}, last_updated_dt=sysdate, last_updated_by=''OMOrderGenerator'' where order_id = ?";
	
	/**
	 * SQL Statement select Order Type as distinct
	 */
	public static final String SQL_SELECT_ORDER_TYPE_DISTINCT = "select distinct(order_type) from c_om_gen_sql_map";
	
	/**
	 * SQL Statement select Namespace Mapping submit order to OSM Cartridge
	 */
	public static final String SQL_SELECT_NAMESPACE_MAPPING = "select key_mapping, key_value from c_om_gen_value_map where key_ref_id = 3001";
	public static final String SQL_SELECT_TYPE_TABLE_MAPPING = "select key_mapping, key_value from c_om_gen_value_map where key_ref_id = 3002";
	
	/**
	 * SQL Statement select for initial filtering condition when we populate order line item
	 */
	public static final String SQL_SELECT_FILTER_CONDITION = "select key_value from c_om_gen_value_map where key_ref_id = 5000";
	
	/**
	 * SQL Statement select order status List
	 */
	public static final String SQL_SELECT_ORDER_STATUS_LIST = "select key_value from c_om_gen_value_map where key_ref_Id = 2000 order by key_mapping";
	
	/**
	 * SQL Statement select Transformation List
	 */
	public static final String SQL_SELECT_TRANSFORMATION_POST_QUERY_LIST = "select key_mapping, key_value from c_om_gen_value_map where key_ref_Id = 6000 order by key_mapping";
	public static final String SQL_SELECT_TRANSFORMATION_PRE_QUERY_LIST = "select key_mapping, key_value from c_om_gen_value_map where key_ref_Id = 6001 order by key_mapping";
	
	/**
	 * SQL Statement select Resource Ref ID Mapping
	 */
	public static final String SQL_SELECT_RESOURCE_REF_ID_MAPPING = "select * from c_resource_property_mapping";
	
	/**
	 * SQL Statement Select Default Value for Tag Name 
	 */
	public static final String SQL_SELECT_DEFAULT_TAG_NAME = "select key_mapping, key_value from c_om_gen_value_map where key_ref_Id = 7000 order by key_mapping";
}
