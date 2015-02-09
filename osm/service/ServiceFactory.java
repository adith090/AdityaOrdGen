package com.m1.sg.osm.service;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.m1.sg.osm.cfg.ConfigurationLoader;

/**
 * 
 * 
 * @author 
 */
public abstract class ServiceFactory {

	// The configuration loader
	private static ConfigurationLoader cfgLoader = ConfigurationLoader.getInstance();	
	
	// Map of factory id and factory instance
	private static Map<String,ServiceFactory> cache = new HashMap<String,ServiceFactory>();	
	
	/**
	 * Constructs an instance of <code>ServiceFactory</code> with the specified
	 * properties from the configuration.
	 * 
	 * @param  prop   The properties 
	 */
    public ServiceFactory(Properties prop) { }     
	
	/**
	 * Creates an instance of <code>Service</code> class from the 
	 * given <code>Service</code> class.
	 * 
	 * @param  clazz
	 * @return An instance of <code>Service</code> class
	 */
	public abstract <T extends Object> T lookupService(Class<T> clazz);	
		
	/**
	 * Gets an instance of <code>Service</code> class from the 
	 * given <code>Service</code> class.
	 * 
	 * @param  clazz       The <code>Service</code> class
	 * @return An instance of <code>Service</code> class.
	 */
	public static <T extends Object> T getService(Class<T> clazz) {
		return lookupServiceFactory(null).lookupService(clazz);
	}
		
	/**
	 * Looks up instance of <cod>ServiceFactory</code> class from 
	 * the given factory id.
	 * 
	 * @param  factoryId   The factory id
	 * @return An instance of <code>ServiceFactory</code> class
	 */
	private static ServiceFactory lookupServiceFactory(String factoryId) {
		factoryId = factoryId == null ? "default" : factoryId;
		ServiceFactory serviceFactory = cache.get(factoryId);
		// lookup from cached factory.
		if(serviceFactory != null) {
			return serviceFactory;			
		// create new instance and cache.	
		} 
		synchronized(cache) {
			serviceFactory = cache.get(factoryId);
			if(serviceFactory == null) {
			    Class<?> clazz = cfgLoader.getServiceFactoryClass(factoryId);
			    Properties prop = cfgLoader.getServiceFactoryProperties(factoryId);
			    if(clazz == null) {
			        throw new RuntimeException("Not found factory id '" + factoryId + "'.");
			    }
			    try {
    		        Constructor<?> constructor = clazz.getConstructor(Properties.class);
    		        serviceFactory = (ServiceFactory)constructor.newInstance(prop);
    		        // save into cache
    		        cache.put(factoryId, serviceFactory);       		    
			    } catch (Exception ex) {
				    throw new RuntimeException(ex);
			    }
			}
			return serviceFactory;
		}
	}
	
}
