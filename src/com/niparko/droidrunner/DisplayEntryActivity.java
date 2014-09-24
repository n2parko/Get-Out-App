package com.niparko.droidrunner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class DisplayEntryActivity extends Activity {

	// Keys for the Database
	public static final String KEY_ROWID = DatabaseAdapter.KEY_ROWID;
	public static final String KEY_INPUT_TYPE = DatabaseAdapter.KEY_INPUT_TYPE;
	public static final String KEY_ACTIVITY_TYPE = DatabaseAdapter.KEY_ACTIVITY_TYPE;
	public static final String KEY_DATE_TIME = DatabaseAdapter.KEY_DATE_TIME;
	public static final String KEY_DISTANCE = DatabaseAdapter.KEY_DISTANCE;
	public static final String KEY_DURATION = DatabaseAdapter.KEY_DURATION;
	public static final String KEY_AVG_SPEED = DatabaseAdapter.KEY_AVG_SPEED;
	public static final String KEY_CALORIES = DatabaseAdapter.KEY_CALORIES;
	public static final String KEY_CLIMB = DatabaseAdapter.KEY_CLIMB;
	public static final String KEY_AVG_HEARTRATE = DatabaseAdapter.KEY_AVG_HEARTRATE;
	public static final String KEY_COMMENT = DatabaseAdapter.KEY_COMMENT;
	public static final String KEY_GPS_DATA = DatabaseAdapter.KEY_GPS_DATA;

	private DatabaseAdapter myDatabase;
	private long currEntryId;
	private Context currContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.displayactivity);

		myDatabase = new DatabaseAdapter(this);
		currContext = this;

		Intent i = getIntent();
		Bundle bun = i.getExtras();

		currEntryId = bun.getLong(KEY_ROWID);

		// Insert data into editText
		EditText editText = (EditText) findViewById(R.id.activity_type_field);
		editText.setText(bun.getString(KEY_ACTIVITY_TYPE));

		editText = (EditText) findViewById(R.id.date_time_field);
		editText.setText(bun.getString(KEY_DATE_TIME));

		editText = (EditText) findViewById(R.id.duration_field);
		editText.setText(bun.getString(KEY_DURATION));

		editText = (EditText) findViewById(R.id.distance_field);
		editText.setText(bun.getString(KEY_DISTANCE));

		// Delete workout callback
		Button deleteButton = (Button) findViewById(R.id.delete_button);
		deleteButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				myDatabase.open();
				myDatabase.deleteWorkout(currEntryId);
				myDatabase.close();

				Toast saveMessage = Toast.makeText(getApplicationContext(),
						"Workout Deleted", Toast.LENGTH_SHORT);
				saveMessage.setGravity(Gravity.CENTER, 0, 0);
				saveMessage.show();

				// Return to startHistory
				Intent i = new Intent().setClass(currContext, PortalActivity.class);
				Bundle bun = new Bundle();
				bun.putInt("selectedTab", 1);
				i.putExtras(bun);
				startActivity(i);

			}

		});

	}
}
