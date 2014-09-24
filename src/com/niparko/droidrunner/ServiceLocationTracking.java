package com.niparko.droidrunner;

import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

// Some code in this class is sampled from the CS69 Lab 5 lab report 
// and Lee's Android Application Development

public class ServiceLocationTracking extends Service implements
		LocationListener, SensorEventListener {

	private final static int MINIMUM_ACCURACY_TOLERANCE = 40;
	public ArrayList<Location> mLocationList;
	private int NOTIFICATION_ID = 1;
	private static final int mFeatLen = Globals.ACCELEROMETER_BLOCK_CAPACITY + 2;
	private LocationManager mLocationManager;
	private Context mContext;
	public int SIZE_OF_ARRAY;
	private final IBinder binder = new DroidBinder();
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private static ArrayBlockingQueue<Double> mAccBuffer;
	private Instances mDataset;
	private Attribute mClassAttribute;
	private OnSensorChangedTask mAsyncTask;
	private Intent mActivityClassificationBroadcast;
	private int mInputType;

	public class DroidBinder extends Binder {
		ServiceLocationTracking getService() {
			return ServiceLocationTracking.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mAccBuffer = new ArrayBlockingQueue<Double>(
				Globals.ACCELEROMETER_BLOCK_CAPACITY);
	}

	@Override
	public void onDestroy() {
		if (mInputType == 1) {
			mSensorManager.unregisterListener(this);
			mAsyncTask.onCancelled();
		}
		super.onDestroy();


		Log.v("onDestroy", "Called");

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		mContext = this;

		mLocationList = new ArrayList<Location>(0);

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
				this);

		Intent intentMap = new Intent(this, MapTrackActivity.class);

		// Ensures that the activity is not re-launched if already running
		intentMap.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent pi = PendingIntent.getActivity(this, 0, intentMap, 0);
		Notification notification = new Notification(R.drawable.clickrun,
				"DroidRunner is recording your path.", System.currentTimeMillis());
		CharSequence name = "mapTrack";
		CharSequence action = "Recording your path...";
		notification.setLatestEventInfo(this, name, action, pi);
		notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;

		startForeground(NOTIFICATION_ID, notification);

		// ***********AUTO INPUT CODE***********
		mInputType = intent.getIntExtra("inputType", 0);

		mActivityClassificationBroadcast = new Intent();
		mActivityClassificationBroadcast
				.setAction(Globals.KEY_CLASSIFICATION_RESULT);

		if (mInputType == 1) {

			mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			mAccelerometer = mSensorManager
					.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

			mSensorManager.registerListener(this, mAccelerometer,
					SensorManager.SENSOR_DELAY_FASTEST);
		}

		Log.v("onStartCommand", "Collector Service Started");

		ArrayList<Attribute> allAttr = new ArrayList<Attribute>();
		DecimalFormat df = new DecimalFormat("0000");
		for (int i = 0; i < Globals.ACCELEROMETER_BLOCK_CAPACITY; i++) {
			allAttr.add(new Attribute("fft_coef_" + df.format(i)));
		}

		allAttr.add(new Attribute("max"));

		ArrayList<String> labelItems = new ArrayList<String>(3);
		labelItems.add("stand");
		labelItems.add("walk");
		labelItems.add("run");

		mClassAttribute = new Attribute(Globals.label, labelItems);

		allAttr.add(mClassAttribute);

		mDataset = new Instances("dataSet", allAttr, 100000);
		mDataset.setClassIndex(mDataset.numAttributes() - 1);

		Log.v("mDataSet", mDataset.toString());

		mAsyncTask = new OnSensorChangedTask();
		mAsyncTask.execute();

		Log.v("OnStartCommand", "ServiceHasBeenStarted");

		return START_STICKY;

	}

	// Call when location is changed
	public void onLocationChanged(Location location) {

		Log.v("starting onLocationChanged", location.toString());

		if (location == null || Math.abs(location.getLatitude()) > 90
				|| Math.abs(location.getLongitude()) > 180) {
			return;
		}

		if (location.hasAccuracy()
				&& location.getAccuracy() > MINIMUM_ACCURACY_TOLERANCE) {
			return;
		}

		synchronized (mLocationList) {
			mLocationList.add(location);
		}

		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(MapTrackActivity.ACTION_LOCATION_UPDATED);
		mContext.sendBroadcast(broadcastIntent);
		Log.d("onLocationChanged", "Location update broadcast sent");

	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		Log.v("onBind", "bound");

		return binder;

	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	private class OnSensorChangedTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			Instance inst = new DenseInstance(mFeatLen);
			inst.setDataset(mDataset);
			int blockSize = 0;

			FFT fft = new FFT(Globals.ACCELEROMETER_BLOCK_CAPACITY);
			double[] accBlock = new double[Globals.ACCELEROMETER_BLOCK_CAPACITY];
			double[] re = accBlock;
			double[] im = new double[Globals.ACCELEROMETER_BLOCK_CAPACITY];

			double max = Double.MIN_VALUE;

			ArrayList<Double> featVect = new ArrayList<Double>(
					Globals.ACCELEROMETER_BLOCK_CAPACITY);

			while (true) {
				try {

					// dump the buffer
					accBlock[blockSize++] = mAccBuffer.take().doubleValue();

					if (blockSize == Globals.ACCELEROMETER_BLOCK_CAPACITY) {
						blockSize = 0;
						max = .0;
						for (double val : accBlock) {
							if (max < val) {
								max = val;
							}
						}

						fft.fft(re, im);

						for (int i = 0; i < re.length; i++) {
							double mag = Math.sqrt(re[i] * re[i] + im[i] * im[i]);
							featVect.add(new Double(mag));
							im[i] = .0;
						}

						featVect.add(new Double(max));
						int classifiedValue = (int) Classifier.classify(featVect.toArray());

						mActivityClassificationBroadcast.putExtra(
								Globals.classifiedActivity, classifiedValue);

						Log.v(Integer.toString(classifiedValue), "received");

						mContext.sendBroadcast(mActivityClassificationBroadcast);
						featVect.clear();
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		protected void onCancelled() {

			super.onCancelled();

		}

	}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			double m = Math.sqrt(event.values[0] * event.values[0] + event.values[1]
					* event.values[1] + event.values[2] * event.values[2]);

			Log.v("SensorChanged", "Sense Changed");

			// Insert element into queue

			try {
				mAccBuffer.add(new Double(m));
			}
			catch (IllegalStateException e) {
				// Hit capacity, dynamically create more space
				ArrayBlockingQueue<Double> newBuf = new ArrayBlockingQueue<Double>(
						mAccBuffer.size() * 2);

				Log.v("IllegalStateException",
						"size of buffer increased to " + newBuf.size());

				mAccBuffer.drainTo(newBuf);
				mAccBuffer = newBuf;
				mAccBuffer.add(new Double(m));
			}

		}
	}

}
