package com.niparko.droidrunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

// Activity to allow the user to input a workout
public class InputExerciseActivity extends ListActivity {

	// Class variables for Dialog callbacks
	private static final int DATE = 0;
	private static final int TIME = 1;
	private static final int DURATION = 2;
	private static final int DISTANCE = 3;
	private static final int CALORIES = 4;
	private static final int HEART_RATE = 5;
	private static final int COMMENT = 6;
	private static final int CHOICE_MODE_MULTIPLE = 2;
	private static final int NO_ENTRY = -1;
	public static final double KM2MILE_RATIO = 1.609;
	public static final double KILO = 1000;

	// Variables to store the user input
	private int mYear = NO_ENTRY, mDay = NO_ENTRY, mMonth = NO_ENTRY,
			mActivityType = NO_ENTRY, mInputType = NO_ENTRY;
	private String mDayString = "00", mMonthString = "00";
	private double mDistance = NO_ENTRY, mDuration = NO_ENTRY,
			mCalories = NO_ENTRY, mHeart = NO_ENTRY;
	private String mComment = "No Comment", mDistanceMeasure;
	private String mHour = "00", mMinute = "00";
	private int hour, minute;

	private String[] entryForm;
	private ListView lstView;
	private SharedPreferences mSettings;
	private Context currContext;
	private EditText textEntryView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inputexercisexml);

		currContext = this;

		Intent i = getIntent();
		mInputType = i.getIntExtra(HistoryActivity.KEY_INPUT_TYPE, 0);
		mActivityType = i.getIntExtra(HistoryActivity.KEY_ACTIVITY_TYPE, 0);

		entryForm = getResources().getStringArray(R.array.manualEntryList);

		// Adapter for listview
		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, entryForm));

		// Initialize values for date picker dialog
		Calendar c = Calendar.getInstance();
		mDay = c.get(Calendar.DAY_OF_MONTH);
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);

		lstView = getListView();
		lstView.setChoiceMode(CHOICE_MODE_MULTIPLE);

		mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		mDistance = NO_ENTRY;

		Button manualButton = (Button) findViewById(R.id.submitButton);
		manualButton.setOnClickListener(new Button.OnClickListener() {

			public void onClick(View v) {
				DatabaseAdapter db = new DatabaseAdapter(currContext);

				// Create the datetime from date and time data
				// Useful link:
				// http://www.pakzilla.com/2007/04/08/inserting-value-in-sql-server%E2%80%99s-datetime-by-javasqltimstamp/
				String dateTime = mMonthString + "." + mDayString + "."
						+ Integer.toString(mYear) + " " + mHour + ":" + mMinute;
				Log.v("onClick", dateTime);
				java.sql.Timestamp sqlTimeStamp = null;

				SimpleDateFormat formatter = new SimpleDateFormat("MM.dd.yyyy HH:mm");
				try {
					Date javaDateTime = (Date) formatter.parse(dateTime);
					sqlTimeStamp = new java.sql.Timestamp(javaDateTime.getTime());
					Log.v("FORMATTED SQL DATE", sqlTimeStamp.toString());
				}
				catch (ParseException e) {
					Log.v("PARSEEXCEPTION", "ERROR");
					e.printStackTrace();
				}

				// Ensure the user has filled out all required fields
				if (mDistance == NO_ENTRY || mDuration == NO_ENTRY
						|| mCalories == NO_ENTRY || mHeart == NO_ENTRY || mYear == NO_ENTRY
						|| mDay == NO_ENTRY || mMonth == NO_ENTRY
						|| mActivityType == NO_ENTRY || mInputType == NO_ENTRY) {

					Toast toast = Toast.makeText(currContext,
							"You must fill out all fields", Toast.LENGTH_SHORT);
					toast.show();
				}
				else {

					Exercise exer = new Exercise(currContext, hour, minute, mDay, mYear, mMonth);
					exer.setDistanceRaw(mDistance);
					exer.setDuration(mDuration);
					exer.setCalorie(mCalories);
					exer.setHeartRate(mHeart);
					exer.setComment(mComment);
					exer.setLocationList(new ArrayList<Location>(0));
					exer.setActivityType(mActivityType);
					exer.setInputType(mInputType);
					exer.setAvgSpeed();
					
					exer.insertToDB();
					
					// Intent to navigate to HistoryActivity
					Intent intent = new Intent().setClass(currContext,
							PortalActivity.class);
					Bundle bun = new Bundle();

					bun.putInt("selectedTab", 1);
					intent.putExtras(bun);
					startActivity(intent);
				}

			}
		});

		manualButton = (Button) findViewById(R.id.cancelButton);
		manualButton.setOnClickListener(new Button.OnClickListener() {

			public void onClick(View v) {

				Intent i = new Intent().setClass(currContext, PortalActivity.class);
				startActivity(i);
			}

		});

	}

	// Callback for listview
	public void onListItemClick(ListView parent, View v, int position, long id) {

		showDialog(position);

	}

	// Callback for showDialog
	protected Dialog onCreateDialog(int id) {

		Log.v("onCreateDialog", Integer.toString(id));

		switch (id) {
		case DATE:
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
		case TIME:
			return new TimePickerDialog(this, mTimeSetListener, hour, minute, false);
		case DURATION:
			return buildDurationDialog(entryForm[DURATION]);
		case DISTANCE:
			return buildDistanceDialog(entryForm[DISTANCE]);
		case CALORIES:
			return buildCaloriesDialog(entryForm[CALORIES]);
		case HEART_RATE:
			return buildHeartDialog(entryForm[HEART_RATE]);
		case COMMENT:
			return buildCommentDialog(entryForm[COMMENT]);

		}
		return null;
	}

	// Create and display the comment dialog
	private Dialog buildCommentDialog(String title) {

		textEntryView = new EditText(this);

		// onClick for clicking ok
		DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				mComment = textEntryView.getText().toString();

			}

		};

		DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				textEntryView.setText("");
			}
		};

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(title).setView(textEntryView)
				.setPositiveButton("Okay", OKListener)
				.setNegativeButton("Cancel", CancelListener);

		return dialogBuilder.show();

	}

	// Create and display the heart rate dialog
	private Dialog buildHeartDialog(String title) {

		textEntryView = new EditText(this);

		// onClick for clicking ok
		DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				mHeart = Double.parseDouble(textEntryView.getText().toString());

			}

		};

		DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				textEntryView.setText("");
			}
		};

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(title + " (BPM)").setView(textEntryView)
				.setPositiveButton("Okay", OKListener)
				.setNegativeButton("Cancel", CancelListener);

		return dialogBuilder.show();

	}

	// Create and display the calories dialog
	private Dialog buildDistanceDialog(String title) {

		textEntryView = new EditText(this);

		mDistanceMeasure = mSettings.getString(getString(R.string.prefKeyUnit),
				getResources().getStringArray(R.array.distanceUnits)[0]);

		Log.v("buildDurationDialog", mDistanceMeasure);

		// onClick for clicking ok
		DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				String distanceString = textEntryView.getText().toString();

				// Handle null string
				if (distanceString.compareTo("") == 0) {
					mDistance = 0;
				}
				else {
					mDistance = Double.parseDouble(distanceString);
				}

				mDistance *= KILO;
				if (mDistanceMeasure.compareTo("Miles") == 0) {
					mDistance *= KM2MILE_RATIO;

				}

			}

		};

		DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				textEntryView.setText("");
			}
		};

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(title + " (" + mDistanceMeasure + ")")
				.setView(textEntryView).setPositiveButton("Okay", OKListener)
				.setNegativeButton("Cancel", CancelListener);

		return dialogBuilder.show();

	}

	// Create and display the calories dialog
	private Dialog buildCaloriesDialog(String title) {

		textEntryView = new EditText(this);

		// onClick for clicking ok
		DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				mCalories = Double.parseDouble(textEntryView.getText().toString());

			}

		};

		DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				textEntryView.setText("");
			}
		};

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(title).setView(textEntryView)
				.setPositiveButton("Okay", OKListener)
				.setNegativeButton("Cancel", CancelListener);

		return dialogBuilder.show();

	}

	// Create and display the duration dialog
	private Dialog buildDurationDialog(String title) {

		textEntryView = new EditText(this);

		// onClick for clicking ok
		DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				mDuration = Double.parseDouble(textEntryView.getText().toString());

			}
		};

		DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				textEntryView.setText("");
			}
		};

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(title + " (seconds)").setView(textEntryView)
				.setPositiveButton("Okay", OKListener)
				.setNegativeButton("Cancel", CancelListener);

		return dialogBuilder.show();

	}

	// Datepicker listener
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			if (dayOfMonth < 9) {
				mDayString = "0" + Integer.toString(dayOfMonth);
			}
			else {
				mDayString = Integer.toString(dayOfMonth);
			}

			if (monthOfYear < 9) {
				mMonthString = "0" + Integer.toString(monthOfYear + 1);
			}
			else {
				mMonthString = Integer.toString(monthOfYear);
			}

		}

	};

	// Timepicker listener
	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

		public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfDay) {
			if (hourOfDay < 9) {
				mHour = "0" + Integer.toString(hourOfDay);
			}
			else {
				mHour = Integer.toString(hourOfDay);
			}
			if (minuteOfDay < 9) {
				mMinute = "0" + Integer.toString(minuteOfDay);
			}
			else {
				mMinute = Integer.toString(minuteOfDay);
			}

		}

	};

}
