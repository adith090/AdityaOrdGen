package com.m1.sg.bcc.om.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jndi.JndiTemplate;

import com.m1.sg.bcc.om.constant.OMPollerConstant;
import com.m1.sg.bcc.om.constant.OMPollerLoggerConstant;
import com.m1.sg.bcc.om.constant.SQLStatementPool;
import com.m1.sg.bcc.om.database.dao.OMJDBCDao;
import com.m1.sg.bcc.om.entity.MappingElementEntity;
import com.m1.sg.bcc.om.entity.PollerConfigurationObject;
import com.m1.sg.bcc.om.logger.OMPollerLogger;
import com.m1.sg.bcc.om.util.OMBeanFactory;
import com.m1.sg.bcc.om.util.OMPollerValueConverter;

/************************************************************************************************************************************
 * 
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.config
 * Description: This class use for load initial configuration from the table, properties file 
 * 				and create under the format we want
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 15/05/2013		Apiluck Songwattanasakul		Initial Class and implement the logic inside				
 ************************************************************************************************************************************/
public class OMPollerConfigurationLoader {
	
	/**
	 *	Properties store all properties configuration load by spring framework
	 */
	private static Properties pollerPropertiesConfiguration;
	private static ConcurrentHashMap<String, String> namespaceMapping = new ConcurrentHashMap<String, String>();
	private static ConcurrentHashMap<String, String> orderTypeTableName = new ConcurrentHashMap<String, String>();
	private static ConcurrentHashMap<String, PollerConfigurationObject> configurationMapping = new ConcurrentHashMap<String, PollerConfigurationObject>();
	private static ArrayList<String> orderStatusList;
	
	private static ArrayList<String> rowSetOfCondition = new ArrayList<String>();
	private static ConcurrentHashMap<String, String> postQueryTransformationMapping;
	private static ConcurrentHashMap<String, String> preQueryTransformationMapping;
	private static ConcurrentHashMap<String, String> tagNameDefaultValueMapping;
	
	private static String resourceTypeMappingXML;
	
