package com.m1.sg.bcc.om.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import com.m1.sg.bcc.om.database.dao.OMJDBCDao;

public class OMXqueryJDBCConnection {
	
	public static String xqueryConnectDatabase(String query, String dataSourceType) throws SQLException{
		return xqueryConnectDatabase(query, new ArrayList<String>(), dataSourceType);
	}

	public static String xqueryConnectDatabase(String query, ArrayList<String> l1, String dataSourceType) throws SQLException{
		
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ArrayList<String> columnNameList = new ArrayList<String>();
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append("<row>");
		try {
			
			OMJDBCDao jdbcDao = new OMJDBCDao();

			if(dataSourceType.equals("CRM")){conn = jdbcDao.getCRMDataSource().getConnection();}
			else {conn = jdbcDao.getOMDataSource().getConnection();}
			pstmt = conn.prepareStatement(query);
			int i=1;
			for(String eachOfParameter : l1){
				pstmt.setString(i, eachOfParameter);
				i++;
			}
			rs = pstmt.executeQuery();
			i=0;
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			for(i=0; i < columnCount; i++){
				columnNameList.add(rsmd.getColumnName(i+1));
			}
			i=0;
			while(rs.next()){
				strBuffer.append("<rowSet>");
				for(i=0; i < columnCount; i++){
					strBuffer.append("<");
					strBuffer.append(columnNameList.get(i));
					strBuffer.append(">");
					strBuffer.append(rs.getString(i+1));
					strBuffer.append("</");
					strBuffer.append(columnNameList.get(i));
					strBuffer.append(">");
				}
				strBuffer.append("</rowSet>");
			}
			
			strBuffer.append("</row>");
			
			return strBuffer.toString();
			
		} catch (Exception e){
			
			e.printStackTrace();
			return null;
			
		} finally { 
			if(rs != null){
				rs.close();
			}
			if(pstmt != null){
				pstmt.close();
			}
			if(conn != null){
				conn.close();
			}
		}
		
	}
	
}
