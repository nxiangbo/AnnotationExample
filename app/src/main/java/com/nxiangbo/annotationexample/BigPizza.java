package com.nxiangbo.annotationexample;

import com.nxiangbo.library.Factory;

@Factory(type = Meal.class, id = "Big")
public class BigPizza implements Meal {
	@Override
	public float getPrice() {
		return 23;
	}
}
