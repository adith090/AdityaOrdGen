package com.m1.sg.osm.database.dao.common;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.m1.sg.osm.database.dao.DaoException;
import com.m1.sg.osm.database.dao.mybatis.AbstractMyBatisDao;

public class CommonDaoImpl extends AbstractMyBatisDao implements CommonDao {

	@Override
	public List findAll(Object entity, String sqlId) throws DaoException {
		return (List)super.findList(entity, sqlId);
	}

	@Override
	public void create(Object arg0) throws DaoException {
		
	}

	@Override
	public void delete(Object arg0) throws DaoException {
		
	}

	@Override
	public Object find(Object arg0) throws DaoException {
		return null;
	}

	@Override
	public void update(Object arg0) throws DaoException {
		
	}

	

	@Override
	public void close() throws DaoException {
		super.getSqlSession().close();
	}

	@Override
	public void create(Object obj, String id) throws DaoException {
		// TODO Auto-generated method stub
		super.create(obj, id);
	}

	@Override
	public void commit() throws DaoException {
		// TODO Auto-generated method stub
		super.getSqlSession().commit();
	}
	
	@Override
	public void update(Object obj, String id) throws DaoException {
		// TODO Auto-generated method stub
		super.update(obj, id);
	}

	@Override
	public Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		return getConnection();
	}
}
