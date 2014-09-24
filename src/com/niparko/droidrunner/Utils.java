package com.niparko.droidrunner;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import com.google.android.maps.GeoPoint;

public class Utils {

	public final static String MILES = "MILES";

	public static boolean getIsMetricFromPerf(Context context) {

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);


		String mDistanceMeasureDisplay = settings.getString(context
				.getString(R.string.prefKeyUnit), context.getResources()
				.getStringArray(R.array.distanceUnits)[0]);

		if (mDistanceMeasureDisplay.compareTo(MILES) == 0)
			return false;
		else
			return true;
	}

	public static byte[] fromLocationArrayToByteArray(Location[] locationArray) {

		int[] intArray = new int[locationArray.length * 2];

		for (int i = 0; i < locationArray.length; i++) {
			intArray[i * 2] = (int) (locationArray[i].getLatitude() * 1E6);
			intArray[(i * 2) + 1] = (int) (locationArray[i].getLongitude() * 1E6);
		}

		ByteBuffer byteBuffer = ByteBuffer.allocate(intArray.length * Integer.SIZE);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(intArray);

		return byteBuffer.array();
	}

	public static Location[] fromByteArrayToLocationArray(byte[] bytePointArray) {

		ByteBuffer byteBuffer = ByteBuffer.wrap(bytePointArray);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();

		int[] intArray = new int[bytePointArray.length / Integer.SIZE];
		intBuffer.get(intArray);

		Location[] locationArray = new Location[intArray.length / 2];

		assert (locationArray != null);

		for (int i = 0; i < locationArray.length; i++) {
			locationArray[i] = new Location("");
			locationArray[i].setLatitude((double) intArray[i * 2] / 1E6F);
			locationArray[i].setLongitude((double) intArray[i * 2 + 1] / 1E6F);
		}
		return locationArray;
	}

	public static GeoPoint fromLocationToGeoPoint(Location loc) {
		return new GeoPoint((int) (loc.getLatitude() * 1E6),
				(int) (loc.getLongitude() * 1E6));
	}

}
