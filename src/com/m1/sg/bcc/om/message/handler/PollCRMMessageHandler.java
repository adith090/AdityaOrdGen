package com.m1.sg.bcc.om.message.handler;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.integration.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.sg.bcc.om.config.OMPollerConfigurationCredential;
import com.m1.sg.bcc.om.config.OMPollerConfigurationLoader;
import com.m1.sg.bcc.om.constant.OMPollerConstant;
import com.m1.sg.bcc.om.constant.OMPollerLoggerConstant;
import com.m1.sg.bcc.om.constant.SQLStatementPool;
import com.m1.sg.bcc.om.database.dao.OMJDBCDao;
import com.m1.sg.bcc.om.entity.MappingElementEntity;
import com.m1.sg.bcc.om.jms.sender.OMPollerJMSSender;
import com.m1.sg.bcc.om.logger.OMPollerLogger;
import com.m1.sg.bcc.om.util.OMPollerClassTypeUtil;
import com.m1.sg.bcc.om.util.OMPollerExecuteFiltering;
import com.m1.sg.bcc.om.util.OMPollerSOAPUtil;
import com.m1.sg.bcc.om.util.OMPollerValueConverter;
import com.m1.sg.bcc.om.util.OMPollerXMLUtil;
import com.m1.sg.bcc.om.util.OMTransformationJDBCTemplate;

