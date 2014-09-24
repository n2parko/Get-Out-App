package com.niparko.droidrunner;

import java.util.Date;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

public class Exercise {

	private int mActivityType;
	private double mAvgSpeed;
	private double mCalorie;
	private double mClimb;
	private String mComment;
	private Context mContext;
	private double mCurSpeed;
	private GregorianCalendar mDateTime;
	private int mDay;
	private DatabaseAdapter mDB;
	private double mDistance;
	private double mDuration;
	private double mHeartRate;
	private int mHour;
	private int mId;
	private int mInputType;
	private boolean mIsLoggingStarted;
	public ArrayList<Location> mLocationList;
	private int mMinute;
	private int mMonth;
	private int mNLocations;
	private int mSecond;
	private GregorianCalendar mTimeStarted;
	private Location[] mTrack;
	private int mYear;
	private static final DecimalFormat mDecimalFormat = new DecimalFormat("#.##");
	private static String mDistanceMeasureUnit;
	private static boolean mIsMetric;
	private static String mSpeedMeasureUnit;
	java.sql.Timestamp sqlTimeStamp;

	private final static double K2MILE_RATIO = InputExerciseActivity.KM2MILE_RATIO;

	public Exercise(Context context) {
		mContext = context;
		mDB = new DatabaseAdapter(mContext);
		final Calendar c = Calendar.getInstance();
		mHour = c.get(Calendar.HOUR_OF_DAY);
		mMinute = c.get(Calendar.MINUTE);
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);
		mSecond = c.get(Calendar.SECOND);

