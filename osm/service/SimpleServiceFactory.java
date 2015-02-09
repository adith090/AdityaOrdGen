package com.m1.sg.osm.service;

import java.util.Properties;

import com.m1.sg.osm.cfg.ConfigurationLoader;

/**
 * 
 * 
 * @author 
 */
public class SimpleServiceFactory extends ServiceFactory {

	// The configuration loader
	private static ConfigurationLoader cfgLoader = ConfigurationLoader.getInstance();	
	
	/**
	 * Constructs an instance of <code>SimpleServiceFactory</code> class
	 * with the specified <code>Properties</code> object.
	 * 
	 * @param prop   The <code>Properties</code> object.
	 */
	public SimpleServiceFactory(Properties prop) {
		super(prop);
	}

	/**
	 * Lookup the <code>Service</code> instance from the given 
	 * <code>Service</code> class.
	 * 
	 * @param  clazz   The <code>Service</code> class
	 * @return An instance of <code>Service</code> class
	 */	
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Object> T lookupService(Class<T> clazz) {
		Class<?> serviceClazz = cfgLoader.getServiceClass(clazz.getSimpleName());	
		if(serviceClazz == null) {
			serviceClazz = cfgLoader.getDaoClass(clazz.getName());
		}		
		if(serviceClazz == null) {
			throw new RuntimeException("Not found service id: " + clazz);
		} else {
			try {		
		        return (T)serviceClazz.newInstance();	        
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}			
		}
	}

}
