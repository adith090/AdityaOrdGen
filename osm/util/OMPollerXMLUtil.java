package com.m1.sg.osm.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/************************************************************************************************************************************
 *  
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.util
 * Description: This utility class created for XML extractor and manipulation 
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMPollerXMLUtil {

	/**
	 * Method create new Document
	 * 
	 * @return
	 * @throws ParserConfigurationException
	 */
	
	public static Document createNewDOMDocument() throws ParserConfigurationException{
		DocumentBuilderFactory domFactory = ObjectFactoryUtil.DOCUMENTBUIDER_FACTORY.get();
		domFactory.setNamespaceAware(false);
		DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
		return domBuilder.newDocument();
	}
	
	/**
	 * Method create Document with initial XML String
	 * 
	 * @param inputXML		XML Text
	 * @return
	 * @throws Exception
	 */
	public static Document createDOMDocument(String inputXML) throws Exception{

		InputSource in = new InputSource();
		StringReader strReader = new StringReader(inputXML);
		
		try {
		
			DocumentBuilderFactory domFactory = ObjectFactoryUtil.DOCUMENTBUIDER_FACTORY.get();
			domFactory.setNamespaceAware(false);
			DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
			
			in.setCharacterStream(strReader);
			
			return domBuilder.parse(in);
		
		} catch (Exception e){
			throw e;
		} finally {
			strReader.close();
		}
	}
	
	/**
	 * convert Document XML to XML String
	 * 
	 * @param doc			XML Document
	 * @return
	 * @throws Exception
	 */
	public static String convertDocumenttoString(Document doc) throws Exception{
		
		TransformerFactory factory = ObjectFactoryUtil.TRANSFORMAER_FACTORY.get();
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter writer = new StringWriter();
		Result result = new StreamResult(writer);
		Source source = new DOMSource(doc);
		transformer.transform(source, result);
		writer.close();
		String finalMessage = writer.toString();
		
		return finalMessage;
		
	}
	
	/**
	 * This method created for create element XML into document. Input is XML Path format for example
	 * Order/OrderHeader/RevisionNumber means create RevisionNumber field under Order/OrderHeader if
	 * this path not appear this method will create element 
	 * 
	 * @param doc		XML Document
	 * @param path		XML Path
	 * @param value		XML Value
	 * @return
	 */
	public static Document createElementFromPath(Document doc, String path, String value){
		String[] pathSplit = path.split("/");
		NodeList rootNodeList = doc.getElementsByTagName(pathSplit[0]);
		int countDeep = 1;
		if(rootNodeList.getLength() != 0){
			Node parentNode = rootNodeList.item(0);
			NodeList tempNodeList = rootNodeList.item(0).getChildNodes();
			while(true){
				if(countDeep == pathSplit.length){break;}
				else {
					ArrayList<Element> childTargetElement = checkContainsElementInNodeList(tempNodeList, pathSplit[countDeep]);
					if(childTargetElement.size() != 0 && !pathSplit[countDeep].contains("*")){
						parentNode = childTargetElement.get(childTargetElement.size()-1);
						tempNodeList = parentNode.getChildNodes();
						countDeep++;
					} else {
						String[] createElementList = Arrays.copyOfRange(pathSplit, countDeep, pathSplit.length);
						int counter = 0;
						for(String eachOfElement : createElementList ){
							eachOfElement = eachOfElement.replace("*", "");
							if(createElementList.length - 1 == counter){parentNode = createElementUnderParent(doc, parentNode, eachOfElement, value);}
							else {parentNode = createElementUnderParent(doc, parentNode, eachOfElement, "");}
							counter++;
						}
						break;
					}
				}
			}
		} else {
			Node parentNode = doc;
			int counter = 0;
			for(String eachOfElement : pathSplit ){
				if(pathSplit.length - 1 == counter){parentNode = createElementUnderParent(doc, parentNode, eachOfElement, value);}
				else {parentNode = createElementUnderParent(doc, parentNode, eachOfElement, "");}
				counter++;
			}
		}
		doc.normalize();
		return doc;
	}
	
	/**
	 * This method create for change all XML Namespace
	 * 
	 * @param doc
	 * @return
	 */
//	public static Document changeXMLNamespace(Document doc){
//		Properties prop = OMPollerConfigurationLoader.getPollerPropertiesConfiguration();
//		String namespace = prop.getProperty("COM_OSM_NAMESPACE");
//		NodeList allElementNode = doc.getElementsByTagName("*");
//		for(int i=0; i < allElementNode.getLength(); i++){
//			Element element = (Element) allElementNode.item(i);
//			doc.renameNode(element, namespace, "im:" + element.getNodeName());
//		}
//		return doc;
//	}
	
	/**
	 * This method created for create element XML under the parent specific
	 * 
	 * @param doc
	 * @param parentNode	Parent Node
	 * @param tagName		Tag Name
	 * @param value			Tag Value
	 * @return
	 */
	private static Node createElementUnderParent(Document doc, Node parentNode, String tagName, String value){
		Element element = doc.createElement(tagName);
		if(!value.equals("null")){Text content = doc.createTextNode(value);element.appendChild(content);}
		else{Text content = doc.createTextNode("");element.appendChild(content);}
		return parentNode.appendChild(element);
	}
	
	/**
	 * This method use for checking the element under the Node List Input.
	 * 
	 * @param nodeList
	 * @param tagName
	 * @return
	 */
	private static ArrayList<Element> checkContainsElementInNodeList(NodeList nodeList, String tagName){
		
		ArrayList<Element> elementList = new ArrayList<Element>();
		
		for(int i=0; i < nodeList.getLength(); i++){
			Node node = nodeList.item(i);
			if(node.getNodeType() == Element.ELEMENT_NODE){
				Element element = (Element) node;
				if(element.getNodeName().equals(tagName)){
					elementList.add(element);
				}
			}
		}
		return elementList;
	}
	
	/**
	 * Read XML From file
	 * 
	 * @param pathXML
	 * @return	file Content
	 * @throws Exception
	 */
	public static String readXMLFile(String pathXML) throws Exception {
		File file = new File(pathXML);
		StringBuffer contents = new StringBuffer();
		BufferedReader reader = null;
		FileReader fileReader = null;

		try {
			fileReader = new FileReader(file);
			reader = new BufferedReader(fileReader);
			String text = null;

			while ((text = reader.readLine()) != null) {
				contents.append(text).append(
						System.getProperty("line.separator"));
			}
		} catch (FileNotFoundException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				if (fileReader != null){
					fileReader.close();
				}
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		return contents.toString();
	}
	
}
