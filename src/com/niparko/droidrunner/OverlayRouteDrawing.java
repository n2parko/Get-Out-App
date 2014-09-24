package com.niparko.droidrunner;

import java.util.ArrayList;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.Log;
import android.widget.TextView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

//Some code in this class is sampled from the CS69 Lab 5 lab report 
//and Lee's Android Application Development

public class OverlayRouteDrawing extends Overlay {

	public ArrayList<Location> mLocationList;

	private ArrayList<GeoPoint> pointList;
	private final static int GPS_LOCATION_CACHE_SIZE = 0;
	public int lastNPoints;
	private Drawable startMarker;
	private Drawable endMarker;
	private int markerWidth;
	private int markerHeight;
	private int currMarkerHeight;
	private int currMarkerWidth;
	private Paint pathPaint;
	private GeoPoint lastReferencePoint;
	private Path path;
	private Rect lastProjectionRect;
	private Rect longRect;
	public int standingCounter;
	public int walkingCounter;
	public int runningCounter;
	private Drawable currDrawing;
	private Drawable standingDrawable;
	private Drawable walkingDrawable;
	private Drawable runningDrawable;

	public OverlayRouteDrawing(Context context) {
		pointList = new ArrayList<GeoPoint>(GPS_LOCATION_CACHE_SIZE);

		final Resources resources = context.getResources();

		standingCounter = 0;
		walkingCounter = 0;
		runningCounter = 0;

		lastNPoints = 0;
		// Initialize drawables
		startMarker = resources.getDrawable(R.drawable.green_dot);
		standingDrawable = resources.getDrawable(R.drawable.red_dot);
		markerWidth = startMarker.getIntrinsicWidth();
		markerHeight = startMarker.getIntrinsicHeight();
		startMarker.setBounds(0, 0, markerWidth, markerHeight);
		standingDrawable.setBounds(0, 0, markerWidth, markerHeight);

		runningDrawable = resources.getDrawable(R.drawable.clickrun);
		walkingDrawable = resources.getDrawable(R.drawable.walk);

		currMarkerWidth = markerWidth;
		currMarkerHeight = markerHeight;

		currDrawing = standingDrawable;

		path = new Path();
		longRect = new Rect();

		// Set up paint object for path
		pathPaint = new Paint();
		pathPaint.setColor(Color.RED);
		pathPaint.setStrokeWidth(3);
		pathPaint.setStyle(Paint.Style.STROKE);
		pathPaint.setAntiAlias(true);

	}

	private TextView getViewById(int exercisetypetext) {
		// TODO Auto-generated method stub
		return null;
	}

	private void drawItemWithOffset(Canvas canvas, Projection projection,
			GeoPoint geoPoint, Drawable item, int offsetX, int offsetY) {

		Point pixelPoint = new Point();
		projection.toPixels(geoPoint, pixelPoint);
		canvas.save();
		canvas.translate(pixelPoint.x + offsetX, pixelPoint.y + offsetY);
		item.draw(canvas);
		canvas.restore();
	}

	// Ensure that the current location has not gone off the map
	// If so, zoomout and recenter
	private void checkOffMap(MapView mapView) {
		int lat = pointList.get(pointList.size() - 1).getLatitudeE6();
		int longitude = pointList.get(pointList.size() - 1).getLongitudeE6();

		if (lat < lastProjectionRect.bottom || lat > lastProjectionRect.top
				|| longitude > longRect.top || longitude < longRect.right) {

			MapController mc = mapView.getController();
			mc.setCenter(pointList.get(pointList.size() - 1));
			mc.zoomOut();

		}
	}

	// Draw the path
	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
		super.draw(canvas, mapView, shadow);

