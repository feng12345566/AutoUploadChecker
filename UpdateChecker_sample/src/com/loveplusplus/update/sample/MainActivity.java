package com.loveplusplus.update.sample;


import com.loveplusplus.update.UpdateChecker;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button btn1 = (Button) findViewById(R.id.button1);
		Button btn2 = (Button) findViewById(R.id.button2);

		btn1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				UpdateChecker.setUpdateServerUrl("http://nat.nat123.net:14313/oa/UpdateChecker.jsp");
				UpdateChecker.checkForDialog(MainActivity.this); 
				
			}
		});
		btn2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				UpdateChecker.setUpdateServerUrl("http://nat.nat123.net:14313/oa/UpdateChecker.jsp");
				UpdateChecker.checkForNotification(MainActivity.this);
				
			}
		});

	}

}
