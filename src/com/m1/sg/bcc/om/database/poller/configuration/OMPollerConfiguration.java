package com.m1.sg.bcc.om.database.poller.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.sg.bcc.om.config.OMPollerConfigurationLoader;
import com.m1.sg.bcc.om.constant.OMPollerConstant;
import com.m1.sg.bcc.om.constant.SQLStatementPool;
import com.m1.sg.bcc.om.database.dao.OMJDBCDao;
import com.m1.sg.bcc.om.util.OMBeanFactory;
import com.m1.sg.bcc.om.util.OMPollerXMLUtil;

/************************************************************************************************************************************
 *  
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.database.poller.configuration
 * Description: This class created for configure fetch size for all poller 
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMPollerConfiguration {

	private Document pollerXMLConfiguration;
	private HashMap<String, ArrayList<String>> sqlConfigMapping = new HashMap<String, ArrayList<String>>();
	
	/**
	 * 	poller configuration retrieve all properties attribute define in the spring framework properties element
	 * 	for this class we use properties for fetch size of the poller 
	 */
	private Properties pollerPropertiesConfiguration = OMPollerConfigurationLoader.getPollerPropertiesConfiguration();
	
	/**
	 * 
	 * @return	Poller SQL for High Priority Poller
	 * @throws Exception 
	 */
	public String getSQLQueryHighPriorityPoller() throws Exception{
		if(sqlConfigMapping.size() == 0){loadHashMap();}
		return sqlConfigMapping.get("HighPriority").get(0);
	}
	
	/**
	 * 
	 * @return	Poller Post Query SQL for High Priority Poller
	 * @throws Exception 
	 */
	public String getSQLPostQueryHighPriorityPoller() throws Exception{
		if(sqlConfigMapping.size() == 0){loadHashMap();}
		return sqlConfigMapping.get("HighPriority").get(1);
	}
	
	/**
	 * 
	 * @return Poller SQL for Medium Priority Poller
	 * @throws Exception
	 */
	public String getSQLQueryMediumPriorityPoller() throws Exception {
		if(sqlConfigMapping.size() == 0){loadHashMap();}
		return sqlConfigMapping.get("MediumPriority").get(0);
	}
	
	/**
	 * 
	 * @return Poller Post Query SQL for Medium Priority Poller
	 * @throws Exception
	 */
	public String getSQLPostQueryMediumPriorityPoller() throws Exception {
		if(sqlConfigMapping.size() == 0){loadHashMap();}
		return sqlConfigMapping.get("MediumPriority").get(1);
	}
	
	/**
	 * 
	 * @return Poller SQL for Low Priority Poller
	 * @throws Exception
	 */
	public String getSQLQueryLowPriorityPoller() throws Exception {
		if(sqlConfigMapping.size() == 0){loadHashMap();}
		return sqlConfigMapping.get("LowPriority").get(0);
	}
	
	/**
	 * 
	 * @return Poller Post Query SQL for Low Priority Poller
	 * @throws Exception
	 */
	public String getSQLPostQueryLowPriorityPoller() throws Exception {
		if(sqlConfigMapping.size() == 0){loadHashMap();}
		return sqlConfigMapping.get("LowPriority").get(1);
	}
	
	/**
	 * 
	 * @return Poller SQL for Opco Poller
	 * @throws Exception
	 */
	public String getSQLQueryOpcoPoller() throws Exception {
		if(sqlConfigMapping.size() == 0){loadHashMap();}
		return sqlConfigMapping.get("Opco").get(0);
	}
	
	/**
	 * 
	 * @return Poller SQL for Tech Request Poller
	 * @throws Exception
	 */
	public String getSQLQueryTechReqPoller() throws Exception {
		if(sqlConfigMapping.size() == 0){loadHashMap();}
		return sqlConfigMapping.get("TechReq").get(0);
	}
	
	/**
	 * 
	 * @return Poller Post Query SQL for Opco Poller
	 * @throws Exception
	 */
	public String getSQLPostQueryOpcoPoller() throws Exception {
		if(sqlConfigMapping.size() == 0){loadHashMap();}
		return sqlConfigMapping.get("Opco").get(1);
	}
	
	/**
	 * 
	 * @return Poller Post Query SQL for Tech Request Poller
	 * @throws Exception
	 */
	public String getSQLPostQueryTechReqPoller() throws Exception {
		if(sqlConfigMapping.size() == 0){loadHashMap();}
		return sqlConfigMapping.get("TechReq").get(1);
	}
	
	/**
	 * this method is stub for calculate max roll per polling for high priority poller
	 * 
	 * @return	Max roll per poller for high priority poller
	 */
	public Number getMaxRowsPerPollHighPriority(){
		return getMaxRowsPerPoll(OMPollerConstant.OMOrderGeneratorPollerID.OMOrderGeneratorHighPriorityPoller.getValue()
				, pollerPropertiesConfiguration.getProperty(OMPollerConstant.PropertiesAttribute.HIGH_PRIORITY_FETCH_SIZE.getValue()));
	}
	
	/**
	 * this method is stub for calculate max roll per polling for medium priority poller
	 * 
	 * @return	Max roll per poller for medium priority poller
	 */
	public Number getMaxRowsPerPollMediumPriority(){
		return getMaxRowsPerPoll(OMPollerConstant.OMOrderGeneratorPollerID.OMOrderGeneratorMediumPriorityPoller.getValue()
				, pollerPropertiesConfiguration.getProperty(OMPollerConstant.PropertiesAttribute.MEDIUM_PRIORITY_FETCH_SIZE.getValue()));
	}
	
	/**
	 * this method is stub for calculate max roll per polling for low priority poller
	 * 
	 * @return	Max roll per poller for low priority poller
	 */
	public Number getMaxRowsPerPollLowPriority(){
		return getMaxRowsPerPoll(OMPollerConstant.OMOrderGeneratorPollerID.OMOrderGeneratorLowPriorityPoller.getValue()
				, pollerPropertiesConfiguration.getProperty(OMPollerConstant.PropertiesAttribute.LOW_PRIORITY_FETCH_SIZE.getValue()));
	}
	
	public Number getMaxRowsPerPollOpco(){
		return getMaxRowsPerPoll(OMPollerConstant.OMOrderGeneratorPollerID.OMOrderGeneratorOpcoPoller.getValue()
				, pollerPropertiesConfiguration.getProperty(OMPollerConstant.PropertiesAttribute.OPCO_FETCH_SIZE.getValue()));
	}
	
	public Number getMaxRowsPerPollTechReq(){
		return getMaxRowsPerPoll(OMPollerConstant.OMOrderGeneratorPollerID.OMOrderGeneratorTechReqPoller.getValue()
				, pollerPropertiesConfiguration.getProperty(OMPollerConstant.PropertiesAttribute.TECHREQ_FETCH_SIZE.getValue()));
	}
	
	/**
	 * this method created for calculate max roll per polling for each poller depend on
	 * parameter pass for each poller
	 * 
	 * @param priority		parameter define for priority poller High Medium and Low
	 * @param defaultValue	default value for fetch size
	 * @return				Max roll per poller value
	 */
	private Number getMaxRowsPerPoll(String pollerId, String defaultValue){
		OMJDBCDao omConnection = new OMJDBCDao();
		JdbcTemplate omJdbcTemplete = omConnection.getOMJDBCTemplate();
		List<Map<String, Object>> resultQuery = omJdbcTemplete.queryForList(SQLStatementPool.SQL_SELECT_POLLER_CONFIG, new Object[]{pollerId});
		if(resultQuery.size() > 0 && resultQuery.get(0) != null){
			String fetchSize = (String) resultQuery.get(0).get(OMPollerConstant.PollerConfigurationSPLColumnName.MAX_ROWS_PER_POLL.getValue());
			Integer fetchSizeInt = Integer.valueOf(fetchSize);
			return fetchSizeInt;
		} else {
			return Integer.valueOf(defaultValue);
		}
	}
	
	private void loadHashMap() throws Exception{
		File sqlConfigFile = (File) OMBeanFactory.getBean("sqlConfigfile");
		pollerXMLConfiguration = OMPollerXMLUtil.createDOMDocument(sqlConfigFile);
		System.out.println("Loading SQL Poller Configuration ....");
		loadSQLConfig();
		System.out.println("Loaded SQL Poller Configuration ....");
	}
	
	private void loadSQLConfig(){
		NodeList nodeList = pollerXMLConfiguration.getElementsByTagName("SQLPollerConfiguration");
		for(int i=0; i < nodeList.getLength(); i++){
			Node eachOfNode = nodeList.item(i);
			NodeList childNode = eachOfNode.getChildNodes();
			String pollerType = "";
			for(int j=0; j < childNode.getLength(); j++){
				Node subNode = childNode.item(j);
				if(subNode.getNodeType() == Element.ELEMENT_NODE){
					Element subNodeElement = (Element) subNode;
					if(subNodeElement.getNodeName().equals("PollerType")){
						pollerType = subNodeElement.getChildNodes().item(0).getNodeValue();
						sqlConfigMapping.put(pollerType, new ArrayList<String>());
					} else {
						if(pollerType.equals("")){pollerType = subNodeElement.getChildNodes().item(0).getNodeValue();sqlConfigMapping.put(pollerType, new ArrayList<String>());}
						else {
							ArrayList<String> tempList = sqlConfigMapping.get(pollerType);
							tempList.add(subNodeElement.getChildNodes().item(0).getNodeValue());
							sqlConfigMapping.put(pollerType, tempList);
						}
					}
				}
			}
			pollerType = "";
		}
	}
	
}