/************************************************************************************************************************************
 * 
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.message.handler
 * Description: This class use for handle the message from the poller and implement the logic we need to generate XML create order to
 * 				OSM
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 15/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class PollCRMMessageHandler {
	
	private String orderType;
	
	/** 
	 * HashMap for store the key for each statement between the processing For Example ORDER_ID, PARENT_ID, LINEITEM_ID ... 
	 **/
	private HashMap<String, ArrayList<Object>> keyPool = new HashMap<String, ArrayList<Object>>();
	
	/**
	 * HashMap for cache value mapping query from database the purpose is reduce database query hit
	 */
	private static ConcurrentHashMap<Object, ConcurrentHashMap<Object, Object>> generalMapping = new ConcurrentHashMap<Object, ConcurrentHashMap<Object,Object>>();
	
	public void setOrderType(String orderType){this.orderType = orderType;}
	
	/**
	 * Message poll from poller and send to this method to execute 
	 * 
	 * @param pollMessage Message get from spring framework poller 
	 */
	public void createCOMCreateOrderMessage(Message<?> pollMessage){
		
		Object crmOrderId = null;
		
		try {
		
			/* clear for the new message processing */
			keyPool.clear();
			
			/* Database Connection Initial Template base on our utility and spring framework */
			OMJDBCDao jdbcDao = new OMJDBCDao();
			OMTransformationJDBCTemplate crmJdbcTemplate = jdbcDao.getCRMJDBCTemplate();
			
			/* JMS Connection Initial Template base on out utility and spring framework */
			OMPollerJMSSender omJMSSender = new OMPollerJMSSender();
			
			ArrayList<?> pollMessageList = (ArrayList<?>) pollMessage.getPayload();
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.INFO.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, "StartPollingPoint", new String[]{"Message Polling ", String.valueOf(pollMessageList.size()), " messages"});
			
			/* Get the Entity List we need to process for construct XML CreateOrder */
			ArrayList<MappingElementEntity> mappingElementList = OMPollerConfigurationLoader.getMappingElementList(this.orderType);
			
			/* Get the dependency between SQL. Program must run dependency SQL before execute next Entity List */
			Map<String, ArrayList<String>> mappingSqlDependency = OMPollerConfigurationLoader.getMappingSqlDependency(this.orderType);
			
			/* HashMap between SQL_ID and each of Entity */
			Map<String, MappingElementEntity> mappingSqlDetails = OMPollerConfigurationLoader.getMappingSqlDetails(this.orderType);
			
			/* All SQL_ID has dependency */
			ArrayList<String> allDependencyList = OMPollerConfigurationLoader.getAllDependencyList(this.orderType);
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, "LoadConfigPoint", new String[]{"MappingSQLDependency = ", mappingSqlDependency.toString()});
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, "LoadConfigPoint", new String[]{"MappingSQLDetails = ", mappingSqlDetails.toString()});
			
			/* Main loop Message */
			for(Object eachOfPollMessage : pollMessageList){
				Document doc = OMPollerXMLUtil.createNewDOMDocument();
 
				/* Message Details for each row per poll */
				for(MappingElementEntity eachOfMee : mappingElementList){
					Map<?, ?> eachOfRow = (Map<?, ?>) eachOfPollMessage;
					crmOrderId = eachOfRow.get(OMPollerConstant.OrderHeaderColumnName.ORDER_ID.getValue());
					
					/* Check if SQL contain in the dependency SQL please skip the process because we process in the dependency loop */					
					if(allDependencyList.contains(eachOfMee.getSqlStatementId())){
						OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.INFO.getSeverity()
								, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
								, String.valueOf(crmOrderId), new String[]{"Skip SQL_ID = ", eachOfMee.getSqlStatementId(), " Because of SQL Dependency"});
						continue;
					}
					
					/* Information in the XML Tag we get from the SQL from poller use this condition for construct the message
					 * SQL Statement ID = 1 meaning is root SQL */
					
					if(eachOfMee.getSqlKey()[0].equals("N/A")){
						List<Map<String, Object>> createElementList = eachOfMee.getMappingElementDetails();
						OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.INFO.getSeverity()
								, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
								, String.valueOf(crmOrderId), new String[]{"Execute SQL_ID = ", eachOfMee.getSqlStatementId()});
						
						/* loop for create the element XML follow by configuration table C_OM_GEN_ELEMENT_MAP */
						for(Map<String, Object> eachOfElementRow : createElementList){
							Object valueXML = eachOfRow.get(eachOfElementRow.get(OMPollerConstant.FieldMappingAttributeName.FROM_COLUMN_NAME.getValue()));
							valueXML = OMPollerValueConverter.valueConverter(valueXML, eachOfElementRow);
							
							OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
									, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
									, String.valueOf(crmOrderId), new String[]{
								"Tag Name is ", String.valueOf(eachOfElementRow.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue())),
								" ", "Value is ", String.valueOf(valueXML) 
							});
							
							doc = OMPollerXMLUtil.createElementFromPath(doc
									, String.valueOf(eachOfElementRow.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()))
									, String.valueOf(valueXML));
						}
						
						/* Update key pool for use in the next execution */
						updateKeyPoolValue(eachOfMee, eachOfRow);
						
						OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
								, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
								, String.valueOf(crmOrderId), new String[]{
							keyPool.toString() 
						});
						
					} 
					
					/* Next step after the Root SQL execute finish follow by configuration define in the table C_OM_GEN_SQL_MAP */					
					else {
						
						String sqlId = eachOfMee.getSqlStatementId();
						String sqlStatement = eachOfMee.getSqlStatement();
						String[] sqlKey = eachOfMee.getSqlKey();
						
						OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.INFO.getSeverity()
								, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
								, String.valueOf(crmOrderId), new String[]{"Execute SQL_ID = ", sqlId});
						
						/* Get Mapping what SQL_ID has dependency with this SQL_ID */
						ArrayList<String> dependencyList = mappingSqlDependency.get(sqlId);
						
						/* Bind Variable Query Solutions */
						Object[] parameterField = mappingKeyInKeyPool(sqlKey);
						if(parameterField.length == 0){continue;}
						List<Map<String, Object>> sqlResult = crmJdbcTemplate.queryForListWithTranformation(sqlStatement, parameterField, OMJDBCDao.jdbcTypeMapping(parameterField), sqlId);
						
						OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
								, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
								, String.valueOf(crmOrderId), new String[]{
							"SQL Statement", sqlStatement 
						});
						
						OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
								, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
								, String.valueOf(crmOrderId), new String[]{
							"SQL Result", sqlResult.toString() 
						});
						
						/* loop for create the element XML follow by configuration table C_OM_GEN_ELEMENT_MAP */
						for(Map<String, Object> eachOfRowSqlResult : sqlResult){
							List<Map<String, Object>> createElementList = eachOfMee.getMappingElementDetails();
							for(Map<String, Object> eachOfElementCreate : createElementList){
								Object valueXML = eachOfRowSqlResult.get(eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.FROM_COLUMN_NAME.getValue()));
								
								/* Value changed by Type support format in OSM For Example DateTime Format */
								valueXML = OMPollerValueConverter.valueConverter(valueXML, eachOfElementCreate);
								
								/* Value changed by mapping from the table */
								if(eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.SQL_VALUE_MAPPING.getValue()) != null){
									
									/* Do for caching reduce database hit time */
									Object cacheRetrieve = retrieveFromMappingCache(eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()), valueXML);
									if(cacheRetrieve == null){
										String sqlStatementMapping = (String)eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.SQL_VALUE_MAPPING.getValue());
										Object mappingValue = mappingFieldValue(sqlStatementMapping, valueXML);
										
										OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
												, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
												, String.valueOf(crmOrderId), new String[]{
											"Tag Name is ", String.valueOf(eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue())),
											" ", "Value is ", String.valueOf(mappingValue) 
										});
										
										doc = OMPollerXMLUtil.createElementFromPath(doc
												, String.valueOf(eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()))
												, String.valueOf(mappingValue));
										updateMappingCache(eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()), valueXML, mappingValue);
										
										OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
												, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
												, String.valueOf(crmOrderId), new String[]{
											keyPool.toString() 
										});
									} else {
										
										OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
												, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
												, String.valueOf(crmOrderId), new String[]{
											"Tag Name is ", String.valueOf(eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue())),
											" ", "Value is ", String.valueOf(cacheRetrieve) 
										});
									
										doc = OMPollerXMLUtil.createElementFromPath(doc
												, String.valueOf(eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()))
												, String.valueOf(cacheRetrieve));
									}
									
								} else {
									
									OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
											, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
											, String.valueOf(crmOrderId), new String[]{
										"Tag Name is ", String.valueOf(eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue())),
										" ", "Value is ", String.valueOf(valueXML) 
									});
									
									doc = OMPollerXMLUtil.createElementFromPath(doc
											, String.valueOf(eachOfElementCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()))
											, String.valueOf(valueXML));
								}
							}
							
							/* Update Value Query from the database into the cache */
							updateKeyPoolValue(eachOfMee, eachOfRowSqlResult);
							
							OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
									, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
									, String.valueOf(crmOrderId), new String[]{
								keyPool.toString() 
							});
							/* loop for the SQL ID has dependency with this SQL_ID */
							for(String eachOfDependencySqlId : dependencyList){
								MappingElementEntity meeDependency = mappingSqlDetails.get(eachOfDependencySqlId);
								Object[] dependencyParameterField = mappingKeyInKeyPool(meeDependency.getSqlKey());
								if(dependencyParameterField.length == 0){continue;}
								List<Map<String, Object>> sqlDependencyResult = crmJdbcTemplate.queryForListWithTranformation(meeDependency.getSqlStatement(), dependencyParameterField, OMJDBCDao.jdbcTypeMapping(dependencyParameterField), eachOfDependencySqlId);
								
								OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.INFO.getSeverity()
										, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
										, String.valueOf(crmOrderId), new String[]{"Execute SQL_ID for Dependency = ", eachOfDependencySqlId});
								
								/* Same Concept as Previous loop */
								for(Map<String, Object> eachOfRowDependencyResult : sqlDependencyResult){
									List<Map<String, Object>> createElementDependencyList = meeDependency.getMappingElementDetails();
									for(Map<String, Object> eachOfElementDependencyCreate : createElementDependencyList){
										Object valueXML = eachOfRowDependencyResult.get(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.FROM_COLUMN_NAME.getValue()));
										valueXML = OMPollerValueConverter.valueConverter(valueXML, eachOfElementDependencyCreate);
										if(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.SQL_VALUE_MAPPING.getValue()) != null){
											Object cacheRetrieve = retrieveFromMappingCache(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()), valueXML);
											if(cacheRetrieve == null){
												String sqlStatementMapping = (String)eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.SQL_VALUE_MAPPING.getValue());
												Object mappingValue = mappingFieldValue(sqlStatementMapping, valueXML);
												
												OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
														, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
														, String.valueOf(crmOrderId), new String[]{
													"Tag Name is ", String.valueOf(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue())),
													" ", "Value is ", String.valueOf(mappingValue) 
												});
												
												doc = OMPollerXMLUtil.createElementFromPath(doc
														, String.valueOf(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()))
														, String.valueOf(mappingValue));
												updateMappingCache(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()), valueXML, mappingValue);
												
												OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
														, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
														, String.valueOf(crmOrderId), new String[]{
													keyPool.toString() 
												});
												
											} else {
												
												OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
														, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
														, String.valueOf(crmOrderId), new String[]{
													"Tag Name is ", String.valueOf(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue())),
													" ", "Value is ", String.valueOf(cacheRetrieve) 
												});
												
												doc = OMPollerXMLUtil.createElementFromPath(doc
														, String.valueOf(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()))
														, String.valueOf(cacheRetrieve));
											}
										} else {
											
											OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
													, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
													, String.valueOf(crmOrderId), new String[]{
												"Tag Name is ", String.valueOf(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue())),
												" ", "Value is ", String.valueOf(valueXML) 
											});
											
											doc = OMPollerXMLUtil.createElementFromPath(doc
													, String.valueOf(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()))
													, String.valueOf(valueXML));
										}
									}
									updateKeyPoolValue(meeDependency, eachOfRowDependencyResult);
									
									OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
											, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
											, String.valueOf(crmOrderId), new String[]{
										keyPool.toString() 
									});
									
									if(mappingSqlDependency.containsKey(eachOfDependencySqlId))
										
										/* Generate XML for dependency more than one level for example 1001 depend on 1002 depend on 1003 ... */
										generateRecursiveDependency(doc, eachOfDependencySqlId, mappingSqlDependency, mappingSqlDetails, String.valueOf(crmOrderId));
								}
							}
						}
					}
				}
				keyPool.clear();
				
				/* Filter Unnecessary Line Item Out */
				doc = filteringLineItem(doc, String.valueOf(crmOrderId));
				
				/* Generate Namespace for submitted to OSM refer from the Product Type Main or Opco */
				doc = OMPollerXMLUtil.changeXMLNamespace(doc, orderType + "_" + getOpCoProvider(doc));
				
				/* Convert XML Document DOM to XML String */
				String requestMessage = OMPollerXMLUtil.convertDocumenttoString(doc);
				
				/* Create SOAP Header and SOAP Body */
				requestMessage = OMPollerSOAPUtil.createSOAPRequest(requestMessage, OMPollerConfigurationCredential.getUsername(), OMPollerConfigurationCredential.getPassword());
				
				OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
						, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
						, String.valueOf(crmOrderId), new String[]{
					requestMessage 
				});
				
				/* Send Message to the JMS Queue */
				omJMSSender.sendMessageToJMSQueue("oracle/communications/ordermanagement/WebServiceQueue", requestMessage, eachOfPollMessage, this.orderType);
			}
			
		} catch (Exception e){
			
			if(crmOrderId != null){
				updatePollerStatusOrderHeader(crmOrderId, this.orderType, OMPollerConfigurationLoader.getOrderStatus().get(1));
			}
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.ERROR.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, "Exception", "" , e);
		}
		
	}
	
	/**
	 * Method for update value into the Cache
	 * 
	 * @param mee 	entity for the table details refer from the table C_OM_GEN_SQL_MAP 
	 * @param row 	data need to update become to the key in next SQL Statement
	 */
	private void updateKeyPoolValue(MappingElementEntity mee, Map<?, ?> row){
		String[] keyOfStatement = mee.getNextSqlKey();
		for(String eachOfKey : keyOfStatement){
			if(keyPool.containsKey(eachOfKey)){
				ArrayList<Object> oldList = keyPool.get(eachOfKey);
				if(row.get(eachOfKey) != null){oldList.add(row.get(eachOfKey));}
				keyPool.put(eachOfKey, oldList);
			} else {
				ArrayList<Object> newList = new ArrayList<Object>();
				if(row.get(eachOfKey) != null){newList.add(row.get(eachOfKey));}
				keyPool.put(eachOfKey, newList);
			}
		}
	}
	
	
	/**
	 * Method for Mapping the field condition for the SQL Statement for example select * from t_order_header where order_id = ? 
	 * this method will map the value for the order_id you define in database configuration
	 * 
	 * @param 	sqlKey key for the query statement for example select * from t_order_header where order_id = ? order_id is a key
	 * @return 	value array in the Object Type 
	 */
	private Object[] mappingKeyInKeyPool(String[] sqlKey){
		
		ArrayList<Object> listOfMapping = new ArrayList<Object>();
		for(String eachOfSqlKey : sqlKey){
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, "KeyPollMapping", new String[]{ eachOfSqlKey
				 
			});
			
			ArrayList<Object> valueList = keyPool.get(eachOfSqlKey); 
			
			if(valueList != null && valueList.size() != 0){
				
				//Support No OLI in the Order XML
				
				Object value = valueList.get(valueList.size()-1);
				String strValue = String.valueOf(value);
				if(eachOfSqlKey.equals(OMPollerConstant.LineItemColumnName.PARENT_ORDER_LINE_ITEM_ID.getValue()) && strValue.equals("0")){
					ArrayList<Object> changeValueList = keyPool.get(OMPollerConstant.LineItemColumnName.ORDER_LINE_ITEM_ID.getValue());
					if(valueList.size() != 0){value = changeValueList.get(changeValueList.size() - 1);}
				}
				
				OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
						, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
						, "KeyPollMapping", new String[]{ eachOfSqlKey, " = ", String.valueOf(value)
					 
				});

				listOfMapping.add(value);
			}
		}
		return listOfMapping.toArray();
	}
	
	/**
	 * Method for generate element for the SQL has dependency more than one level
	 * 
	 * @param doc						Document XML Input 
	 * @param rootDependency			Parent of the dependency for example 1001 has dependency with 1002 so 1001 is a parent
	 * @param mappingSqlDependency		HashMap between SQL_ID and SQL_ID dependency with
	 * @param mappingSqlDetails			HashMap between SQL_ID and SQL_ID details
	 * @return							Document create element for the dependency for each SQL already
	 * @throws Exception				if date time convert got problem we will throw the exception
	 */
	private Document generateRecursiveDependency(Document doc
			, String rootDependency
			, Map<String, ArrayList<String>> mappingSqlDependency
			, Map<String, MappingElementEntity> mappingSqlDetails
			, String crmOrderId) throws Exception{
		
		OMJDBCDao jdbcDao = new OMJDBCDao();
		OMTransformationJDBCTemplate crmJdbcTemplate = jdbcDao.getCRMJDBCTemplate();
		
		ArrayList<String> dependencyList = mappingSqlDependency.get(rootDependency);
		
		for(String eachOfRecursiveDependencySqlId : dependencyList){
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, String.valueOf(crmOrderId), new String[]{
				"Execute Recursive SQL ID = ", eachOfRecursiveDependencySqlId  
			});
			
			MappingElementEntity meeRecursiveDependency = mappingSqlDetails.get(eachOfRecursiveDependencySqlId);
			Object[] dependencyParameterField = mappingKeyInKeyPool(meeRecursiveDependency.getSqlKey());
			Integer countParameter = StringUtils.countMatches(meeRecursiveDependency.getSqlStatement(), "?");
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, String.valueOf(crmOrderId), new String[]{
				"Parameter Count = ", String.valueOf(countParameter), "Dependency Parameter Field = ", String.valueOf(dependencyParameterField.length)  
			});
			
			if(dependencyParameterField.length == 0 || dependencyParameterField.length != countParameter){continue;}
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, String.valueOf(crmOrderId), new String[]{
				"Execute SQL Statement Recursive = ", meeRecursiveDependency.getSqlStatement()
			});
			
			List<Map<String, Object>> sqlDependencyResult = crmJdbcTemplate.queryForListWithTranformation(meeRecursiveDependency.getSqlStatement(), dependencyParameterField, OMJDBCDao.jdbcTypeMapping(dependencyParameterField), eachOfRecursiveDependencySqlId);
			for(Map<String, Object> eachOfRowDependencyResult : sqlDependencyResult){
				List<Map<String, Object>> createElementDependencyList = meeRecursiveDependency.getMappingElementDetails();
				for(Map<String, Object> eachOfElementDependencyCreate : createElementDependencyList){
					Object valueXML = eachOfRowDependencyResult.get(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.FROM_COLUMN_NAME.getValue()));
					valueXML = OMPollerValueConverter.valueConverter(valueXML, eachOfElementDependencyCreate);
					if(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.SQL_VALUE_MAPPING.getValue()) != null){
						Object cacheRetrieve = retrieveFromMappingCache(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()), valueXML);
						if(cacheRetrieve == null){
							String sqlStatementMapping = (String)eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.SQL_VALUE_MAPPING.getValue());
							Object mappingValue = mappingFieldValue(sqlStatementMapping, valueXML);
							doc = OMPollerXMLUtil.createElementFromPath(doc
									, String.valueOf(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()))
									, String.valueOf(mappingValue));
							updateMappingCache(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()), valueXML, mappingValue);
						} else {
				
							doc = OMPollerXMLUtil.createElementFromPath(doc
									, String.valueOf(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()))
									, String.valueOf(cacheRetrieve));
						}
					} else {
						doc = OMPollerXMLUtil.createElementFromPath(doc
								, String.valueOf(eachOfElementDependencyCreate.get(OMPollerConstant.FieldMappingAttributeName.XML_PATH.getValue()))
								, String.valueOf(valueXML));
					}
				}
				updateKeyPoolValue(meeRecursiveDependency, eachOfRowDependencyResult);
				if(mappingSqlDependency.containsKey(eachOfRecursiveDependencySqlId)){
					/* Generate XML for dependency more than one level for example 1001 depend on 1002 depend on 1003 ... */
					generateRecursiveDependency(doc, eachOfRecursiveDependencySqlId, mappingSqlDependency, mappingSqlDetails, String.valueOf(crmOrderId));
				}
			}
		}
		return doc;
	}
	
	 
	/**
	 * Method for mapping value retrieve from configuration table
	 * 
	 * @param sqlStatementMapping	SQL Statement configuration in the database for mapping between value in the table
	 * @param originalField			Key for mapping in the table mapping
	 * @return						Map result in the Object Type
	 */
	private Object mappingFieldValue(String sqlStatementMapping, Object originalField){
		OMJDBCDao jdbcDao = new OMJDBCDao();
		OMTransformationJDBCTemplate omJDBCTemplate = jdbcDao.getOMJDBCTemplate();
		Object[] paramsMapping = OMPollerClassTypeUtil.objectToArray(originalField);
		int[] jdbcTempleteType = OMJDBCDao.jdbcTypeMapping(paramsMapping);
		List<Map<String, Object>> mappingResult = omJDBCTemplate.queryForList(sqlStatementMapping, paramsMapping, jdbcTempleteType);
		if(mappingResult.size() == 0){return null;}
		else {
			Map<String, Object> targetMap = mappingResult.get(mappingResult.size()-1);
			if(targetMap.keySet().size() == 0){return null;}
			else {return targetMap.get(targetMap.keySet().toArray()[0]);}
		}
	}
	
	
	/**
	 * retrieve the value from cache if it 's Hit return the object if It 's Miss return null
	 * 
	 * @param keyXMLPath	For each element we have XML_PATH is a key
	 * @param mapKey		For each mapping we have mapping key
	 * @return				if key found in the cache return the value if not return null
	 */
	private Object retrieveFromMappingCache(Object keyXMLPath, Object mapKey){
		if(generalMapping.containsKey(keyXMLPath)){
			if(generalMapping.get(keyXMLPath).containsKey(mapKey)){
				return generalMapping.get(keyXMLPath).get(mapKey);
			}
		}
		return null;
	}
	
	
	/**
	 * bring the new value update into the cache
	 * 
	 * @param keyXMLPath	For each element we have XML_PATH is a key
	 * @param mapKey		For each mapping we have mapping key
	 * @param value			Value need to put into cache
	 */
	private void updateMappingCache(Object keyXMLPath, Object mapKey, Object value){
		if(value != null){
			if(generalMapping.containsKey(keyXMLPath)){
				if(!generalMapping.get(keyXMLPath).containsKey(mapKey)){
					generalMapping.get(keyXMLPath).put(mapKey, value);
				} 
			} else {
				ConcurrentHashMap<Object, Object> createMap = new ConcurrentHashMap<Object, Object>();
				createMap.put(mapKey, value);
				generalMapping.put(keyXMLPath, createMap);
			}
		}
	}
	
	private void updatePollerStatusOrderHeader(Object orderId, String orderType, String status){
		OMJDBCDao omJDBCDao = new OMJDBCDao();
		OMTransformationJDBCTemplate crmTemplate = omJDBCDao.getCRMJDBCTemplate();
		Object[] paramsMap = new Object[]{orderId};
		crmTemplate.update(MessageFormat.format(SQLStatementPool.SQL_UPDATE_ERROR_STATUS_CRM, 
				new Object[]{OMPollerConfigurationLoader.getOrderTypeTableMapping().get(orderType), status})
				, paramsMap, OMJDBCDao.jdbcTypeMapping(paramsMap));
	}
	
	private Document filteringLineItem(Document originalDoc, String crmOrderId) throws Exception{
		NodeList orderLineItemList = OMPollerXMLUtil.executeXPath("//OrderLineItem", originalDoc);
		
		OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
				, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
				, String.valueOf(crmOrderId), new String[]{
			"Number Of Order Line Item -> " + orderLineItemList.getLength()
		});
		
		ArrayList<String> conditionList = OMPollerConfigurationLoader.getFilterConditionList();
		for(int i=0; i < orderLineItemList.getLength(); i++){
			Node eachOfOrderLineItem = orderLineItemList.item(i);
			Document orderLineItemDoc = OMPollerXMLUtil.convertNodetoDocument(eachOfOrderLineItem);
			String lineitemXML = OMPollerXMLUtil.convertDocumenttoString(orderLineItemDoc);
			int conditionCount = 1;
			for(String condition: conditionList){
				
				String conditionResult = OMPollerExecuteFiltering.executeXQuery(lineitemXML, condition);
				
				OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
						, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
						, String.valueOf(crmOrderId), new String[]{
					"LineItemNumber -> " + (i+1),
					" Condition Number -> " + conditionCount,
					" Condition Value -> " + conditionResult
				});
				if(Boolean.valueOf(conditionResult)){
					eachOfOrderLineItem.getParentNode().removeChild(eachOfOrderLineItem);
					break;
				}
				conditionCount++;
			}
		}
		
		return originalDoc;
	}
	
	private String getOpCoProvider(Document doc) throws XPathExpressionException{
		NodeList nodeList = OMPollerXMLUtil.executeXPath("//Order/OrderHeader/OpCoProvide/text()", doc);
		if(nodeList.getLength() == 0){return "";}
		else {return nodeList.item(0).getNodeValue();}
	}
	
}
