package com.m1.bcc.spl.util;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
import org.xml.sax.SAXException;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 07/11/2012					Ravikumar G				Created
 * 07/05/2013					Sudharsan 				Bug 2044 [Internal] Logging Level update to ERROR In Catch and remove system.out.println if any
 * 10/01/2014					Ravikumar G				Bug#23067 - Added isValidPath method to check xpath in xml
 ******************************************************************************/

public class XPathReader {

	private XPath xPath;
	private Document document;

	public XPathReader() {}

	/**
	 *
	 * @param requestXml
	 */
	public XPathReader(String requestXml) {
		initialize(requestXml);
	}

	/**
	 *
	 * @param requestXml
	 */
	public void initialize(String requestXml) {
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(false);
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			document = documentBuilder.parse(new ByteArrayInputStream(requestXml.getBytes()));
			xPath = XPathFactory.newInstance().newXPath();
		}catch(ParserConfigurationException parserConfigurationException) {
			parserConfigurationException.printStackTrace();
		}catch(SAXException saxException) {
			saxException.printStackTrace();
		}catch(IOException ioException) {
			ioException.printStackTrace();
		}
	}

	/**
	 *
	 * @param expression
	 * @param returnType
	 * @return
	 * @throws XPathExpressionException
	 */
	public Object getXpathValue(String expression, QName returnType) throws XPathExpressionException {
		Object commandParameterValue = xPath.evaluate(expression, document, XPathConstants.STRING);
		return commandParameterValue;
	}

	public Boolean isValidPath(String path) throws Exception {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
	    XPathExpression expr1 = xpath.compile(path);
	    Boolean validPath = (Boolean) (expr1.evaluate(document, XPathConstants.BOOLEAN));
	    return validPath;
	}
	
	public Document getDocument(String requestXml) {
		Document xmlDocument = null;
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(false);
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			xmlDocument = documentBuilder.parse(new ByteArrayInputStream(requestXml.getBytes()));
		}catch(ParserConfigurationException parserConfigurationException) {
			parserConfigurationException.printStackTrace();
		}catch(SAXException saxException) {
			saxException.printStackTrace();
		}catch(IOException ioException) {
			ioException.printStackTrace();
		}
		return xmlDocument;
	}

	
	public static NodeList executeXPath(String xpath, Document fullDoc) throws XPathExpressionException{
		XPathFactory xPathFac = XPathFactory.newInstance();
		XPath xPath = xPathFac.newXPath();
		XPathExpression expr = xPath.compile(xpath);
		NodeList nodeListResult = (NodeList) expr.evaluate(fullDoc, XPathConstants.NODESET);
		return nodeListResult;
	}
	
	/**
	 * @param nodeList
	 * @return
	 * @throws ParserConfigurationException
	 */
	public static Document convertNodeListToDocument(NodeList nodeList) throws ParserConfigurationException{
		Document newDoc = createNewDOMDocument();
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
			//break;
		}
		
		return newDoc;
	}


	public static Document createNewDOMDocument() throws ParserConfigurationException{
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(false);
		DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
		return domBuilder.newDocument();
	}
	
}

class NamespaceResolver implements NamespaceContext {
    // the delegate
    private Document document;

    public NamespaceResolver() {
    }

    /**
     * This constructor stores the source document to search the namespaces in
     * it.
     *
     * @param document
     *            source document
     */
    public NamespaceResolver(Document document) {
    	//System.out.println("NAME SPACE URI IN CONSTRUCTOR " + document.lookupNamespaceURI(null));
        this.document = document;
    }

    /**
     * The lookup for the namespace uri is delegated to the stored document.
     *
     * @param prefix
     *            to search for
     * @return uri
     */
    public String getNamespaceURI(String prefix) {
    	 if (prefix == null) {
             throw new IllegalArgumentException("No prefix provided!");
         } else if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
             return "http://m1.com.sg/bcc/osm/tom/provrequest";
         } else if (prefix.equals("ns1")) {
             return "http://m1.com.sg/bcc/osm/tom/provrequest";
         } else {
             return XMLConstants.NULL_NS_URI;
         }
    }

    /**
     * This method is not needed in this context, but can be implemented in a
     * similar way.
     */
    public String getPrefix(String namespaceURI) {
        // not implemented yet
        return null;

    }

    public Iterator getPrefixes(String namespaceURI) {
        // not implemented yet
        return null;
    }
}



