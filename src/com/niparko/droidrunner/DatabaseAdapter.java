package com.niparko.droidrunner;

import java.sql.Blob;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

// This class is a wrapper class for database manipulation
// Code sampled from Lab Write-Up and Lee's Beginning Android 218-224
public class DatabaseAdapter {

	// Keys for the database
	public static final String KEY_ROWID = "_id";
	public static final String KEY_INPUT_TYPE = "input_type";
	public static final String KEY_ACTIVITY_TYPE = "activity_type";
	public static final String KEY_DATE_TIME = "date_time";
	public static final String KEY_DISTANCE = "distance";
	public static final String KEY_DURATION = "duration";
	public static final String KEY_AVG_SPEED = "avg_speed";
	public static final String KEY_CALORIES = "calories";
	public static final String KEY_CLIMB = "climb";
	public static final String KEY_AVG_HEARTRATE = "avg_heartrate";
	public static final String KEY_COMMENT = "comment";
	public static final String KEY_GPS_DATA = "gps_data";

	public static final String DATABASE_NAME = "droidRunner Database";
	public static final int DATABASE_VERSION = 1;
	public static final String TABLE_NAME_ENTRIES = "entries";

	// String of SQL commands to create database
	private static final String CREATE_TABLE_ENTRIES = "CREATE TABLE IF NOT EXISTS "
			+ "entries (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ "input_type INTEGER NOT NULL, "
			+ "activity_type INTEGER NOT NULL, "
			+ "date_time DATETIME NOT NULL, "
			+ "duration DOUBLE NOT NULL, "
			+ "distance DOUBLE, "
			+ "avg_speed DOUBLE, "
			+ "calories DOUBLE, "
			+ "climb DOUBLE, "
			+ "avg_heartrate DOUBLE, "
			+ "comment TEXT, " + "gps_data BLOB " + ");";

	private SQLiteDatabase db;

	// private DBHelper dbHelper;
	private DBHelper dbHelper;

	public DatabaseAdapter(Context context) {
		dbHelper = new DBHelper(context);
	}

	private static class DBHelper extends SQLiteOpenHelper {

		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		// Create database
		@Override
		public void onCreate(SQLiteDatabase database) {
			try {
				database.execSQL(CREATE_TABLE_ENTRIES);
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}

		// Upgrade database
		@Override
		public void onUpgrade(SQLiteDatabase database, int oldVersion,
				int newVersion) {
			Log.w(
					"onUpgrade",
					"Database is being updated from version "
							+ Integer.toString(oldVersion) + " to version "
							+ Integer.toString(newVersion));

			database.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
			onCreate(database);

		}
	}

	// Open the database
	public DatabaseAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	// Close the database
	public void close() {
		dbHelper.close();
	}

	// Insert a workout
	public long insertWorkout(int inputType, int activityType,
			java.sql.Timestamp dateTime, double duration, double distance,
			double avg_speed, double calories, double climb,
			double avgHR, String comment, Location[] gpsData) {

		byte[] gpsDataByte = null;
		if(gpsData != null){
			gpsDataByte = new byte[gpsData.length];
			gpsDataByte = Utils.fromLocationArrayToByteArray(gpsData);
		}
		
		long timeStampLong = dateTime.getTime();

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_INPUT_TYPE, inputType);
		initialValues.put(KEY_ACTIVITY_TYPE, activityType);
		initialValues.put(KEY_DATE_TIME, timeStampLong);
		initialValues.put(KEY_DURATION, duration);
		initialValues.put(KEY_DISTANCE, distance);
		initialValues.put(KEY_AVG_SPEED, avg_speed);

		initialValues.put(KEY_CALORIES, calories);
		initialValues.put(KEY_CLIMB, climb);
		initialValues.put(KEY_AVG_HEARTRATE, avgHR);
		initialValues.put(KEY_COMMENT, comment);
		initialValues.put(KEY_GPS_DATA, gpsDataByte);

		return db.insert(TABLE_NAME_ENTRIES, null, initialValues);
	}

	// Delete a workout from the database
	public boolean deleteWorkout(long rowId) {
		return db.delete(TABLE_NAME_ENTRIES, KEY_ROWID + "=" + rowId, null) > 0;
	}

	// Get a workout from the database using rowId
	public Cursor getWorkout(long rowId) throws SQLException {
		Cursor mCursor = db.query(TABLE_NAME_ENTRIES, new String[] { KEY_ROWID,
				KEY_INPUT_TYPE, KEY_ACTIVITY_TYPE, KEY_DATE_TIME, KEY_DURATION,
				KEY_DISTANCE, KEY_AVG_SPEED, KEY_CALORIES, KEY_CLIMB,
				KEY_AVG_HEARTRATE, KEY_COMMENT, KEY_GPS_DATA }, KEY_ROWID
				+ "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	// Pull all workouts from the database
	public Cursor getAllWorkouts() {
		return db.query(TABLE_NAME_ENTRIES, new String[] { KEY_ROWID,
				KEY_INPUT_TYPE, KEY_ACTIVITY_TYPE, KEY_DATE_TIME, KEY_DURATION,
				KEY_DISTANCE, KEY_AVG_SPEED, KEY_CALORIES, KEY_CLIMB,
				KEY_AVG_HEARTRATE, KEY_COMMENT, KEY_GPS_DATA }, null,
				null, null, null, null);
	}
}