		synchronized (pointList) {

			if (pointList.size() != 0) {
				if (lastReferencePoint != pointList.get(pointList.size() - 1)
						|| !checkScreenRectHasNotChanged(mapView)) {

					// Start point
					if (lastNPoints == 0) {
						MapController mc = mapView.getController();
						mc.setCenter(pointList.get(0));
						lastReferencePoint = pointList.get(0);
						checkScreenRectHasNotChanged(mapView);
						lastNPoints++;

					}
					else if (!checkScreenRectHasNotChanged(mapView)) {
						checkOffMap(mapView);
						Log.v("path new", "new path");
					}
					else {
						checkOffMap(mapView);
						updatePath(mapView.getProjection(), lastNPoints);
					}
				}
				drawItemWithOffset(canvas, mapView.getProjection(), pointList.get(0),
						startMarker, -markerWidth / 2, -markerHeight);

				drawItemWithOffset(canvas, mapView.getProjection(),
						pointList.get(pointList.size() - 1), currDrawing,
						-markerWidth / 2, -markerHeight);
			}
		}

		canvas.drawPath(path, pathPaint);
		return true;
	}

	// Check if map view has changed
	private boolean checkScreenRectHasNotChanged(MapView mapView) {
		Rect rect = new Rect();
		GeoPoint p = mapView.getMapCenter();
		boolean flag = true;

		// Top right
		p = mapView.getProjection().fromPixels(mapView.getWidth(), 0);
		int latTR = p.getLatitudeE6();
		int longTR = p.getLongitudeE6();

		// Top left
		p = mapView.getProjection().fromPixels(0, 0);
		int latTL = p.getLatitudeE6();
		int longTL = p.getLongitudeE6();

		// Bottom right
		p = mapView.getProjection().fromPixels(mapView.getWidth(),
				mapView.getHeight());
		int latBR = p.getLatitudeE6();
		int longBR = p.getLongitudeE6();

		// bottom left
		p = mapView.getProjection().fromPixels(0, mapView.getHeight());
		int latBL = p.getLatitudeE6();
		int longBL = p.getLongitudeE6();

		// Create the rects
		rect.set(latBL, latTR, latTL, latBR);
		longRect.set(longBL, longTR, longTL, longBR);

		// if no view, set lastProjectionRect
		if (lastProjectionRect == null) {
			lastProjectionRect = rect;
			Log.v("lastProjectionRect", "wasNull");
		}
		else {
			flag = (lastProjectionRect.equals(rect));
			if (!flag) {
				// Redraw path
				lastProjectionRect = rect;
				path = new Path();
				lastReferencePoint = pointList.get(0);
				updatePath(mapView.getProjection(), 0);
			}
		}

		return flag;
	}

	// Update the path with new GPS data
	private void updatePath(Projection projection, int startIndex) {
		Point pNew = new Point();
		Point pOld = new Point();

		projection.toPixels(lastReferencePoint, pOld);
		path.moveTo(pOld.x, pOld.y);

		for (int i = startIndex; i < pointList.size(); i++) {

			projection.toPixels(pointList.get(i), pNew);
			path.lineTo(pNew.x, pNew.y);

		}

		lastNPoints = pointList.size() - 1;
		lastReferencePoint = pointList.get(lastNPoints);

	}

	// Convert a location to a geopoint
	public GeoPoint convertLocToGeo(Location loc) {
		int pLat = (int) (loc.getLatitude() * 1000000F);
		int pLong = (int) (loc.getLongitude() * 1000000F);
		GeoPoint p = new GeoPoint(pLat, pLong);
		return p;
	}

	// add a point to the list of points
	public void pointAdded(Location loc) {

		Log.v("Point added", loc.toString());

		GeoPoint p = convertLocToGeo(loc);
		pointList.add(p);

	}

	public void classifierChanged(int newClassifier) {
		if (newClassifier == 0) {
			currDrawing = standingDrawable;
			standingCounter++;
		}
		else if (newClassifier == 1) {
			currDrawing = walkingDrawable;
			walkingCounter++;
		}
		else if (newClassifier == 2) {
			currDrawing = runningDrawable;
			runningCounter++;
		}
		currMarkerHeight = runningDrawable.getIntrinsicHeight();
		currMarkerWidth = runningDrawable.getIntrinsicWidth();
		currDrawing.setBounds(0, 0, markerWidth, markerHeight);
	}

	public void startStaticOverlayRoute(ArrayList<Location> arrayList) {

		for (int i = 0; i < arrayList.size(); i++) {
			pointAdded(arrayList.get(i));
		}
	}

}
