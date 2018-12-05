package com.nxiangbo.annotationexample;

import com.nxiangbo.library.Factory;

@Factory(id="Fruit", type = Meal.class)
public class FruitPizza  implements Meal{
	@Override
	public float getPrice() {
		return 12.0f;
	}
}
