package com.nxiangbo.annotationexample;

import com.nxiangbo.library.Factory;

public class Pizzas {
	MealFactory factory = new MealFactory();

	public Meal order(String mealName) {
		return factory.create(mealName);
	}
}
