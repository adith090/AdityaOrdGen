package com.m1.sg.osm.main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.m1.sg.osm.database.dao.DaoFactory;
import com.m1.sg.osm.database.dao.common.CommonDao;
import com.m1.sg.osm.database.entity.common.BillRefMappingEntity;
import com.m1.sg.osm.database.entity.common.ExtIDAttrMappingEntity;
import com.m1.sg.osm.database.entity.common.ExtnDataMappingEntity;
import com.m1.sg.osm.database.entity.common.MappingEntity;
import com.m1.sg.osm.util.DOMUtils;
import com.m1.sg.osm.util.OMPollerXMLUtil;
import com.m1.sg.osm.util.ObjectFactoryUtil;
import com.m1.sg.osm.util.SOAPUtil;
import com.m1.sg.osm.util.XqueryExecuteUtil;
import com.mslv.oms.automation.AutomationContext;
import com.mslv.oms.automation.AutomationException;
import com.mslv.oms.automation.TaskContext;
import com.mslv.oms.automation.plugin.AbstractSendAutomator;
import common.util.TALogger;
import common.util.constants.TAConstants;

/*
 * This Class is to map the CRM order to the C1 TOM order.
 * The mapping values are read from the C_OM_BILL_C1_ATTR_MAP table, and the C1 orderXML is constructed according to the Xpath
 */

/************************************************************************************ 
 * MODIFICATION HISTORY
 ************************************************************************************ 
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 ************************************************************************************
 * 21/06/2013					Aditya					Created
 * 24/09/2013					Aditya					Enhancement to the framework
 * 														Added functionality to handle 
 * 														extended data
 ************************************************************************************/

public class CreateSendC1TOMOrder extends AbstractSendAutomator {

