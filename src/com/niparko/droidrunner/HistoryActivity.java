package com.niparko.droidrunner;

import java.sql.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class HistoryActivity extends ListActivity {

	// Keys for the database
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
	public static final double KILOS = InputExerciseActivity.KILO;
	public static final String FROM_INTENT = "FROMINTENT";
	private static final double KM2MILE_RATIO = InputExerciseActivity.KM2MILE_RATIO;

	private DatabaseAdapter myDatabase;
	private Context currContext;
	private Cursor mActivityEntryCursor;
	private DatabaseCursorAdapter dcAdapter;

	private int mRowIdIndex;
	private int mActivityIndex;
	private int mTimeIndex;
	private int mDurationIndex;
	private int mDistanceIndex;
	private String mDistanceMeasure;
	String[] arrayWorkout;

	private SharedPreferences mSettings;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.historyxml);

		currContext = this;

		myDatabase = new DatabaseAdapter(this);
		myDatabase.open();
		mActivityEntryCursor = myDatabase.getAllWorkouts();

		// Get distance units from preferences
		mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mDistanceMeasure = mSettings.getString(getString(R.string.prefKeyUnit),
				getResources().getStringArray(R.array.distanceUnits)[0]);

		mActivityEntryCursor.moveToFirst();

		// Get indices
		mRowIdIndex = (mActivityEntryCursor.getColumnIndex(myDatabase.KEY_ROWID));
		mActivityIndex = mActivityEntryCursor
				.getColumnIndex(myDatabase.KEY_ACTIVITY_TYPE);
		mTimeIndex = mActivityEntryCursor.getColumnIndex(myDatabase.KEY_DATE_TIME);
		mDurationIndex = mActivityEntryCursor
				.getColumnIndex(myDatabase.KEY_DURATION);
		mDistanceIndex = mActivityEntryCursor
				.getColumnIndex(myDatabase.KEY_DISTANCE);

		startManagingCursor(mActivityEntryCursor);

		// dcAdapter to add data from database to ListView
		dcAdapter = new DatabaseCursorAdapter(currContext, R.layout.workout_row,
				mActivityEntryCursor);
		setListAdapter(dcAdapter);

		arrayWorkout = getResources().getStringArray(
				R.array.spinnerItemActivityType);

	}

	// onResume to change the units when user changes distance preference
	@Override
	public void onResume() {

		super.onResume();
		// Get distance units from preferences
		mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mDistanceMeasure = mSettings.getString(getString(R.string.prefKeyUnit),
				getResources().getStringArray(R.array.distanceUnits)[0]);

		// dcAdapter to add data from database to ListView
		dcAdapter = new DatabaseCursorAdapter(currContext, R.layout.workout_row,
				mActivityEntryCursor);

		setListAdapter(dcAdapter);

	}

	// DatabaseCursorAdapter subclass binds data to the list view to display
	// workouts in HistoryActivity
	// Code sampled from
	// http://stackoverflow.com/questions/5300787/how-do-i-create-a-custom-cursor-adapter-for-a-listview-for-use-with-images-and-t
	public class DatabaseCursorAdapter extends ResourceCursorAdapter {

		public static final int LAYOUT_ID = R.layout.workout_row;

		public DatabaseCursorAdapter(Context context, int layout, Cursor c) {
			super(context, LAYOUT_ID, c);
		}

		public View newView(Context context, Cursor cur, ViewGroup parent) {
			LayoutInflater li = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			return li.inflate(LAYOUT_ID, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cur) {

			Long time = mActivityEntryCursor.getLong(mTimeIndex);
			String formattedTime = parseTime(time);

			double distance = mActivityEntryCursor.getDouble(mDistanceIndex);
			String distanceString = parseDistance(distance);

			double duration = mActivityEntryCursor.getDouble(mDurationIndex);

			TextView tvListText = (TextView) view.findViewById(R.id.text1);
			tvListText.setText((arrayWorkout[mActivityEntryCursor
					.getInt(mActivityIndex)] + ", " + formattedTime));

			tvListText = (TextView) view.findViewById(R.id.text2);
			tvListText.setText(distanceString + ", " + duration + " secs");

		}

	}

	// Callback for a click on a list item
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		mActivityEntryCursor.moveToPosition(position);

		// Create intent to launch DisplayEntryActivity
		int i = mActivityEntryCursor.getInt(mActivityEntryCursor
				.getColumnIndex(KEY_ACTIVITY_TYPE));

		Bundle extras = new Bundle();

		extras.putBoolean(StartActivity.LIVE_MAP, false);

		extras.putInt(KEY_ROWID, mActivityEntryCursor.getInt(mRowIdIndex));
		extras.putInt(KEY_ACTIVITY_TYPE, i);
		extras.putString(KEY_DATE_TIME,
				parseTime(mActivityEntryCursor.getLong(mTimeIndex)));
		extras.putString(KEY_DURATION,
				Double.toString(mActivityEntryCursor.getDouble(mDurationIndex))
						+ " secs");
		extras.putString(KEY_DISTANCE,
				parseDistance(mActivityEntryCursor.getDouble(mDistanceIndex)));

		// True if from history, false if from start activity
		extras.putBoolean(FROM_INTENT, true);

		Intent intent = new Intent().setClass(this, MapTrackActivity.class);
		intent.putExtras(extras);
		startActivity(intent);

	}

	// Parse the distance
	private String parseDistance(double distance) {
		distance /= KILOS;
		if (mDistanceMeasure.compareTo("Miles") == 0) {
			distance /= KM2MILE_RATIO;
		}
		DecimalFormat decimalFormat = new DecimalFormat("#.##");

		return decimalFormat.format(distance) + " " + mDistanceMeasure;
	}

	// Parse the time
	private String parseTime(long millSecs) {
		Long time = millSecs;
		Date dateOfWorkout = new Date(time);
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm");
		return sdf.format(dateOfWorkout);
	}

}
