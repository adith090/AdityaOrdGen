package com.m1.sg.bcc.om.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/************************************************************************************************************************************
 *  
 * @author a.songwattanasakul
 * Package com.m1.sg.bcc.om.util
 * Description: This utility class created for convert object type in out application
 * Modification Log:
 * Date				Name							Description
 * ----------------------------------------------------------------------------------------------------------------------------------
 * 16/05/2013		Apiluck Songwattanasakul 		Initial Class and implement the logic inside
 *************************************************************************************************************************************/

public class OMPollerClassTypeUtil {

	/**
	 * Convert Object to Object array
	 * 
	 * @param objectTarget	Object you need convert to object array
	 * @return				Object array
	 */
	public static Object[] objectToArray(Object objectTarget){
		ArrayList<Object> objectList = new ArrayList<Object>();
		objectList.add(objectTarget);
		return objectList.toArray();
	}
	
	public static Object createObject(String className, Object value) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException{
		Class<?> baseCls = Class.forName(className);
		Constructor<?> newCon = baseCls.getConstructor(value.getClass());
		return newCon.newInstance(value);
	}
	
}