	@Override
	protected void makeRequest(String inputData, AutomationContext context,
			TextMessage textMessage) throws AutomationException {
		// TODO Auto-generated method stub

		String crmOrderId = "";
		String exitStatus = "";
		TALogger taLogger = TALogger.getTALogger();
		TaskContext taskContext = (TaskContext) context;
		CommonDao commonDao = null;

		try {
			textMessage.setJMSCorrelationID(taskContext.getOrderId() + "_" + taskContext.getOrderHistoryId());
			textMessage.setJMSType(taskContext.getTaskMnemonic());
			commonDao = DaoFactory.getDao(CommonDao.class);
			MappingEntity mapEntity = new MappingEntity();
			BillRefMappingEntity billRefMappingEntity = new BillRefMappingEntity();

			List<MappingEntity> mappingEntityList = commonDao.findAll(mapEntity, MappingEntity.SQL_QUERY_BILL_C1_MAP);

			List<BillRefMappingEntity> billRefMappingEntityList = commonDao.findAll(billRefMappingEntity, BillRefMappingEntity.SQL_QUERY_BILL_LOOKUP_REF);

			Document inputDocument = DOMUtils.createDOMDocument((removeNamespaceAndPreamble(inputData)));
			/*
			 * Special handling for scenario - PD Reconnect. All the action codes in the order have to change to ADD, since we need to perform
			 * New Service in C1
			 */
			NodeList scenarioName = inputDocument.getElementsByTagName("ScenarioName");

			for (int i=0; i<scenarioName.getLength(); i++) {
				String scenario = scenarioName.item(i).getChildNodes().item(0).getNodeValue();
				if (scenario.equalsIgnoreCase("PD Reconnect")) {
					NodeList actionCodeList = inputDocument.getElementsByTagName("ActionCode");
					for (int j = 0; j<actionCodeList.getLength(); j++) {
						actionCodeList.item(j).getChildNodes().item(0).setTextContent("ADD");
					}
				}
			}

			Document c1TomOrderDoc = DOMUtils.createNewDOMDocument();

			/*
			 *  Map the Entries in the Bill LookUp Ref based on the lookupRefId and the lookupValue
			 */
			Map<Key, String> billMappingEntityMap = new HashMap<Key, String>();
			for (int i=0; i<billRefMappingEntityList.size(); i++) {
				Key key = new Key(billRefMappingEntityList.get(i).getLookupRefId(), billRefMappingEntityList.get(i).getLookupValue());
				billMappingEntityMap.put(key, billRefMappingEntityList.get(i).getResultValue());
			}

			//Use groupIDs to group the entries in the HashMap -- A TreeMap is used to implement a SortedMap
			Map<Integer, List<MappingEntity>> mappingEntityMap = new TreeMap<Integer, List<MappingEntity>>();
			for(int i=0; i<mappingEntityList.size(); i++) {
				Integer key = Integer.valueOf(mappingEntityList.get(i).getElementGrouping());
				List<MappingEntity> mappingList = mappingEntityMap.get(key);
				if(mappingList == null) {
					mappingList = new ArrayList<MappingEntity>();
				}

				/*
				 * Setting the functionID based on the lookupRefId and grouping number
				 */
				mappingEntityList.get(i).setFunctionID(
						billMappingEntityMap.get(
								new Key(mappingEntityList.get(i).getFuncLookupRefId(), 
										mappingEntityList.get(i).getElementGrouping())));
				mappingList.add(mappingEntityList.get(i));
				mappingEntityMap.put(key, mappingList);
			}

			//Iterate through the groupIDs
			for (Integer key : mappingEntityMap.keySet()) {
				List<MappingEntity> list = mappingEntityMap.get(key);
				String functionID = list.get(0).getFunctionID();
				if (functionID.equals("GENERAL_MAPPINNG")) {
					c1TomOrderDoc = createDocXML(inputDocument, c1TomOrderDoc, list, billMappingEntityMap);
				}
				else if (functionID.equals("EXTERNALID_MAPPING")) {
					c1TomOrderDoc = createDocExtIDXML(inputDocument, c1TomOrderDoc, list);
				}
				else if (functionID.equals("FUNC_REF_MAPPING")) {
					c1TomOrderDoc = createDocFuncMapping(inputDocument, c1TomOrderDoc, list, billMappingEntityMap);
				}
				else if (functionID.equals("COMPONENT_MAPPING")) {
					c1TomOrderDoc = createDocComponentMapping(inputDocument, c1TomOrderDoc, list, billMappingEntityMap);
				}
				else if (functionID.equals("EXTENDED_DATA_MAPPING")) {
					c1TomOrderDoc = createExtendedDataMapping(inputDocument, c1TomOrderDoc, list, billMappingEntityMap);
				}
				else if (functionID.equals("CORRIDOR_PLAN_MAPPING")) {
					c1TomOrderDoc = createCorridorPlanMapping(inputDocument, c1TomOrderDoc, list, billMappingEntityMap);
				}
			}
			String output = DOMUtils.convertDocumenttoString(c1TomOrderDoc);

			//Setting the namespace and other message properties
			output = "<im:Order xmlns:im=\"http://m1.com.sg/bcc/osm/tom/ordersirequestprov\">" + 
					"<corrID>" + taskContext.getOrderId() + "_" + taskContext.getOrderHistoryId()+"</corrID>" +
					"<JMSType>" + taskContext.getTaskMnemonic() + "</JMSType>" +
					output + 
					"</im:Order>";

			output = createSOAPMessage(output);
			textMessage.setJMSCorrelationID(taskContext.getOrderId() + "_" + taskContext.getOrderHistoryId());
			textMessage.setJMSType(taskContext.getTaskMnemonic());
			textMessage.setStringProperty("_wls_mimehdrContent_Type", "text/xml; charset=\"utf-8\"");
			textMessage.setStringProperty("URI", "/osm/wsapi");
			textMessage.setText(output);

			taLogger.log(crmOrderId, String.valueOf(taskContext.getOrderId()), 01, output, "osmsomlogging");
			exitStatus = "success";
		} 
		catch (Exception e){
			e.printStackTrace();
			taLogger.log(crmOrderId, String.valueOf(taskContext.getOrderId()), TAConstants.LOG_ERROR, "ERROR", "osmsomlogging", e);

		} finally {
			if(commonDao != null){
				commonDao.close();
			}
		}
	}

