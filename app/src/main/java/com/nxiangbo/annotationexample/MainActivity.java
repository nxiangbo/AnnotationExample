package com.nxiangbo.annotationexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Pizzas pizzas = new Pizzas();
		Log.d("nxb", "Big price = "+pizzas.order("Big").getPrice());
		Log.d("nxb", "Fruit price = "+pizzas.order("Fruit").getPrice());

	}
}
