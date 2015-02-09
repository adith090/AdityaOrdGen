package com.m1.sg.osm.database.util;

import com.m1.sg.osm.database.dao.DaoFactory;
import com.m1.sg.osm.database.dao.common.CommonDao;
import com.m1.sg.osm.database.entity.common.OSMTransactionEntity;

public class UpdateUtil {

	public static void updateOSMTransaction(OSMTransactionEntity osmEntity, String sqlId) throws Exception{

		CommonDao commonDao = null;
		
		try {
			
			commonDao = DaoFactory.getDao(CommonDao.class);
			commonDao.update(osmEntity, sqlId);
			
			commonDao.commit();
			
		} catch (Exception e){
			
			throw e;
			
		} finally {
			if(commonDao != null){
				commonDao.close();
			}
		}
	}
	
}
