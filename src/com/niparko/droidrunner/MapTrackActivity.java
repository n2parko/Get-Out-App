package com.niparko.droidrunner;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

//Some code in this class is sampled from the CS69 Lab 5 lab report

public class MapTrackActivity extends MapActivity {

	// Constants
	public static final String ACTION_LOCATION_UPDATED = "Action Added";

	private MapView mapView;
	private MapController mc;
	private boolean mIsBound;
	private Intent mServiceIntent;
	public final static int LIVE_LOGGER = 1;
	private SharedPreferences mSettings;
	private int mLastClassified; // Assume standing
	private int mInputType;

	private IntentFilter mLocationUpdateFilter;
	private IntentFilter mAccelUpdateFilter;
	private Boolean isLive;
	private String mDistanceMeasure;

	private OverlayRouteDrawing mapOverlay;
	private ServiceLocationTracking trackingService;

	private Context mContext;
	public ArrayList<Location> mLocationList;
	private TextView mTextViewClassified;
	private String[] mArrayActivity;

	private Exercise mEntry;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.maptracklayout);
		mContext = this;

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		mapView = (MapView) findViewById(R.id.mapView);
		mapView.setBuiltInZoomControls(true);
		mc = mapView.getController();

		mEntry = new Exercise(mContext);
		Bundle extras = getIntent().getExtras();
		mInputType = extras.getInt(HistoryActivity.KEY_INPUT_TYPE);
		mEntry.setInputType(mInputType);
		mEntry.setActivityType(extras.getInt(HistoryActivity.KEY_ACTIVITY_TYPE));
		mEntry.setId(extras.getInt(HistoryActivity.KEY_ROWID, -1));
		Boolean hideButtons = extras.getBoolean(HistoryActivity.FROM_INTENT);

		// Get distance units from preferences
		mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mDistanceMeasure = mSettings.getString(getString(R.string.prefKeyUnit),
				getResources().getStringArray(R.array.distanceUnits)[0]);

		Button saveButton = (Button) findViewById(R.id.saveWorkout);
		Button cancelButton = (Button) findViewById(R.id.cancelWorkout);

		// Show buttons
		if (!hideButtons) {
			// Set up click listeners for buttons
			saveButton.setOnClickListener(new OnClickListener() {

				public void onClick(View view) {

					if (mInputType == 1) {

						int exercise = Math.max(mapOverlay.standingCounter,
								Math.max(mapOverlay.walkingCounter, mapOverlay.standingCounter));

						Log.v(Integer.toString(exercise),
								Integer.toString(mapOverlay.runningCounter));
						Log.v(Integer.toString(mapOverlay.standingCounter),
								Integer.toString(mapOverlay.walkingCounter));

						if (exercise == mapOverlay.standingCounter) {
							mEntry.setActivityType(0);
						}
						else if (exercise == mapOverlay.walkingCounter) {
							mEntry.setActivityType(1);
						}
						else {
							mEntry.setActivityType(2);
						}
					}

					mEntry.insertToDB();
					Intent i = new Intent().setClass(mContext, PortalActivity.class);
					Bundle extras = new Bundle();
					extras.putInt("selectedTab", 1);

					i.putExtras(extras);

					if (isLive) {
						trackingService.stopForeground(true);
						doUnbindService();
						stopService(mServiceIntent);
						unregisterReceiver(intentReceiver);
						((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
								.cancel(1);
						isLive = false;
					}

					startActivity(i);

				}

			});

			cancelButton.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {

					Intent i = new Intent().setClass(mContext, PortalActivity.class);
					Bundle extras = new Bundle();
					extras.putInt("selectedTab", 0);

					if (isLive) {
						trackingService.stopForeground(false);
						doUnbindService();
						stopService(mServiceIntent);
						unregisterReceiver(intentReceiver);
						((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
								.cancel(1);
						isLive = false;
					}

					i.putExtras(extras);

					startActivity(i);
				}

			});
		}
		else {
			cancelButton.setVisibility(View.GONE);
			saveButton.setText("Delete Workout");
			saveButton.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					mEntry.deleteEntryInDB();
					Intent i = new Intent().setClass(mContext, PortalActivity.class);
					Bundle extras = new Bundle();
					extras.putInt("selectedTab", 2);
					startActivity(i);
				}

			});

		}

		mArrayActivity = getResources().getStringArray(
				R.array.spinnerItemActivityType);
		int activityType = extras.getInt(HistoryActivity.KEY_ACTIVITY_TYPE);
		mLastClassified = activityType;
		mTextViewClassified = (TextView) findViewById(R.id.exerciseTypeText);
		mTextViewClassified.setText(mArrayActivity[mLastClassified]);

		int inputType = mEntry.getInputType();

		mapOverlay = new OverlayRouteDrawing(this);
		List<Overlay> listOfOverlays = mapView.getOverlays();
		listOfOverlays.add(mapOverlay);

		Bundle bun = getIntent().getExtras();
		isLive = bun.getBoolean(StartActivity.LIVE_MAP);

		if (inputType == 1 || inputType == 2) {

			mLocationUpdateFilter = new IntentFilter();
			mLocationUpdateFilter.addAction(ACTION_LOCATION_UPDATED);
			mLocationUpdateFilter.addAction(Globals.KEY_CLASSIFICATION_RESULT);

			registerReceiver(intentReceiver, mLocationUpdateFilter);

			mServiceIntent = new Intent(mContext, ServiceLocationTracking.class);
			Bundle b = new Bundle();
			b.putInt("inputType", mEntry.getInputType());

			mServiceIntent.putExtras(b);

			doBindService();
			startService(mServiceIntent);
			mc.setZoom(17);
			mEntry.startLogging();

		}
		else {
			try {
				mEntry.readFromDB();
				updateStatDisplay();
				mapOverlay.startStaticOverlayRoute(mEntry.mLocationList);
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Log.v("onCreate", "Activity Created");

	}
	// Receiver for service broadcast
	private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().compareTo(Globals.KEY_CLASSIFICATION_RESULT) == 0) {
				Log.v("Received", "accel");

				int i = intent.getIntExtra(Globals.classifiedActivity, mLastClassified);
				mapOverlay.classifierChanged(i);

				if (i != mLastClassified) {

					mTextViewClassified.setText(mArrayActivity[i]);

					mLastClassified = i;

				}
			}
			else {

				Log.v("Received", Integer.toString(mLocationList.size()));
				if (mLocationList.size() != 0) {
					Location loc = mLocationList.get(mLocationList.size() - 1);
					try {
						mEntry.updateStats();
						updateStatDisplay();
					}
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mapOverlay.pointAdded(loc);
				}
			}
		}
	};

	private void updateStatDisplay() {

		boolean isMetric = true;

		Log.v("tag", mDistanceMeasure);

		if (mDistanceMeasure.compareTo("Miles") == 0) {
			isMetric = false;
		}

		DecimalFormat decimalFormat = new DecimalFormat("#.##");

		// Text for the exercise type
		TextView tv = (TextView) findViewById(R.id.currSpeedValText);
		double currSpeed = mEntry.getCurrSpeed();
		if (isMetric) {
			tv.setText(decimalFormat.format(currSpeed
					* InputExerciseActivity.KM2MILE_RATIO)
					+ " km/h");
		}
		else {
			tv.setText((Double.toString(currSpeed)) + " miles/h");
		}

		tv = (TextView) findViewById(R.id.avgSpeedValText);
		double avgSpeed = mEntry.getAvgSpeed();
		if (isMetric) {
			tv.setText(decimalFormat.format(avgSpeed
					* InputExerciseActivity.KM2MILE_RATIO)
					+ " km/h");
		}
		else {
			tv.setText(Double.toString(avgSpeed) + " miles/h");
		}

		tv = (TextView) findViewById(R.id.climbValText);
		double climb = mEntry.getClimb();
		if (isMetric) {
			tv.setText(decimalFormat.format(climb
					* InputExerciseActivity.KM2MILE_RATIO)
					+ " Meters");
		}
		else {
			tv.setText(Double.toString(climb) + " Miles");
		}

		tv = (TextView) findViewById(R.id.calorieValText);
		double cal = mEntry.getCalVal();
		tv.setText(Double.toString(cal) + " Calories");

		tv = (TextView) findViewById(R.id.distanceValText);
		double dist = mEntry.getDistVal();
		if (isMetric) {
			tv.setText(decimalFormat.format(dist
					* InputExerciseActivity.KM2MILE_RATIO)
					+ " Kilometers");
		}
		else {
			tv.setText(Double.toString(dist) + " Miles");
		}

	}

	// Set up the zoom function
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_3:
			mc.zoomIn();
			break;
		case KeyEvent.KEYCODE_0:
			mc.zoomOut();
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void doBindService() {
		boolean returned = bindService(mServiceIntent, connection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;

		Log.v("doBindService", Boolean.toString(returned));
	}

	private void doUnbindService() {
		if (mIsBound) {
			unbindService(connection);
			mIsBound = false;
		}
	}

	// Set up the ServiceConnection for the service bind
	private ServiceConnection connection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {
			trackingService = ((ServiceLocationTracking.DroidBinder) service)
					.getService();
			mEntry.mLocationList = trackingService.mLocationList;
			mapOverlay.mLocationList = trackingService.mLocationList;
			mLocationList = trackingService.mLocationList;

		}

		public void onServiceDisconnected(ComponentName name) {
			stopService(mServiceIntent);
			trackingService = null;
		}

	};

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	// Check to see if back is pressed
	@Override
	public void onBackPressed() {
		if (isLive) {
			trackingService.stopForeground(true);
			doUnbindService();
			stopService(mServiceIntent);
			unregisterReceiver(intentReceiver);
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(1);
			isLive = false;
		}

		super.onBackPressed();

	}
}