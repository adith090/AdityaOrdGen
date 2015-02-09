package com.m1.sg.osm.database.dao.common;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.m1.sg.osm.database.dao.Dao;
import com.m1.sg.osm.database.dao.DaoException;

public interface CommonDao extends Dao {
	
	public List findAll(Object entity, String sqlId) throws DaoException;
	public void create(Object entity, String sqlId) throws DaoException;
	public void close() throws DaoException ;
	public void commit() throws DaoException;
	public void update(Object entity, String sqlId) throws DaoException;
	public Connection getConnection() throws SQLException;
}