	static {
		
		try {
			
			Class.forName("oracle.jdbc.OracleDriver");
			
			System.out.println("Load JNDI Initial Context .......................");
			JndiTemplate jndiTemplate = (JndiTemplate) OMBeanFactory.getBean("jndiTemplate");
			jndiTemplate.getContext();
			
			System.out.println("Load Poller Properties Configuration ..........");
			pollerPropertiesConfiguration = (Properties) OMBeanFactory.getBean("PropertiesList");
			
			System.out.println("Load Element XML Mapping ......................");
			generateMappingElementXML();
			loadNamespaceMapping();
			loadMappingOrderTypeTableName();
			
			System.out.println("Load Order Line Item Filter Condition ............");
			loadFilterCondition();
			System.out.println("Number Of the condition -> " + rowSetOfCondition.size());
			
			System.out.println("Load Order Status ...");
			orderStatusList = getOrderStatus();
			System.out.println("Number Of the Status -> " + orderStatusList.size());
			
			System.out.println("Load Post Query Transformation ..........");
			postQueryTransformationMapping = getQueryTransformation(SQLStatementPool.SQL_SELECT_TRANSFORMATION_POST_QUERY_LIST);
			System.out.println("Number Of Post Query Tranformation -> " + postQueryTransformationMapping.size());
			
			System.out.println("Load Default Value Mapping ..............");
			tagNameDefaultValueMapping = getQueryTransformation(SQLStatementPool.SQL_SELECT_DEFAULT_TAG_NAME);
			System.out.println("Number Of Tag Name Mapping -> " + tagNameDefaultValueMapping.size());
			
			System.out.println("Load Pre Query Transformation ...........");
			preQueryTransformationMapping = getQueryTransformation(SQLStatementPool.SQL_SELECT_TRANSFORMATION_PRE_QUERY_LIST);
			System.out.println("Number Of Post Query Tranformation -> " + preQueryTransformationMapping.size());
			
			System.out.println("Load Resource Ref ID Mapping ..........");
			resourceTypeMappingXML = initialResourceRefIDMapping();
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, "Load Start UP Configuration", new String[]{"Resource Ref ID Mapping = ", resourceTypeMappingXML});
			
			System.out.println("Finish Load Configuration Application Started ...");
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static ArrayList<String> getOrderStatus(){
		ArrayList<String> statusList = new ArrayList<String>();
		OMJDBCDao jdbcDao = new OMJDBCDao();
		JdbcTemplate omTemplete = jdbcDao.getOMJDBCTemplate();
		List<Map<String, Object>> orderTypeList = omTemplete.queryForList(SQLStatementPool.SQL_SELECT_ORDER_STATUS_LIST);
		for(Map<String, Object> eachOfRecord : orderTypeList){
			statusList.add(String.valueOf(eachOfRecord.get(OMPollerConstant.KeyMappingAttribute.KEY_VALUE.getValue())));
		}
		return statusList;
	}
	
	/**
	 * @return	getter method return Properties from configuration
	 */
	public static Properties getPollerPropertiesConfiguration(){
		return pollerPropertiesConfiguration;
	}
	
	/**
	 * @return	getter method return ArrayList store SQL information
	 */
	public static ArrayList<MappingElementEntity> getMappingElementList(String orderType){
		return configurationMapping.get(orderType).getMapElementList();
	}
	
	/**
	 * @return	getter method return Mapping between SQL_ID and SQL information
	 */
	public static Map<String, ArrayList<String>> getMappingSqlDependency(String orderType){
		return configurationMapping.get(orderType).getMappingSqlDependency();
	}
	
	/**
	 * @return	getter method return Mapping between SQL_ID and SQL Dependency
	 */
	public static Map<String, MappingElementEntity> getMappingSqlDetails(String orderType){
		return configurationMapping.get(orderType).getMappingSqlDetails();
	}
	
	/**
	 * @return	getter method return ArrayList store SQL_ID has sql depend on this SQL_ID
	 */
	public static ArrayList<String> getAllDependencyList(String orderType){
		return configurationMapping.get(orderType).getAllDependencySqlId();
	}
	
	public static ConcurrentHashMap<String, String> getNamespaceMapping(){
		return namespaceMapping;
	}
	
	public static ConcurrentHashMap<String, String> getOrderTypeTableMapping(){
		return orderTypeTableName;
	}
	
	public static ArrayList<String> getFilterConditionList(){
		return rowSetOfCondition;
	}
	
	public static ConcurrentHashMap<String, String> getPostQueryTranformationMapping(){
		return postQueryTransformationMapping;
	}
	
	public static ConcurrentHashMap<String, String> getPreQueryTransformationMapping(){
		return preQueryTransformationMapping;
	}
	
	public static String getResourceRefIDMapping(){
		return resourceTypeMappingXML;
	}
	
	public static ConcurrentHashMap<String, String> getTagNameDefaultMapping(){
		return tagNameDefaultValueMapping;
	}
	
	/**
	 * Load Filter Condition
	 */
	private static void loadFilterCondition(){
		
		OMJDBCDao jdbcDao = new OMJDBCDao();
		JdbcTemplate omTemplete = jdbcDao.getOMJDBCTemplate();
		
		List<Map<String, Object>> conditionList = omTemplete.queryForList(SQLStatementPool.SQL_SELECT_FILTER_CONDITION);
		for(Map<String, Object> eachOfCondition : conditionList){
			Object condition = eachOfCondition.get(OMPollerConstant.KeyMappingAttribute.KEY_VALUE.getValue());
			rowSetOfCondition.add(String.valueOf(condition));
		}
		
	}
	 
	/**
	 * Generate all element and load initial configuration 
	 * 
	 * @return	
	 */
	private static void generateMappingElementXML(){
		
		OMJDBCDao jdbcDao = new OMJDBCDao();
		JdbcTemplate omTemplete = jdbcDao.getOMJDBCTemplate();
		
		List<Map<String, Object>> orderTypeList = omTemplete.queryForList(SQLStatementPool.SQL_SELECT_ORDER_TYPE_DISTINCT);
		
		for(Map<String, Object> eachOfOrderType : orderTypeList){
		
			String orderType = String.valueOf(eachOfOrderType.get(OMPollerConstant.SqlIdAttributeName.ORDER_TYPE.getValue()));
			
			List<Map<String, Object>> sqlDetails = omTemplete.queryForList(SQLStatementPool.SQL_SELECT_SQL_LIST, 
					new Object[]{orderType});
			
			PollerConfigurationObject pollerConfigurationObject = new PollerConfigurationObject();
			
			ArrayList<String> allDependencySqlId = new ArrayList<String>();
			Map<String, ArrayList<String>> mappingSqlDependency = Collections.synchronizedMap(new LinkedHashMap<String, ArrayList<String>>());
			ArrayList<MappingElementEntity> mapElementList = new ArrayList<MappingElementEntity>();
			Map<String, MappingElementEntity> mappingSqlDetails = Collections.synchronizedMap(new LinkedHashMap<String, MappingElementEntity>());
			
			for(Map<String, Object> eachOfSqlDetailsRow : sqlDetails){
				
				MappingElementEntity mee = new MappingElementEntity();
				mee.setSqlStatement(String.valueOf(eachOfSqlDetailsRow.get(OMPollerConstant.SqlIdAttributeName.SQL_STAT.getValue())));
				
				String sqlId = String.valueOf(eachOfSqlDetailsRow.get(OMPollerConstant.SqlIdAttributeName.SQL_STAT_ID.getValue()));
				String sqlKey = String.valueOf(eachOfSqlDetailsRow.get(OMPollerConstant.SqlIdAttributeName.SQL_KEY.getValue()));
				String nextSqlKey = String.valueOf(eachOfSqlDetailsRow.get(OMPollerConstant.SqlIdAttributeName.NEXT_SQL_KEY.getValue()));
				
				List<Map<String, Object>> dependencyList = omTemplete.queryForList(SQLStatementPool.SQL_SELECT_SQL_DEPENDENCY, new Object[]{sqlId});
				ArrayList<String> sqlIdDependencyList = new ArrayList<String>();
				for(Map<String, Object> eachOfDependency : dependencyList){
					sqlIdDependencyList.add(String.valueOf(eachOfDependency.get(OMPollerConstant.SqlIdAttributeName.SQL_STAT_ID.getValue())));
				}
				allDependencySqlId.addAll(sqlIdDependencyList);
				mappingSqlDependency.put(sqlId, sqlIdDependencyList);
				mee.setSqlKey(sqlKey.split("\\|"));
				mee.setSqlStatementId(sqlId);
				mee.setNextSqlKey(nextSqlKey.split("\\|"));
				mee.setSqlDependency(String.valueOf(eachOfSqlDetailsRow.get(OMPollerConstant.SqlIdAttributeName.SQL_DEPENDENCY.getValue())));
				
				List<Map<String, Object>> elementCreated = omTemplete.queryForList(SQLStatementPool.SQL_SELECT_ELEMENT_IN_SQL_ID, new Object[]{sqlId});
				mee.setMappingElementDetails(elementCreated);
				
				mapElementList.add(mee);
				
				mappingSqlDetails.put(sqlId, mee);
			}
			
			pollerConfigurationObject.setAllDependencySqlId(allDependencySqlId);
			pollerConfigurationObject.setMapElementList(mapElementList);
			pollerConfigurationObject.setMappingSqlDependency(mappingSqlDependency);
			pollerConfigurationObject.setMappingSqlDetails(mappingSqlDetails);
			
			configurationMapping.put(orderType, pollerConfigurationObject);
			
		}
		
	}
	
	/**
	 * Load Namespace Mapping contains the number of COM Cartridge
	 */
	
	private static void loadNamespaceMapping(){
		OMJDBCDao jdbcDao = new OMJDBCDao();
		JdbcTemplate omTemplete = jdbcDao.getOMJDBCTemplate();
		List<Map<String, Object>> namespaceMappingResult = omTemplete.queryForList(SQLStatementPool.SQL_SELECT_NAMESPACE_MAPPING);
		for(Map<String, Object> eachOfRow : namespaceMappingResult){
			String key = String.valueOf(eachOfRow.get(OMPollerConstant.KeyMappingAttribute.KEY_MAPPING.getValue()));
			String value = String.valueOf(eachOfRow.get(OMPollerConstant.KeyMappingAttribute.KEY_VALUE.getValue()));
			namespaceMapping.put(key, value);
		}
	}
	
	/**
	 * Load Mapping Order Type separated the product Order or Opco
	 */
	
	private static void loadMappingOrderTypeTableName(){
		OMJDBCDao jdbcDao = new OMJDBCDao();
		JdbcTemplate omTemplete = jdbcDao.getOMJDBCTemplate();
		List<Map<String, Object>> typeTableMapping = omTemplete.queryForList(SQLStatementPool.SQL_SELECT_TYPE_TABLE_MAPPING);
		for(Map<String, Object> eachOfRow : typeTableMapping){
			String key = String.valueOf(eachOfRow.get(OMPollerConstant.KeyMappingAttribute.KEY_MAPPING.getValue()));
			String value = String.valueOf(eachOfRow.get(OMPollerConstant.KeyMappingAttribute.KEY_VALUE.getValue()));
			orderTypeTableName.put(key, value);
		}
	}
	
	/**
	 * Load Post Query Transformation After Select the information from CRM Table
	 * @return Hash Map between SQL ID and Post Query Transformation
	 */
	
	private static ConcurrentHashMap<String, String> getQueryTransformation(String SqlID){
		ConcurrentHashMap<String, String> temp = new ConcurrentHashMap<String, String>();
		OMJDBCDao jdbcDao = new OMJDBCDao();
		JdbcTemplate omTemplete = jdbcDao.getOMJDBCTemplate();
		List<Map<String, Object>> postQueryMapping = omTemplete.queryForList(SqlID);
		for(Map<String, Object> eachOfRow : postQueryMapping){
			temp.put(String.valueOf(eachOfRow.get(OMPollerConstant.KeyMappingAttribute.KEY_MAPPING.getValue()))
					, String.valueOf(eachOfRow.get(OMPollerConstant.KeyMappingAttribute.KEY_VALUE.getValue())));
		}
		return temp;
	}
	
	private static String initialResourceRefIDMapping() throws Exception{
		OMJDBCDao jdbcDao = new OMJDBCDao();
		JdbcTemplate omTemplete = jdbcDao.getCRMJDBCTemplate();
		List<Map<String, Object>> resourceRefIDMapping = omTemplete.queryForList(SQLStatementPool.SQL_SELECT_RESOURCE_REF_ID_MAPPING);
		return OMPollerValueConverter.convertResultsettoXML(resourceRefIDMapping);
	}
	
}
