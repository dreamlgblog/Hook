package com.example.com_dream_hook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class HookActivity extends Activity {
	
	private Button text;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hook_activity);
		text = (Button) findViewById(R.id.text);
		//text.setText("third");
		String stringExtra = getIntent().getStringExtra("name");
		//Intent intentExtra = getIntent().getParcelableExtra("oldIntent");
		//String stringExtra = intentExtra.getStringExtra("name");
		System.out.println("HookActivity:::::"+stringExtra);
		/*text.setText(stringExtra);*/
		
		text.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(HookActivity.this,ThirdActivity.class);
				startActivity(intent);
			}
		});
		
	}
}
