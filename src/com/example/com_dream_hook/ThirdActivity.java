package com.example.com_dream_hook;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;

public class ThirdActivity extends Activity{
	
	
	
	private Button text;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hook_activity);
		text = (Button) findViewById(R.id.text);
		text.setText("third");
	}
}
