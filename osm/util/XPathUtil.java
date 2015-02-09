package com.m1.sg.osm.util;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class XPathUtil {

	public static NodeList executeXPath(String xpath, Document fullDoc) throws XPathExpressionException{
		XPathFactory xPathFac = ObjectFactoryUtil.XPATH_FACTORY.get();
		XPath xPath = xPathFac.newXPath();
		XPathExpression expr = xPath.compile(xpath);
		NodeList nodeListResult = (NodeList) expr.evaluate(fullDoc, XPathConstants.NODESET);
		return nodeListResult;
	}
	
	public static Document convertNodeListToDocument(NodeList nodeList) throws ParserConfigurationException{
		Document newDoc = DOMUtils.createNewDOMDocument();
		Element root = null;
		root = newDoc.createElement("root");
		newDoc.appendChild(root);
		for(int i=0; i < nodeList.getLength(); i++){
			Node node = nodeList.item(i);
			
			if(node.getNodeType() == Element.ATTRIBUTE_NODE){
				Attr elementCache = (Attr) node;
				Text textNode = newDoc.createTextNode(elementCache.getValue());
				root.appendChild(textNode);
			}
			else if(node.getNodeType() == Element.TEXT_NODE){
				Text textNode = newDoc.createTextNode(node.getTextContent());
				root.appendChild(textNode);
			}
			else {
				Node copyNode = newDoc.importNode(node, true);
				root.appendChild(copyNode);
			}
			break;
		}
		
		return newDoc;
	}
}
