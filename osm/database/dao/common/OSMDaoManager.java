package com.m1.sg.osm.database.dao.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.m1.sg.osm.database.dao.Dao;
import com.m1.sg.osm.database.dao.DaoFactory;

public class OSMDaoManager {
	Map<String,Dao> daoMap;
	
	public OSMDaoManager(){
		daoMap = new HashMap<String,Dao>();
	}
	
	public Dao getDao(Class daoClass){
		String className = daoClass.getName();
		if(!daoMap.containsKey(className)){
			daoMap.put(className, DaoFactory.getDao(daoClass));
		}
		return daoMap.get(className);
	}
	
	public void close(){
		for(Entry<String,Dao> item:daoMap.entrySet()){
			((OSMDaoManager) item.getValue()).close();
		}
	}
	
}
