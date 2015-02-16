package com.m1.bcc.spl.util;

import java.io.UnsupportedEncodingException;
import org.springframework.core.convert.converter.Converter;

import com.m1.bcc.spl.constants.ApplicationConstants;

import common.util.TALogger;

/**
 * 
 * @author ravikumar.gopalan Simple byte array to String converter;
 */
public class ByteArrayToStringConverter implements Converter<byte[], String> {
	private String charSet = "UTF-8";

	public String convert(byte[] bytes) {
		try {
			TALogger taLogger = TALogger.getTALogger();
			taLogger.log("In ByteArrayToStringConverter Class",ApplicationConstants.LOG_INFO,"bytes.length="+bytes.length+"     bytes.toString()="+bytes.toString(),"adapterlogging");
			String stringResponse=new String(bytes,bytes.length);
			taLogger.log("In ByteArrayToStringConverter Class",ApplicationConstants.LOG_INFO,"stringResponse.length()="+stringResponse.length()+"  stringResponse="+stringResponse,"adapterlogging");
			
			return stringResponse;
			
		} catch (Exception e) {
			e.printStackTrace();
			return new String(bytes);
		}
	}

	/**
	 * 
	 * @return charSet
	 */
	public String getCharSet() {
		return charSet;
	}

	/**
	 * 
	 * @param charSet
	 *            the charSet to set
	 */
	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}
}