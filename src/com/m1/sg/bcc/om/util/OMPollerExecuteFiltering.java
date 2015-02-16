package com.m1.sg.bcc.om.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;

public class OMPollerExecuteFiltering {
	
	public static String executeXQuery(String inputXML, String xquery){
		
		StringWriter strWriter = null;
		InputStream inpStram = null;
		StringReader strReader = null;
		
		try {
			
			strWriter = new StringWriter();
			
			inpStram = new ByteArrayInputStream(xquery.getBytes());
			
			Configuration C = new Configuration();
			StaticQueryContext SQC = new StaticQueryContext(C);
			DynamicQueryContext DQC = new DynamicQueryContext(C);
		
			Properties props=new Properties();
			props.setProperty(OutputKeys.METHOD,"xml");
			props.setProperty(OutputKeys.INDENT,"no");
			props.setProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
			
			XQueryExpression expression = null;
		
			expression = SQC.compileQuery(inpStram, null);
			
			strReader = new StringReader(inputXML);
			InputSource xmlInput = new InputSource(strReader);
			SAXSource SAXs=new SAXSource(xmlInput);
			
			DocumentInfo DI=SQC.buildDocument(SAXs);
            DQC.setContextItem(DI);
            
            expression.run(DQC,new StreamResult(strWriter),props);
            
            return strWriter.toString();
			
		} catch (Exception e){
			e.printStackTrace();
			return null;
		} finally {
			if(strReader != null){strReader.close();}
			if(strWriter != null){try {strWriter.close();} catch (IOException e) {e.printStackTrace();}}
			if(inpStram != null){try {inpStram.close();} catch (IOException e) {e.printStackTrace();}}
		}
		
	}
	
}