	private static Document createCorridorPlanMapping(Document inputDocument,
			Document c1TomOrderDoc, List<MappingEntity> list, Map<Key, String> billMappingEntityMap) throws Exception {

		NodeList lineItemList = XqueryExecuteUtil.executeXQuery("//ControlData/Functions/*/orderItem/orderItemRef", inputDocument);
		for (int i=0; i<lineItemList.getLength(); i++) {
			Document lineItemDoc = createDocFromNode(lineItemList.item(i));
			//Get a list of all the AttributesIDs with TARGET
			String firstCrmTagXpath = list.get(0).getCrmTagXpath();
			if (firstCrmTagXpath == null || firstCrmTagXpath.isEmpty() || firstCrmTagXpath.equals("-")) {
				throw new IllegalArgumentException("First Xpath of Corridor Mapping cannot be empty or '-'");
			}
			NodeList lineItemAttrIDList = XqueryExecuteUtil.executeXQuery(firstCrmTagXpath, lineItemDoc);
			for (int y=0; y<lineItemAttrIDList.getLength(); y++) {
				//For each AttributeID populate the AttributeID, PointTarget, PointOrigin etc.
				String attributeID = lineItemAttrIDList.item(y).getTextContent();
				c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, list.get(0).getC1TagXpath(), attributeID);

				//Loop through rest of the items to populate in the List
				for (int z=1; z<list.size(); z++) {
					String xquery = list.get(z).getCrmTagXpath();

					if (xquery.contains("#ATTRID#")) {
						xquery = xquery.replace("#ATTRID#", "'" + attributeID + "'");
					}
					//Create nodes in the c1Doc
					String c1Xpath = list.get(z).getC1TagXpath();

					//Logic for checking any lookup or default value for the attribute
					String crmTagValue = "-"; //Value set to '-' by default

					if (xquery == null || xquery.equals("-") ) {
						String defaultValue = list.get(z).getDefaultValue();
						if (defaultValue == null) {
							return c1TomOrderDoc;
						}
						crmTagValue = defaultValue;
					}
					else {
						NodeList result = XqueryExecuteUtil.executeXQuery(xquery, lineItemDoc);
						crmTagValue = getNodeValue(result);
					}

					String attrLookUpRefId = list.get(z).getAttrLookupRefId();
					if (attrLookUpRefId != null && !attrLookUpRefId.isEmpty()) {

						crmTagValue = billMappingEntityMap.get(
								new Key(attrLookUpRefId, crmTagValue));
					}
					c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, c1Xpath, crmTagValue);
				}
			}
		}
		return c1TomOrderDoc;
	}

	private static Document createExtendedDataMapping(Document inputDocument,
			Document c1TomOrderDoc, List<MappingEntity> list, Map<Key, String> billMappingEntityMap) throws XPathExpressionException {
		CommonDao commonDao = DaoFactory.getDao(CommonDao.class);
		ExtnDataMappingEntity extnDataMappingEntity = new ExtnDataMappingEntity();
		List<ExtnDataMappingEntity> extnDataMappingList = commonDao.findAll(extnDataMappingEntity, ExtnDataMappingEntity.SQL_QUERY_BILL_EXTNDATA_MAP);

		Map<String, Map<Integer, ExtnDataMappingEntity>> groupDataMap = new HashMap<String, Map<Integer, ExtnDataMappingEntity>>();
		for (ExtnDataMappingEntity record : extnDataMappingList) {
			if (groupDataMap.containsKey(record.getMappingName())) {
				// Had first record in the list
				TreeMap<Integer, ExtnDataMappingEntity> recordDataMap = (TreeMap<Integer, ExtnDataMappingEntity>) groupDataMap.get(record.getMappingName());
				recordDataMap.put(Integer.valueOf(record.getSeqNo()), record);
			} else {
				// First Record for each group
				TreeMap<Integer, ExtnDataMappingEntity> recordDataMap = new TreeMap<Integer, ExtnDataMappingEntity>();
				recordDataMap.put(Integer.valueOf((record.getSeqNo())), record);
				groupDataMap.put(record.getMappingName(), recordDataMap);
			}
		}
		// Example for extract value
		Set<String> mappingNameList = groupDataMap.keySet();
		for (String mappingName : mappingNameList) {
			TreeMap<Integer, ExtnDataMappingEntity> recordDataMap = (TreeMap<Integer, ExtnDataMappingEntity>) groupDataMap.get(mappingName);
			c1TomOrderDoc = ProcessExtendedDataEntity(inputDocument,
					c1TomOrderDoc, billMappingEntityMap, recordDataMap);

		}		
		return c1TomOrderDoc;
	}

	private static Document ProcessExtendedDataEntity(Document inputDocument,
			Document c1TomOrderDoc, Map<Key, String> billMappingEntityMap,
			TreeMap<Integer, ExtnDataMappingEntity> recordDataMap) throws XPathExpressionException {
		for (Integer recordKey : recordDataMap.keySet()) {
			ExtnDataMappingEntity extnDataMappingEntity = recordDataMap.get(recordKey);
			String paramValue  = extnDataMappingEntity.getValue();
			if (paramValue.equals("-")) {
				paramValue = extnDataMappingEntity.getDefaultValue();
			}
			else if (paramValue.contains("fn:concat(")) {
				//Remove the fn:concat and the enclosing brackets '()'
				paramValue = paramValue.substring(10, paramValue.length());
				paramValue = paramValue.substring(0, paramValue.length()-1);

				//Split string based on the values to be concatenated
				String[] concatStringValues = paramValue.split(",");
				for (int y=0; y< concatStringValues.length; y++) {
					concatStringValues[y] = getNodeValue(XqueryExecuteUtil.executeXQuery(concatStringValues[y], inputDocument));
				}
				String finalConcatString = "";

				for (String string : concatStringValues) {
					finalConcatString = finalConcatString + string + "_";
				}
				finalConcatString = finalConcatString.substring(0, finalConcatString.length()-1);
				paramValue = finalConcatString;
			}
			else {
				paramValue = getNodeValue(XqueryExecuteUtil.executeXQuery(paramValue, inputDocument));
			}
			//Check if there is a need to check the Ref Table
			if (extnDataMappingEntity.getLookupRefId() != null ) {
				paramValue = billMappingEntityMap.get(
						new Key(extnDataMappingEntity.getLookupRefId(), 
								paramValue));
				if (paramValue == null) {
					paramValue = "-";
				}
			}
			//If the element at Seq = 1 is empty, then do not populated the entire list

			if (extnDataMappingEntity.getSeqNo().equals("1") && (paramValue == null || paramValue.isEmpty()) || paramValue.equals("-")) {
				return c1TomOrderDoc;
			}
			c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, extnDataMappingEntity.getParamC1Xpath(), paramValue);
		}
		return c1TomOrderDoc;
	}
	private static Document createDocExtIDXML(Document inputDocument,
			Document c1TomOrderDoc, List<MappingEntity> list) throws Exception {

		CommonDao commonDao = DaoFactory.getDao(CommonDao.class);
		ExtIDAttrMappingEntity extIDAttrMappingEntity = new ExtIDAttrMappingEntity();
		List<ExtIDAttrMappingEntity> extIDMappingList = commonDao.findAll(extIDAttrMappingEntity, ExtIDAttrMappingEntity.SQL_QUERY_BILL_EXTID_ATTR_MAP);
		//Group ExternalIDs based on the ExternalID Type
		Map<Integer, List<ExtIDAttrMappingEntity>> extIDMap = new HashMap<Integer, List<ExtIDAttrMappingEntity>>();
		for (int i=0; i<extIDMappingList.size(); i++) {
			Integer key = Integer.valueOf(extIDMappingList.get(i).getExtIDType());
			if(key.equals("-")) {
				continue;
			}
			List<ExtIDAttrMappingEntity> mappingList = extIDMap.get(key);
			if (mappingList == null) {
				mappingList = new ArrayList<ExtIDAttrMappingEntity>();
			}
			mappingList.add(extIDMappingList.get(i));
			extIDMap.put(key, mappingList);
		}

		//Further group the entries based on the OPG
		Map<Integer, Map<String, List<ExtIDAttrMappingEntity>>> extIDFinalMap = 
				new HashMap<Integer, Map<String, List<ExtIDAttrMappingEntity>>>();
		for (Integer key : extIDMap.keySet()) {
			List<ExtIDAttrMappingEntity> extIDGrouping = extIDMap.get(key);

			Map<String, List<ExtIDAttrMappingEntity>> innerMap = new HashMap<String, List<ExtIDAttrMappingEntity>>();
			for(int i=0; i<extIDGrouping.size(); i++)
			{
				String innerKey = extIDGrouping.get(i).getOpgRefID();
				if (innerKey.equals('-')) {
					continue;
				}
				List<ExtIDAttrMappingEntity> mappingList = innerMap.get(innerKey);
				if(mappingList == null)
				{
					mappingList = new ArrayList<ExtIDAttrMappingEntity>();
				}
				mappingList.add(extIDGrouping.get(i));
				innerMap.put(innerKey, mappingList);
			}
			extIDFinalMap.put(key, innerMap);
		}

		for (Integer extIDType : extIDFinalMap.keySet()) {
			Map<String, List<ExtIDAttrMappingEntity>> innerOPGMap = extIDFinalMap.get(extIDType);
			for (String opgRefID : innerOPGMap.keySet()) {
				List<ExtIDAttrMappingEntity> listExtIDMapping = innerOPGMap.get(opgRefID);

				String stringExtIDType = extIDType + "";
				if (stringExtIDType.equals("91")) {
					c1TomOrderDoc = processExtID91(inputDocument, c1TomOrderDoc, listExtIDMapping);
				}
				else {
					if (opgRefID.equals("-")) {
						continue;
					}
					else if (opgRefID.equals("*")) {
						//Iterate through all the lineItems
						NodeList lineItemList = XqueryExecuteUtil.executeXQuery("//ControlData/Functions/*/orderItem/orderItemRef", inputDocument);
						for (int i=0; i<lineItemList.getLength(); i++) {
							Document lineItemDoc = createDocFromNode(lineItemList.item(i));
							c1TomOrderDoc = processExtIDMap(lineItemDoc, c1TomOrderDoc, listExtIDMapping);
						}
					}
					else {
						//Retrieve only the lineItem with the specific OPG
						String xpath  = "//Functions/*/orderItem/orderItemRef[OMGroupRefID = '" + opgRefID + "']";
						//"//OrderItem[OMGroupRefID = '" + opgRefID + "']";
						NodeList lineItemList = XqueryExecuteUtil.executeXQuery(xpath, inputDocument);
						for (int i=0; i<lineItemList.getLength(); i++) {
							Document lineItemDoc = createDocFromNode(lineItemList.item(i));
							c1TomOrderDoc = processExtIDMap(lineItemDoc, c1TomOrderDoc, listExtIDMapping);
						}
					}
				}
			}
		}
		return c1TomOrderDoc;
	}

	private String createSOAPMessage(String output) throws Exception {
		Document credDoc = getAuthenticationDoc();
		String username = credDoc.getElementsByTagName("Username").item(0).getTextContent();
		String password = com.m1.sg.osm.util.AESEncryption.aesDecrypt(credDoc.getElementsByTagName("Password").item(0).getTextContent());

		return SOAPUtil.createSOAPRequestToNonCritical(output, username, password);
	}

	private static Document createDocComponentMapping(Document inputDocument,
			Document c1TomOrderDoc, List<MappingEntity> list, Map<Key, String> billMappingEntityMap) throws XPathExpressionException, ParserConfigurationException {
		NodeList lineItemList = XqueryExecuteUtil.executeXQuery("//ControlData/Functions/*/orderItem/orderItemRef", inputDocument);

		for (int i=0; i<lineItemList.getLength(); i++) {
			Document lineItemDoc = createDocFromNode(lineItemList.item(i));
			for (MappingEntity mappingEntity : list) {
				c1TomOrderDoc = createElement(c1TomOrderDoc, lineItemDoc, mappingEntity, billMappingEntityMap);
			}
		}
		return c1TomOrderDoc;
	}

	private static Document createElement(Document c1TomOrderDoc,
			Document lineItemDoc, MappingEntity mappingEntity, Map<Key, String> billMappingEntityMap) throws XPathExpressionException {
		String c1Xpath = mappingEntity.getC1TagXpath();
		String crmXpath = mappingEntity.getCrmTagXpath();
		String crmTagValue = "-"; //Value set to '-' by default

		if (crmXpath == null || crmXpath.equals("-") ) {
			String defaultValue = mappingEntity.getDefaultValue();
			if (defaultValue == null) {
				return c1TomOrderDoc;
			}
			crmTagValue = defaultValue;
		}
		else {
			NodeList result = XqueryExecuteUtil.executeXQuery(crmXpath, lineItemDoc);
			crmTagValue = getNodeValue(result);
		}

		String attrLookUpRefId = mappingEntity.getAttrLookupRefId();
		if (attrLookUpRefId != null && !attrLookUpRefId.isEmpty()) {

			crmTagValue = billMappingEntityMap.get(
					new Key(attrLookUpRefId, crmTagValue));
		}

		if (crmTagValue == null || crmTagValue.isEmpty()) {
			crmTagValue = "-";
		}

		c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, c1Xpath, crmTagValue);
		return c1TomOrderDoc;
	}

	private static Document createDocFromNode(Node item) throws ParserConfigurationException {
		Document newDocument = DOMUtils.createNewDOMDocument();
		Node importedNode = newDocument.importNode(item, true);
		newDocument.appendChild(importedNode);
		return newDocument;		
	}

	private static Document createDocFuncMapping(Document inputDocument,
			Document c1TomOrderDoc, List<MappingEntity> list, Map<Key, String> billMappingEntityMap) throws XPathExpressionException {
		for (MappingEntity mappingEntity : list) {
			String c1Xpath = mappingEntity.getC1TagXpath();
			String crmXpath = mappingEntity.getCrmTagXpath();
			String crmTagValue = "";
			if (!crmXpath.equals("-")) {				
				NodeList result = XqueryExecuteUtil.executeXQuery(crmXpath, inputDocument);
				crmTagValue = getNodeValue(result);
			}
			if (mappingEntity.getC1Tag().equals("FuncRefID")) {
				String[] splitTagValue = crmTagValue.split("_");
				String funcName = splitTagValue[splitTagValue.length - 1];
				funcName = "SomFunc_" + funcName;
				crmTagValue = funcName;
			}
			if (mappingEntity.getC1Tag().equals("C1Flag")) {
				crmTagValue = "Y";
			}
			if (mappingEntity.getC1Tag().equals("TaskRequest")) {
				crmTagValue = "New";
			}
			c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, c1Xpath, crmTagValue);
		}
		return c1TomOrderDoc;
	}

	private static Document processExtID91(Document inputDocument, Document c1TomOrderDoc,
			List<ExtIDAttrMappingEntity> listExtIDMapping) throws XPathExpressionException {

		ExtIDAttrMappingEntity currentValue = new ExtIDAttrMappingEntity();
		ExtIDAttrMappingEntity previousValue = new ExtIDAttrMappingEntity();
		ExtIDAttrMappingEntity actionCode = new ExtIDAttrMappingEntity();

		for (ExtIDAttrMappingEntity extIDAttrMappingEntity : listExtIDMapping) {
			String attrName = extIDAttrMappingEntity.getAttrName();
			if (attrName.contains("Previous")) {
				previousValue = extIDAttrMappingEntity;
			}
			else if (attrName.contains("Current")) {
				currentValue = extIDAttrMappingEntity;
			}
			else if (attrName.contains("Action")) {
				actionCode = extIDAttrMappingEntity;
			}
		}
		if (previousValue.getCrmXpath().equals("-") || currentValue.getCrmXpath().equals("-")) {
			return c1TomOrderDoc;
		}
		//Check if the current and previous values exist. Populate only if both exist
		NodeList resultCurValue = XqueryExecuteUtil.executeXQuery(currentValue.getCrmXpath(), inputDocument);
		String crmTagValueCurrent = getNodeValue(resultCurValue);
		NodeList resultPrevValue = XqueryExecuteUtil.executeXQuery(previousValue.getCrmXpath(), inputDocument);
		String crmTagValuePrevious = getNodeValue(resultPrevValue);

		if (crmTagValueCurrent.isEmpty() || crmTagValueCurrent.equals("-"))
		{
			return c1TomOrderDoc;
		}
		if (crmTagValueCurrent != null && !crmTagValueCurrent.isEmpty() && 
				crmTagValuePrevious != null && !crmTagValuePrevious.isEmpty()) {

			//The following ExternalIDType tag should always be inserted first
			//The following is to build the xpath for ExternalIDType
			String extIDXpath = currentValue.getC1Xpath();
			extIDXpath = extIDXpath.replaceAll("/ExternalID/", "/ExternalID*/");
			StringBuilder sBuilder = new StringBuilder(extIDXpath);
			sBuilder.replace(extIDXpath.lastIndexOf("/") + 1, extIDXpath.length(), "ExternalIDType" );
			extIDXpath = sBuilder.toString();
			c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, extIDXpath, currentValue.getExtIDType());

			// Set the line for ParentCompLineItemID
			NodeList result = XqueryExecuteUtil.executeXQuery(currentValue.getCrmXpath() + "/../LineItemID", inputDocument);
			String lineItemID = getNodeValue(result);
			String parentCompLineItemIDXpath = currentValue.getC1Xpath();
			sBuilder = new StringBuilder(parentCompLineItemIDXpath);
			sBuilder.replace(parentCompLineItemIDXpath.lastIndexOf("/") + 1, parentCompLineItemIDXpath.length(), "ParentCompLineItemID" );
			parentCompLineItemIDXpath = sBuilder.toString();
			c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, parentCompLineItemIDXpath, lineItemID);
			c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, previousValue.getC1Xpath(), crmTagValuePrevious);
			c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, currentValue.getC1Xpath(), crmTagValueCurrent);

			String crmXpathActionCode = actionCode.getCrmXpath();
			if (!crmXpathActionCode.equals("-")) {
				NodeList resultActionValue = XqueryExecuteUtil.executeXQuery(crmXpathActionCode, inputDocument);
				String crmTagValueAction = getNodeValue(resultActionValue);

				if (crmTagValueAction.equals("New Service")) {
					crmTagValueAction = "ADD";
				}
				else if (crmTagValueAction.equals("Terminate")) {
					crmTagValueAction = "REMOVE";
				}
				else if (crmTagValueAction.equals("Change Bill Plan")) {
					crmTagValueAction = "UPDATE";
				}
				if(!crmTagValueAction.isEmpty() || !crmTagValueAction.equals('-')) {
					c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, actionCode.getC1Xpath(), crmTagValueAction);
				}
			}
		}
		return c1TomOrderDoc;
	}

	private static Document processExtIDMap(Document lineItemDoc,
			Document c1TomOrderDoc, List<ExtIDAttrMappingEntity> listExtIDMapping) throws XPathExpressionException {
		NodeList result = XqueryExecuteUtil.executeXQuery("//LineItemID", lineItemDoc);
		String lineItemID = getNodeValue(result);
		result = XqueryExecuteUtil.executeXQuery("//LineItem[ParentLineItemID = '0']/LineItemID", lineItemDoc);
		String mainLineItemID = getNodeValue(result);

		ExtIDAttrMappingEntity currentValue = new ExtIDAttrMappingEntity();
		ExtIDAttrMappingEntity previousValue = new ExtIDAttrMappingEntity();
		ExtIDAttrMappingEntity actionCode = new ExtIDAttrMappingEntity();

		for (ExtIDAttrMappingEntity extIDAttrMappingEntity : listExtIDMapping) {
			String attrName = extIDAttrMappingEntity.getAttrName();
			if (attrName.contains("Previous")) {
				previousValue = extIDAttrMappingEntity;
			}
			else if (attrName.contains("Current")) {
				currentValue = extIDAttrMappingEntity;
			}
			else if (attrName.contains("Action")) {
				actionCode = extIDAttrMappingEntity;
			}
		}
		String crmXpathCurrentValue = processXpath(mainLineItemID, lineItemID, currentValue.getCrmXpath());

		if (crmXpathCurrentValue.isEmpty() || crmXpathCurrentValue.equals("-")) {
			return c1TomOrderDoc;
		}
		else {
			NodeList resultCurValue = XqueryExecuteUtil.executeXQuery(crmXpathCurrentValue, lineItemDoc);
			String crmTagValueCurrent = getNodeValue(resultCurValue);

			if (crmTagValueCurrent.isEmpty() || crmTagValueCurrent.equals("-")) {
				return c1TomOrderDoc;
			}
			if (crmTagValueCurrent != null && !crmTagValueCurrent.isEmpty()) {
				//The following ExternalIDType tag should always be inserted first
				//The following is to build the xpath for ExternalIDType
				String extIDXpath = currentValue.getC1Xpath();
				extIDXpath = extIDXpath.replaceAll("/ExternalID/", "/ExternalID*/");
				StringBuilder sBuilder = new StringBuilder(extIDXpath);
				sBuilder.replace(extIDXpath.lastIndexOf("/") + 1, extIDXpath.length(), "ExternalIDType" );
				extIDXpath = sBuilder.toString();
				c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, extIDXpath, currentValue.getExtIDType());

				// Set the line for ParentCompLineItemID
				String parentCompLineItemIDXpath = currentValue.getC1Xpath();
				sBuilder = new StringBuilder(parentCompLineItemIDXpath);
				sBuilder.replace(parentCompLineItemIDXpath.lastIndexOf("/") + 1, parentCompLineItemIDXpath.length(), "ParentCompLineItemID" );
				parentCompLineItemIDXpath = sBuilder.toString();
				c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, parentCompLineItemIDXpath, lineItemID);

				String crmXpathPreviousValue = previousValue.getCrmXpath();
				if (!crmXpathPreviousValue.equals("-")) {
					NodeList resultPrevValue = XqueryExecuteUtil.executeXQuery(processXpath(mainLineItemID, lineItemID, crmXpathPreviousValue), lineItemDoc);
					String crmTagValuePrevious = getNodeValue(resultPrevValue);

					if(crmTagValuePrevious!= null) {
						if (!crmTagValuePrevious.isEmpty()) {
							c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, previousValue.getC1Xpath(), crmTagValuePrevious);
						}
					}
				}
				c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, currentValue.getC1Xpath(), crmTagValueCurrent);
				String crmXpathActionCode = actionCode.getCrmXpath();
				if (!crmXpathActionCode.equals("-")) {
					NodeList resultActionValue = XqueryExecuteUtil.executeXQuery(processXpath(mainLineItemID, lineItemID, crmXpathActionCode), lineItemDoc);
					String crmTagValueAction = getNodeValue(resultActionValue);

					if(crmTagValueAction!= null) {
						if (!crmTagValueAction.isEmpty()) {
							c1TomOrderDoc = OMPollerXMLUtil.createElementFromPath(c1TomOrderDoc, actionCode.getC1Xpath(), crmTagValueAction);
						}
					}
				}
			}
		}
		return c1TomOrderDoc;
	}

	private static String processXpath(String mainLineItemID, String lineItemID,
			String xpath) {

		Pattern pattern = Pattern.compile("(\\#)(.*?)(\\#)");
		Matcher matcher = pattern.matcher(xpath);
		List<String> listMatches = new ArrayList<String>();

		while(matcher.find())
		{
			listMatches.add(matcher.group(2));
		}
		for(String s : listMatches)
		{
			if (s.equalsIgnoreCase("LineItemID")) {
				xpath.replace("#" + s + "#", "'" + lineItemID + "'");
			}
			else if (s.equalsIgnoreCase("MainLineItemID")) {
				xpath.replace("#" + s + "#", "'" + mainLineItemID + "'");
			}
		}
		return xpath;
	}

	private static String getNodeValue(NodeList result) {

		if (result==null || result.getLength() == 0) {
			return "-";
		}
		else {			
			return result.item(0).getTextContent();
		}
	}

	private static Document createDocXML(Document inputDocument,
			Document c1TomOrderDoc, List<MappingEntity> list, Map<Key, String> billMappingEntityMap) throws XPathExpressionException {
		for (MappingEntity mappingEntity : list) {
			c1TomOrderDoc = createElement(c1TomOrderDoc, inputDocument, mappingEntity, billMappingEntityMap);
		}
		return c1TomOrderDoc;
	}

	private static String removeNamespaceAndPreamble(String xmlString) {
		// Original
//		return xmlString.replaceAll("(<\\?[^<]*\\?>)?", ""). /* remove preamble */
//				replaceAll(" xmlns.*?(\"|\').*?(\"|\')", "")  /* remove xmlns declaration */
//				.replaceAll("(<)(\\w+:)(.*?>)", "$1$3").replaceAll("im:", "") /* remove opening tag prefix */
//				.replaceAll("(</)(\\w+:)(.*?>)", "$1$3"); /* remove closing tags prefix */
		// Tuned
		return xmlString.replaceAll("<\\?.+?>", "") /* remove preamble */
			   .replaceAll(" xmlns.+?(\"|\').+?(\"|\')", "") /* remove xmlns declaration */
			   .replaceAll("(<)(\\w+:)(.+?>)", "$1$3").replaceAll("im:", "") /* remove opening tag prefix */
			   .replaceAll("(</)(\\w+:)(.+?>)", "$1$3"); /* remove closing tags prefix */
	}
	
	private static Document getAuthenticationDoc() throws Exception {
		File credFile = new File ("conf/osmapp_secure.xml");
		DocumentBuilderFactory dbFactory = ObjectFactoryUtil.DOCUMENTBUIDER_FACTORY.get();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(credFile);
		doc.getDocumentElement().normalize();
		
		return doc;
	}
}
