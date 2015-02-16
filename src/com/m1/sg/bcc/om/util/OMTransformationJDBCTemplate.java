package com.m1.sg.bcc.om.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.m1.sg.bcc.om.config.OMPollerConfigurationLoader;
import com.m1.sg.bcc.om.constant.OMPollerLoggerConstant;
import com.m1.sg.bcc.om.logger.OMPollerLogger;

public class OMTransformationJDBCTemplate extends JdbcTemplate {

	private final String xmlListFormat = "<ParameterInputList>{0}</ParameterInputList>";
	private final String xmlChildFormat = "<ParameterInput{0}>{1}</ParameterInput{0}>";
	private final String rootFormat = "<root>{0}</root>";
	
	public OMTransformationJDBCTemplate(DataSource dataSource) {
		super(dataSource);
	}

	public List<Map<String, Object>> queryForListWithTranformation(String sql, Object[] args,
			int[] elementType, String sqlId) throws Exception {
		
		OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
				, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
				, "Pre SQL Transformation XML", new String[]{
			"Hit Query"
		});
		
		if(OMPollerConfigurationLoader.getPreQueryTransformationMapping().containsKey(sqlId)){
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, "Pre SQL Transformation XML", new String[]{
				"Input Pre SQL Transformation ", sql
			});
			sql = transformationQuery(sql, OMPollerConfigurationLoader.getPreQueryTransformationMapping().get(sqlId), args);
			if(sql != null){
				OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
						, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
						, "Pre SQL Transformation XML", new String[]{
					"Output Pre SQL Transformation ", sql
				});
			} else {return new ArrayList<Map<String,Object>>();}
		}
		if(OMPollerConfigurationLoader.getPostQueryTranformationMapping().containsKey(sqlId)){
			List<Map<String, Object>> resultList;
			List<Map<String, Object>> rawResultList = super.queryForList(sql, args, elementType);
			if(rawResultList.size() == 0){return rawResultList;}
			String resultXML = OMPollerValueConverter.convertResultsettoXML(rawResultList);
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, "Post Transformation XML", new String[]{
				"Input Post Transformation ", resultXML
			});
			
			String output = OMPollerExecuteFiltering.executeXQuery(resultXML
					, String.valueOf(OMPollerConfigurationLoader.getPostQueryTranformationMapping().get(sqlId)));
			
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, "Post Transformation XML", new String[]{
				"Output Post Transformation ", output
			});
			
			if(rawResultList.size() != 0){
				resultList = OMPollerValueConverter.convertXMLtoResultSet(output
						, getCanonicalNameMapping(rawResultList.get(0)));
			} else {
				resultList = OMPollerValueConverter.convertXMLtoResultSet(output
						, getCanonicalNameMapping(new ConcurrentHashMap<String, Object>()));
			}
			
			return resultList;
		}
		else {return super.queryForList(sql, args, elementType);}
	}
	
	private Map<String, String> getCanonicalNameMapping(Map<String, Object> rawResult){
		Map<String, String> canonicalNameMapping = new ConcurrentHashMap<String, String>();
		for(String eachOfColumn : rawResult.keySet()){
			if(rawResult.get(eachOfColumn) == null){continue;}
			canonicalNameMapping.put(eachOfColumn, rawResult.get(eachOfColumn).getClass().getCanonicalName());
		}
		return canonicalNameMapping;
	}
	
	private String transformationQuery(String originalQuery, String conditionProcessing, Object[] keyValue) throws Exception{
		
		String inputXML = appendKeyValue(OMPollerConfigurationLoader.getResourceRefIDMapping(), keyValue) ;
		
		OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
				, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
				, "Transformation Processing", new String[]{
			"Input Pre SQL Transformation After Transform ", inputXML
		});
		
		String result = OMPollerExecuteFiltering.executeXQuery(inputXML, conditionProcessing);
		
		OMStringTemplate stemplate = new OMStringTemplate(originalQuery);
		HashMap<String, String> mapParams = new HashMap<String, String>();
		Document docResult = OMPollerXMLUtil.createDOMDocument(result);
		NodeList rl = OMPollerXMLUtil.executeXPath("//ParammeterList/*", docResult);
		for(int i=0; i < rl.getLength(); i++){
			if(rl.item(i).getNodeType() == Element.ELEMENT_NODE){
				Element element = (Element) rl.item(i);
				if(element.getChildNodes().getLength() == 0){mapParams.put(element.getNodeName(), null);}
				else {mapParams.put(element.getNodeName(), element.getChildNodes().item(0).getNodeValue());}
			}
		}
		String newQuery = stemplate.substitute(mapParams);
		OMStringTemplate newTemplate = new OMStringTemplate(newQuery);
		if(!newTemplate.checkPattern()){
			OMPollerLogger.log(OMPollerLoggerConstant.LogLevel.DEBUG.getSeverity()
					, OMPollerLoggerConstant.LoggerName.ORDERGENERATORLOGGER.getValue()
					, "Transformation Processing", new String[]{
				"New Query ", newQuery
			});
			return newQuery;
		} else {return null;}
	}
	
	private String appendKeyValue(String original, Object[] keyValue){
		StringBuffer strBuffer = new StringBuffer();
		for(int i=0; i < keyValue.length; i++){
			strBuffer.append(MessageFormat.format(xmlChildFormat, new Object[]{i, String.valueOf(keyValue[i])}));
		}
		return MessageFormat.format(rootFormat, original + MessageFormat.format(xmlListFormat, strBuffer.toString()));
	}
	
}
