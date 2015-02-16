package com.m1.sg.bcc.om.constant;

/************************************************************************************************************************************
 * 
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.constant
 * Description: This class use for define constant call by all java code in the project  
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 15/05/2013		Apiluck Songwattanasakul		Initial Class and implement the logic inside				
 ************************************************************************************************************************************/

public class OMPollerConstant {
	
	/**
	 * 
	 * Column Name Constant for table C_OM_GEN_SQL_MAP
	 *
	 */
	public static enum SqlIdAttributeName{
		SQL_STAT_ID("SQL_STAT_ID"), SQL_KEY("SQL_KEY"), SQL_STAT("SQL_STAT"),
		NEXT_SQL_KEY("NEXT_SQL_KEY"), SQL_DEPENDENCY("SQL_DEPENDENCY"), ORDER_TYPE("ORDER_TYPE");
		private final String name;
		private SqlIdAttributeName(String name) {
			this.name = name;
		}
		public String getValue() {
			return this.name;
		}
	};
	
	/**
	 * 
	 * Column Name Constant for table C_OM_GEN_ELEMENT_MAP
	 *
	 */
	public static enum FieldMappingAttributeName{
		ELEMENT_NAME ("ELEMENT_NAME"), XML_PATH ("XML_PATH"), FROM_TABLE_NAME("FROM_TABLE_NAME"),
		FROM_COLUMN_NAME("FROM_COLUMN_NAME"), SEQ_ELEMENT("SEQ_ELEMENT"), SQL_STAT_ID("SQL_STAT_ID"),
		SQL_VALUE_MAPPING("SQL_VALUE_MAPPING"), ELEMENT_DATA_TYPE("ELEMENT_DATA_TYPE"), DATETIME_FORMAT_ID("DATETIME_FORMAT_ID");
		private final String name;
		private FieldMappingAttributeName(String name) {
			this.name = name;
		}
		public String getValue() {
			return this.name;
		}
	};
	
	/**
	 * 
	 * Constant for the data type define for the XML element
	 *
	 */
	public static enum ElementDataType{
		STRING("STRING"), DATETIME("DATETIME"), INTEGER("INTEGER");
		private final String name;
		private ElementDataType(String name){
			this.name = name;
		}
		public String getValue(){
			return this.name;
		}
	};
	
	/**
	 * 
	 * Constant define for the properties attribute use for poller configure
	 *
	 */
	public static enum PropertiesAttribute{
		HIGH_PRIORITY_DEFAULT_TIME_INTERVAL("HIGH_PRIORITY_DEFAULT_TIME_INTERVAL"),
		MEDIUM_PRIORITY_DEFAULT_TIME_INTERVAL("MEDIUM_PRIORITY_DEFAULT_TIME_INTERVAL"),
		LOW_PRIORITY_DEFAULT_TIME_INTERVAL("LOW_PRIORITY_DEFAULT_TIME_INTERVAL"),
		OPCO_DEFAULT_TIME_INTERVAL("OPCO_DEFAULT_TIME_INTERVAL"),
		TECHREQ_DEFAULT_TIME_INTERVAL("TECHREQ_DEFAULT_TIME_INTERVAL"),
		HIGH_PRIORITY_FETCH_SIZE("HIGH_PRIORITY_FETCH_SIZE"),
		MEDIUM_PRIORITY_FETCH_SIZE("MEDIUM_PRIORITY_FETCH_SIZE"),
		OPCO_FETCH_SIZE("OPCO_FETCH_SIZE"),
		TECHREQ_FETCH_SIZE("TECHREQ_FETCH_SIZE"),
		LOW_PRIORITY_FETCH_SIZE("LOW_PRIORITY_FETCH_SIZE");
		private final String name;
		private PropertiesAttribute(String name) {
			this.name = name;
		}
		public String getValue(){
			return this.name;
		}
	};
	
	/**
	 * 
	 * Constant for configure the JMS properties
	 *
	 */
	public static enum JMSProperties{
		JMS_TYPE("OrderGenerator"), PRIORITY("5"), LIMIT("false");
		private final String name;
		private JMSProperties(String name){
			this.name = name;
		}
		public String getValue(){
			return this.name;
		}
	};
	
	/**
	 * 
	 * Column Name Constant for table C_OM_POLL_INT
	 * 
	 */
	public static enum PollerConfigurationSPLColumnName {
		POLLER_ID("POLLER_ID"), POLLER_NAME("POLLER_NAME"), TIME_INT("TIME_INT"), TIME_UNIT("TIME_UNIT")
		, MAX_ROWS_PER_POLL("MAX_ROWS_PER_POLL"), LAST_POLL_TIME("LAST_POLL_TIME");
		private final String name;
		private PollerConfigurationSPLColumnName(String name){
			this.name = name;
		}
		public String getValue(){
			return this.name;
		}
	};
	
	/**
	 * 
	 * Poller ID List
	 * 
	 */
	public static enum OMOrderGeneratorPollerID{
		OMOrderGeneratorHighPriorityPoller("OMOrderGeneratorHighPriorityPoller"),
		OMOrderGeneratorMediumPriorityPoller("OMOrderGeneratorMediumPriorityPoller"),
		OMOrderGeneratorLowPriorityPoller("OMOrderGeneratorLowPriorityPoller"),
		OMOrderGeneratorOpcoPoller("OMOrderGeneratorOpcoPoller"),
		OMOrderGeneratorTechReqPoller("OMOrderGeneratorTechReqPoller");
		private final String name;
		private OMOrderGeneratorPollerID(String name){
			this.name = name;
		}
		public String getValue(){
			return this.name;
		}
	};
	
	/**
	 * 
	 * Column Name for T_ORDER_HEADER
	 * 
	 */
	public static enum OrderHeaderColumnName{
		ORDER_ID("ORDER_ID"),
		ORDER_STATUS("ORDER_STATUS"),
		ORDER_TYPE("ORDER_TYPE"),
		REVISION_NUMBER("REVISION_NUMBER"),
		ORDER_SOURCE("ORDER_SOURCE"),
		PRIORITY("PRIORITY"),
		PARENT_ORDER_ID("PARENT_ORDER_ID");
		private final String name;
		private OrderHeaderColumnName(String name){
			this.name = name;
		}
		public String getValue(){
			return this.name;
		}
	};
	
	public static enum LineItemColumnName{
		PARENT_ORDER_LINE_ITEM_ID("PARENT_LINE_ITEM_ID"),
		ORDER_LINE_ITEM_ID("ORDER_LINE_ITEM_ID");
		private final String name;
		private LineItemColumnName(String name){
			this.name = name;
		}
		public String getValue(){
			return this.name;
		}
	};
	
	public static enum KeyMappingAttribute {
		KEY_MAPPING("KEY_MAPPING"),
		KEY_VALUE("KEY_VALUE");
		private final String name;
		private KeyMappingAttribute(String name){
			this.name = name;
		}
		public String getValue(){
			return this.name;
		}
	};
}
