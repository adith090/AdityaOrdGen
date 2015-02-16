package com.m1.sg.bcc.om.monitor.execute;

import org.springframework.jdbc.core.JdbcTemplate;

import com.m1.sg.bcc.om.database.dao.OMJDBCDao;

public class PollerTrafficMonitorExecute {

	public void trafficMonitorExecute(){
		OMJDBCDao omJDBCDao = new OMJDBCDao();
		JdbcTemplate crmTemplate = omJDBCDao.getCRMJDBCTemplate();
		crmTemplate.update("call UPDATE_OM_POLLER_MONITOR()");
	}
	
}
