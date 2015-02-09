package com.m1.sg.osm.util;

import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.sg.osm.entity.ChangeValueEntity;


public class XMLParserUtilNonIndex {
	
	public static Document parseOSMOrder(String osmOrderXML) throws Exception {
		
		try {
			if(osmOrderXML.indexOf("</Order>")<0){
				return null;
			}
			else {
				
				int startIdx = osmOrderXML.indexOf("<Order>");
				int endIdx = osmOrderXML.indexOf("</Order>") + "</Order>".length();
				
				String orderStr = osmOrderXML.substring(
						startIdx,
						endIdx);
				
//				orderStr = orderStr.replace("<Order>", "<Order xmlns = \"http://www.m1.com/sg/osm/tom\" index");
				
				
				return DOMUtils.createDOMDocument(orderStr);
			}
			
		} catch(Exception e){
			throw e;
		} 
		
	}
	
	public static Document parseOSMControlData(String osmControlDataXML) throws Exception {
		
		try {
			if(osmControlDataXML.indexOf("</ControlData>")<0){
				return null;
			}
			else {
				
				int startIdx = osmControlDataXML.indexOf("<ControlData index");
				int endIdx = osmControlDataXML.indexOf("</ControlData>") + "</ControlData>".length();
				
				String orderStr = osmControlDataXML.substring(
						startIdx,
						endIdx);
				
				orderStr = orderStr.replace("<ControlData index", "<ControlData xmlns = \"http://www.m1.com/sg/osm/tom\" index");
				
				orderStr = orderStr.replace("<soapenv:Envelope", "<![CDATA[<soapenv:Envelope");
				orderStr = orderStr.replace("</soapenv:Envelope>", "</soapenv:Envelope>]]>");
				
				return DOMUtils.createDOMDocument(orderStr);
			
			}
			
		} catch(Exception e){
			e.printStackTrace();
			throw e;
		} 
		
	}
	
public static HashMap<String, ChangeValueEntity> extractParamsUpdate(Document docRequest, Document docResponse){
		
		NodeList nodeParamsIdList = docResponse.getElementsByTagName("ParamID");
		HashMap<String, ChangeValueEntity> mapList = new HashMap<String, ChangeValueEntity>();
		for(int i=0; i < nodeParamsIdList.getLength(); i++){
			ChangeValueEntity changeObj = new ChangeValueEntity();
			Node eachNode = nodeParamsIdList.item(i);	
			Node parentNode = eachNode.getParentNode();
			for(int j=0; j < parentNode.getChildNodes().getLength(); j++){
				Node eachNodeChild = parentNode.getChildNodes().item(j);
				if(eachNodeChild.getNodeType() == Element.ELEMENT_NODE){
					if(eachNodeChild.getNodeName().equals("ParamID")){
						changeObj.setParamsId(eachNodeChild.getChildNodes().item(0).getNodeValue());
					} else if(eachNodeChild.getNodeName().equals("ParamType")){
						changeObj.setParamsType(eachNodeChild.getChildNodes().item(0).getNodeValue());
					} else if(eachNodeChild.getNodeName().equals("CurrentValue")){
						changeObj.setCurrentValue(eachNodeChild.getChildNodes().item(0).getNodeValue());
					} else if(eachNodeChild.getNodeName().equals("PreviousValue")){
						changeObj.setPreviousValue(eachNodeChild.getChildNodes().item(0).getNodeValue());
					}
				}
			}
			mapList.put(changeObj.getParamsId(), changeObj);
		}
		
		return mapList;
		
	}
	
	public static Document updateRequestMessage(Document docRequest, HashMap<String, ChangeValueEntity> mapParams){
		
		for(String eachOfKey : mapParams.keySet()){
			NodeList nodeList = docRequest.getElementsByTagName(mapParams.get(eachOfKey).getParamsType() + "ID");
			for(int i=0 ; i < nodeList.getLength(); i++){
				Node eachNode = nodeList.item(i);
				if(!eachNode.getChildNodes().item(0).getNodeValue().equals(eachOfKey)){continue;}
				Node tempNode = eachNode.getNextSibling();
				while(tempNode != null){
					if(tempNode.getNodeType() == Element.ELEMENT_NODE){
						if(tempNode.getNodeName().equals("CurrentValue")){
							tempNode.getChildNodes().item(0).setNodeValue(mapParams.get(eachOfKey).getCurrentValue());
						} else if(tempNode.getNodeName().equals("PreviousValue")){
							tempNode.getChildNodes().item(0).setNodeValue(mapParams.get(eachOfKey).getPreviousValue());
						}
					}
					tempNode = tempNode.getNextSibling();
				}
			}
		}
		
		return docRequest;
		
	}
	
}
