package com.niparko.droidrunner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;

public class StartActivity extends Activity {

	Spinner spinnerInputType;
	Spinner spinnerExerciseType;
	Button inputDataButton;
	Context currContext;
	public final static String LIVE_MAP = "LIVE_MAP";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startxml);

		currContext = this;

		spinnerInputType = (Spinner) findViewById(R.id.spinnerInputType);
		spinnerExerciseType = (Spinner) findViewById(R.id.spinnerExerciseType);
		inputDataButton = (Button) findViewById(R.id.inputExerciseButton);
		
		spinnerInputType.setOnItemSelectedListener(new MyOnItemSelectedListener());
		
		

		inputDataButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				Bundle bun = new Bundle();
				Intent i = new Intent();
				int inputType = spinnerInputType.getSelectedItemPosition();
				int exerciseType = spinnerExerciseType.getSelectedItemPosition();

				bun.putInt(HistoryActivity.KEY_INPUT_TYPE, inputType);
				bun.putInt(HistoryActivity.KEY_ACTIVITY_TYPE, exerciseType);
				bun.putBoolean(HistoryActivity.FROM_INTENT, false);

				if (inputType == 0) {
					i.setClass(currContext, InputExerciseActivity.class);
				}
				else {
					bun.putBoolean(LIVE_MAP, true);
					i.setClass(currContext, MapTrackActivity.class);
				}

				i.putExtras(bun);
				startActivity(i);

			}

		});

	}

	public class MyOnItemSelectedListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View v, int pos, long arg3) {

			if (pos == 1) {
				spinnerExerciseType.setEnabled(false);
				inputDataButton.setText("Start Classifier");
			}
		}

		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub

		}

	}
}
