package com.m1.sg.osm.main;

import java.util.Random;

/*
 * The class is used to behave as Keys for HasMaps
 */

/************************************************************************************ 
 * MODIFICATION HISTORY
 ************************************************************************************ 
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 ************************************************************************************
 * 24/09/2013					Aditya					Created
 ************************************************************************************/

public class Key {

	private final String x;
	private final String y;

	public Key(String x, String y) {
		this.x = x;
		this.y = y;
	}
	public Key(String x) {
		this.x = x;
		this.y= "";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Key)) return false;
		Key key = (Key) o;
		return x.equals(key.x) && y.equals(key.y);
	}

	@Override
	public int hashCode() {
		int hashX = x.hashCode();
		int hashY = y.hashCode();
		int returnHashCode = hashX + hashY;
		return returnHashCode;
	}
}