		setDateTime();
	}

	public Exercise(Context context, int hour, int minute, int day, int year, int month) {
		
		mContext = context;
		mDB = new DatabaseAdapter(mContext);
		mHour = hour;
		mMinute = minute;
		mYear = year;
		mMonth = month;
		mDay = day;
		mSecond = 0;

		setDateTime();	}

	private void setDateTime() {
		String dateTime = Integer.toString(mMonth) + "." + Integer.toString(mDay)
				+ "." + Integer.toString(mYear) + " " + mHour + ":" + mMinute;
		Log.v("onClick", dateTime);
		sqlTimeStamp = null;

		SimpleDateFormat formatter = new SimpleDateFormat("M.dd.yyyy HH:mm");
		try {
			Date javaDateTime = (Date) formatter.parse(dateTime);
			sqlTimeStamp = new java.sql.Timestamp(javaDateTime.getTime());
			Log.v("FORMATTED SQL DATE", sqlTimeStamp.toString());
		}
		catch (ParseException e) {
			Log.v("PARSEEXCEPTION", "ERROR");
			e.printStackTrace();
		}
	}

	public long insertToDB() {

		if (mLocationList != null) {
			synchronized (mLocationList) {
				mTrack = new Location[mLocationList.size()];
				mTrack = mLocationList.toArray(mTrack);
				Log.v("MLOCATIONLISTNOTNULL", Integer.toString(mTrack.length));
			}
		}

		mDB.open();
		long newID = mDB.insertWorkout(mInputType, mActivityType, sqlTimeStamp,
				mDuration, mDistance, mAvgSpeed, mCalorie, mClimb, mHeartRate,
				mComment, mTrack);
		mDB.close();
		return newID;
	}

	public void deleteEntryInDB() {
		deleteEntryInDB(mContext, mId);
	}

	public static void deleteEntryInDB(Context context, long id) {
		DatabaseAdapter db = new DatabaseAdapter(context);
		db.open();
		db.deleteWorkout(id);
		db.close();
	}

	public void readFromDB() throws Exception {
		if (mId <= 0) {
			throw new Exception();
		}
		mDB.open();
		Cursor c = mDB.getWorkout(mId);

		setInputType(c.getInt(c.getColumnIndex(DatabaseAdapter.KEY_INPUT_TYPE)));
		setActivityType(c.getInt(c
				.getColumnIndex(DatabaseAdapter.KEY_ACTIVITY_TYPE)));
		setDateTime(c.getLong(c.getColumnIndex(DatabaseAdapter.KEY_DATE_TIME)));
		setDuration(c.getDouble(c.getColumnIndex(DatabaseAdapter.KEY_DURATION)));
		setDistanceRaw(c.getDouble(c.getColumnIndex(DatabaseAdapter.KEY_DISTANCE)));
		setClimb(c.getDouble(c.getColumnIndex(DatabaseAdapter.KEY_CLIMB)));
		setCalorie(c.getDouble(c.getColumnIndex(DatabaseAdapter.KEY_CALORIES)));
		setAvgSpeed(c.getDouble(c.getColumnIndex(DatabaseAdapter.KEY_AVG_SPEED)));
		setComment(c.getString(c.getColumnIndex(DatabaseAdapter.KEY_COMMENT)));
		setHeartRate(c.getDouble(c
				.getColumnIndex(DatabaseAdapter.KEY_AVG_HEARTRATE)));

		
		byte[] byteTrack = c
				.getBlob(c.getColumnIndex(DatabaseAdapter.KEY_GPS_DATA));

		boolean b = c.isNull(c.getColumnIndex(DatabaseAdapter.KEY_GPS_DATA));
				
		Location[] locarray = Utils.fromByteArrayToLocationArray(byteTrack);
		ArrayList<Location> loclist = new ArrayList<Location>(
				Arrays.asList(locarray));
		Log.v("null", Integer.toString(loclist.size()));
		setLocationList(loclist);

		c.close();
		mDB.close();

	}

	public ArrayList<Location> setLocationList(ArrayList<Location> loclist) {
		mLocationList = loclist;
		return mLocationList;
	}

	public int getInputType() {
		return mInputType;
	}

	public double setHeartRate(double hr) {
		mHeartRate = hr;
		return mHeartRate;
	}

	public String setComment(String string) {
		mComment = string;
		return mComment;
	}

	private double setAvgSpeed(double speed) {
		mAvgSpeed = speed;
		return mAvgSpeed;
	}

	public double setCalorie(double calorie) {
		mCalorie = calorie;
		return mCalorie;
	}

	private double setClimb(double d) {
		mClimb = d;
		return mClimb;
	}

	public double setDistanceRaw(double distance) {
		mDistance = distance;
		return mDistance;
	}

	public double setDuration(double duration) {
		mDuration = duration;
		return mDuration;
	}

	// MAKE SURE THIS WORKS!
	private Date setDateTime(long dt) {
		Calendar c = Calendar.getInstance();
		Date d = new Date(dt);
		c.setTime(d);

		mHour = c.get(Calendar.HOUR_OF_DAY);
		mMinute = c.get(Calendar.MINUTE);
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH) + 1;
		mDay = c.get(Calendar.DAY_OF_MONTH);
		mSecond = c.get(Calendar.SECOND);

		return d;
	}

	public int setActivityType(int activityType) {
		mActivityType = activityType;
		return mActivityType;
	}

	public int setInputType(int inputType) {
		mInputType = inputType;
		return mInputType;
	}
	
	public void setAvgSpeed(){
		mAvgSpeed = mDistance / (mDuration / 60);
	}

	public void startLogging() {

		mDistance = 0.0;
		mDuration = 0;
		mCalorie = 0;
		mClimb = 0.0;
		mTimeStarted = new GregorianCalendar();
		mAvgSpeed = 0;
		mNLocations = 0;

		mIsLoggingStarted = true;

	}
	

	public void updateStats() throws Exception {
		if (!mIsLoggingStarted)
			throw new Exception();

		// Dumping to Location[] track for faster unlock
		synchronized (mLocationList) {

			if (mLocationList.size() == mNLocations || mLocationList.size() < 2) {
				return;
			}

			// Check where to start, edge case is when just started
			int iStart = (mNLocations == 0) ? 1 : mNLocations;
			Location prevLoc, currLoc;
			for (int i = iStart; i < mLocationList.size(); i++) {
				prevLoc = mLocationList.get(i - 1);
				currLoc = mLocationList.get(i);

				mDistance += prevLoc.distanceTo(currLoc);

				if (prevLoc.hasAltitude() && currLoc.hasAltitude()
						&& prevLoc.getAltitude() < currLoc.getAltitude()) {
					mClimb += currLoc.getAltitude() - prevLoc.getAltitude();
				}
			}
			mNLocations = mLocationList.size();

			mCurSpeed = mLocationList.get(mNLocations - 1).hasSpeed() ? mLocationList
					.get(mNLocations - 1).getSpeed() : .0;
		}
		mDuration = (double) (((System.currentTimeMillis() - mTimeStarted
				.getTimeInMillis()) / 1000));
		mCalorie = (int) mDistance / 15;

		mAvgSpeed = mDuration > 0 ? mDistance / mDuration : .0;
	}

	public void setId(int id) {
		Log.v("ID SET TO", Integer.toString(id));
		mId = id;
	}

	public void setIsMetric(boolean isMetricFromPerf) {
		mIsMetric = isMetricFromPerf;

	}

	public boolean getIsMetric() {
		return mIsMetric;

	}

	public double getCurrSpeed() {
		if(mIsMetric){
		return Double.parseDouble((mDecimalFormat.format(mCurSpeed)));
		}
		else{
			return Double.parseDouble((mDecimalFormat.format(mCurSpeed / K2MILE_RATIO)));
		}
	}

	public double getAvgSpeed() {
		if (mIsMetric) {
			return Double.parseDouble((mDecimalFormat.format(mAvgSpeed)));
		}
		else {
			return Double.parseDouble((mDecimalFormat
					.format(mAvgSpeed / K2MILE_RATIO)));

		}
	}

	public double getCalVal() {

		return Double.parseDouble((mDecimalFormat.format(mCalorie)));
	}

	public double getDistVal() {
		if (mIsMetric) {
			return Double.parseDouble((mDecimalFormat.format(mDistance / 1000)));
		}
		else {
			return Double.parseDouble((mDecimalFormat.format(mDistance / 1000
					/ K2MILE_RATIO)));

		}

	}

	public double getClimb() {
		if (mIsMetric) {
			return Double.parseDouble((mDecimalFormat.format(mClimb)));
		}
		else {
			return Double.parseDouble((mDecimalFormat.format(mClimb / 1000
					/ K2MILE_RATIO)));
		}
	}

}
