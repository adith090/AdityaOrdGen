package com.m1.sg.osm.util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.m1.sg.osm.util.XqueryUtil;

public class XqueryExecuteUtil {
	
	
	public static NodeList executeXQuery(String xquery, Document lineItemDoc){
				
		try {
			xquery = "<List>{" + xquery + "}</List>";
            String output = XqueryUtil.executeXQueryText(lineItemDoc, xquery);
   			Document document = DOMUtils.createDOMDocument(output);
   			NodeList list = document.getElementsByTagName("List").item(0).getChildNodes();
   			return list;
			
		} catch (Exception e){
			e.printStackTrace();
			return null;
		} 
		
	}
	
}
