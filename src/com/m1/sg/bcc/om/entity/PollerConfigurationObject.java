package com.m1.sg.bcc.om.entity;

import java.util.ArrayList;
import java.util.Map;

public class PollerConfigurationObject {

	/**
	 * 	ArrayList store SQL information load from table 
	 * 	C_OM_GEN_ELEMENT_MAP and C_OM_GEN_SQL_MAP 
	 */
	private ArrayList<MappingElementEntity> mapElementList;
	
	/**
	 *	Mapping between SQL_ID and SQL information load from table 
	 * 	C_OM_GEN_ELEMENT_MAP and C_OM_GEN_SQL_MAP
	 */
	private Map<String, MappingElementEntity> mappingSqlDetails;
	
	/**
	 *	Mapping between SQL_ID and SQL Dependency load from table C_OM_GEN_SQL_MAP	 
	 */
	private Map<String, ArrayList<String>> mappingSqlDependency;
	
	/**
	 * 	ArrayList store SQL_ID has sql depend on this SQL_ID
	 */
	private ArrayList<String> allDependencySqlId;

	// Getter Setter Sections
	
	public ArrayList<MappingElementEntity> getMapElementList() {
		return mapElementList;
	}

	public void setMapElementList(ArrayList<MappingElementEntity> mapElementList) {
		this.mapElementList = mapElementList;
	}

	public Map<String, MappingElementEntity> getMappingSqlDetails() {
		return mappingSqlDetails;
	}

	public void setMappingSqlDetails(
			Map<String, MappingElementEntity> mappingSqlDetails) {
		this.mappingSqlDetails = mappingSqlDetails;
	}

	public Map<String, ArrayList<String>> getMappingSqlDependency() {
		return mappingSqlDependency;
	}

	public void setMappingSqlDependency(
			Map<String, ArrayList<String>> mappingSqlDependency) {
		this.mappingSqlDependency = mappingSqlDependency;
	}

	public ArrayList<String> getAllDependencySqlId() {
		return allDependencySqlId;
	}

	public void setAllDependencySqlId(ArrayList<String> allDependencySqlId) {
		this.allDependencySqlId = allDependencySqlId;
	}
	
}
