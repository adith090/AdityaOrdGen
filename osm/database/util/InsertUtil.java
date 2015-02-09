package com.m1.sg.osm.database.util;

import com.m1.sg.osm.database.dao.DaoFactory;
import com.m1.sg.osm.database.dao.common.CommonDao;
import com.m1.sg.osm.database.entity.common.OSMTransactionEntity;
import com.mslv.oms.automation.TaskContext;
import common.util.TALogger;

public class InsertUtil {

	public static void insertOSMTransaction(OSMTransactionEntity osmEntity, String sqlId) throws Exception{
		
		CommonDao commonDao = null;
		
		try {
			
			commonDao = DaoFactory.getDao(CommonDao.class);
			commonDao.create(osmEntity, sqlId);
			
			commonDao.commit();
			
		} catch (Exception e){
			
			throw e;
			
		} finally {
			if(commonDao != null){
				commonDao.close();
			}
		}
		
	}
	
	public static void printLogInsert(OSMTransactionEntity osmEntity, TaskContext taskContext, TALogger taLogger, String category){
		
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "======================================", category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "OrderId ==>" + osmEntity.getOrderId(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "SourceTransactionId ==>" + osmEntity.getSourceTransactionId(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "TransactionType ==>" + osmEntity.getTransactionType(), category);
		
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "Revision ==>" + osmEntity.getRevision(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "OrderLineItemId ==>" + osmEntity.getOrderLineItemId(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "MainOrderLineItemId ==>" + osmEntity.getMainOrderLineItemId(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "PreviousStatus ==>" + osmEntity.getPreviousStatus(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "Status ==>" + osmEntity.getStatus(), category);
		
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "PollMessage ==>" + osmEntity.getPollStatus(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "InsertDateTime ==>" + osmEntity.getInsertDateTime(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "InsertBy ==>" + osmEntity.getInsertBy(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "UpdateDataTime ==>" + osmEntity.getUpdateDateTime(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "UpdateBy ==>" + osmEntity.getUpdateBy(), category);
		taLogger.log("", String.valueOf(taskContext.getOrderId()), 01, "======================================", category);
	}
	
}
