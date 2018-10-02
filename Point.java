package com.meghal.yeppar.mygeoar;



public class Point {
	public double longitude = 0f;
	public double latitude = 0f;
	public String description;
	public float x, y = 0;
	
	public Point(double lat, double lon, String desc) {
		this.latitude = lat;
		this.longitude = lon;
		this.description = desc;
	}

	@Override
	public String toString() {
		return latitude+","+longitude+","+description;
	}
}
