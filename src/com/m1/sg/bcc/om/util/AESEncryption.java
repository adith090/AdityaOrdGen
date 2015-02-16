package com.m1.sg.bcc.om.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import weblogic.utils.encoders.BASE64Decoder;

public class AESEncryption {

	private static final String PWD = "29483756012948536";
	
	public static String aesDecrypt(String text) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

		//setup key
		byte[] keyBytes= new byte[16];
		byte[] b= PWD.getBytes("UTF-8");
		int len= b.length; 
		if (len > keyBytes.length) {
			len = keyBytes.length;
		}
		System.arraycopy(b, 0, keyBytes, 0, len);
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(keyBytes);
		cipher.init(Cipher.DECRYPT_MODE,keySpec,ivSpec);

		BASE64Decoder decoder = new BASE64Decoder();
		byte [] results = cipher.doFinal(decoder.decodeBuffer(text));
		return new String(results,"UTF-8");
	}
	
}
