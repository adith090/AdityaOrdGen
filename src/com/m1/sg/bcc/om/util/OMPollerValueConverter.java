package com.m1.sg.bcc.om.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.sg.bcc.om.constant.OMPollerConstant;
import com.m1.sg.bcc.om.constant.SQLStatementPool;
import com.m1.sg.bcc.om.database.dao.OMJDBCDao;

/************************************************************************************************************************************
 *  
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.util
 * Description: This utility class created for format value convert in the XML Tag 
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMPollerValueConverter {

	private static ConcurrentHashMap<Object, Object> dateTimeMappingCache = new ConcurrentHashMap<Object, Object>();
	
	/**
	 * 
	 * this method for stub value converter mapping
	 * 
	 * @param original
	 * @param rowProcess
	 * @return
	 * @throws Exception
	 */
	public static Object valueConverter(Object original, Map<String, Object> rowProcess) throws Exception{
		if(rowProcess.get(OMPollerConstant.FieldMappingAttributeName.ELEMENT_DATA_TYPE.getValue()) != null && original != null){
			return convert(String.valueOf(original), rowProcess);
		} else {
			return original;
		}
	}
	
	/**
	 * this method for the value converter mapping
	 * 
	 * @param original
	 * @param rowProcess
	 * @return
	 * @throws Exception
	 */
	private static String convert(String original, Map<String, Object> rowProcess) throws Exception {
		if(rowProcess.get(OMPollerConstant.FieldMappingAttributeName.ELEMENT_DATA_TYPE.getValue()).equals(
				OMPollerConstant.ElementDataType.DATETIME.getValue())){
			
			if(rowProcess.get(OMPollerConstant.FieldMappingAttributeName.DATETIME_FORMAT_ID.getValue()) != null){
				
				Object keyDateTimeFormatId = rowProcess.get(OMPollerConstant.FieldMappingAttributeName.DATETIME_FORMAT_ID.getValue());
				String dateTimeFormat;
				if(dateTimeMappingCache.containsKey(keyDateTimeFormatId)){
					dateTimeFormat = String.valueOf(dateTimeMappingCache.get(keyDateTimeFormatId));
				} else {
					OMJDBCDao daoFactory = new OMJDBCDao();
					JdbcTemplate omJDBCTemplete = daoFactory.getOMJDBCTemplate();
					Object[] paramsMapping = OMPollerClassTypeUtil.objectToArray(keyDateTimeFormatId);
					
					List<Map<String, Object>> mappingResult = omJDBCTemplete.queryForList(SQLStatementPool.SQL_SELECT_CONVERT_DATETIME_FORMAT
							, paramsMapping, OMJDBCDao.jdbcTypeMapping(paramsMapping));
					Map<String, Object> rowMapResult = mappingResult.get(0);
					dateTimeFormat = String.valueOf(rowMapResult.get(rowMapResult.keySet().toArray()[0]));
					dateTimeMappingCache.put(keyDateTimeFormatId, dateTimeFormat);
				} 
				return OMPollerDateTimeUtil.convert(original, dateTimeFormat, OMPollerDateTimeUtil.XML_DATE_TIME_FORMAT);
			} else {
				return original;
			}
		} else {
			return original;
		}
	}
	
	public static String convertResultsettoXML(List<Map<String, Object>> resultSet) throws Exception{
		Document docXML = OMPollerXMLUtil.createNewDOMDocument();
		Node rootRowNode = docXML.createElement("row");
		for(Map<String, Object> eachOfRow : resultSet){
			Node rowSetNode = docXML.createElement("rowSet");
			for(String key : eachOfRow.keySet()){
				Node targetNode = docXML.createElement(key);
				Node textNode;
				if(eachOfRow.get(key) != null){textNode = docXML.createTextNode(String.valueOf(eachOfRow.get(key)));}
				else {textNode = docXML.createTextNode("");}
				targetNode.appendChild(textNode);
				rowSetNode.appendChild(targetNode);
			}
			rootRowNode.appendChild(rowSetNode);
		}
		docXML.appendChild(rootRowNode);
		return OMPollerXMLUtil.convertDocumenttoString(docXML);
	}
	
	public static List<Map<String, Object>> convertXMLtoResultSet(String XMLResult, Map<String, String> classTypeMapping) throws Exception{
		List<Map<String, Object>> resultSet = new ArrayList<Map<String,Object>>();
		Document docXML = OMPollerXMLUtil.createDOMDocument(XMLResult);
		NodeList resultList = docXML.getElementsByTagName("rowSet");
		for(int i=0; i < resultList.getLength(); i++){
			Map<String, Object> eachRow = new HashMap<String, Object>();
			Node eachOfRowSet = resultList.item(i);
			for(int j=0; j < eachOfRowSet.getChildNodes().getLength(); j++){
				Node childNode = eachOfRowSet.getChildNodes().item(j);
				if(childNode.getNodeType() == Element.ELEMENT_NODE){
					Element targetNode = (Element) childNode;
					if(classTypeMapping.containsKey(targetNode.getNodeName())){
						if(targetNode.getChildNodes().getLength() == 0){eachRow.put(targetNode.getNodeName(), null);}
						else {
							eachRow.put(targetNode.getNodeName()
									, OMPollerClassTypeUtil.createObject(
											classTypeMapping.get(targetNode.getNodeName())
											, targetNode.getChildNodes().item(0).getNodeValue()));
						}
					}
					else {
						if(targetNode.getChildNodes().getLength() == 0){eachRow.put(targetNode.getNodeName(), null);}
						else {
							eachRow.put(targetNode.getNodeName(), targetNode.getChildNodes().item(0).getNodeValue());
						}
					}
				}
			}
			resultSet.add(eachRow);
		}
		return resultSet;
		
	}
	
}
